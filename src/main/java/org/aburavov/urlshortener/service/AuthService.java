package org.aburavov.urlshortener.service;

import org.aburavov.urlshortener.dto.TokenResponse;
import org.aburavov.urlshortener.model.UserStatus;
import org.aburavov.urlshortener.repository.UserRepository;
import org.aburavov.urlshortener.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public TokenResponse login(String email, String password) {
        if (email == null || password == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }
        var user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is blocked");
        }
        log.info("user {} logged in", user.getEmail());
        return new TokenResponse(jwtService.createToken(user));
    }
}
