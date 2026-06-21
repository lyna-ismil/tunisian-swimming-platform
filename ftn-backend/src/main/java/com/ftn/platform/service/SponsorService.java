package com.ftn.platform.service;

import com.ftn.platform.dto.SponsorDTO;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorStatus;
import com.ftn.platform.repository.SponsorRepository;
import com.ftn.platform.repository.SponsorshipRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final SponsorRepository sponsorRepository;
    private final SponsorshipRequestRepository requestRepository;

    public List<SponsorDTO> getAllSponsors() {
        return sponsorRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public SponsorDTO getSponsorById(Long id) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Sponsor not found with id: " + id));
        return mapToDTO(sponsor);
    }

    @Transactional
    public SponsorDTO createSponsor(SponsorDTO dto) {
        Sponsor sponsor = mapToEntity(dto);
        return mapToDTO(sponsorRepository.save(sponsor));
    }

    @Transactional
    public SponsorDTO updateSponsor(Long id, SponsorDTO dto) {
        Sponsor existing = sponsorRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Sponsor not found with id: " + id));
        
        existing.setName(dto.name());
        existing.setTier(dto.tier());
        existing.setDescription(dto.description());
        existing.setLogoUrl(dto.logoUrl());
        existing.setWebsite(dto.website());
        existing.setContactEmail(dto.contactEmail());
        existing.setStatus(SponsorStatus.valueOf(dto.status().toUpperCase()));
        // Note: athletesCount, competitionsCount, totalValue are updated separately when requests are approved
        
        return mapToDTO(sponsorRepository.save(existing));
    }

    @Transactional
    public void deleteSponsor(Long id) {
        sponsorRepository.deleteById(id);
    }

    public Map<String, Object> getStats() {
        List<Sponsor> all = sponsorRepository.findAll();
        long totalSponsors = all.size();
        long activeSponsorships = all.stream().filter(s -> s.getStatus() == SponsorStatus.ACTIVE).count();
        double totalValue = all.stream().mapToDouble(Sponsor::getTotalValue).sum();
        int sponsoredAthletes = all.stream().mapToInt(Sponsor::getAthletesCount).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSponsors", totalSponsors);
        stats.put("activeSponsorships", activeSponsorships);
        stats.put("totalValue", totalValue);
        stats.put("sponsoredAthletes", sponsoredAthletes);
        return stats;
    }

    public List<Map<String, Object>> getRevenueTrends() {
        LocalDate startDate = LocalDate.now().withDayOfMonth(1).minusMonths(5);

        // Build ordered 6-month window: [month-5, ..., current month]
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i);
            result.add(new HashMap<>(Map.of(
                "month", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "monthNum", month.getMonthValue(),
                "revenue", 0.0
            )));
        }

        // Overlay with real DB data
        requestRepository.findMonthlyRevenue(startDate).forEach(row -> {
            int monthNum = ((Number) row[1]).intValue();
            double revenue = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            result.stream()
                .filter(m -> ((Number) m.get("monthNum")).intValue() == monthNum)
                .findFirst()
                .ifPresent(m -> m.put("revenue", revenue));
        });

        // Remove helper field before returning
        result.forEach(m -> m.remove("monthNum"));
        return result;
    }

    public List<Map<String, Object>> getSponsorshipGrowth() {
        LocalDate startDate = LocalDate.now().withDayOfMonth(1).minusMonths(5);

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i);
            result.add(new HashMap<>(Map.of(
                "month", month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                "monthNum", month.getMonthValue(),
                "count", 0L
            )));
        }

        sponsorRepository.findMonthlyActiveSponsorCount(startDate).forEach(row -> {
            int monthNum = ((Number) row[1]).intValue();
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            result.stream()
                .filter(m -> ((Number) m.get("monthNum")).intValue() == monthNum)
                .findFirst()
                .ifPresent(m -> m.put("count", count));
        });

        result.forEach(m -> m.remove("monthNum"));
        return result;
    }

    private SponsorDTO mapToDTO(Sponsor s) {
        return new SponsorDTO(
                s.getId(), s.getName(), s.getTier(), s.getDescription(), s.getLogoUrl(),
                s.getWebsite(), s.getContactEmail(), s.getStatus().name(),
                s.getAthletesCount(), s.getCompetitionsCount(),
                s.getStartDate() != null ? s.getStartDate().toString() : null,
                s.getTotalValue(),
                s.getPreferredDisciplines() != null ? parseStringList(s.getPreferredDisciplines()) : List.of()
        );
    }

    private Sponsor mapToEntity(SponsorDTO dto) {
        return Sponsor.builder()
                .name(dto.name())
                .tier(dto.tier())
                .description(dto.description())
                .logoUrl(dto.logoUrl())
                .website(dto.website())
                .contactEmail(dto.contactEmail())
                .preferredDisciplines(dto.preferredDisciplines() != null ? toJson(dto.preferredDisciplines()) : "[]")
                .status(SponsorStatus.valueOf(dto.status().toUpperCase()))
                .build();
    }

    private List<String> parseStringList(String json) {
        try {
            if (json == null || json.isBlank()) return List.of();
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of(json);
        }
    }

    private String toJson(List<String> values) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(values);
        } catch (Exception ex) {
            return "[]";
        }
    }
}
