package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sponsorship_package")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorshipPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_event_id", nullable = false)
    @ToString.Exclude
    private CompetitionEvent competitionEvent;

    @Column(nullable = false)
    private String title;

    private String price;

    @Column(length = 1000)
    private String benefits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status;

    private String sponsorName;
}
