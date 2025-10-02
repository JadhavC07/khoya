package com.project.khoya.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"};

    public CloudinaryService() {
        Dotenv dotenv = Dotenv.load();
        this.cloudinary = new Cloudinary(dotenv.get("CLOUDINARY_URL"));
        cloudinary.config.secure = true;
        log.info("Cloudinary configured for cloud: {}", cloudinary.config.cloudName);
    }

    public String uploadImage(MultipartFile file) throws IOException {
        validateFile(file);

        try {
            // Generate unique public ID for the image
            String publicId = "khoya/alerts/" + UUID.randomUUID().toString();

            // Upload parameters
            Map uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "khoya/alerts",
                    "resource_type", "image",
                    "overwrite", false,
                    "quality", "auto:good",
                    "fetch_format", "auto"

            );

            // Upload the file
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully to Cloudinary: {}", secureUrl);

            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new IOException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    public boolean deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return false;
            }

            // Extract public_id from Cloudinary URL
            String publicId = extractPublicId(imageUrl);
            if (publicId == null) {
                log.warn("Could not extract public ID from URL: {}", imageUrl);
                return false;
            }

            // Delete from Cloudinary
            Map deleteParams = ObjectUtils.asMap("resource_type", "image");
            Map result = cloudinary.uploader().destroy(publicId, deleteParams);

            String resultStatus = (String) result.get("result");
            boolean success = "ok".equals(resultStatus);

            if (success) {
                log.info("Image deleted successfully from Cloudinary: {}", publicId);
            } else {
                log.warn("Failed to delete image from Cloudinary: {}", result);
            }

            return success;

        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary: {}", imageUrl, e);
            return false;
        }
    }

    private String extractPublicId(String cloudinaryUrl) {
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{format}
            if (!cloudinaryUrl.contains("cloudinary.com")) {
                return null;
            }

            int uploadIndex = cloudinaryUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            String afterUpload = cloudinaryUrl.substring(uploadIndex + 8);

            // Remove version if present (v1234567890/)
            if (afterUpload.matches("^v\\d+/.*")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf('/') + 1);
            }

            // Remove file extension
            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }

            return afterUpload;

        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", cloudinaryUrl, e);
            return null;
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

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File is not a valid image");
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(lastDotIndex) : "";
    }
}