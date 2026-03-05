package com.example.lms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Value("${storage.path:./uploads}")
    private String storagePath;

    @Bean
    public Path storageRoot() throws Exception {
        Path root = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }
}
