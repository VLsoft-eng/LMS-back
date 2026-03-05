package com.example.lms.service;

import com.example.lms.dto.ClassDto;
import com.example.lms.dto.CreateClassRequest;
import com.example.lms.dto.UpdateClassRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository       classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassSecurityService  classSecurityService;

    public ClassDto createClass(CreateClassRequest request, UserEntity currentUser) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public List<ClassDto> getMyClasses(UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ClassDto joinClass(String code, UserEntity currentUser) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public ClassDto updateClass(UUID classId, UpdateClassRequest request, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public void deleteClass(UUID classId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public String getClassCode(UUID classId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
