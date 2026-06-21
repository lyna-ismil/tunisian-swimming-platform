package com.ftn.platform.repository;

import com.ftn.platform.entity.CompetitionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompetitionResultRepository extends JpaRepository<CompetitionResult, Long> {
    List<CompetitionResult> findByAthleteId(Long athleteId);
}
