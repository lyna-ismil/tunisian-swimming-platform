package com.ftn.platform.repository;

import com.ftn.platform.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    Athlete findByUserId(Long userId);
}
