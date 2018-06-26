package com.akholodok.stats.aggregator.service;

import com.akholodok.stats.aggregator.model.Stats;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;

/**
 * Statistics aggregator which is based on bucketing.
 * Each bucket represents aggregated statistics for specific second.
 * <p>
 * <b>Complexity:</b>
 * <p>
 * For duration K, where 1 < K << N:
 * {@link BucketStatsAggregator#add(Instant, double)} - complexity if O(1);
 * {@link BucketStatsAggregator#getStats()} (Instant, double)} - complexity if O(1);
 * memory consumption - O(1)
 * <p>
 * For duration K, where K == N, prefer to use another implementation, since:
 * {@link BucketStatsAggregator#add(Instant, double)} - complexity if O(N);
 * {@link BucketStatsAggregator#getStats()} (Instant, double)} - complexity if O(N);
 * memory consumption - O(N)
 */
public class BucketStatsAggregator implements StatsAggregator {

    public static final BinaryOperator<Bucket> DEFAULT_BUCKET_REDUCER =
        (b1, b2) -> new Bucket(
            b1.epochSeconds,
            b1.count + b2.count,
            Math.min(b1.min, b2.min),
            Math.max(b1.max, b2.max),
            b1.sum + b2.sum);

    private final AtomicReferenceArray<Bucket> buckets;
    private final TimeSource timeSource;
    private final BinaryOperator<Bucket> bucketReducer;

    public BucketStatsAggregator(int duration,
                                 TimeSource timeSource,
                                 BinaryOperator<Bucket> bucketReducer) {
        this.buckets = new AtomicReferenceArray<>(duration);
        this.timeSource = timeSource;
        this.bucketReducer = bucketReducer;
    }

    public BucketStatsAggregator(int duration,
                                 TimeSource timeSource) {
        this(duration, timeSource, DEFAULT_BUCKET_REDUCER);
    }

    static class Bucket {

        // number of seconds since unix epoch
        private final long epochSeconds;

        private final long count;
        private final double min;
        private final double max;
        private final double sum;

        Bucket(long epochSeconds, long count, double min, double max, double sum) {
            this.count = count;
            this.min = min;
            this.max = max;
            this.sum = sum;
            this.epochSeconds = epochSeconds;
        }

        Bucket(double value, long epochSeconds) {
            this(epochSeconds, 1, value, value, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Bucket bucket = (Bucket) o;
            return epochSeconds == bucket.epochSeconds &&
                count == bucket.count &&
                Double.compare(bucket.min, min) == 0 &&
                Double.compare(bucket.max, max) == 0 &&
                Double.compare(bucket.sum, sum) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(epochSeconds, count, min, max, sum);
        }
    }

    @Override
    public boolean add(Instant timestamp, double value) {

        Bucket oldBucket;
        Bucket newBucket;
        int index;

        do {
            if (!isValidTimestamp(timeSource.now(), timestamp)) {
                return false;
            }

            long statEpochSeconds = timestamp.getEpochSecond();
            index = (int) (statEpochSeconds % buckets.length());
            oldBucket = buckets.get(index);

            newBucket = new Bucket(value, statEpochSeconds);
            // if oldBucket has no value yet or it is associated with timestamp
            // which is older than aggregation duration window - just insert newBucket
            // otherwise - merge old and new
            if (oldBucket != null && statEpochSeconds == oldBucket.epochSeconds) {
                newBucket = bucketReducer.apply(oldBucket, newBucket);
            }

        } while (!buckets.compareAndSet(index, oldBucket, newBucket));

        return true;
    }

    @Override
    public Optional<Stats> getStats() {

        long end = timeSource.now().getEpochSecond() + 1;
        long start = end - getDuration();

        return IntStream.range(0, buckets.length())
            .mapToObj(buckets::get)
            .filter(Objects::nonNull)
            .filter(bucket -> bucket.epochSeconds >= start && bucket.epochSeconds < end)
            .reduce(bucketReducer)
            .map(bucket -> new Stats(bucket.count, bucket.min, bucket.max, bucket.sum));
    }

    @Override
    public int getDuration() {
        return buckets.length();
    }
}
