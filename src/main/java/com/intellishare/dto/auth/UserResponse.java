package com.intellishare.dto.auth;

import com.intellishare.model.User;

public record UserResponse(Long id, String name, String email, String badgeNumber, String agency,
                           String role, String department, String phone, String avatarUrl) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getBadgeNumber(),
                user.getAgency(), user.getRole(), user.getDepartment(), user.getPhone(), user.getAvatarUrl());
    }
}
