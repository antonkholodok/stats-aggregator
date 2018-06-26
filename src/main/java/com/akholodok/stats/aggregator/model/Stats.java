package com.akholodok.stats.aggregator.model;

public class Stats {

    private final long count;
    private final double min;
    private final double max;
    private final double sum;
    private final double avg;

    public Stats(long count, double min, double max, double sum) {
        this.count = count;
        this.min = min;
        this.max = max;
        this.sum = sum;
        this.avg = sum / count;
    }

    public long getCount() {
        return count;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getSum() {
        return sum;
    }

    public double getAvg() {
        return avg;
    }
}
