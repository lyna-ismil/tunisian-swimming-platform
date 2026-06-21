package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConfig {

    @Id
    @Column(length = 100)
    private String configKey;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String configValue;
}
