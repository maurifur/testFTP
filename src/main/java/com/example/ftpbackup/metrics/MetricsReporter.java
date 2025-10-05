package com.example.ftpbackup.metrics;

import com.example.ftpbackup.sftp.RemoteFile;

import java.time.Instant;

public interface MetricsReporter {

    void recordSuccess(RemoteFile remoteFile, String s3Key, Instant start);

    void recordFailure(RemoteFile remoteFile, Instant start, Exception exception);

    void recordFatal(Exception exception);

    void finish(TransferStatistics statistics);
}
