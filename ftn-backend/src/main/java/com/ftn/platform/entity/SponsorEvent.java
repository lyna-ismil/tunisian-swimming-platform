package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sponsor_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    private Sponsor sponsor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private CompetitionEvent event;

    private Double sponsorshipAmount;
    
    private String packageType; // NAMING_RIGHTS, BRANDING, MEDIA, etc.
    
    private LocalDate matchedAt;
    
    private String status; // PENDING, ACTIVE, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String aiEvaluation; // AI-generated assessment JSON
}
