package org.aburavov.urlshortener.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.aburavov.urlshortener.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt-secret}")
    private String secret;
    @Value("${app.jwt-ttl-seconds}")
    private int ttlSeconds;

    public String createToken(User user) {
        var now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSeconds * 1000L))
                .signWith(key())
                .compact();
    }

    // throws JwtException if the token is broken or expired
    public String getEmail(String token) {
        var claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
