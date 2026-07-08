package com.ftn.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorFeaturesDTO {
    private String sponsorId;
    private String tier;
    private Integer contractDurationMonths;
    private Double totalValueTnd;
    private Integer numPreviousContracts;
    private Double investmentTrendPct;
    private Integer availablePackages;
    private Integer packagesTaken;
    private Double engagementRate;
    private Integer athletesSponsored;
    private Double athleteTurnoverRate;
    private Integer competitionsPerYear;
    private Integer lastActivityDays;
    private Integer daysSinceStart;
    private Integer daysUntilEnd;
    private String status;
}
