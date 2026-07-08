package com.ftn.platform.repository;

import com.ftn.platform.entity.PFEStatus;
import com.ftn.platform.entity.SponsorPFE;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SponsorPFERepository extends JpaRepository<SponsorPFE, Long> {

    List<SponsorPFE> findBySponsorId(Long sponsorId);

    List<SponsorPFE> findByStatus(PFEStatus status);
}
