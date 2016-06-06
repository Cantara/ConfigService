package no.cantara.cs.cloudwatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.Assert.*;

/**
 * @author Sindre Mehus
 */
public class CloudWatchLoggerTest {

    @Test
    public void testBigList() throws Exception {
        List<List<String>> partitions = CloudWatchLogger.partitionList(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h"), 3);

        assertThat(partitions, hasSize(3));
        assertThat(partitions.get(0), is(Arrays.asList("a", "b", "c")));
        assertThat(partitions.get(1), is(Arrays.asList("d", "e", "f")));
        assertThat(partitions.get(2), is(Arrays.asList("g", "h")));
    }

    @Test
    public void testSmallList() throws Exception {
        List<List<String>> partitions = CloudWatchLogger.partitionList(Arrays.asList("a", "b"), 3);

        assertThat(partitions, hasSize(1));
        assertThat(partitions.get(0), is(Arrays.asList("a", "b")));
    }

    @Test
    public void testEmptyList() throws Exception {
        List<List<String>> partitions = CloudWatchLogger.partitionList(Collections.emptyList(), 3);

        assertThat(partitions, hasSize(0));
    }
}