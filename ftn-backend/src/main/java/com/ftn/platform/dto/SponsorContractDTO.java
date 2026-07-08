package com.ftn.platform.dto;

import com.ftn.platform.entity.ContractStatus;
import com.ftn.platform.entity.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SponsorContractDTO(
    Long id,
    Long sponsorId,
    String sponsorName,
    String contractNumber,
    ContractType contractType,
    Long subjectId,
    String subjectName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal value,
    String currency,
    ContractStatus status,
    String description,
    String documentUrl,
    String aiSummary,
    List<ContractClauseDTO> clauses,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
