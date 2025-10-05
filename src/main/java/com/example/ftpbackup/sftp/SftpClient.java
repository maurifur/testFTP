package com.example.ftpbackup.sftp;

import com.example.ftpbackup.config.TransferConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class SftpClient implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpClient.class);

    private final Session session;
    private final ChannelSftp channel;

    private SftpClient(Session session, ChannelSftp channel) {
        this.session = session;
        this.channel = channel;
    }

    public static SftpClient connect(TransferConfig config) throws JSchException {
        JSch jsch = new JSch();
        config.knownHosts().ifPresent(path -> {
            try {
                jsch.setKnownHosts(path.toString());
            } catch (JSchException e) {
                throw new IllegalStateException("No se pudo configurar known_hosts", e);
            }
        });

        config.privateKey().ifPresent(keyPath -> {
            try {
                if (config.passphrase().isPresent()) {
                    jsch.addIdentity(keyPath.toString(), config.passphrase().get());
                } else {
                    jsch.addIdentity(keyPath.toString());
                }
            } catch (JSchException e) {
                throw new IllegalStateException("No se pudo cargar la llave privada", e);
            }
        });

        Session session = jsch.getSession(config.username(), config.host(), config.port());
        config.password().ifPresent(session::setPassword);
        session.setConfig("PreferredAuthentications", "publickey,password");
        session.setConfig("StrictHostKeyChecking", config.knownHosts().isPresent() ? "yes" : "no");
        session.connect();

        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        LOGGER.info("Conectado a SFTP {}:{}", config.host(), config.port());
        return new SftpClient(session, channel);
    }

    public List<RemoteFile> listFiles(String remoteDirectory) throws SftpException {
        List<RemoteFile> files = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<ChannelSftp.LsEntry> entries = channel.ls(remoteDirectory);
        for (ChannelSftp.LsEntry entry : entries) {
            if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                continue;
            }
            boolean isDir = entry.getAttrs().isDir();
            String fullPath = remoteDirectory.endsWith("/")
                    ? remoteDirectory + entry.getFilename()
                    : remoteDirectory + "/" + entry.getFilename();
            files.add(new RemoteFile(fullPath, entry.getAttrs().getSize(), isDir));
        }
        return files;
    }

    public InputStream open(String remotePath) throws SftpException {
        return channel.get(remotePath);
    }

    public void delete(String remotePath) throws SftpException {
        channel.rm(remotePath);
    }

    @Override
    public void close() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        LOGGER.info("Conexión SFTP cerrada");
    }
}
