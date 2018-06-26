package com.akholodok.stats.aggregator.service;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.akholodok.stats.aggregator.model.Stats;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class BucketStatsAggregatorTests {

    private static final int SECONDS = 60;

    private TimeSource timeSource;
    private StatsAggregator aggregator;

    @Before
    public void setUp() {
        timeSource = mock(TimeSource.class);
        aggregator = new BucketStatsAggregator(SECONDS, timeSource);
    }

    @Test
    public void testDefaultBucketReducer() {
        long epochSeconds = Instant.now().getEpochSecond();
        assertEquals(
            new BucketStatsAggregator.Bucket(epochSeconds, 5, 1.0, 13.0, 30.0),
            BucketStatsAggregator.DEFAULT_BUCKET_REDUCER.apply(
                new BucketStatsAggregator.Bucket(epochSeconds, 2, 1.0, 10.0, 10.0),
                new BucketStatsAggregator.Bucket(epochSeconds, 3, 2.0, 13.0, 20.0)));
    }

    @Test
    public void testAddObsoleteTimestamp() {
        Instant now = Instant.now();
        when(timeSource.now()).thenReturn(now);
        Instant timestamp = now.minusSeconds(SECONDS - 1).truncatedTo(ChronoUnit.SECONDS).minusMillis(1L);
        assertFalse(aggregator.add(timestamp, 10.0));
    }

    @Test
    public void testAddTimestampInFuture() {
        Instant now = Instant.now();
        when(timeSource.now()).thenReturn(now);
        Instant timestamp = now.plusMillis(1L);
        assertFalse(aggregator.add(timestamp, 10.0));
    }

    @Test
    public void testAddTimestampIsNow() {
        Instant now = Instant.now();
        when(timeSource.now()).thenReturn(now);
        assertTrue(aggregator.add(now, 10.0));

        when(timeSource.now()).thenReturn(now.plusMillis(1L));
        Optional<Stats> statsOpt = aggregator.getStats();
        assertTrue(statsOpt.isPresent());

        Stats stats = statsOpt.get();
        assertEquals(1L, stats.getCount());
        assertEquals(10.0, stats.getMin());
        assertEquals(10.0, stats.getMax());
        assertEquals(10.0, stats.getSum());
        assertEquals(10.0, stats.getAvg());
    }

    @Test
    public void testGetStatsEmpty() {
        when(timeSource.now()).thenReturn(Instant.now());
        assertFalse(aggregator.getStats().isPresent());
    }

    @Test
    public void testGetStatsObsolete() {
        Instant now = Instant.now();
        when(timeSource.now()).thenReturn(now);
        assertTrue(aggregator.add(now, 10.0));

        when(timeSource.now()).thenReturn(now.plusSeconds(SECONDS));
        Optional<Stats> statsOpt = aggregator.getStats();
        assertFalse(statsOpt.isPresent());
    }
}
