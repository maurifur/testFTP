package com.example.ftpbackup.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public final class TransferStatistics {

    private final Instant startTime;
    private Instant endTime;
    private final AtomicInteger totalDiscovered = new AtomicInteger();
    private final AtomicInteger successfulTransfers = new AtomicInteger();
    private final AtomicInteger failedTransfers = new AtomicInteger();

    public TransferStatistics(Instant startTime) {
        this.startTime = startTime;
    }

    public void incrementDiscovered() {
        totalDiscovered.incrementAndGet();
    }

    public void incrementUploaded() {
        successfulTransfers.incrementAndGet();
    }

    public void incrementFailed() {
        failedTransfers.incrementAndGet();
    }

    public void markFinished(Instant endTime) {
        this.endTime = endTime;
    }

    public int totalDiscovered() {
        return totalDiscovered.get();
    }

    public int successfulTransfers() {
        return successfulTransfers.get();
    }

    public int failedTransfers() {
        return failedTransfers.get();
    }

    public Instant startTime() {
        return startTime;
    }

    public Instant endTime() {
        return endTime;
    }

    public Duration duration() {
        Instant finished = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, finished);
    }
}
