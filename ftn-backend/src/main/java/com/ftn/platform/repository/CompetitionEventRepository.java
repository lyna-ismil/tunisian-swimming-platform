package com.ftn.platform.repository;

import com.ftn.platform.entity.CompetitionEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionEventRepository extends JpaRepository<CompetitionEvent, Long> {
    
    @EntityGraph(attributePaths = {"packages"})
    List<CompetitionEvent> findAll();
}
