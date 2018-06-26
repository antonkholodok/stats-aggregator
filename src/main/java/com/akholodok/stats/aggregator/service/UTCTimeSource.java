package com.akholodok.stats.aggregator.service;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UTCTimeSource implements TimeSource {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
