package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findFirstByEmailAndTokenAndUsedFalseOrderByExpiresAtDesc(String email, String token);
    void deleteByEmail(String email);
}
