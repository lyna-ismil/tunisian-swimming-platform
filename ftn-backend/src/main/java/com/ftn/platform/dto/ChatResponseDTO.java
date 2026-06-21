package com.ftn.platform.dto;

import java.time.LocalDateTime;

public record ChatResponseDTO(
        Long id,
        String role,
        String content,
        LocalDateTime timestamp
) {
}
