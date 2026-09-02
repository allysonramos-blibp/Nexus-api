package com.nexus.nexus_api.repository;

import com.nexus.nexus_api.model.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesRepository extends JpaRepository<Preferences, Long> {
    Optional<Preferences> findByUserId(Long userId);
}
