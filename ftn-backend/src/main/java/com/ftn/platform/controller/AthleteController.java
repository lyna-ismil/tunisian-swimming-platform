package com.ftn.platform.controller;

import com.ftn.platform.entity.*;
import com.ftn.platform.service.AthleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/athletes")
@RequiredArgsConstructor
public class AthleteController {

    private final AthleteService athleteService;

    // Hardcoded user ID for simulation
    private final Long CURRENT_USER_ID = 1L;

    @GetMapping("/profile")
    public Athlete getProfile() {
        return athleteService.getAthleteProfile(CURRENT_USER_ID);
    }

    @PutMapping("/profile")
    public Athlete updateProfile(@RequestBody Athlete athlete) {
        return athleteService.updateProfile(CURRENT_USER_ID, athlete);
    }

    @GetMapping("/{athleteId}/licenses")
    public List<License> getLicenses(@PathVariable Long athleteId) {
        return athleteService.getLicenses(athleteId);
    }

    @GetMapping("/{athleteId}/rankings")
    public List<Ranking> getRankings(@PathVariable Long athleteId) {
        return athleteService.getRankings(athleteId);
    }

    @GetMapping("/{athleteId}/registrations")
    public List<CompetitionRegistration> getRegistrations(@PathVariable Long athleteId) {
        return athleteService.getRegistrations(athleteId);
    }

    @GetMapping("/{athleteId}/results")
    public List<CompetitionResult> getResults(@PathVariable Long athleteId) {
        return athleteService.getResults(athleteId);
    }

    @GetMapping("/notifications")
    public List<Notification> getNotifications() {
        return athleteService.getNotifications(CURRENT_USER_ID);
    }
}
