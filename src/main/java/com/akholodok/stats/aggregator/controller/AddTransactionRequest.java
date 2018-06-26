package com.akholodok.stats.aggregator.controller;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AddTransactionRequest {

    private Double amount;
    private Long timestamp;

    public AddTransactionRequest() {

    }

    public AddTransactionRequest(Double amount, Long timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    @NotNull(message = "'amount' should be provided")
    public Double getAmount() {
        return amount;
    }

    @NotNull(message = "'timestamp' should be provided")
    @Min(value = 0, message = "'timestamp' should be greater than 0")
    public Long getTimestamp() {
        return timestamp;
    }
}
