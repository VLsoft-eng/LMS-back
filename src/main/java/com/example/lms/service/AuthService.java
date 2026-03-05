package com.example.lms.service;

import com.example.lms.dto.AuthResponse;
import com.example.lms.dto.LoginRequest;
import com.example.lms.dto.RegisterRequest;
import com.example.lms.entity.UserEntity;
import com.example.lms.exception.ConflictException;
import com.example.lms.repository.UserRepository;
import com.example.lms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already in use: " + request.email());
        }

        UserEntity user = UserEntity.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .dateOfBirth(request.dateOfBirth())
                .build();

        user = userRepository.save(user);
        return new AuthResponse(generateToken(user), UserService.toDto(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponse(generateToken(user), UserService.toDto(user));
    }

    private String generateToken(UserEntity user) {
        UserDetails principal = User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();
        return jwtService.generateToken(principal);
    }
}
