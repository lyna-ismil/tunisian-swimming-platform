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
public class BatchPredictionResponseDTO {
    private List<ChurnPredictionDTO> predictions;
    private Integer totalAtRisk;
    private Double totalValueAtRiskTnd;
}
