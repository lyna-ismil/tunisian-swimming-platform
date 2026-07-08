package com.ftn.platform.service;

import com.ftn.platform.dto.SponsorshipRequestDTO;
import com.ftn.platform.entity.RequestStatus;
import com.ftn.platform.entity.Sponsor;
import com.ftn.platform.entity.SponsorshipRequest;
import com.ftn.platform.repository.SponsorRepository;
import com.ftn.platform.repository.SponsorshipRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorshipRequestService {

    private final SponsorshipRequestRepository requestRepository;
    private final SponsorRepository sponsorRepository;
    private final SponsorService sponsorService;
    private final AtomicInteger counter = new AtomicInteger(100);

    public List<SponsorshipRequestDTO> getRequests(String status, String type, String priority, String search) {
        List<SponsorshipRequest> requests;
        
        if (search != null && !search.trim().isEmpty()) {
            requests = requestRepository.findByApplicantContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(search, search);
        } else {
            requests = requestRepository.findAll();
        }

        return requests.stream()
                .filter(r -> status == null || r.getStatus().name().equalsIgnoreCase(status))
                .filter(r -> type == null || r.getType().equalsIgnoreCase(type))
                .filter(r -> priority == null || r.getPriority().equalsIgnoreCase(priority))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SponsorshipRequestDTO getRequestById(String id) {
        SponsorshipRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Request not found with id: " + id));
        return mapToDTO(request);
    }

    @Transactional
    public SponsorshipRequestDTO createRequest(SponsorshipRequestDTO dto) {
        SponsorshipRequest request = SponsorshipRequest.builder()
                .id(String.format("REQ-%03d", counter.incrementAndGet()))
                .applicant(dto.applicant())
                .applicantLogo(dto.applicantLogo())
                .type(dto.type())
                .sponsorName(dto.sponsorName())
                .requestDate(LocalDate.now())
                .amount(dto.amount())
                .priority(dto.priority())
                .status(RequestStatus.PENDING)
                .build();
        return mapToDTO(requestRepository.save(request));
    }

    @Transactional
    public SponsorshipRequestDTO updateRequest(String id, SponsorshipRequestDTO dto) {
        SponsorshipRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new com.ftn.platform.exception.EntityNotFoundException("Request not found with id: " + id));
        
        RequestStatus oldStatus = request.getStatus();
        RequestStatus newStatus = RequestStatus.valueOf(dto.status().toUpperCase());
        
        request.setStatus(newStatus);
        SponsorshipRequest saved = requestRepository.save(request);
        
        // Always trigger recalculation on status change
        if (oldStatus != newStatus) {
            updateSponsorStatsForApproval(saved);
        }
        
        return mapToDTO(saved);
    }

    private void updateSponsorStatsForApproval(SponsorshipRequest request) {
        // Find sponsor by name
        sponsorRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(request.getSponsorName()))
                .findFirst()
                .ifPresent(sponsor -> sponsorService.recalculateSponsorStats(sponsor.getId()));
    }

    @Transactional
    public void deleteRequest(String id) {
        requestRepository.findById(id).ifPresent(request -> {
            requestRepository.deleteById(id);
            updateSponsorStatsForApproval(request);
        });
    }

    public Map<String, Object> getStats() {
        long total = requestRepository.count();
        long pending = requestRepository.countByStatus(RequestStatus.PENDING);
        long approved = requestRepository.countByStatus(RequestStatus.APPROVED);
        long rejected = requestRepository.countByStatus(RequestStatus.REJECTED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        return stats;
    }

    private SponsorshipRequestDTO mapToDTO(SponsorshipRequest r) {
        return new SponsorshipRequestDTO(
                r.getId(), r.getApplicant(), r.getApplicantLogo(), r.getType(),
                r.getSponsorName(), r.getRequestDate() != null ? r.getRequestDate().toString() : null,
                r.getAmount(), r.getPriority(), r.getStatus().name()
        );
    }
}
