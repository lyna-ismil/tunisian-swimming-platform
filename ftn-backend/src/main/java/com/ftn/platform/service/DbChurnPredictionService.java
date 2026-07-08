package com.ftn.platform.service;

import com.ftn.platform.dto.*;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorStatus;
import com.ftn.platform.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DbChurnPredictionService {

    private final SponsorRepository sponsorRepository;
    private final MLServiceClient mlClient;

    public List<AtRiskSponsorDTO> getAtRiskSponsors(Double threshold, String tierFilter, String statusFilter) {
        List<Sponsor> sponsors = sponsorRepository.findAll();
        log.info("Found {} real sponsors in DB for churn prediction", sponsors.size());

        if (sponsors.isEmpty()) {
            return List.of();
        }

        List<SponsorFeaturesDTO> featuresList = sponsors.stream()
            .map(this::toFeatures)
            .collect(Collectors.toList());

        featuresList.forEach(f -> log.debug("Feature: sponsorId={}, tier={}, totalValueTnd={}, status={}",
            f.getSponsorId(), f.getTier(), f.getTotalValueTnd(), f.getStatus()));

        BatchPredictionRequestDTO request = BatchPredictionRequestDTO.builder()
            .sponsors(featuresList)
            .build();

        BatchPredictionResponseDTO response = mlClient.predictBatch(request);

        if (response == null || response.getPredictions() == null) {
            log.warn("Batch predict returned null or empty predictions");
            return List.of();
        }

        log.info("Batch predict returned {} predictions", response.getPredictions().size());
        response.getPredictions().forEach(p -> log.info("  Predict: sponsorId={}, prob={}, risk={}",
            p.getSponsorId(), p.getChurnProbability(), p.getRiskLevel()));

        Map<String, Sponsor> sponsorMap = sponsors.stream()
            .collect(Collectors.toMap(s -> "SPN-" + s.getId(), s -> s));

        double t = threshold != null ? threshold : 0.3;

        Map<String, SponsorFeaturesDTO> featureMap = featuresList.stream()
            .collect(Collectors.toMap(SponsorFeaturesDTO::getSponsorId, f -> f));

        List<AtRiskSponsorDTO> atRisk = response.getPredictions().stream()
            .filter(p -> p.getChurnProbability() != null && p.getChurnProbability() >= t)
            .map(p -> {
                Sponsor s = sponsorMap.get(p.getSponsorId());
                SponsorFeaturesDTO f = featureMap.get(p.getSponsorId());
                if (s == null) return null;
                double featureValue = f != null && f.getTotalValueTnd() != null ? f.getTotalValueTnd() : 50000.0;
                double predictedLoss = (p.getWillChurn() != null && p.getWillChurn())
                    ? featureValue
                    : (p.getChurnProbability() != null ? featureValue * p.getChurnProbability() : 0.0);
                int daysUntilEnd = f != null && f.getDaysUntilEnd() != null ? f.getDaysUntilEnd() : 180;
                return AtRiskSponsorDTO.builder()
                    .sponsorId("SPN-" + s.getId())
                    .sponsorName(s.getName())
                    .tier(f != null && f.getTier() != null ? f.getTier() : "BRONZE")
                    .churnProbability(p.getChurnProbability())
                    .riskLevel(p.getRiskLevel())
                    .daysUntilEnd(daysUntilEnd)
                    .totalValueTnd(featureValue)
                    .predictedValueLossTnd(Math.round(predictedLoss * 100.0) / 100.0)
                    .build();
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(AtRiskSponsorDTO::getChurnProbability).reversed())
            .collect(Collectors.toList());

        log.info("Returning {} at-risk sponsors out of {} real sponsors (threshold={})", atRisk.size(), sponsors.size(), t);
        return atRisk;
    }

    private SponsorFeaturesDTO toFeatures(Sponsor s) {
        LocalDate now = LocalDate.now();

        long daysSinceStart = s.getStartDate() != null
            ? ChronoUnit.DAYS.between(s.getStartDate(), now)
            : 365;

        long daysUntilEnd = s.getEndDate() != null
            ? Math.max(1, ChronoUnit.DAYS.between(now, s.getEndDate()))
            : 180;

        long contractDurationMonths = (int) Math.max(1, Math.min(60, 
            (daysSinceStart + daysUntilEnd) / 30));

        Double totalValue = s.getTotalValue();
        if (totalValue == null || totalValue <= 0) {
            totalValue = 50000.0;
        }

        String mlStatus;
        if (s.getStatus() == SponsorStatus.ACTIVE) {
            mlStatus = daysUntilEnd <= 90 ? "EXPIRING_SOON" : "ACTIVE";
        } else if (s.getStatus() == SponsorStatus.EXPIRING_SOON) {
            mlStatus = "EXPIRING_SOON";
        } else {
            mlStatus = "INACTIVE";
        }

        return SponsorFeaturesDTO.builder()
            .sponsorId("SPN-" + s.getId())
            .tier(s.getTier() != null ? s.getTier() : "BRONZE")
            .contractDurationMonths((int) contractDurationMonths)
            .totalValueTnd(totalValue)
            .numPreviousContracts(0)
            .investmentTrendPct(0.0)
            .availablePackages(Math.max(1, s.getCompetitionsCount() != null ? s.getCompetitionsCount() : 5))
            .packagesTaken(Math.min(3, s.getCompetitionsCount() != null ? s.getCompetitionsCount() : 0))
            .engagementRate(Math.min(1.0, Math.max(0.1, 
                s.getAthletesCount() != null ? Math.min(1.0, s.getAthletesCount() / 200.0) : 0.3)))
            .athletesSponsored(s.getAthletesCount() != null ? s.getAthletesCount() : 0)
            .athleteTurnoverRate(0.0)
            .competitionsPerYear(s.getCompetitionsCount() != null ? s.getCompetitionsCount() : 0)
            .lastActivityDays(Math.max(0, 365 - (int) daysSinceStart))
            .daysSinceStart((int) Math.max(1, daysSinceStart))
            .daysUntilEnd((int) Math.max(1, daysUntilEnd))
            .status(mlStatus)
            .build();
    }
}
