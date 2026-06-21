package com.ftn.platform.dto;

public record SponsorshipRequestDTO(
        String id,
        String applicant,
        String applicantLogo,
        String type,
        String sponsorName,
        String requestDate,
        Double amount,
        String priority,
        String status
) {
}
