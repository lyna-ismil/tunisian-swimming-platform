package com.ftn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.dto.AthleteMatchDTO;
import com.ftn.platform.dto.MatchGenerationRequestDTO;
import com.ftn.platform.dto.MatchGenerationResponseDTO;
import com.ftn.platform.entity.AthleteMatch;
import com.ftn.platform.entity.MatchStatus;
import com.ftn.platform.repository.AthleteMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AthleteMatchService {

    private final AthleteMatchRepository matchRepository;
    private final SmartMatchingService smartMatchingService;
    private final ObjectMapper objectMapper;

    public List<AthleteMatchDTO> getAllMatches() {
        return matchRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public AthleteMatchDTO createMatch(AthleteMatchDTO dto) {
        AthleteMatch match = AthleteMatch.builder()
                .athleteName(dto.athleteName())
                .discipline(dto.discipline())
                .rank(dto.rank())
                .suggestedSponsor(dto.suggestedSponsor())
                .matchScore(dto.matchScore())
                .reason(dto.reason())
                .status(MatchStatus.PROPOSED)
                .build();
        return mapToDTO(matchRepository.save(match));
    }

    @Transactional
    public AthleteMatchDTO proposeMatch(Long id) {
        AthleteMatch match = matchRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Match not found: " + id));
        match.setStatus(MatchStatus.CONTACTED);
        return mapToDTO(matchRepository.save(match));
    }

    @Transactional
    public AthleteMatchDTO dismissMatch(Long id) {
        AthleteMatch match = matchRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Match not found: " + id));
        match.setStatus(MatchStatus.DISMISSED);
        return mapToDTO(matchRepository.save(match));
    }

    @Transactional
    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }

    @Transactional
    public MatchGenerationResponseDTO generateMatches(MatchGenerationRequestDTO request) {
        return smartMatchingService.generateMatches(request);
    }

    @Transactional
    public MatchGenerationResponseDTO regenerateMatches() {
        return smartMatchingService.regenerateMatches();
    }

    public List<AthleteMatchDTO> getSmartSuggestions(Long sponsorId) {
        return smartMatchingService.getSmartSuggestions(sponsorId);
    }

    private AthleteMatchDTO mapToDTO(AthleteMatch m) {
        return new AthleteMatchDTO(
                m.getId(),
                m.getAthlete() != null ? m.getAthlete().getId() : null,
                m.getSponsor() != null ? m.getSponsor().getId() : null,
                m.getAthleteName(),
                m.getAthletePhotoUrl(),
                m.getDiscipline(),
                m.getRank(),
                m.getSuggestedSponsor(),
                m.getSponsorLogoUrl(),
                m.getMatchScore(),
                m.getScoreColor(),
                m.getConfidence(),
                m.getReason(),
                m.getKeyFactorsJson() != null ? parseKeyFactors(m.getKeyFactorsJson()) : java.util.List.of(),
                m.getStatus().name()
        );
    }

    private List<String> parseKeyFactors(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of(json);
        }
    }
}
