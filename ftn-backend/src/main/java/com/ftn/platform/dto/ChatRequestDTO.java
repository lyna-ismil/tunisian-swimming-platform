package com.ftn.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequestDTO(
        Long sessionId,
        @NotBlank @Size(max = 2000) String message
) {
}
