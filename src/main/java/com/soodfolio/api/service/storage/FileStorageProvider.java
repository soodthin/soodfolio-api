package com.soodfolio.api.service.storage;

public interface FileStorageProvider {
    String store(byte[] content, String filename, String contentType, String subDir);
}
