package com.ftn.platform.dto;

public record SponsorshipPackageDTO(
        Long id,
        String title,
        String price,
        String benefits,
        String status,
        String sponsorName
) {
}
