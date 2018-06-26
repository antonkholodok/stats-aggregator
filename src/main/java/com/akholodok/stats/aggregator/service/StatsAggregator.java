package com.akholodok.stats.aggregator.service;

import com.akholodok.stats.aggregator.model.Stats;

import java.time.Instant;
import java.util.Optional;

/**
 * Aggregation service that consumes {@code [timestamp:value]} pairs
 * and aggregates them into {@link Stats} for the last
 * {@link StatsAggregator#getDuration()} seconds.
 */
public interface StatsAggregator {

    /**
     * Adds new {@code [timestamp:value]} pair.
     *
     * @param timestamp Milliseconds since Unix Epoch.
     * @param value Value to add.
     * @return {@code true} if value added, otherwise {@code false} if {@code timestamp}
     * is obsolete (more than {@link StatsAggregator#getDuration()} seconds already has
     * passed since @{@code timestamp}).
     */
    boolean add(Instant timestamp, double value);

    /**
     * Get aggregated view for the last {@link StatsAggregator#getDuration()} seconds.
     *
     * @return {@code Optional.of(stats)} in case if there any data received
     * for the last {@link StatsAggregator#getDuration()} seconds.
     * Otherwise return {@code Optional.empty()}
     */
    Optional<Stats> getStats();

    /**
     * Get aggregation window duration in seconds.
     *
     * @return Aggregation window duration in seconds.
     */
    int getDuration();

    default boolean isValidTimestamp(Instant now, Instant timestamp) {
        return (now.getEpochSecond() - timestamp.getEpochSecond() < getDuration()) && !timestamp.isAfter(now);
    }
}
