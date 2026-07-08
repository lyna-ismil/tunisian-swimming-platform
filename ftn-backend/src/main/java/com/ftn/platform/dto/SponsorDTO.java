package com.ftn.platform.dto;

import jakarta.validation.constraints.PositiveOrZero;

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
        String endDate,
        @PositiveOrZero(message = "totalValue must be greater than or equal to 0") Double totalValue,
        List<String> preferredDisciplines
) {
}
