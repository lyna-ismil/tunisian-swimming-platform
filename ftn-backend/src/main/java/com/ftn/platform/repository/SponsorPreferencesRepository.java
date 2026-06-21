package com.ftn.platform.repository;

import com.ftn.platform.entity.SponsorPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SponsorPreferencesRepository extends JpaRepository<SponsorPreferences, Long> {
    Optional<SponsorPreferences> findBySponsorId(Long sponsorId);
    void deleteBySponsorId(Long sponsorId);
}