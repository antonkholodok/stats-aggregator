package com.akholodok.stats.aggregator.service;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

public class BucketStatsAggregatorStressTests {

    private static final int SECONDS = 60;
    private static final int ROUNDS = SECONDS;
    private static final int OPS_PER_ROUND = 300;
    private static final int WORKERS_COUNT = Math.max(Runtime.getRuntime().availableProcessors(), 2);

    private StatsAggregator aggregator;
    private TimeSource timeSource;

    @Before
    public void setUp() {
        timeSource = mock(TimeSource.class);
        aggregator = new BucketStatsAggregator(SECONDS, timeSource);
    }

    @Test(timeout = 5_000)
    public void testParallel() {

        //
        // Run N worker threads in parallel each performing K operations rounds.
        // Each round is represented by one second and performs M read and write operations.
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

        // setup barrier to force worker threads to wait for
        // time increment to one second
        AtomicInteger roundsCompleted = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(
            WORKERS_COUNT,
            () -> when(timeSource.now()).thenReturn(now.plusSeconds(roundsCompleted.incrementAndGet())));

        List<Thread> threads = IntStream.range(0, WORKERS_COUNT)
            .mapToObj(i -> new Thread(new WorkerRunnable(aggregator, timeSource, barrier, ROUNDS, OPS_PER_ROUND)))
            .peek(Thread::start)
            .collect(Collectors.toList());

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                fail("Working thread was interrupted");
            }
        }

        // decrement time to one second to undo last barrier action
        when(timeSource.now()).thenReturn(now.plusSeconds(roundsCompleted.get() - 1));

        Optional<Stats> statsOpt = aggregator.getStats();
        assertTrue(statsOpt.isPresent());

        Stats stats = statsOpt.get();
        assertEquals(1.0, stats.getMin());
        assertEquals((double) OPS_PER_ROUND, stats.getMax());
        assertEquals(WORKERS_COUNT * ROUNDS * OPS_PER_ROUND, stats.getCount());
        assertEquals((double) (WORKERS_COUNT * ROUNDS * OPS_PER_ROUND * (OPS_PER_ROUND + 1) / 2), stats.getSum());
    }

    static class WorkerRunnable implements Runnable {

        private final StatsAggregator aggregator;
        private final TimeSource timeSource;

        private final int rounds;
        private final int opsPerRound;

        // barrier to wait at the end of each round
        private CyclicBarrier barrier;

        public WorkerRunnable(StatsAggregator aggregator,
                              TimeSource timeSource,
                              CyclicBarrier barrier,
                              int rounds,
                              int opsPerRound) {
            this.aggregator = aggregator;
            this.timeSource = timeSource;
            this.barrier = barrier;
            this.rounds = rounds;
            this.opsPerRound = opsPerRound;
        }

        @Override
        public void run() {
            for (int round = 0; round < rounds; ++round) {
                IntStream.range(1, opsPerRound + 1).forEach(i -> {
                    aggregator.add(timeSource.now(), i);
                    aggregator.getStats();
                });
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
