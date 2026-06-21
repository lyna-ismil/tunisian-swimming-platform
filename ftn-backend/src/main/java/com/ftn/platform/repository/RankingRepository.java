package com.ftn.platform.repository;

import com.ftn.platform.entity.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    List<Ranking> findByAthleteIdOrderByUpdateDateDesc(Long athleteId);
}
