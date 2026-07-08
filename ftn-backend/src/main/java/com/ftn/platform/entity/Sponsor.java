package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sponsor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String tier;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    private String website;

    private String contactEmail;

    @Builder.Default
    @Column(columnDefinition = "TEXT")
    private String preferredDisciplines = "[]";

    @OneToOne(mappedBy = "sponsor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private SponsorPreferences preferences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SponsorStatus status;

    @Builder.Default
    private Integer athletesCount = 0;

    @Builder.Default
    private Integer competitionsCount = 0;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Double totalValue = 0.0;
}
