package com.ftn.platform.dto;

import java.math.BigDecimal;

public record ContractClauseDTO(
    Long id,
    String clauseType,
    String extractedText,
    String aiInterpretation,
    Integer pageNumber,
    BigDecimal confidenceScore
) {}
