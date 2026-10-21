package com.intellishare.security;

import com.intellishare.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtTokenService(@Value("${intellishare.jwt.secret}") String secret,
                           @Value("${intellishare.jwt.expiration:PT1H}") Duration expiration) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        try {
            this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("JWT_SECRET must be a Base64 encoded key of at least 256 bits");
        }
        if (expiration.isZero() || expiration.isNegative()) {
            throw new IllegalStateException("JWT_EXPIRATION must be positive");
        }
        this.expiration = expiration;
    }

    public String issue(User user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getEmail()).claim("uid", user.getId())
                .claim("role", user.getRole()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration))).signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpirationSeconds() { return expiration.toSeconds(); }
}
