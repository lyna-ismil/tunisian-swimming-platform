package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "athlete_match")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AthleteMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id")
    @ToString.Exclude
    private Athlete athlete;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id")
    @ToString.Exclude
    private Sponsor sponsor;

    @Column(nullable = false)
    private String athleteName;

    private String athletePhotoUrl;

    private String discipline;

    @Column(name = "rank_position")
    private Integer rank;

    private String suggestedSponsor;

    private String sponsorLogoUrl;

    private Integer matchScore;

    private String confidence;

    @Column(length = 1000)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String keyFactorsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;
    
    public String getScoreColor() {
        if (matchScore == null) return "text-red-600";
        if (matchScore >= 90) return "text-green-600";
        if (matchScore >= 75) return "text-blue-600";
        if (matchScore >= 60) return "text-orange-500";
        return "text-red-600";
    }
}
