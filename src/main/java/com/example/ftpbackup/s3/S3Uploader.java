package com.example.ftpbackup.s3;

import com.example.ftpbackup.config.TransferConfig;
import com.example.ftpbackup.sftp.RemoteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public final class S3Uploader implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Uploader.class);

    private final TransferConfig config;
    private final S3Client s3Client;

    public S3Uploader(TransferConfig config) {
        this.config = config;
        this.s3Client = buildClient(config);
    }

    private S3Client buildClient(TransferConfig config) {
        S3Client.Builder builder = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create());
        config.region().ifPresent(builder::region);
        return builder.build();
    }

    public void upload(RemoteFile remoteFile, String key, java.io.InputStream inputStream) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(config.bucket())
                    .key(key)
                    .contentLength(remoteFile.size())
                    .build();
            LOGGER.info("Subiendo {} a s3://{}/{}", remoteFile.path(), config.bucket(), key);
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, remoteFile.size()));
        } catch (S3Exception e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Error al subir archivo a S3", e);
        }
    }

    @Override
    public void close() {
        s3Client.close();
    }
}
