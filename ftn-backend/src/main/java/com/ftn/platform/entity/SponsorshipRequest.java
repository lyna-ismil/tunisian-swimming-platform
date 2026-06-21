package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sponsorship_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsorshipRequest {

    @Id
    @Column(length = 20)
    private String id; // format: REQ-001

    @Column(nullable = false)
    private String applicant;

    @Column(columnDefinition = "TEXT")
    private String applicantLogo;

    private String type; // ATHLETE, CLUB, EVENT

    private String sponsorName;

    private LocalDate requestDate;

    private Double amount;

    private String priority; // HIGH, MEDIUM, LOW

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
}
