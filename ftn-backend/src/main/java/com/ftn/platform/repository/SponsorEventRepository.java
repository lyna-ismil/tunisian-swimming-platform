package com.ftn.platform.repository;

import com.ftn.platform.entity.SponsorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorEventRepository extends JpaRepository<SponsorEvent, Long> {
    List<SponsorEvent> findBySponsorId(Long sponsorId);
    List<SponsorEvent> findByEventId(Long eventId);
    boolean existsBySponsorIdAndEventId(Long sponsorId, Long eventId);
}
