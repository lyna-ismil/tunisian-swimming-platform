package com.ftn.platform.controller;

import com.ftn.platform.dto.AthleteMatchDTO;
import com.ftn.platform.dto.MatchGenerationRequestDTO;
import com.ftn.platform.dto.MatchGenerationResponseDTO;
import com.ftn.platform.service.AthleteMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/matches")
@RequiredArgsConstructor
public class AthleteMatchController {

    private final AthleteMatchService matchService;

    @GetMapping
    public List<AthleteMatchDTO> getAllMatches() {
        return matchService.getAllMatches();
    }

    @PostMapping
    public AthleteMatchDTO createMatch(@RequestBody AthleteMatchDTO matchDTO) {
        return matchService.createMatch(matchDTO);
    }

    @PutMapping("/{id}/propose")
    public AthleteMatchDTO proposeMatch(@PathVariable Long id) {
        return matchService.proposeMatch(id);
    }

    @PutMapping("/{id}/dismiss")
    public AthleteMatchDTO dismissMatch(@PathVariable Long id) {
        return matchService.dismissMatch(id);
    }

    @DeleteMapping("/{id}")
    public void deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
    }

    @PostMapping("/generate")
    public MatchGenerationResponseDTO generateMatches(@RequestBody(required = false) MatchGenerationRequestDTO request) {
        return matchService.generateMatches(request);
    }

    @PostMapping("/regenerate")
    public MatchGenerationResponseDTO regenerateMatches() {
        return matchService.regenerateMatches();
    }

    @GetMapping("/smart-suggestions")
    public List<AthleteMatchDTO> getSmartSuggestions(@RequestParam Long sponsorId) {
        return matchService.getSmartSuggestions(sponsorId);
    }
}
