package no.cantara.cs.cloudwatch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;

import com.amazonaws.services.logs.model.InputLogEvent;

import no.cantara.cs.dto.event.Event;
import no.cantara.cs.dto.event.EventTag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author Sindre Mehus
 */
public class CloudWatchLoggerTest {

    @Test
    public void testRebatchTag() throws Exception {

        EventTag eventTag = createEventTag(0, 1);
        List<CloudWatchLogger.LogRequest> logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(0));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(1, 1);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(1));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(25, 100);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(3));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(8, 131_072 - 26);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(1));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(9, 131_072 - 26);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(2));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(16, 131_072 - 26);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 100);
        assertThat(logRequests, hasSize(2));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(17, 131_072 - 26);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 100);
        assertThat(logRequests, hasSize(3));
        verifyLogRequests(logRequests, eventTag);

        eventTag = createEventTag(1, 262_144 - 26);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(1));
        verifyLogRequests(logRequests, eventTag);

        // Long line should be truncated
        eventTag = createEventTag(1, 262_144 - 26 + 1);
        logRequests = CloudWatchLogger.rebatchTag(null, null, null, eventTag, 10);
        assertThat(logRequests, hasSize(1));
        assertThat(logRequests.get(0).getLogEvents(), hasSize(1));
        assertThat(logRequests.get(0).getLogEvents().get(0).getMessage().endsWith("..."), is(true));
    }

    private void verifyLogRequests(List<CloudWatchLogger.LogRequest> logRequests, EventTag eventTag) {
        List<String> unpackedLines = new ArrayList<>();

        for (CloudWatchLogger.LogRequest logRequest : logRequests) {
            for (InputLogEvent logEvent : logRequest.getLogEvents()) {
                unpackedLines.add(logEvent.getMessage());
            }
        }
        assertThat(unpackedLines, equalTo(eventTag.getEvents()));
    }

    private EventTag createEventTag(int lineCount, int lineLength) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < lineCount; i++) {
            events.add(new Event(0, RandomStringUtils.randomAlphabetic(lineLength)));
        }

        EventTag eventTag = new EventTag();
        eventTag.addEvents(events);
        return eventTag;
    }
}