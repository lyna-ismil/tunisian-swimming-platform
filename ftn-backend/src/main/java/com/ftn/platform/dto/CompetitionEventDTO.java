package com.ftn.platform.dto;

import java.util.List;

public record CompetitionEventDTO(
        Long id,
        String name,
        String bannerUrl,
        String eventDate,
        String location,
        Integer athletesCount,
        Integer clubsCount,
        String audience,
        Boolean streaming,
        Double revenueTarget,
        List<SponsorshipPackageDTO> packages
) {
}
