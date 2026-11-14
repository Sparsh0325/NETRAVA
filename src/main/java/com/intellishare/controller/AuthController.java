package com.intellishare.controller;

import com.intellishare.dto.auth.AuthResponse;
import com.intellishare.dto.auth.LoginRequest;
import com.intellishare.dto.auth.RegisterRequest;
import com.intellishare.dto.auth.UserResponse;
import com.intellishare.dto.auth.UpdateProfileRequest;
import com.intellishare.model.User;
import com.intellishare.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return authService.login(request); }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User user) { return authService.currentUser(user); }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal User user, @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(user, request);
    }
}
