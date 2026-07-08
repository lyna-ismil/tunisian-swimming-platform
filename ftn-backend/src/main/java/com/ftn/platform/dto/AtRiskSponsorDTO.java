package com.ftn.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtRiskSponsorDTO {
    private String sponsorId;
    private String sponsorName;
    private String tier;
    private Double churnProbability;
    private String riskLevel;
    private Integer daysUntilEnd;
    private Double totalValueTnd;
    private Double predictedValueLossTnd;
}
