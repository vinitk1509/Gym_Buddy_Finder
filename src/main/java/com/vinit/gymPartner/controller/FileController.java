package com.vinit.gymPartner.controller;

import com.vinit.gymPartner.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads/profile-pictures")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            // Resolve the file location based on the configured upload directory
            Path filePath = Paths.get(fileStorageService.getUploadDir()).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not found: " + filename);
            }
            // Guess content type (default to OCTET_STREAM)
            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(filePath);
            } catch (Exception ignored) {}
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serve file", ex);
        }
    }
}
