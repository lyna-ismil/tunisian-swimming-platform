package com.ftn.platform.controller;

import com.ftn.platform.dto.SponsorEventDTO;
import com.ftn.platform.dto.SponsorEventRequestDTO;
import com.ftn.platform.service.SponsorEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors/profiles/{sponsorId}/events")
@RequiredArgsConstructor
public class SponsorEventController {

    private final SponsorEventService sponsorEventService;

    @GetMapping
    public List<SponsorEventDTO> getSponsorEvents(@PathVariable Long sponsorId) {
        return sponsorEventService.getSponsorEvents(sponsorId);
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluateMatch(
            @PathVariable Long sponsorId,
            @RequestBody Map<String, Object> request) {
        Long eventId = Long.valueOf(request.get("eventId").toString());
        String packageType = (String) request.get("packageType");
        Double amount = request.containsKey("sponsorshipAmount") ? Double.valueOf(request.get("sponsorshipAmount").toString()) : null;
        return sponsorEventService.evaluateEvent(sponsorId, eventId, packageType, amount);
    }

    @PostMapping
    public SponsorEventDTO matchEvent(
            @PathVariable Long sponsorId,
            @RequestBody SponsorEventRequestDTO requestDTO) {
        return sponsorEventService.addEventToSponsor(sponsorId, requestDTO);
    }

    @DeleteMapping("/{sponsorEventId}")
    public void removeEvent(@PathVariable Long sponsorId, @PathVariable Long sponsorEventId) {
        sponsorEventService.removeEventFromSponsor(sponsorId, sponsorEventId);
    }
}
