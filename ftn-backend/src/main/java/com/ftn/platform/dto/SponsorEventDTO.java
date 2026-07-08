package com.ftn.platform.dto;

import java.util.Map;

public record SponsorEventDTO(
    Long id,
    Long sponsorId,
    String sponsorName,
    Long eventId,
    String eventName,
    String eventDate,
    String location,
    Double sponsorshipAmount,
    String packageType,
    String matchedAt,
    String status,
    Map<String, Object> aiEvaluation
) {}
