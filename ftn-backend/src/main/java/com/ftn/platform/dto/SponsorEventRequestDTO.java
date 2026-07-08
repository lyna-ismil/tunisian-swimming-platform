package com.ftn.platform.dto;

public record SponsorEventRequestDTO(
    Long eventId,
    Double sponsorshipAmount,
    String packageType,
    String aiEvaluation
) {}
