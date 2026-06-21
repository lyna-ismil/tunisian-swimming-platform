package com.ftn.platform.dto;

import java.util.List;

public record MatchGenerationRequestDTO(
        List<Long> sponsorIds,
        Integer maxMatchesPerSponsor,
        Boolean persist
) {
}