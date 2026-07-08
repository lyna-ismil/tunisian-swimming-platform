package com.ftn.platform.dto;

import com.ftn.platform.entity.PFEStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SponsorPFEDTO(
    Long id,
    Long sponsorId,
    Long contractId,
    String studentName,
    String studentEmail,
    String university,
    String specialty,
    String pfeTitle,
    String pfeDescription,
    LocalDate startDate,
    LocalDate endDate,
    PFEStatus status,
    BigDecimal sponsorGrant,
    LocalDateTime createdAt
) {}
