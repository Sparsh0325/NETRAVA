package com.intellishare.repository;

import com.intellishare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByBadgeNumberIgnoreCase(String badgeNumber);
    boolean existsByAgencyIgnoreCaseAndActiveTrue(String agency);
    List<User> findTop1ByAgencyIgnoreCaseAndActiveTrueOrderByIdAsc(String agency);
}
