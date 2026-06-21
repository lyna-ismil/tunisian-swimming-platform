package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "license")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "athlete_id")
    @ToString.Exclude
    private Athlete athlete;

    private String licenseNumber;
    private LocalDate validFrom;
    private LocalDate validTo;
    
    // ACTIVE, EXPIRED, PENDING
    private String status;
    private String documentUrl;
}
