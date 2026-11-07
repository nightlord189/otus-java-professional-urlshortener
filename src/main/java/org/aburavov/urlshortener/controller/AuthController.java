package org.aburavov.urlshortener.controller;

import org.aburavov.urlshortener.dto.LoginRequest;
import org.aburavov.urlshortener.dto.RegisterRequest;
import org.aburavov.urlshortener.dto.TokenResponse;
import org.aburavov.urlshortener.dto.UserResponse;
import org.aburavov.urlshortener.model.UserRole;
import org.aburavov.urlshortener.service.AuthService;
import org.aburavov.urlshortener.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody RegisterRequest request) {
        var user = userService.create(request.email(), request.password(), UserRole.USER);
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }
}
