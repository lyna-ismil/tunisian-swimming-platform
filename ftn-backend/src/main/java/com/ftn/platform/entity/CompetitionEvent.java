package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "competition_event")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bannerUrl;

    private String eventDate;

    private String location;

    private Integer athletesCount;

    private Integer clubsCount;

    private String audience;

    private Boolean streaming;

    private Double revenueTarget;

    @OneToMany(mappedBy = "competitionEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<SponsorshipPackage> packages = new ArrayList<>();
}
