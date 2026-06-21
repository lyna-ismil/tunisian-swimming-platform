package com.ftn.platform.dto;

import java.util.List;

public record SponsorDTO(
        Long id,
        String name,
        String tier,
        String description,
        String logoUrl,
        String website,
        String contactEmail,
        String status,
        Integer athletesCount,
        Integer competitionsCount,
        String startDate,
        Double totalValue,
        List<String> preferredDisciplines
) {
}
