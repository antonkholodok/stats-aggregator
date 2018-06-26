package com.akholodok.stats.aggregator.service;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

import java.time.Instant;

public class BucketStatsAggregatorTests {

    @Test
    public void testDefaultBucketReducer() {
        long epochSeconds = Instant.now().getEpochSecond();
        assertEquals(
            new BucketStatsAggregator.Bucket(epochSeconds, 5, 1.0, 13.0, 30.0),
            BucketStatsAggregator.DEFAULT_BUCKET_REDUCER.apply(
                new BucketStatsAggregator.Bucket(epochSeconds, 2, 1.0, 10.0, 10.0),
                new BucketStatsAggregator.Bucket(epochSeconds, 3, 2.0, 13.0, 20.0)));
    }
}
