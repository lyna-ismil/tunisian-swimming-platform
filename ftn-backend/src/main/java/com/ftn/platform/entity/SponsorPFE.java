package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsor_pfe")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SponsorPFE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", nullable = false)
    @ToString.Exclude
    private Sponsor sponsor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    @ToString.Exclude
    private SponsorContract contract;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_email")
    private String studentEmail;

    private String university;

    private String specialty;

    @Column(name = "pfe_title", nullable = false, length = 500)
    private String pfeTitle;

    @Column(name = "pfe_description", columnDefinition = "TEXT")
    private String pfeDescription;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PFEStatus status = PFEStatus.PROPOSED;

    @Column(name = "sponsor_grant", precision = 15, scale = 2)
    private BigDecimal sponsorGrant;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
