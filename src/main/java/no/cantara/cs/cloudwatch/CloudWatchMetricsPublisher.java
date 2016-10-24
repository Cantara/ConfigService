package no.cantara.cs.cloudwatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import no.cantara.cs.util.Configuration;

/**
 * Publishes custom metrics to Amazon CloudWatch.
 * <p>
 * Currently the only metric published is the number of heartbeats received per client.
 * <p>
 * Uses the following configuration properties:
 * <ul>
 * <li>cloudwatch.metrics.enabled - Whether Amazon CloudWatch metrics publishing is enabled.</li>
 * <li>cloudwatch.region - The AWS region to use, e.g., "eu-west-1".</li>
 * <li>cloudwatch.metrics.namespace - The namespace to use for custom CloudWatch metrics.</li>
 * <li>cloudwatch.metrics.intervalSeconds - How often to publish metrics.</li>
 * </ul>
 *
 * @author Sindre Mehus
 */
@Service
public class CloudWatchMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchMetricsPublisher.class);

    private final ConcurrentMap<String, Integer> heartbeats = new ConcurrentSkipListMap<>();
    private AmazonCloudWatch awsClient;
    private String namespace;

    public CloudWatchMetricsPublisher() {
        if (Configuration.getBoolean("cloudwatch.metrics.enabled")) {
            init();
        }
    }

    private void init() {
        String region = Configuration.getString("cloudwatch.region");
        int intervalSeconds = Configuration.getInt("cloudwatch.metrics.intervalSeconds");
        namespace = Configuration.getString("cloudwatch.metrics.namespace");

        awsClient = new AmazonCloudWatchClient().withRegion(Region.getRegion(Regions.fromName(region)));
        log.info("Created CloudWatch metrics publisher for AWS region {}, using namespace {}", region, namespace);

        // Start thread which regularly publishes metrics.
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(new Worker(), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Registers a heartbeat from the given client. Does nothing if CloudWatch metric publishing is disabled.
     * <p>
     * This method is thread-safe and non-blocking.
     */
    public void registerHeartbeat(String clientId) {
        if (awsClient != null) {
            heartbeats.merge(clientId, 1, (oldValue, value) -> oldValue + value);
        }
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            try {
                List<MetricDatum> metricData = new ArrayList<>();

                Date now = new Date();
                metricData.addAll(heartbeats.entrySet().stream().map(entry -> new MetricDatum().withMetricName("Heartbeats")
                                                                                               .withDimensions(new Dimension().withName("Client").withValue(entry.getKey()))
                                                                                               .withTimestamp(now)
                                                                                               .withUnit(StandardUnit.Count)
                                                                                               .withValue(entry.getValue().doubleValue())).collect(Collectors.toList()));
                heartbeats.clear();

                if (!metricData.isEmpty()) {
                    awsClient.putMetricData(new PutMetricDataRequest().withNamespace(namespace).withMetricData(metricData));
                }

            } catch (Throwable e) {
                log.error("Failed to publish CloudWatch metrics: {}", e.toString());
            }
        }
    }
}
