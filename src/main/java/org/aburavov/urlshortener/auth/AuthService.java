package org.aburavov.urlshortener.auth;

import org.aburavov.urlshortener.auth.dto.TokenResponse;
import org.aburavov.urlshortener.user.User;
import org.aburavov.urlshortener.user.UserService;
import org.aburavov.urlshortener.user.UserStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JwtProperties jwtProperties) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public TokenResponse login(String email, String password) {
        User user = userService.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new LockedException("User is blocked");
        }
        String token = jwtService.issueToken(user);
        return new TokenResponse(token, "Bearer", jwtProperties.ttl().toSeconds());
    }
}
