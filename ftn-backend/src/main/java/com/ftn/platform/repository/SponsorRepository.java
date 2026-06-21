package com.ftn.platform.repository;

import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, Long> {
    List<Sponsor> findByStatus(SponsorStatus status);
    long countByStatus(SponsorStatus status);

    @Query("SELECT DATE_FORMAT(s.startDate, '%b') as month, MONTH(s.startDate) as monthNum, COUNT(s) as count " +
           "FROM Sponsor s " +
           "WHERE s.status = 'ACTIVE' AND s.startDate >= :startDate " +
           "GROUP BY MONTH(s.startDate), DATE_FORMAT(s.startDate, '%b') " +
           "ORDER BY MONTH(s.startDate)")
    List<Object[]> findMonthlyActiveSponsorCount(@Param("startDate") LocalDate startDate);
}
