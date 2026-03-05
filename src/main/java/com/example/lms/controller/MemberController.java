package com.example.lms.controller;

import com.example.lms.dto.AssignRoleRequest;
import com.example.lms.dto.MemberDto;
import com.example.lms.entity.UserEntity;
import com.example.lms.security.CurrentUser;
import com.example.lms.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes/{classId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public Page<MemberDto> getMembers(@PathVariable UUID classId,
                                      @CurrentUser UserEntity currentUser,
                                      Pageable pageable) {
        return memberService.getMembers(classId, currentUser.getId(), pageable);
    }

    @PutMapping("/{memberUserId}/role")
    public MemberDto assignRole(@PathVariable UUID classId,
                                @PathVariable UUID memberUserId,
                                @Valid @RequestBody AssignRoleRequest request,
                                @CurrentUser UserEntity currentUser) {
        return memberService.assignRole(classId, memberUserId, request, currentUser.getId());
    }
}
