package com.ftn.platform.controller;

import com.ftn.platform.dto.SponsorDTO;
import com.ftn.platform.service.SponsorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors/profiles")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping
    public List<SponsorDTO> getAllSponsors() {
        return sponsorService.getAllSponsors();
    }

    @GetMapping("/{id}")
    public SponsorDTO getSponsorById(@PathVariable Long id) {
        return sponsorService.getSponsorById(id);
    }

    @PostMapping
    public SponsorDTO createSponsor(@Valid @RequestBody SponsorDTO sponsorDTO) {
        return sponsorService.createSponsor(sponsorDTO);
    }

    @PutMapping("/{id}")
    public SponsorDTO updateSponsor(@PathVariable Long id, @Valid @RequestBody SponsorDTO sponsorDTO) {
        return sponsorService.updateSponsor(id, sponsorDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteSponsor(@PathVariable Long id) {
        sponsorService.deleteSponsor(id);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return sponsorService.getStats();
    }

    @GetMapping("/analytics/revenue-trends")
    public List<Map<String, Object>> getRevenueTrends() {
        return sponsorService.getRevenueTrends();
    }

    @GetMapping("/analytics/growth")
    public List<Map<String, Object>> getSponsorshipGrowth() {
        return sponsorService.getSponsorshipGrowth();
    }
}
