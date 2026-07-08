package com.ftn.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_clauses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractClause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    @ToString.Exclude
    private SponsorContract contract;

    @Column(name = "clause_type", nullable = false, length = 50)
    private String clauseType;

    @Column(name = "extracted_text", nullable = false, columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "ai_interpretation", columnDefinition = "TEXT")
    private String aiInterpretation;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
