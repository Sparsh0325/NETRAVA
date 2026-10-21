package com.intellishare.dto.auth;

public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) { }
