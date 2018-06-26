package com.akholodok.stats.aggregator.conf;

import com.akholodok.stats.aggregator.service.BucketStatsAggregator;
import com.akholodok.stats.aggregator.service.StatsAggregator;
import com.akholodok.stats.aggregator.service.BaseTimeSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.akholodok.stats.aggregator")
public class AppConfiguration {

    @Bean
    public StatsAggregator bucketStatsAggregator(@Value("${stats-aggregator.bucket.seconds:60}") int seconds,
                                                 BaseTimeSource timeSource) {
        return new BucketStatsAggregator(seconds, timeSource);
    }
}
