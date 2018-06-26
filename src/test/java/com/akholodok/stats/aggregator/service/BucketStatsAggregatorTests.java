package com.akholodok.stats.aggregator.service;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.akholodok.stats.aggregator.model.Stats;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BucketStatsAggregatorTests {

    private static final int SECONDS = 60;

    private StatsAggregator aggregator;
    private TimeSource timeSource;

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

    @Test(timeout = 5_000)
    public void testConcurrentScenario() {

        //
        // Run N threads in parallel each performing K aggregator inserts rounds.
        // Each round is represented by one second and performs [1..M] inserts.
        //
        // So the total expected stats:
        // stats = {
        //      count = N * K * M
        //      min   = 1
        //      max   = M
        //      sum   = N * K * (M * (M + 1) / 2)
        // }
        //

        Instant now = Instant.now();
        when(timeSource.now()).thenReturn(now);

        int threadsCount = Math.max(Runtime.getRuntime().availableProcessors(), 2);
        int inserts = 30;
        int rounds = SECONDS;
        AtomicInteger roundsCompleted = new AtomicInteger();

        CyclicBarrier barrier = new CyclicBarrier(
            threadsCount,
            () -> when(timeSource.now()).thenReturn(now.plusSeconds(roundsCompleted.incrementAndGet())));

        List<Thread> threads = IntStream.range(0, threadsCount)
            .mapToObj(i -> new Thread(new SeedRunnable(aggregator, timeSource, barrier, rounds, inserts)))
            .peek(Thread::start)
            .collect(Collectors.toList());

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //
        when(timeSource.now()).thenReturn(now.plusSeconds(roundsCompleted.get() - 1));
        Optional<Stats> statsOpt = aggregator.getStats();
        assertTrue(statsOpt.isPresent());

        Stats stats = statsOpt.get();
        assertEquals(1.0, stats.getMin());
        assertEquals((double) inserts, stats.getMax());
        assertEquals(threadsCount * rounds * inserts, stats.getCount());
        assertEquals((double) (threadsCount * rounds * inserts * (inserts + 1) / 2), stats.getSum());
    }

    class SeedRunnable implements Runnable {

        private final StatsAggregator aggregator;
        private final TimeSource timeSource;

        private final int rounds;
        private final int inserts;

        private CyclicBarrier barrier;

        public SeedRunnable(StatsAggregator aggregator,
                            TimeSource timeSource,
                            CyclicBarrier barrier,
                            int rounds, int inserts) {
            this.aggregator = aggregator;
            this.timeSource = timeSource;
            this.barrier = barrier;
            this.rounds = rounds;
            this.inserts = inserts;
        }

        @Override
        public void run() {
            for (int round = 0; round < rounds; ++round) {
                IntStream.range(1, inserts + 1).forEach(i -> aggregator.add(timeSource.now(), i));
                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
