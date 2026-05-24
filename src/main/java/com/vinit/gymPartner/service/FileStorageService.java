package com.vinit.gymPartner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import javax.imageio.ImageIO;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;




@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${file.upload-dir:uploads/profile-pictures}")
    private String uploadDir;

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    public String getUploadDir() {
        return uploadDir;
    }

    public String storeFile(MultipartFile file){
        try{
            validateImageFile(file);

            if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
                Cloudinary cloudinary = new Cloudinary(cloudinaryUrl);
                Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
                return uploadResult.get("secure_url").toString();
            }

            Path uploadPath = Paths.get(uploadDir);
            if(!Files.exists(uploadPath)){
                Files.createDirectories(uploadPath);
            }
            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT)
                    : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            Path targetPath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/profile-pictures/" + newFilename;
        } catch (IOException e){
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private void validateImageFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new RuntimeException("Only JPG, PNG, and WebP images are allowed");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Unsupported image file extension");
        }

        if (!contentType.equalsIgnoreCase("image/webp")) {
            try (InputStream inputStream = file.getInputStream()) {
                if (ImageIO.read(inputStream) == null) {
                    throw new RuntimeException("Uploaded file is not a valid image");
                }
            }
        }
    }
}
