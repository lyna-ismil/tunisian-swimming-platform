package com.ftn.platform.service;

import com.ftn.platform.dto.SponsorDTO;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorStatus;
import com.ftn.platform.repository.SponsorRepository;
import com.ftn.platform.repository.SponsorshipRequestRepository;
import com.ftn.platform.repository.SponsorContractRepository;
import com.ftn.platform.entity.ContractType;
import com.ftn.platform.entity.ContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final SponsorContractRepository contractRepository;

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
        if (dto.totalValue() != null && dto.totalValue() < 0) {
            throw new IllegalArgumentException("Sponsor total value must be greater than or equal to 0");
        }
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
        
        if (dto.startDate() != null && !dto.startDate().isBlank()) {
            existing.setStartDate(LocalDate.parse(dto.startDate()));
        } else {
            existing.setStartDate(null);
        }
        
        if (dto.endDate() != null && !dto.endDate().isBlank()) {
            existing.setEndDate(LocalDate.parse(dto.endDate()));
        } else {
            existing.setEndDate(null);
        }
        
        if (dto.totalValue() != null) {
            if (dto.totalValue() < 0) {
                throw new IllegalArgumentException("Sponsor total value must be greater than or equal to 0");
            }
            existing.setTotalValue(dto.totalValue());
        }
        if (dto.athletesCount() != null) existing.setAthletesCount(dto.athletesCount());
        if (dto.competitionsCount() != null) existing.setCompetitionsCount(dto.competitionsCount());
        if (dto.preferredDisciplines() != null) existing.setPreferredDisciplines(toJson(dto.preferredDisciplines()));
        
        Sponsor saved = sponsorRepository.save(existing);
        return mapToDTO(saved);
    }

    @Transactional
    public void recalculateSponsorStats(Long sponsorId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId).orElse(null);
        if (sponsor == null) return;
        
        BigDecimal totalValue = contractRepository.sumActiveContractValues(sponsorId);
        long athleteCount = contractRepository.countActiveContractsByType(sponsorId, ContractType.ATHLETE);
        long competitionCount = contractRepository.countActiveContractsByType(sponsorId, ContractType.COMPETITION);

        if (totalValue != null && totalValue.compareTo(BigDecimal.ZERO) > 0) {
            sponsor.setTotalValue(totalValue.doubleValue());
        } else if (sponsor.getTotalValue() == null) {
            sponsor.setTotalValue(0.0);
        }
        
        sponsor.setAthletesCount((int) athleteCount);
        sponsor.setCompetitionsCount((int) competitionCount);
        sponsorRepository.save(sponsor);
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
        long activeContractsCount = contractRepository.countByStatus(ContractStatus.ACTIVE);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSponsors", totalSponsors);
        stats.put("activeSponsorships", activeSponsorships);
        stats.put("totalValue", totalValue);
        stats.put("sponsoredAthletes", sponsoredAthletes);
        stats.put("activeContractsCount", activeContractsCount);
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
                s.getEndDate() != null ? s.getEndDate().toString() : null,
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
