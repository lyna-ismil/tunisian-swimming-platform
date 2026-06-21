package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sponsor_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false, unique = true)
    @ToString.Exclude
    private Sponsor sponsor;

    @Builder.Default
    @Convert(converter = StringListJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> preferredDisciplines = new ArrayList<>();

    private Integer minRankPosition;
    private Integer maxAthleteAge;
    private String targetGender;
    private Integer minFINAPoints;
    private String geographicPreference;
    private Double contractValueRangeMin;
    private Double contractValueRangeMax;
}