package com.danasea.backend.modules.service.application.ports;

public interface FileStoragePort {

    /**
     * Uploads file data to remote storage.
     *
     * @param fileData the raw byte array of the file
     * @param originalFilename the original filename
     * @param folder the target storage folder/path
     * @return secure URL of the uploaded file
     */
    String uploadFile(byte[] fileData, String originalFilename, String folder);

    /**
     * Deletes a file from remote storage by its URL or identifier.
     *
     * @param fileUrl the URL or public ID of the file to delete
     */
    void deleteFile(String fileUrl);
}
