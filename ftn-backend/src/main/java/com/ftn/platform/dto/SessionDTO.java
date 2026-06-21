package com.ftn.platform.dto;

import java.time.LocalDateTime;

public record SessionDTO(
        Long id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime lastActivityAt,
        int messageCount,
        String summary,
        String status
) {
}
