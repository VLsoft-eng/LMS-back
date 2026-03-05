package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.service.FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class FileControllerIT extends AbstractIntegrationTest {

    private static final String FILES_URL = "/api/v1/files";

    @Autowired
    private FileStorageServiceImpl fileStorageService;

    private String storedFilename;

    @BeforeEach
    void setUp() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello, World!".getBytes());
        storedFilename = fileStorageService.store(file);
    }

    @Test
    void getFile_fileExists_returns200() throws Exception {
        mockMvc.perform(get(FILES_URL + "/" + storedFilename))
                .andExpect(status().isOk())
                .andExpect(content().bytes("Hello, World!".getBytes()))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    void getFile_fileNotFound_returns404() throws Exception {
        mockMvc.perform(get(FILES_URL + "/nonexistent-uuid-file.pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFile_unauthenticated_allowed_returns200() throws Exception {
        mockMvc.perform(get(FILES_URL + "/" + storedFilename))
                .andExpect(status().isOk());
    }

    @Test
    void getFile_pathTraversal_returns4xx() throws Exception {
        mockMvc.perform(get(FILES_URL + "/../../../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }
}
