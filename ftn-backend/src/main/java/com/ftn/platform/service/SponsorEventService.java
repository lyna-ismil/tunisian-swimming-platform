package com.ftn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.dto.SponsorEventDTO;
import com.ftn.platform.dto.SponsorEventRequestDTO;
import com.ftn.platform.entity.CompetitionEvent;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorEvent;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.CompetitionEventRepository;
import com.ftn.platform.repository.SponsorEventRepository;
import com.ftn.platform.repository.SponsorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorEventService {

    private final SponsorEventRepository sponsorEventRepository;
    private final SponsorRepository sponsorRepository;
    private final CompetitionEventRepository competitionEventRepository;
    private final SponsorshipAIEvaluationService aiEvaluationService;
    private final ObjectMapper objectMapper;

    public List<SponsorEventDTO> getSponsorEvents(Long sponsorId) {
        return sponsorEventRepository.findBySponsorId(sponsorId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> evaluateEvent(Long sponsorId, Long eventId, String packageType, Double amount) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));
        CompetitionEvent event = competitionEventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        String aiEvaluationJson = aiEvaluationService.evaluateSponsorship(sponsor, event, packageType, amount);
        
        try {
            return objectMapper.readValue(aiEvaluationJson, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of("error", "Failed to parse AI evaluation");
        }
    }

    @Transactional
    public SponsorEventDTO addEventToSponsor(Long sponsorId, SponsorEventRequestDTO dto) {
        if (sponsorEventRepository.existsBySponsorIdAndEventId(sponsorId, dto.eventId())) {
            throw new RuntimeException("Sponsor is already linked to this event");
        }

        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));
        CompetitionEvent event = competitionEventRepository.findById(dto.eventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        SponsorEvent sponsorEvent = SponsorEvent.builder()
                .sponsor(sponsor)
                .event(event)
                .sponsorshipAmount(dto.sponsorshipAmount())
                .packageType(dto.packageType())
                .matchedAt(LocalDate.now())
                .status("ACTIVE")
                .aiEvaluation(dto.aiEvaluation())
                .build();

        return mapToDTO(sponsorEventRepository.save(sponsorEvent));
    }

    @Transactional
    public void removeEventFromSponsor(Long sponsorId, Long sponsorEventId) {
        SponsorEvent event = sponsorEventRepository.findById(sponsorEventId)
                .orElseThrow(() -> new EntityNotFoundException("SponsorEvent not found"));
                
        if (!event.getSponsor().getId().equals(sponsorId)) {
            throw new RuntimeException("SponsorEvent does not belong to this sponsor");
        }
        
        sponsorEventRepository.delete(event);
    }

    private SponsorEventDTO mapToDTO(SponsorEvent event) {
        Map<String, Object> aiMap = null;
        if (event.getAiEvaluation() != null) {
            try {
                aiMap = objectMapper.readValue(event.getAiEvaluation(), Map.class);
            } catch (Exception ignored) {}
        }
        
        return new SponsorEventDTO(
                event.getId(),
                event.getSponsor().getId(),
                event.getSponsor().getName(),
                event.getEvent().getId(),
                event.getEvent().getName(),
                event.getEvent().getEventDate(),
                event.getEvent().getLocation(),
                event.getSponsorshipAmount(),
                event.getPackageType(),
                event.getMatchedAt() != null ? event.getMatchedAt().toString() : null,
                event.getStatus(),
                aiMap
        );
    }
}
