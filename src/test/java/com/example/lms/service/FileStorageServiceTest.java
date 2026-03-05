package com.example.lms.service;

import com.example.lms.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(tempDir);
    }

    @Test
    void should_storeMultipartFile_andReturnFilename() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "document.pdf", "application/pdf", "content".getBytes());

        String filename = fileStorageService.store(file);

        assertThat(filename).matches("^[a-f0-9-]{36}\\.pdf$");
        assertThat(tempDir.resolve(filename)).exists();
        assertThat(Files.readAllBytes(tempDir.resolve(filename))).isEqualTo("content".getBytes());
    }

    @Test
    void should_storeBase64_andReturnFilename() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("image data".getBytes());

        String filename = fileStorageService.storeBase64(base64, ".png");

        assertThat(filename).matches("^[a-f0-9-]{36}\\.png$");
        assertThat(tempDir.resolve(filename)).exists();
        assertThat(Files.readAllBytes(tempDir.resolve(filename))).isEqualTo("image data".getBytes());
    }

    @Test
    void should_getFileAsResource_whenFileExists() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello world".getBytes());
        String filename = fileStorageService.store(file);

        Resource resource = fileStorageService.getFileAsResource(filename);

        assertThat(resource.exists()).isTrue();
        try (var is = resource.getInputStream()) {
            assertThat(new String(is.readAllBytes())).isEqualTo("hello world");
        }
    }

    @Test
    void should_throwResourceNotFound_whenFileNotExists() {
        assertThatThrownBy(() -> fileStorageService.getFileAsResource("nonexistent.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throwResourceNotFound_whenPathTraversalAttempted() {
        assertThatThrownBy(() -> fileStorageService.getFileAsResource("../../../etc/passwd"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_throw_whenFileExceeds10MB() throws Exception {
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
        MultipartFile file = new MockMultipartFile("file", "large.bin", "application/octet-stream", largeContent);

        assertThatThrownBy(() -> fileStorageService.store(file))
                .hasMessageContaining("10MB")
                .isInstanceOf(IllegalArgumentException.class);
    }
}
