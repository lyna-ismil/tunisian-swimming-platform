package com.ftn.platform.controller;

import com.ftn.platform.dto.CompetitionEventDTO;
import com.ftn.platform.dto.SponsorshipPackageDTO;
import com.ftn.platform.service.CompetitionEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/events")
@RequiredArgsConstructor
public class CompetitionEventController {

    private final CompetitionEventService eventService;

    @GetMapping
    public List<CompetitionEventDTO> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/{id}")
    public CompetitionEventDTO getEventById(@PathVariable Long id) {
        return eventService.getEventById(id);
    }

    @PostMapping
    public CompetitionEventDTO createEvent(@RequestBody CompetitionEventDTO eventDTO) {
        return eventService.createEvent(eventDTO);
    }

    @PutMapping("/{id}")
    public CompetitionEventDTO updateEvent(@PathVariable Long id, @RequestBody CompetitionEventDTO eventDTO) {
        return eventService.updateEvent(id, eventDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }

    @PostMapping("/{eventId}/packages")
    public SponsorshipPackageDTO addPackage(@PathVariable Long eventId, @RequestBody SponsorshipPackageDTO packageDTO) {
        return eventService.addPackage(eventId, packageDTO);
    }

    @PutMapping("/{eventId}/packages/{packageId}")
    public SponsorshipPackageDTO updatePackage(@PathVariable Long eventId, @PathVariable Long packageId, @RequestBody SponsorshipPackageDTO packageDTO) {
        return eventService.updatePackage(eventId, packageId, packageDTO);
    }

    @DeleteMapping("/{eventId}/packages/{packageId}")
    public void deletePackage(@PathVariable Long eventId, @PathVariable Long packageId) {
        eventService.deletePackage(eventId, packageId);
    }
}
