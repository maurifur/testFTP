package com.example.ftpbackup;

import com.example.ftpbackup.config.TransferConfig;
import com.example.ftpbackup.metrics.LoggingMetricsReporter;
import com.example.ftpbackup.metrics.MetricsReporter;
import com.example.ftpbackup.metrics.TransferStatistics;
import com.example.ftpbackup.s3.S3Uploader;
import com.example.ftpbackup.sftp.RemoteFile;
import com.example.ftpbackup.sftp.SftpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private App() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("Debe proveer la ruta de un archivo de configuración con --config=<path>");
            System.exit(1);
        }

        Path configPath = parseConfigArgument(args[0]);
        TransferConfig config;
        try {
            config = TransferConfig.fromPropertiesFile(configPath);
        } catch (IOException e) {
            LOGGER.error("No se pudo cargar la configuración desde {}", configPath, e);
            System.exit(1);
            return;
        }

        MetricsReporter metricsReporter = new LoggingMetricsReporter();
        TransferStatistics statistics = new TransferStatistics(Instant.now());

        try (SftpClient sftpClient = SftpClient.connect(config);
             S3Uploader s3Uploader = new S3Uploader(config)) {

            List<RemoteFile> remoteFiles = sftpClient.listFiles(config.remoteDirectory());
            LOGGER.info("Se encontraron {} archivos en {}", remoteFiles.size(), config.remoteDirectory());

            for (RemoteFile remoteFile : remoteFiles) {
                if (remoteFile.isDirectory()) {
                    LOGGER.debug("Omitiendo directorio {}", remoteFile.path());
                    continue;
                }

                LOGGER.info("Procesando archivo {}", remoteFile.path());
                statistics.incrementDiscovered();
                Instant start = Instant.now();
                try (var inputStream = sftpClient.open(remoteFile.path())) {
                    String key = config.resolveS3Key(remoteFile);
                    s3Uploader.upload(remoteFile, key, inputStream);
                    statistics.incrementUploaded();
                    metricsReporter.recordSuccess(remoteFile, key, start);

                    if (config.deleteAfterUpload()) {
                        sftpClient.delete(remoteFile.path());
                        LOGGER.info("Archivo {} eliminado del SFTP tras copia exitosa", remoteFile.path());
                    }
                } catch (Exception ex) {
                    statistics.incrementFailed();
                    metricsReporter.recordFailure(remoteFile, start, ex);
                    LOGGER.error("Fallo al procesar {}: {}", remoteFile.path(), ex.getMessage(), ex);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error general durante la transferencia", e);
            metricsReporter.recordFatal(e);
            statistics.markFinished(Instant.now());
            System.exit(1);
            return;
        }

        statistics.markFinished(Instant.now());
        metricsReporter.finish(statistics);

        if (statistics.failedTransfers() > 0) {
            LOGGER.error("Proceso finalizado con errores: {} fallidos de {} archivos", statistics.failedTransfers(), statistics.totalDiscovered());
            System.exit(2);
        } else {
            LOGGER.info("Proceso finalizado satisfactoriamente: {} archivos transferidos", statistics.successfulTransfers());
        }
    }

    private static Path parseConfigArgument(String arg) {
        if (!arg.startsWith("--config=")) {
            LOGGER.error("Argumento inválido. Utilizar --config=<ruta>");
            System.exit(1);
        }
        String path = arg.substring("--config=".length());
        if (path.isBlank()) {
            LOGGER.error("La ruta del archivo de configuración no puede estar vacía");
            System.exit(1);
        }
        return Path.of(path);
    }
}
