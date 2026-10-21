package com.intellishare.service;

import com.intellishare.dto.auth.AuthResponse;
import com.intellishare.dto.auth.LoginRequest;
import com.intellishare.dto.auth.RegisterRequest;
import com.intellishare.dto.auth.UserResponse;
import com.intellishare.model.User;
import com.intellishare.audit.AuditService;
import com.intellishare.repository.UserRepository;
import com.intellishare.security.JwtTokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final AuditService audit;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens, AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String badge = request.badgeNumber().trim();
        if (users.existsByEmailIgnoreCase(email) || users.existsByBadgeNumberIgnoreCase(badge)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or badge number is already registered");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), badge,
                request.agency().trim(), clean(request.department()), clean(request.phone()));
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or badge number is already registered");
        }
        audit.record(user, "AUTH_REGISTER", "user:" + user.getId(), "Account registered");
        return response(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        user.setLastLoginAt(OffsetDateTime.now());
        audit.record(user, "AUTH_LOGIN", "user:" + user.getId(), "Successful login");
        return response(user);
    }

    public UserResponse currentUser(User user) { return UserResponse.from(user); }

    private AuthResponse response(User user) {
        return new AuthResponse(tokens.issue(user), "Bearer", tokens.getExpirationSeconds(), UserResponse.from(user));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
