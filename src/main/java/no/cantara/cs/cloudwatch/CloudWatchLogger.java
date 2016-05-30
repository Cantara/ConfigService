package no.cantara.cs.cloudwatch;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogsAsyncClient;
import com.amazonaws.services.logs.model.*;
import com.google.common.collect.Lists;
import no.cantara.cs.dto.event.EventFile;
import no.cantara.cs.dto.event.EventGroup;
import no.cantara.cs.dto.event.EventTag;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import no.cantara.cs.util.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Forwards log events from ConfigService clients to Amazon CloudWatch.
 * <p>
 * Assumes that a CloudWatch log group exists, but dynamically creates log streams within that group (one log stream
 * per client ID / tag combination).
 * <p>
 * Uses the following configuration properties:
 * <ul>
 *     <li>cloudwatch.region - The AWS region to use, e.g., "eu-west-1".</li>
 *     <li>cloudwatch.logGroup - The name of an existing CloudWatch log group.</li>
 *     <li>cloudwatch.maxBatchSize - The max number of log events to bundle per AWS call. Default value: {@link #DEFAULT_MAX_BATCH_SIZE}. Must not exceed 10,000.</li>
 *     <li>cloudwatch.internalQueueSize - The max number of log requests to buffer internally. Default value: {@link #DEFAULT_INTERNAL_QUEUE_SIZE}.</li>
 * </ul>
 *
 * @author Sindre Mehus
 */
@Service
public class CloudWatchLogger {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchLogger.class);

    private static final int DEFAULT_MAX_BATCH_SIZE = 512;
    private static final int DEFAULT_INTERNAL_QUEUE_SIZE = 1024;

    private String logGroup;
    private int maxBatchSize;
    private LinkedBlockingQueue<LogRequest> logRequestQueue;
    private AWSLogsAsyncClient awsClient;

    public CloudWatchLogger() {
        if (Configuration.getBoolean("cloudwatch.enabled")) {
            init();
        }
    }

    private void init() {
        String region = Configuration.getString("cloudwatch.region");
        logGroup = Configuration.getString("cloudwatch.logGroup");
        maxBatchSize = Configuration.getInt("cloudwatch.maxBatchSize", DEFAULT_MAX_BATCH_SIZE);
        int internalQueueSize = Configuration.getInt("cloudwatch.internalQueueSize", DEFAULT_INTERNAL_QUEUE_SIZE);
        logRequestQueue = new LinkedBlockingQueue<>(internalQueueSize);

        awsClient = new AWSLogsAsyncClient().withRegion(Region.getRegion(Regions.fromName(region)));
        log.info("Created CloudWatch logger for log group {} in AWS region {}", logGroup, region);

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
        for (EventGroup eventGroup : eventsStore.getEventGroups().values()) {
            for (EventFile eventFile : eventGroup.getFiles().values()) {
                for (Map.Entry<String, EventTag> entry : eventFile.getTags().entrySet()) {
                    result.addAll(rebatchTag(clientId, entry.getKey(), entry.getValue()));
                }
            }
        }
        return result;
    }

    private List<LogRequest> rebatchTag(String clientId, String tagName, EventTag eventTag) {
        List<LogRequest> result = new ArrayList<>();
        Date now = new Date();

        for (List<String> partition : Lists.partition(eventTag.getEvents(), maxBatchSize)) {
            LogRequest logRequest = new LogRequest(clientId + "-" + tagName);
            partition.forEach(line -> logRequest.addLogEvent(now, line));
            result.add(logRequest);
        }

        return result;
    }

    private static class LogRequest {
        private final String logStream;
        private final List<InputLogEvent> logEvents = new ArrayList<>();

        LogRequest(String logStream) {
            this.logStream = logStream;
        }

        void addLogEvent(Date time, String message) {
            if (StringUtils.isNotEmpty(message)) {
                logEvents.add(new InputLogEvent().withMessage(message).withTimestamp(time.getTime()));
            }
        }
    }

    private class Worker implements Runnable {

        private String sequenceToken;

        @Override
        public void run() {

            while (true) {
                LogRequest request = null;
                try {

                    request = logRequestQueue.take();
                    send(request);

                } catch (DataAlreadyAcceptedException e) {
                    sequenceToken = e.getExpectedSequenceToken();

                } catch (InvalidSequenceTokenException e) {
                    sequenceToken = e.getExpectedSequenceToken();
                    resend(request);

                } catch (ResourceNotFoundException e) {
                    createLogStreamAndResend(request);

                } catch (Exception e) {
                    log.warn("Failed to send log events to CloudWatch", e);
                }
            }
        }

        private void send(LogRequest request) {
            PutLogEventsResult result = awsClient.putLogEvents(new PutLogEventsRequest(logGroup, request.logStream, request.logEvents).withSequenceToken(sequenceToken));
            sequenceToken = result.getNextSequenceToken();
            log.debug("Sent {} log events to CloudWatch {}/{}", request.logEvents.size(), logGroup, request.logStream);
        }

        private void createLogStreamAndResend(LogRequest request) {
            try {
                awsClient.createLogStream(new CreateLogStreamRequest(logGroup, request.logStream));
                log.info("Created log stream {}", request.logStream);
            } catch (Exception e) {
                log.warn("Failed to create log stream " + request.logStream + " in log group " + logGroup + ". Make sure log group exists.", e);
                return;
            }
            resend(request);
        }

        private void resend(LogRequest request) {
            try {
                send(request);
            } catch (Exception e) {
                log.warn("Failed to re-send log events to log stream " + request.logStream + " in log group " + logGroup, e);
            }
        }
    }
}
