package com.akholodok.stats.aggregator.service;

import java.time.Instant;

public interface TimeSource {

    Instant now();
}
