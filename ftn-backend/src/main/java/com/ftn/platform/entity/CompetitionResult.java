package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "competition_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionResult {

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

    private String eventName;
    private String recordTime; // e.g. "00:52.41"
    private Integer rankPosition;
    private Integer points;
}
