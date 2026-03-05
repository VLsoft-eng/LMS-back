package com.example.lms.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
public class FileStorageServiceImpl {

    private final Path storageRoot;

    public FileStorageServiceImpl(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    public String store(MultipartFile file) {
        throw new UnsupportedOperationException("TDD: implement store");
    }

    public String storeBase64(String base64Data, String extension) {
        throw new UnsupportedOperationException("TDD: implement storeBase64");
    }

    public Resource getFileAsResource(String filename) {
        throw new UnsupportedOperationException("TDD: implement getFileAsResource");
    }
}
