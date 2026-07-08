package com.ftn.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MatchEvaluationResponseDTO(
        Long id,
        Long sponsorId,
        Long athleteId,
        String athleteName,
        String athletePhotoUrl,
        String sponsorName,
        String sponsorLogoUrl,
        Integer matchScore,
        String verdict,
        String confidence,
        String explanation,
        List<String> strengths,
        List<String> weaknesses,
        Map<String, Object> potentialROI,
        Map<String, Object> audienceOverlap,
        List<Map<String, Object>> keyFactors,
        LocalDateTime evaluationDate
) {}
