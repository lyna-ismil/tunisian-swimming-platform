package com.ftn.platform.repository;

import com.ftn.platform.entity.SponsorshipRequest;
import com.ftn.platform.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SponsorshipRequestRepository extends JpaRepository<SponsorshipRequest, String> {
    List<SponsorshipRequest> findByStatus(RequestStatus status);
    List<SponsorshipRequest> findByApplicantContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(String applicant, String sponsor);
    long countByStatus(RequestStatus status);

    @Query("SELECT DATE_FORMAT(r.requestDate, '%b') as month, MONTH(r.requestDate) as monthNum, SUM(r.amount) as revenue " +
           "FROM SponsorshipRequest r " +
           "WHERE r.status = 'APPROVED' AND r.requestDate >= :startDate " +
           "GROUP BY MONTH(r.requestDate), DATE_FORMAT(r.requestDate, '%b') " +
           "ORDER BY MONTH(r.requestDate)")
    List<Object[]> findMonthlyRevenue(@Param("startDate") LocalDate startDate);
}
