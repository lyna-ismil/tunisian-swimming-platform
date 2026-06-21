package com.ftn.platform.repository;

import com.ftn.platform.entity.SponsorshipPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorshipPackageRepository extends JpaRepository<SponsorshipPackage, Long> {
    List<SponsorshipPackage> findByCompetitionEventId(Long eventId);
}
