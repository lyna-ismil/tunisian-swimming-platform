package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competition_registration")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id")
    @ToString.Exclude
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id")
    @ToString.Exclude
    private CompetitionEvent competitionEvent;

    // APPROVED, PENDING, REJECTED
    private String status;
    
    // e.g. "100m Freestyle, 50m Butterfly"
    private String eventsList;

    private LocalDateTime registrationDate;

    @PrePersist
    protected void onCreate() {
        registrationDate = LocalDateTime.now();
    }
}
