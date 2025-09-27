package com.project.khoya.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-file-size:5MB}")
    private String maxFileSize;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    public String uploadImage(MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);

        // Create upload directory if it doesn't exist
        Path uploadPath = createUploadDirectory();

        // Generate unique filename
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        // Save file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File uploaded successfully: {}", fileName);

        // Return relative path that can be used as URL
        return "/uploads/" + fileName;
    }

    public boolean deleteImage(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return false;
            }

            // Extract filename from path
            String fileName = imagePath.replace("/uploads/", "");
            Path filePath = Paths.get(uploadDir, fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", fileName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Error deleting file: {}", imagePath, e);
            return false;
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getFileExtension(originalFileName).toLowerCase();
        boolean isValidExtension = false;
        for (String allowedExt : ALLOWED_EXTENSIONS) {
            if (extension.equals(allowedExt)) {
                isValidExtension = true;
                break;
            }
        }

        if (!isValidExtension) {
            throw new IllegalArgumentException("Invalid file type. Only images are allowed (jpg, jpeg, png, gif, webp)");
        }

        // Check if it's actually an image by reading content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File is not a valid image");
        }
    }

    private Path createUploadDirectory() throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Upload directory created: {}", uploadPath.toAbsolutePath());
        }
        return uploadPath;
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return "IMG_" + timestamp + "_" + uniqueId + extension;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
    }

    public String getUploadDirectory() {
        return uploadDir;
    }
}