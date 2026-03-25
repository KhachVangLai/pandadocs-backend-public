package com.pandadocs.api.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class FirebaseStorageService {

    @Autowired
    private Storage storage;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    @PostConstruct
    public void init() {
        if (bucketName != null) {
            bucketName = bucketName.trim();
        }
    }

    // ==================== UPLOAD METHODS ====================

    /**
     * Upload template file (docx, pdf, pptx, etc.)
     * Path: templates/user{userId}_{timestamp}_{filename}
     */
    public String uploadTemplate(MultipartFile file, Long userId) throws IOException {
        validateFile(file, 50 * 1024 * 1024, // 50MB max
                new String[] { "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
                        "application/pdf", // .pdf
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation", // .pptx
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" // .xlsx
                });

        String fileName = generateFileName("templates", userId, file.getOriginalFilename());

        // Detect content type from file extension to ensure correct type
        String contentType = detectContentType(file.getOriginalFilename());

        return uploadFileWithContentType(file, fileName, contentType, false); // private file
    }

    /**
     * Upload user avatar (jpg, png)
     * Path: avatars/user{userId}.jpg
     * Auto resize to 300x300px
     */
    public String uploadAvatar(MultipartFile file, Long userId) throws IOException {
        validateFile(file, 5 * 1024 * 1024, // 5MB max
                new String[] { "image/jpeg", "image/png", "image/jpg" });

        // Resize image to 300x300
        byte[] resizedImage = resizeImage(file.getInputStream(), 300, 300);

        String fileName = "avatars/user" + userId + ".jpg";
        return uploadFile(resizedImage, fileName, "image/jpeg", true); // public file
    }

    /**
     * Upload template preview images (jpg, png)
     * Path: previews/template{templateId}_{index}.jpg
     * Auto resize to 1200x800px
     */
    public List<String> uploadPreviewImages(List<MultipartFile> files, Long templateId) throws IOException {
        if (files.size() > 5) {
            throw new IllegalArgumentException("Maximum 5 preview images allowed");
        }

        List<String> urls = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            validateFile(file, 5 * 1024 * 1024, // 5MB max
                    new String[] { "image/jpeg", "image/png", "image/jpg" });

            // Resize to 1200x800
            byte[] resizedImage = resizeImage(file.getInputStream(), 1200, 800);

            String fileName = "previews/template" + templateId + "_" + (i + 1) + ".jpg";
            String url = uploadFile(resizedImage, fileName, "image/jpeg", true); // public file
            urls.add(url);
        }
        return urls;
    }

    // ==================== DOWNLOAD METHODS ====================

    /**
     * Generate signed URL for private files (templates)
     * URL expires in 1 hour
     */
    public String generateSignedUrl(String fileUrl) {
        try {
            // Extract file path from full URL
            String filePath = extractPathFromUrl(fileUrl);

            BlobId blobId = BlobId.of(bucketName, filePath);
            Blob blob = storage.get(blobId);

            if (blob == null || !blob.exists()) {
                throw new RuntimeException("File not found: " + filePath);
            }

            // Generate signed URL valid for 1 hour
            return blob.signUrl(1, TimeUnit.HOURS).toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage());
        }
    }

    /**
     * Download file as byte array
     */
    public byte[] downloadFile(String fileUrl) {
        String filePath = extractPathFromUrl(fileUrl);
        BlobId blobId = BlobId.of(bucketName, filePath);
        Blob blob = storage.get(blobId);

        if (blob == null || !blob.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        return blob.getContent();
    }

    /**
     * Get file info (name, content-type) from URL
     */
    public FileInfo getFileInfo(String fileUrl) {
        String filePath = extractPathFromUrl(fileUrl);
        BlobId blobId = BlobId.of(bucketName, filePath);
        Blob blob = storage.get(blobId);

        if (blob == null || !blob.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        // Extract original filename from path
        String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
        // Remove timestamp prefix (e.g., user123_1234567890_resume.docx -> resume.docx)
        if (filename.contains("_")) {
            String[] parts = filename.split("_", 3);
            if (parts.length == 3) {
                filename = parts[2]; // Get original filename
            }
        }

        return new FileInfo(filename, blob.getContentType());
    }

    /**
     * Inner class to hold file info
     */
    public static class FileInfo {
        private final String filename;
        private final String contentType;

        public FileInfo(String filename, String contentType) {
            this.filename = filename;
            this.contentType = contentType;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }
    }

    // ==================== DELETE METHODS ====================

    /**
     * Delete file from Firebase Storage
     */
    public void deleteFile(String fileUrl) {
        String filePath = extractPathFromUrl(fileUrl);
        BlobId blobId = BlobId.of(bucketName, filePath);
        boolean deleted = storage.delete(blobId);

        if (!deleted) {
            throw new RuntimeException("Failed to delete file: " + filePath);
        }
    }

    /**
     * Delete old avatar before uploading new one
     */
    public void deleteOldAvatar(Long userId) {
        String fileName = "avatars/user" + userId + ".jpg";
        BlobId blobId = BlobId.of(bucketName, fileName);
        storage.delete(blobId); // Ignore if not exists
    }

    /**
     * Delete all preview images for a template
     */
    public void deletePreviewImages(Long templateId) {
        for (int i = 1; i <= 5; i++) {
            String fileName = "previews/template" + templateId + "_" + i + ".jpg";
            BlobId blobId = BlobId.of(bucketName, fileName);
            storage.delete(blobId); // Ignore if not exists
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Detect content type from file extension
     */
    private String detectContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String lowerFilename = filename.toLowerCase();

        if (lowerFilename.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerFilename.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerFilename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFilename.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lowerFilename.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerFilename.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerFilename.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerFilename.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * Upload file to Firebase Storage with explicit content type
     */
    private String uploadFileWithContentType(MultipartFile file, String fileName, String contentType, boolean isPublic) throws IOException {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(blobId)
                .setContentType(contentType);

        // Set metadata for public access
        if (isPublic) {
            blobInfoBuilder.setContentDisposition("inline");
        }

        BlobInfo blobInfo = blobInfoBuilder.build();

        // Upload
        storage.create(blobInfo, file.getBytes());

        // Return public URL
        return getPublicUrl(fileName);
    }

    /**
     * Upload file to Firebase Storage
     */
    private String uploadFile(MultipartFile file, String fileName, boolean isPublic) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = detectContentType(file.getOriginalFilename());
        }
        return uploadFileWithContentType(file, fileName, contentType, isPublic);
    }

    /**
     * Upload byte array to Firebase Storage
     */
    private String uploadFile(byte[] data, String fileName, String contentType, boolean isPublic) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(blobId)
                .setContentType(contentType);

        if (isPublic) {
            blobInfoBuilder.setContentDisposition("inline");
        }

        BlobInfo blobInfo = blobInfoBuilder.build();

        // Upload
        storage.create(blobInfo, data);

        // Return public URL
        return getPublicUrl(fileName);
    }

    /**
     * Generate public URL for file
     */
    private String getPublicUrl(String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName, encodedFileName);
    }

    /**
     * Extract file path from full Firebase URL
     */
    private String extractPathFromUrl(String fileUrl) {
        try {
            // URL format: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{path}?alt=media
            String[] parts = fileUrl.split("/o/");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid Firebase Storage URL");
            }
            String pathWithParams = parts[1];
            String path = pathWithParams.split("\\?")[0];
            return java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract path from URL: " + e.getMessage());
        }
    }

    /**
     * Generate unique file name
     */
    private String generateFileName(String folder, Long userId, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitizedFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return folder + "/user" + userId + "_" + timestamp + "_" + sanitizedFilename;
    }

    /**
     * Validate file size and type
     */
    private void validateFile(MultipartFile file, long maxSize, String[] allowedTypes) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed: " + (maxSize / 1024 / 1024) + "MB");
        }

        boolean validType = false;
        for (String type : allowedTypes) {
            if (type.equals(file.getContentType())) {
                validType = true;
                break;
            }
        }

        if (!validType) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: " + String.join(", ", allowedTypes));
        }
    }

    /**
     * Resize image maintaining aspect ratio
     */
    private byte[] resizeImage(InputStream inputStream, int maxWidth, int maxHeight) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);

        if (originalImage == null) {
            throw new IOException("Failed to read image");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(originalImage)
                .size(maxWidth, maxHeight)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
}
