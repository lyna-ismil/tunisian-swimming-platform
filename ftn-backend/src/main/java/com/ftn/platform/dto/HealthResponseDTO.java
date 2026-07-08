package com.ftn.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponseDTO {
    private String status;
    private Boolean modelLoaded;
    private String modelType;
    private String version;
}
