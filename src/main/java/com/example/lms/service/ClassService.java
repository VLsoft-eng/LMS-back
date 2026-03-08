package com.example.lms.service;

import com.example.lms.dto.ClassDto;
import com.example.lms.dto.CreateClassRequest;
import com.example.lms.dto.UpdateClassRequest;
import com.example.lms.entity.ClassEntity;
import com.example.lms.entity.ClassMemberEntity;
import com.example.lms.entity.Role;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ConflictException;
import com.example.lms.exception.ResourceNotFoundException;
import com.example.lms.repository.ClassMemberRepository;
import com.example.lms.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {

    private static final String      CHARS  = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClassRepository      classRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassSecurityService  classSecurityService;

    @Transactional
    public ClassDto createClass(CreateClassRequest request, UserEntity currentUser) {
        String code = generateUniqueCode();

        ClassEntity cls = ClassEntity.builder()
                .name(request.name())
                .code(code)
                .ownerId(currentUser.getId())
                .build();
        cls = classRepository.save(cls);

        ClassMemberEntity member = ClassMemberEntity.builder()
                .classId(cls.getId())
                .userId(currentUser.getId())
                .role(Role.OWNER)
                .build();
        classMemberRepository.save(member);

        return toDto(cls, member);
    }

    @Transactional(readOnly = true)
    public Page<ClassDto> getMyClasses(UUID userId, Pageable pageable) {
        return classRepository.findAllByMembersUserId(userId, pageable)
                .map(cls -> {
                    ClassMemberEntity member = classMemberRepository
                            .findByClassIdAndUserId(cls.getId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Member record not found for class " + cls.getId()));
                    return toDto(cls, member);
                });
    }

    @Transactional
    public ClassDto joinClass(String code, UserEntity currentUser) {
        ClassEntity cls = classRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class not found with code: " + code));

        if (classMemberRepository.findByClassIdAndUserId(cls.getId(), currentUser.getId()).isPresent()) {
            throw new ConflictException("Already a member of this class");
        }

        ClassMemberEntity member = ClassMemberEntity.builder()
                .classId(cls.getId())
                .userId(currentUser.getId())
                .role(Role.STUDENT)
                .build();
        classMemberRepository.save(member);

        return toDto(cls, member);
    }

    @Transactional
    public ClassDto updateClass(UUID classId, UpdateClassRequest request, UUID userId) {
        ClassMemberEntity member = classSecurityService.requireOwner(classId, userId);

        ClassEntity cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        cls.setName(request.name());
        cls = classRepository.save(cls);

        return toDto(cls, member);
    }

    @Transactional
    public void deleteClass(UUID classId, UUID userId) {
        classSecurityService.requireOwner(classId, userId);

        ClassEntity cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        classRepository.delete(cls);
    }

    @Transactional(readOnly = true)
    public String getClassCode(UUID classId, UUID userId) {
        classSecurityService.requireOwnerOrTeacher(classId, userId);

        ClassEntity cls = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + classId));
        return cls.getCode().trim();
    }

    private ClassDto toDto(ClassEntity cls, ClassMemberEntity member) {
        long count = classMemberRepository.countByClassId(cls.getId());
        return new ClassDto(
                cls.getId(),
                cls.getName(),
                cls.getCode().trim(),
                member.getRole(),
                (int) count,
                cls.getCreatedAt()
        );
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            code = sb.toString();
        } while (classRepository.existsByCode(code));
        return code;
    }
}
