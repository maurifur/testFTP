package com.example.ftpbackup.sftp;

public record RemoteFile(String path, long size, boolean directory) {

    public boolean isDirectory() {
        return directory;
    }

    public String fileName() {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }
}
