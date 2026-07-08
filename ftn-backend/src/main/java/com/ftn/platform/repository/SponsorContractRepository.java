package com.ftn.platform.repository;

import com.ftn.platform.entity.ContractStatus;
import com.ftn.platform.entity.ContractType;
import com.ftn.platform.entity.SponsorContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorContractRepository extends JpaRepository<SponsorContract, Long> {

    List<SponsorContract> findBySponsorId(Long sponsorId);

    List<SponsorContract> findBySponsorIdAndStatus(Long sponsorId, ContractStatus status);

    List<SponsorContract> findBySponsorIdAndContractType(Long sponsorId, ContractType type);

    List<SponsorContract> findBySponsorIdAndStatusAndContractType(Long sponsorId, ContractStatus status, ContractType type);

    Optional<SponsorContract> findByContractNumber(String contractNumber);

    @Query("SELECT COUNT(c) FROM SponsorContract c WHERE c.contractNumber LIKE :prefix%")
    long countByContractNumberStartingWith(String prefix);

    @Query("SELECT COALESCE(SUM(c.value), 0) FROM SponsorContract c WHERE c.sponsor.id = :sponsorId AND c.status = 'ACTIVE'")
    BigDecimal sumActiveContractValues(Long sponsorId);

    @Query("SELECT COUNT(c) FROM SponsorContract c WHERE c.sponsor.id = :sponsorId AND c.status = 'ACTIVE' AND c.contractType = :type")
    long countActiveContractsByType(Long sponsorId, ContractType type);

    long countByStatus(ContractStatus status);
}
