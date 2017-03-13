package no.cantara.cs.cloudwatch;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CloudWatchMetricsPublisherTest {

    @Test
    public void testPartitionList() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n");

        List<List<String>> chunkedList = CloudWatchMetricsPublisher.partitionList(list, 4);

        assertEquals(chunkedList.size(), 4);
        assertEquals(chunkedList.get(0), Arrays.asList("a", "b", "c", "d"));
        assertEquals(chunkedList.get(1), Arrays.asList("e", "f", "g", "h"));
        assertEquals(chunkedList.get(2), Arrays.asList("i", "j", "k", "l"));
        assertEquals(chunkedList.get(3), Arrays.asList("m", "n"));

        List<String> emptyList = new ArrayList<>();
        assertTrue(CloudWatchMetricsPublisher.partitionList(emptyList, 3).isEmpty());
    }
}
