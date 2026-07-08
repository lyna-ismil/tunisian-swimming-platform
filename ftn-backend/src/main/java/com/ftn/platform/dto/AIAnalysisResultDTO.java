package com.ftn.platform.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AIAnalysisResultDTO(
    String contractSummary,
    List<String> parties,
    FinancialValue financialValue,
    DurationInfo duration,
    String paymentTerms,
    List<String> deliverables,
    ExclusivityInfo exclusivity,
    String terminationClause,
    List<String> risks,
    List<String> keyDates,
    List<ExtractedClause> clauses
) {
    public record FinancialValue(BigDecimal amount, String currency) {}
    public record DurationInfo(LocalDate start, LocalDate end, boolean autoRenewal) {}
    public record ExclusivityInfo(boolean exists, String sector) {}
    public record ExtractedClause(String type, String text, String interpretation, Integer page) {}
}
