package com.example.demo.dto;

import com.example.demo.model.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String email, String fullName, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
