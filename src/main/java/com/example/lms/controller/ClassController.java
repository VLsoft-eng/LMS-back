package com.example.lms.controller;

import com.example.lms.dto.ClassDto;
import com.example.lms.dto.CreateClassRequest;
import com.example.lms.dto.JoinClassRequest;
import com.example.lms.dto.UpdateClassRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassDto createClass(@Valid @RequestBody CreateClassRequest request,
                                @CurrentUser UserEntity currentUser) {
        return classService.createClass(request, currentUser);
    }

    @GetMapping
    public Page<ClassDto> getMyClasses(@CurrentUser UserEntity currentUser, Pageable pageable) {
        return classService.getMyClasses(currentUser.getId(), pageable);
    }

    @PostMapping("/join")
    public ClassDto joinClass(@Valid @RequestBody JoinClassRequest request,
                              @CurrentUser UserEntity currentUser) {
        return classService.joinClass(request.code(), currentUser);
    }

    @PutMapping("/{id}")
    public ClassDto updateClass(@PathVariable UUID id,
                                @Valid @RequestBody UpdateClassRequest request,
                                @CurrentUser UserEntity currentUser) {
        return classService.updateClass(id, request, currentUser.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClass(@PathVariable UUID id,
                            @CurrentUser UserEntity currentUser) {
        classService.deleteClass(id, currentUser.getId());
    }

    @GetMapping("/{id}/code")
    public Map<String, String> getClassCode(@PathVariable UUID id,
                                            @CurrentUser UserEntity currentUser) {
        return Map.of("code", classService.getClassCode(id, currentUser.getId()));
    }
}
