package com.ftn.platform.dto;

import java.util.List;

public record SponsorPreferencesDTO(
        Long id,
        Long sponsorId,
        List<String> preferredDisciplines,
        Integer minRankPosition,
        Integer maxAthleteAge,
        String targetGender,
        Integer minFINAPoints,
        String geographicPreference,
        Double contractValueRangeMin,
        Double contractValueRangeMax
) {
}