package com.example.lms.controller;

import com.example.lms.service.FileStorageServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Файлы", description = "Публичная раздача файлов")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@SecurityRequirements
public class FileController {

    private final FileStorageServiceImpl fileStorageService;

    @Operation(summary = "Получить файл", description = "Публичный доступ к файлу по имени. Без аутентификации.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно получен"),
            @ApiResponse(responseCode = "404", description = "Файл не найден")
    })
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource resource = fileStorageService.getFileAsResource(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
