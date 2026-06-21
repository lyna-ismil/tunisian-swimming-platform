package com.ftn.platform.repository;

import com.ftn.platform.entity.AthleteMatch;
import com.ftn.platform.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AthleteMatchRepository extends JpaRepository<AthleteMatch, Long> {
    List<AthleteMatch> findByStatus(MatchStatus status);
    List<AthleteMatch> findBySponsorId(Long sponsorId);
}
