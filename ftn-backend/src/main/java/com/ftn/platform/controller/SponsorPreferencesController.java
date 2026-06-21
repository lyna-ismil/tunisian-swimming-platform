package com.ftn.platform.controller;

import com.ftn.platform.dto.SponsorPreferencesDTO;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorPreferences;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.SponsorPreferencesRepository;
import com.ftn.platform.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/{id}/preferences")
@RequiredArgsConstructor
public class SponsorPreferencesController {

    private final SponsorPreferencesRepository preferencesRepository;
    private final SponsorRepository sponsorRepository;

    @GetMapping
    public SponsorPreferencesDTO getPreferences(@PathVariable Long id) {
        return preferencesRepository.findBySponsorId(id).map(this::toDto).orElseThrow(() -> new EntityNotFoundException("Preferences not found for sponsor: " + id));
    }

    @PostMapping
    public SponsorPreferencesDTO createOrUpdate(@PathVariable Long id, @RequestBody SponsorPreferencesDTO dto) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found with id: " + id));

        SponsorPreferences preferences = preferencesRepository.findBySponsorId(id).orElseGet(SponsorPreferences::new);
        preferences.setSponsor(sponsor);
        applyDto(preferences, sponsor, dto);
        return toDto(preferencesRepository.save(preferences));
    }

    @PutMapping
    public SponsorPreferencesDTO update(@PathVariable Long id, @RequestBody SponsorPreferencesDTO dto) {
        return createOrUpdate(id, dto);
    }

    @DeleteMapping
    public void delete(@PathVariable Long id) {
        preferencesRepository.deleteBySponsorId(id);
    }

    private void applyDto(SponsorPreferences preferences, Sponsor sponsor, SponsorPreferencesDTO dto) {
        preferences.setSponsor(sponsor);
        preferences.setPreferredDisciplines(dto.preferredDisciplines() != null ? dto.preferredDisciplines() : List.of());
        preferences.setMinRankPosition(dto.minRankPosition());
        preferences.setMaxAthleteAge(dto.maxAthleteAge());
        preferences.setTargetGender(dto.targetGender());
        preferences.setMinFINAPoints(dto.minFINAPoints());
        preferences.setGeographicPreference(dto.geographicPreference());
        preferences.setContractValueRangeMin(dto.contractValueRangeMin());
        preferences.setContractValueRangeMax(dto.contractValueRangeMax());
    }

    private SponsorPreferencesDTO toDto(SponsorPreferences preferences) {
        return new SponsorPreferencesDTO(
                preferences.getId(),
                preferences.getSponsor() != null ? preferences.getSponsor().getId() : null,
                preferences.getPreferredDisciplines(),
                preferences.getMinRankPosition(),
                preferences.getMaxAthleteAge(),
                preferences.getTargetGender(),
                preferences.getMinFINAPoints(),
                preferences.getGeographicPreference(),
                preferences.getContractValueRangeMin(),
                preferences.getContractValueRangeMax()
        );
    }
}