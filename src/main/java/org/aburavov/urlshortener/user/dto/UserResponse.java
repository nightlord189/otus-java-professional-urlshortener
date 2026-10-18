package org.aburavov.urlshortener.user.dto;

import java.time.Instant;
import org.aburavov.urlshortener.user.User;
import org.aburavov.urlshortener.user.UserRole;
import org.aburavov.urlshortener.user.UserStatus;

public record UserResponse(Long id, String email, UserRole role, UserStatus status, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
