package com.ftn.platform.controller;

import com.ftn.platform.dto.AthleteMatchDTO;
import com.ftn.platform.dto.EvaluateMatchRequestDTO;
import com.ftn.platform.dto.MatchEvaluationResponseDTO;
import com.ftn.platform.service.AthleteMatchService;
import com.ftn.platform.service.SmartMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors/matches")
@RequiredArgsConstructor
public class AthleteMatchController {

    private final AthleteMatchService matchService;
    private final SmartMatchingService smartMatchingService;

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

    @PostMapping("/evaluate")
    public MatchEvaluationResponseDTO evaluateMatch(@RequestBody EvaluateMatchRequestDTO request) {
        return matchService.evaluateMatch(request);
    }

    @GetMapping("/athletes")
    public List<SmartMatchingService.AthleteProfile> getAthletePool(
            @RequestParam(required = false) String discipline,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) Integer rankMax,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String club) {
        return smartMatchingService.getAthletePool(discipline, ageMin, ageMax, rankMax, gender, club);
    }
}
