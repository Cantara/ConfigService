package no.cantara.cs.cloudwatch;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogsAsyncClient;
import com.amazonaws.services.logs.model.*;
import no.cantara.cs.config.ConstrettoConfig;
import no.cantara.cs.dto.event.EventFile;
import no.cantara.cs.dto.event.EventGroup;
import no.cantara.cs.dto.event.EventTag;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Forwards log events from ConfigService clients to Amazon CloudWatch.
 * <p>
 * Assumes that the CloudWatch log groups exist, but dynamically creates log streams within the groups (one log stream
 * per client ID / tag combination).
 * <p>
 * Uses the following configuration properties:
 * <ul>
 * <li>cloudwatch.logging.enabled - Whether CloudWatch logging is enabled.</li>
 * <li>cloudwatch.region - The AWS region to use, e.g., "eu-west-1".</li>
 * <li>cloudwatch.logging.maxBatchSize - The max number of log events to bundle per AWS call. Default value: {@link #DEFAULT_MAX_BATCH_SIZE}. Must not exceed 10,000.</li>
 * <li>cloudwatch.logging.internalQueueSize - The max number of log requests to buffer internally. Default value: {@link #DEFAULT_INTERNAL_QUEUE_SIZE}.</li>
 * </ul>
 *
 * @author Sindre Mehus
 */
@Service
public class CloudWatchLogger {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchLogger.class);

    private static final int DEFAULT_MAX_BATCH_SIZE = 512;
    private static final int DEFAULT_INTERNAL_QUEUE_SIZE = 1024;

    private int maxBatchSize;
    private LinkedBlockingQueue<LogRequest> logRequestQueue;
    private AWSLogsAsyncClient awsClient;

    public CloudWatchLogger() {
        if (ConstrettoConfig.getBoolean("cloudwatch.logging.enabled")) {
            init();
        }
    }

    private void init() {
        String region = ConstrettoConfig.getString("cloudwatch.region");
        maxBatchSize = ConstrettoConfig.getInt("cloudwatch.logging.maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
        int internalQueueSize = ConstrettoConfig.getInt("cloudwatch.logging.internalQueueSize", DEFAULT_INTERNAL_QUEUE_SIZE);
        logRequestQueue = new LinkedBlockingQueue<>(internalQueueSize);

        awsClient = new AWSLogsAsyncClient().withRegion(Region.getRegion(Regions.fromName(region)));
        log.info("Created CloudWatch logger for AWS region {}", region);

        // Start thread which consumes the log request queue.
        Thread workerThread = new Thread(new Worker());
        workerThread.setDaemon(true);
        workerThread.setName("cloudwatch-logger");
        workerThread.start();
    }

    /**
     * Logs events to Amazon CloudWatch. Does nothing if CloudWatch logging is disabled.
     * This method is thread-safe and non-blocking.
     */
    public void log(String clientId, ExtractedEventsStore eventsStore) {
        if (awsClient == null || eventsStore == null) {
            return;
        }

        List<LogRequest> logRequests = rebatch(clientId, eventsStore);
        for (LogRequest logRequest : logRequests) {
            if (!logRequestQueue.offer(logRequest)) {
                log.warn("No space available in internal queue, {} logging events from {} was discarded", logRequest.logEvents.size(), clientId);
            }
        }
    }

    private List<LogRequest> rebatch(String clientId, ExtractedEventsStore eventsStore) {
        List<LogRequest> result = new ArrayList<>();
        for (Map.Entry<String, EventGroup> eventGroupEntry : eventsStore.getEventGroups().entrySet()) {
            String groupName = eventGroupEntry.getKey();
            for (EventFile eventFile : eventGroupEntry.getValue().getFiles().values()) {
                for (Map.Entry<String, EventTag> entry : eventFile.getTags().entrySet()) {
                    result.addAll(rebatchTag(clientId, groupName, entry.getKey(), entry.getValue(), maxBatchSize));
                }
            }
        }
        return result;
    }

    static List<LogRequest> rebatchTag(String clientId, String groupName, String tagName, EventTag eventTag, int maxBatchSize) {
        List<LogRequest> result = new ArrayList<>();

        if (eventTag.getEvents().isEmpty()) {
            return result;
        }

        // AWS CloudWatch limits:
        //
        // The maximum batch size is 1,048,576 bytes. This size is calculated as the sum of all event messages in UTF-8, plus 26 bytes for each log event.
        // The maximum size for a single log event is 262,144 bytes.
        final int MAX_BATCH_SIZE_BYTES = 1_048_576;
        final int MAX_EVENT_SIZE_BYTES = 262_144;

        LogRequest logRequest = new LogRequest(groupName, clientId + "-" + tagName);
        result.add(logRequest);

        Date now = new Date();
        int batchSize = 0;
        for (String line : eventTag.getEvents()) {
            int eventSize = 26 + line.getBytes(StandardCharsets.UTF_8).length;

            // Truncate lines that are bigger than 256 KB
            if (eventSize > MAX_EVENT_SIZE_BYTES) {
                line = line.substring(0, Math.min(line.length(), 100_000)) + "...";
                eventSize = 26 + line.getBytes(StandardCharsets.UTF_8).length;
            }

            if (batchSize + eventSize > MAX_BATCH_SIZE_BYTES || logRequest.logEvents.size() == maxBatchSize) {
                logRequest = new LogRequest(groupName, clientId + "-" + tagName);
                result.add(logRequest);
                batchSize = 0;
            }
            logRequest.addLogEvent(now, line);
            batchSize += eventSize;
        }

        return result;
    }

    static class LogRequest {
        private final Destination destination;
        private final List<InputLogEvent> logEvents = new ArrayList<>();

        LogRequest(String logGroup, String logStream) {
            destination = new Destination(logGroup, logStream);
        }

        void addLogEvent(Date time, String message) {
            if (StringUtils.isNotEmpty(message)) {
                logEvents.add(new InputLogEvent().withMessage(message).withTimestamp(time.getTime()));
            }
        }

        List<InputLogEvent> getLogEvents() {
            return logEvents;
        }
    }

    private static class Destination {
        private final String logGroup;
        private final String logStream;

        Destination(String logGroup, String logStream) {
            this.logGroup = logGroup;
            this.logStream = logStream;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Destination that = (Destination) o;
            return Objects.equals(logGroup, that.logGroup) &&
                   Objects.equals(logStream, that.logStream);
        }

        @Override
        public int hashCode() {
            return Objects.hash(logGroup, logStream);
        }
    }

    private class Worker implements Runnable {

        private final Map<Destination, String> sequenceTokens = new LinkedHashMap<>();

        @Override
        public void run() {

            while (true) {
                LogRequest request = null;
                try {

                    request = logRequestQueue.take();
                    send(request);

                } catch (DataAlreadyAcceptedException e) {
                    sequenceTokens.put(request.destination, e.getExpectedSequenceToken());

                } catch (InvalidSequenceTokenException e) {
                    sequenceTokens.put(request.destination, e.getExpectedSequenceToken());
                    resend(request);

                } catch (ResourceNotFoundException e) {
                    createLogStreamAndResend(request);

                } catch (Exception e) {
                    log.warn("Failed to send log events to CloudWatch", e);
                }
            }
        }

        private void send(LogRequest request) {
            String sequenceToken = sequenceTokens.get(request.destination);
            PutLogEventsResult result = awsClient.putLogEvents(new PutLogEventsRequest(request.destination.logGroup, request.destination.logStream, request.logEvents).withSequenceToken(sequenceToken));
            sequenceTokens.put(request.destination, result.getNextSequenceToken());
            log.debug("Sent {} log events to CloudWatch {}/{}", request.logEvents.size(), request.destination.logGroup, request.destination.logStream);
        }

        private void createLogStreamAndResend(LogRequest request) {
            try {
                awsClient.createLogStream(new CreateLogStreamRequest(request.destination.logGroup, request.destination.logStream));
                sequenceTokens.remove(request.destination);
                log.info("Created log stream {}", request.destination.logStream);
            } catch (Exception e) {
                log.warn("Failed to create log stream " + request.destination.logStream + " in log group " + request.destination.logGroup + ". Make sure log group exists.", e);
                return;
            }
            resend(request);
        }

        private void resend(LogRequest request) {
            try {
                send(request);
            } catch (Exception e) {
                log.warn("Failed to re-send log events to log stream " + request.destination.logStream + " in log group " + request.destination.logGroup, e);
            }
        }
    }
}
