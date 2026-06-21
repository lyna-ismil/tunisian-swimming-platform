package com.ftn.platform.dto;

import java.util.List;

public record MatchGenerationResponseDTO(
        int sponsorsProcessed,
        int matchesGenerated,
        List<AthleteMatchDTO> matches,
        String message
) {
}