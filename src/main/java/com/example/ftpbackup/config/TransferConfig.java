package com.example.ftpbackup.config;

import com.example.ftpbackup.sftp.RemoteFile;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public record TransferConfig(
        String host,
        int port,
        String username,
        Optional<String> password,
        Optional<Path> privateKey,
        Optional<String> passphrase,
        Optional<Path> knownHosts,
        String remoteDirectory,
        String bucket,
        Optional<String> bucketPrefix,
        boolean deleteAfterUpload,
        Optional<Region> region
) {

    public static TransferConfig fromPropertiesFile(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Properties properties = new Properties();
            properties.load(in);
            return fromProperties(properties);
        }
    }

    public static TransferConfig fromProperties(Properties properties) {
        String host = require(properties, "sftp.host");
        int port = Integer.parseInt(properties.getProperty("sftp.port", "22"));
        String username = require(properties, "sftp.username");
        Optional<String> password = Optional.ofNullable(properties.getProperty("sftp.password"));
        Optional<Path> privateKey = Optional.ofNullable(properties.getProperty("sftp.privateKeyPath"))
                .filter(s -> !s.isBlank())
                .map(Path::of);
        Optional<String> passphrase = Optional.ofNullable(properties.getProperty("sftp.passphrase"));
        Optional<Path> knownHosts = Optional.ofNullable(properties.getProperty("sftp.knownHosts"))
                .filter(s -> !s.isBlank())
                .map(Path::of);
        String remoteDirectory = require(properties, "sftp.remoteDirectory");
        String bucket = require(properties, "s3.bucket");
        Optional<String> prefix = Optional.ofNullable(properties.getProperty("s3.prefix"))
                .filter(s -> !s.isBlank());
        boolean deleteAfterUpload = Boolean.parseBoolean(properties.getProperty("transfer.deleteAfterUpload", "false"));
        Optional<Region> region = Optional.ofNullable(properties.getProperty("s3.region"))
                .filter(s -> !s.isBlank())
                .map(Region::of);

        if (password.isEmpty() && privateKey.isEmpty()) {
            throw new IllegalArgumentException("Se debe configurar password o privateKeyPath para el SFTP");
        }

        return new TransferConfig(
                host,
                port,
                username,
                password,
                privateKey,
                passphrase,
                knownHosts,
                remoteDirectory,
                bucket,
                prefix,
                deleteAfterUpload,
                region
        );
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta la propiedad requerida: " + key);
        }
        return value;
    }

    public String resolveS3Key(RemoteFile file) {
        String fileName = file.fileName();
        return bucketPrefix
                .map(prefix -> prefix.endsWith("/") ? prefix + fileName : prefix + "/" + fileName)
                .orElse(fileName);
    }
}
