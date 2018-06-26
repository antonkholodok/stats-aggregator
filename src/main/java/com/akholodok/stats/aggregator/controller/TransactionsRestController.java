package com.akholodok.stats.aggregator.controller;

import com.akholodok.stats.aggregator.service.StatsAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import javax.validation.Valid;

@RestController(TransactionsRestController.PATH)
public class TransactionsRestController {

    public static final String PATH = "transactions";

    private final StatsAggregator statsAggregator;

    @Autowired
    public TransactionsRestController(StatsAggregator statsAggregator) {
        this.statsAggregator = statsAggregator;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> add(@Valid @RequestBody AddTransactionRequest transaction) {
        boolean added = statsAggregator.add(
            Instant.ofEpochMilli(transaction.getTimestamp()),
            transaction.getAmount());
        return added
            ? ResponseEntity.ok().build()
            : ResponseEntity.noContent().build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get() {
        return statsAggregator.getStats()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
