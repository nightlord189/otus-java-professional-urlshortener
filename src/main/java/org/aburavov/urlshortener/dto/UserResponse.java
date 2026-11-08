package org.aburavov.urlshortener.dto;

import org.aburavov.urlshortener.model.User;
import org.aburavov.urlshortener.model.UserRole;
import org.aburavov.urlshortener.model.UserStatus;

import java.time.Instant;

public record UserResponse(Long id, String email, UserRole role, UserStatus status, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
