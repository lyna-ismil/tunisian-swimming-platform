package com.ftn.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelMetricsDTO {
    private Double aucRoc;
    private Double avgPrecision;
    private Double precision;
    private Double recall;
    private Double f1Score;
    private Double cvAucMean;
    private String trainingDate;
    private Integer datasetSize;
}
