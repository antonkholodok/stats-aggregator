package com.akholodok.stats.aggregator.service;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BaseTimeSource implements TimeSource {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
