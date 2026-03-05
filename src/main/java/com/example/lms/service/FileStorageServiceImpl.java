package com.example.lms.service;

import com.example.lms.exception.ResourceNotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Service
public class FileStorageServiceImpl {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private final Path storageRoot;

    public FileStorageServiceImpl(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    public String store(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetPath = storageRoot.resolve(filename).normalize();
            Files.write(targetPath, file.getBytes());
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }

    public String storeBase64(String base64Data, String extension) {
        byte[] decoded = Base64.getDecoder().decode(base64Data);
        if (decoded.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
        }

        String filename = UUID.randomUUID() + (extension.startsWith(".") ? extension : "." + extension);

        try {
            Path targetPath = storageRoot.resolve(filename).normalize();
            Files.write(targetPath, decoded);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }

    public Resource getFileAsResource(String filename) {
        Path targetPath = storageRoot.resolve(filename).normalize();
        Path absoluteRoot = storageRoot.toAbsolutePath().normalize();
        Path absoluteTarget = targetPath.toAbsolutePath().normalize();

        if (!absoluteTarget.startsWith(absoluteRoot)) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }

        if (!Files.exists(absoluteTarget) || !Files.isRegularFile(absoluteTarget)) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }

        try {
            Resource resource = new UrlResource(absoluteTarget.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File not found: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalFilename.length() - 1) {
            return originalFilename.substring(lastDot);
        }
        return "";
    }
}
