package com.ftn.platform.dto;

import java.util.List;

public record AthleteMatchDTO(
        Long id,
        Long athleteId,
        Long sponsorId,
        String athleteName,
        String athletePhotoUrl,
        String discipline,
        Integer rank,
        String suggestedSponsor,
        String sponsorLogoUrl,
        Integer matchScore,
        String scoreColor,
        String confidence,
        String reason,
        List<String> keyFactors,
        String status
) {
}
