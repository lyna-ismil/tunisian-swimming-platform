package com.ftn.platform.dto;

import com.ftn.platform.entity.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreateRequest(
    Long sponsorId,
    ContractType contractType,
    Long subjectId,
    String subjectName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal value,
    String description,
    String status
) {}
