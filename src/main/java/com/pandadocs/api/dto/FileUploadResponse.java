package com.pandadocs.api.dto;

import java.util.List;

public class FileUploadResponse {
    private String fileUrl;
    private List<String> fileUrls;
    private String message;
    private long fileSize;
    private String fileName;

    public FileUploadResponse() {
    }

    public FileUploadResponse(String fileUrl, String message) {
        this.fileUrl = fileUrl;
        this.message = message;
    }

    public FileUploadResponse(List<String> fileUrls, String message) {
        this.fileUrls = fileUrls;
        this.message = message;
    }

    // Getters and Setters
    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public List<String> getFileUrls() {
        return fileUrls;
    }

    public void setFileUrls(List<String> fileUrls) {
        this.fileUrls = fileUrls;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
