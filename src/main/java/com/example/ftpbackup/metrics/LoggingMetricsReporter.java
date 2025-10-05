package com.example.ftpbackup.metrics;

import com.example.ftpbackup.sftp.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public final class LoggingMetricsReporter implements MetricsReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMetricsReporter.class);

    @Override
    public void recordSuccess(RemoteFile remoteFile, String s3Key, Instant start) {
        Duration duration = Duration.between(start, Instant.now());
        LOGGER.info("Archivo {} transferido a {} en {} ms", remoteFile.path(), s3Key, duration.toMillis());
    }

    @Override
    public void recordFailure(RemoteFile remoteFile, Instant start, Exception exception) {
        Duration duration = Duration.between(start, Instant.now());
        LOGGER.error("Error al transferir {} después de {} ms: {}", remoteFile.path(), duration.toMillis(), exception.getMessage());
    }

    @Override
    public void recordFatal(Exception exception) {
        LOGGER.error("Error fatal en la ejecución del proceso", exception);
    }

    @Override
    public void finish(TransferStatistics statistics) {
        LOGGER.info("Resumen de ejecución: total={}, exitosos={}, fallidos={}, duración={} ms",
                statistics.totalDiscovered(),
                statistics.successfulTransfers(),
                statistics.failedTransfers(),
                statistics.duration().toMillis());
    }
}
