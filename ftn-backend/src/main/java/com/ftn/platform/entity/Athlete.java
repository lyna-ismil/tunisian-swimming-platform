package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "athlete")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Athlete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional link to User account
    private Long userId;

    private String fullName;
    private String photoUrl;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String clubAffiliation;
    private String contactPhone;
}
