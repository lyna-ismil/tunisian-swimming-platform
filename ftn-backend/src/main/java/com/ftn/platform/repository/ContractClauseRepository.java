package com.ftn.platform.repository;

import com.ftn.platform.entity.ContractClause;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractClauseRepository extends JpaRepository<ContractClause, Long> {

    List<ContractClause> findByContractId(Long contractId);

    void deleteByContractId(Long contractId);
}
