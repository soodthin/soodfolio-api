package com.soodfolio.api.service.storage;

public interface FileStorageProvider {

    /**
     * Stores file content and returns access URL.
     *
     * @param content     Raw file bytes
     * @param subDir      Subdirectory or category (e.g. "images", "resumes")
     * @param fileName    Target filename (e.g. "uuid.png", "uuid.pdf")
     * @param contentType MIME type (e.g. "image/png", "application/pdf")
     * @param isImage     True if the file is an image, false for raw/PDF
     * @return Public or relative URL to the stored resource
     */
    String store(byte[] content, String subDir, String fileName, String contentType, boolean isImage);
}
