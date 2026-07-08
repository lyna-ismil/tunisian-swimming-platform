package com.ftn.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChurnPredictionDTO {
    private String sponsorId;
    private Double churnProbability;
    private Boolean willChurn;
    private String riskLevel;
    private Double expectedValueNextTnd;
    private String recommendation;
    private List<String> topRiskFactors;
    private String modelUsed;
}
