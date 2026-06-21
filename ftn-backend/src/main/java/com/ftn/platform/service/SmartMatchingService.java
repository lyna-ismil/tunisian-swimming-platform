package com.ftn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.dto.AthleteMatchDTO;
import com.ftn.platform.dto.MatchGenerationRequestDTO;
import com.ftn.platform.dto.MatchGenerationResponseDTO;
import com.ftn.platform.entity.*;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.exception.RateLimitExceededException;
import com.ftn.platform.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartMatchingService {

    private static final int DEFAULT_MAX_MATCHES_PER_SPONSOR = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    private static final String SYSTEM_PROMPT = "You are the FTN Smart Matching Engine. Analyze the athlete and sponsor profiles below. Respond ONLY in this JSON format: {\"matchScore\":0-100,\"reason\":\"2-3 sentence explanation of why this is a good/poor match\",\"confidence\":\"HIGH|MEDIUM|LOW\",\"keyFactors\":[\"factor 1\",\"factor 2\",\"factor 3\"]}. Consider performance alignment, brand fit, geographic relevance, historical sponsorship success, and financial viability.";

    private final AthleteRepository athleteRepository;
    private final RankingRepository rankingRepository;
    private final CompetitionResultRepository resultRepository;
    private final LicenseRepository licenseRepository;
    private final SponsorRepository sponsorRepository;
    private final SponsorPreferencesRepository sponsorPreferencesRepository;
    private final AthleteMatchRepository matchRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.url:https://api.groq.com/openai/v1}")
    private String groqUrl;

    private final Map<Long, Instant> sponsorGenerationTimestamps = new ConcurrentHashMap<>();
    private WebClient groqClient;

    @PostConstruct
    void init() {
        this.groqClient = webClientBuilder.baseUrl(groqUrl).build();
    }

    @Transactional
    public MatchGenerationResponseDTO generateMatches(MatchGenerationRequestDTO request) {
        List<Sponsor> sponsors = resolveSponsors(request);
        int maxMatches = resolveMaxMatches(request);
        boolean persist = request == null || request.persist() == null || request.persist();

        if (persist) {
            archiveProposedMatchesForSponsors(sponsors);
        }

        List<AthleteMatchDTO> generatedMatches = new ArrayList<>();
        for (Sponsor sponsor : sponsors) {
            generatedMatches.addAll(generateMatchesForSponsorInternal(sponsor, maxMatches, persist));
        }

        return new MatchGenerationResponseDTO(
                sponsors.size(),
                generatedMatches.size(),
                generatedMatches,
                persist ? "Generated and saved smart matches." : "Generated smart match preview."
        );
    }

    @Transactional
    public MatchGenerationResponseDTO regenerateMatches() {
        archiveProposedMatches();
        return generateMatches(new MatchGenerationRequestDTO(null, DEFAULT_MAX_MATCHES_PER_SPONSOR, true));
    }

    @Transactional(readOnly = true)
    public List<AthleteMatchDTO> getSmartSuggestions(Long sponsorId) {
        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found with id: " + sponsorId));
        return generateMatchesForSponsorInternal(sponsor, DEFAULT_MAX_MATCHES_PER_SPONSOR, false);
    }

    @Scheduled(cron = "0 0 2 ? * MON")
    public void regenerateMatchesWeekly() {
        try {
            MatchGenerationResponseDTO response = regenerateMatches();
            log.info("Weekly smart matching job completed: {} sponsors, {} matches", response.sponsorsProcessed(), response.matchesGenerated());
        } catch (Exception ex) {
            log.error("Weekly smart matching job failed", ex);
        }
    }

    private List<Sponsor> resolveSponsors(MatchGenerationRequestDTO request) {
        if (request != null && request.sponsorIds() != null && !request.sponsorIds().isEmpty()) {
            return sponsorRepository.findAllById(request.sponsorIds());
        }
        return sponsorRepository.findByStatus(SponsorStatus.ACTIVE);
    }

    private int resolveMaxMatches(MatchGenerationRequestDTO request) {
        if (request == null || request.maxMatchesPerSponsor() == null || request.maxMatchesPerSponsor() < 1) {
            return DEFAULT_MAX_MATCHES_PER_SPONSOR;
        }
        return request.maxMatchesPerSponsor();
    }

    private List<AthleteMatchDTO> generateMatchesForSponsorInternal(Sponsor sponsor, int maxMatches, boolean persist) {
        ensureGenerationAllowed(sponsor.getId());

        SponsorPreferences preferences = sponsorPreferencesRepository.findBySponsorId(sponsor.getId()).orElse(null);
        SponsorProfile sponsorProfile = buildSponsorProfile(sponsor, preferences);
        List<CandidateMatch> candidates = buildCandidates(sponsorProfile, preferences, maxMatches);

        List<AthleteMatchDTO> generated = new ArrayList<>();
        for (CandidateMatch candidate : candidates) {
            AiMatchResponse aiResponse = generateAiMatch(candidate.athleteProfile(), sponsorProfile);
            if (aiResponse == null) {
                aiResponse = buildFallbackResponse(candidate, sponsorProfile);
            }

            AthleteMatch match = buildMatchEntity(candidate.athleteProfile(), sponsorProfile, aiResponse);
            if (persist) {
                match = matchRepository.save(match);
            }
            generated.add(mapToDTO(match));
        }

        return generated;
    }

    private List<CandidateMatch> buildCandidates(SponsorProfile sponsorProfile, SponsorPreferences preferences, int maxMatches) {
        List<AthleteProfile> athletes = buildAthleteProfiles();
        return athletes.stream()
                .map(athlete -> new CandidateMatch(athlete, calculateFallbackScore(athlete, sponsorProfile, preferences)))
                .filter(candidate -> isEligible(candidate.athleteProfile(), preferences))
                .sorted(Comparator.comparingInt(CandidateMatch::ruleScore).reversed())
                .limit(Math.max(maxMatches * 2L, maxMatches))
                .collect(Collectors.toList());
    }

    private boolean isEligible(AthleteProfile athlete, SponsorPreferences preferences) {
        if (preferences == null) {
            return true;
        }

        if (preferences.getTargetGender() != null && !preferences.getTargetGender().isBlank()) {
            if (athlete.gender() == null || !preferences.getTargetGender().equalsIgnoreCase(athlete.gender())) {
                return false;
            }
        }

        if (preferences.getMaxAthleteAge() != null && athlete.age() != null && athlete.age() > preferences.getMaxAthleteAge()) {
            return false;
        }

        if (preferences.getMinRankPosition() != null && athlete.currentRank() != null && athlete.currentRank() > preferences.getMinRankPosition()) {
            return false;
        }

        if (preferences.getMinFINAPoints() != null && athlete.finaPoints() != null && athlete.finaPoints() < preferences.getMinFINAPoints()) {
            return false;
        }

        return true;
    }

    private List<AthleteProfile> buildAthleteProfiles() {
        return athleteRepository.findAll().stream()
                .map(this::buildAthleteProfile)
                .toList();
    }

    private AthleteProfile buildAthleteProfile(Athlete athlete) {
        List<Ranking> rankings = rankingRepository.findByAthleteIdOrderByUpdateDateDesc(athlete.getId());
        List<CompetitionResult> results = resultRepository.findByAthleteId(athlete.getId()).stream()
                .sorted(Comparator.comparingLong(CompetitionResult::getId).reversed())
                .toList();
        List<License> licenses = licenseRepository.findByAthleteId(athlete.getId());

        Ranking latestRanking = rankings.isEmpty() ? null : rankings.get(0);
        License latestLicense = licenses.stream()
                .max(Comparator.comparing(license -> Objects.toString(license.getValidTo(), "")))
                .orElse(null);
        List<ResultSummary> recentResults = results.stream()
                .limit(3)
                .map(result -> new ResultSummary(result.getEventName(), result.getRecordTime(), result.getRankPosition(), result.getPoints()))
                .toList();

        return new AthleteProfile(
                athlete.getId(),
                athlete.getFullName(),
                athlete.getDateOfBirth() != null ? Period.between(athlete.getDateOfBirth(), LocalDate.now()).getYears() : null,
                latestRanking != null ? latestRanking.getCategory() : inferDiscipline(results),
                latestRanking != null ? latestRanking.getRankPosition() : null,
                latestRanking != null ? latestRanking.getPoints() : null,
                recentResults,
                latestLicense != null ? latestLicense.getStatus() : null,
                athlete.getClubAffiliation(),
                calculateTrend(results, rankings),
                athlete.getGender(),
                athlete.getNationality(),
                athlete.getPhotoUrl()
        );
    }

    private String inferDiscipline(List<CompetitionResult> results) {
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0).getEventName();
    }

    private String calculateTrend(List<CompetitionResult> results, List<Ranking> rankings) {
        List<Integer> points = new ArrayList<>();
        rankings.stream().limit(3).map(Ranking::getPoints).filter(Objects::nonNull).forEach(points::add);
        if (points.size() < 2) {
            results.stream().limit(3).map(CompetitionResult::getPoints).filter(Objects::nonNull).forEach(points::add);
        }

        if (points.size() < 2) {
            return "stable";
        }

        int latest = points.get(0);
        int previous = points.get(1);
        if (latest > previous + 10) {
            return "improving";
        }
        if (latest < previous - 10) {
            return "declining";
        }
        return "stable";
    }

    private SponsorProfile buildSponsorProfile(Sponsor sponsor, SponsorPreferences preferences) {
        List<String> preferredDisciplines = new ArrayList<>();
        if (sponsor.getPreferredDisciplines() != null && !sponsor.getPreferredDisciplines().isBlank()) {
            try {
                preferredDisciplines.addAll(objectMapper.readValue(sponsor.getPreferredDisciplines(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
            } catch (JsonProcessingException ignored) {
                preferredDisciplines.add(sponsor.getPreferredDisciplines());
            }
        }
        if (preferences != null && preferences.getPreferredDisciplines() != null) {
            preferredDisciplines.addAll(preferences.getPreferredDisciplines());
        }

        Set<String> historicalDisciplines = matchRepository.findBySponsorId(sponsor.getId()).stream()
                .map(AthleteMatch::getDiscipline)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new SponsorProfile(
                sponsor.getId(),
                sponsor.getName(),
                sponsor.getTier(),
                sponsor.getTotalValue(),
                sponsor.getStatus() != null ? sponsor.getStatus().name() : null,
                preferredDisciplines.stream().distinct().toList(),
                new ArrayList<>(historicalDisciplines),
                sponsor.getAthletesCount(),
                preferences != null ? preferences.getMinRankPosition() : null,
                preferences != null ? preferences.getMaxAthleteAge() : null,
                preferences != null ? preferences.getTargetGender() : null,
                preferences != null ? preferences.getMinFINAPoints() : null,
                preferences != null ? preferences.getGeographicPreference() : null,
                preferences != null ? preferences.getContractValueRangeMin() : null,
                preferences != null ? preferences.getContractValueRangeMax() : null,
                sponsor.getLogoUrl()
        );
    }

    private AiMatchResponse generateAiMatch(AthleteProfile athleteProfile, SponsorProfile sponsorProfile) {
        try {
            Map<String, Object> userPayload = new LinkedHashMap<>();
            userPayload.put("athlete", athleteProfile);
            userPayload.put("sponsor", sponsorProfile);

            Map<String, Object> systemMessage = Map.of("role", "system", "content", SYSTEM_PROMPT);
            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", "ATHLETE PROFILE:\n" + objectMapper.writeValueAsString(athleteProfile) + "\nSPONSOR PROFILE:\n" + objectMapper.writeValueAsString(sponsorProfile) + "\nRespond ONLY with JSON."
            );

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 300);

            JsonNode response = groqClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                return null;
            }

            String content = response.path("choices").path(0).path("message").path("content").asText();
            return parseAiResponse(content);
        } catch (Exception ex) {
            log.warn("Groq matching request failed for athlete {} and sponsor {}", athleteProfile.name(), sponsorProfile.name(), ex);
            return null;
        }
    }

    private AiMatchResponse parseAiResponse(String content) {
        try {
            String json = extractJson(content);
            JsonNode node = objectMapper.readTree(json);
            List<String> keyFactors = new ArrayList<>();
            if (node.has("keyFactors") && node.get("keyFactors").isArray()) {
                node.get("keyFactors").forEach(item -> keyFactors.add(item.asText()));
            }
            return new AiMatchResponse(
                    clampScore(node.path("matchScore").asInt(0)),
                    node.path("reason").asText("Compatibility is based on ranking, discipline fit, and sponsor needs."),
                    node.path("confidence").asText("MEDIUM"),
                    keyFactors.isEmpty() ? List.of("discipline fit", "performance trend", "sponsor budget") : keyFactors
            );
        } catch (Exception ex) {
            log.warn("Failed to parse Groq response: {}", content, ex);
            return null;
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return "{}";
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private AiMatchResponse buildFallbackResponse(CandidateMatch candidate, SponsorProfile sponsorProfile) {
        int score = candidate.ruleScore();
        List<String> factors = new ArrayList<>();
        if (candidate.athleteProfile().discipline() != null && sponsorProfile.preferredDisciplines().stream().anyMatch(d -> d.equalsIgnoreCase(candidate.athleteProfile().discipline()))) {
            factors.add("discipline alignment");
        }
        if (candidate.athleteProfile().performanceTrend() != null) {
            factors.add("performance trend is " + candidate.athleteProfile().performanceTrend());
        }
        if (candidate.athleteProfile().licenseStatus() != null) {
            factors.add("license status " + candidate.athleteProfile().licenseStatus());
        }
        if (factors.isEmpty()) {
            factors.add("overall profile compatibility");
        }

        return new AiMatchResponse(
                score,
                score >= 75
                        ? "The athlete aligns well with the sponsor's discipline and performance expectations. The brand fit is strong and the athlete has a competitive profile."
                        : "The fit is moderate and may need stronger discipline or performance alignment before outreach.",
                score >= 90 ? "HIGH" : score >= 75 ? "MEDIUM" : "LOW",
                factors.stream().distinct().limit(3).toList()
        );
    }

    private int calculateFallbackScore(AthleteProfile athlete, SponsorProfile sponsor, SponsorPreferences preferences) {
        int score = 35;

        if (athlete.finaPoints() != null) {
            score += Math.min(25, athlete.finaPoints() / 40);
        }

        if (athlete.currentRank() != null) {
            score += Math.max(0, 20 - Math.min(20, athlete.currentRank()));
        }

        if (athlete.discipline() != null && sponsor.preferredDisciplines().stream().anyMatch(d -> d.equalsIgnoreCase(athlete.discipline()))) {
            score += 18;
        }

        if (athlete.discipline() != null && sponsor.historicalDisciplines().stream().anyMatch(d -> d.equalsIgnoreCase(athlete.discipline()))) {
            score += 12;
        }

        if (athlete.performanceTrend() != null) {
            switch (athlete.performanceTrend()) {
                case "improving" -> score += 10;
                case "declining" -> score -= 5;
                default -> score += 4;
            }
        }

        if (preferences != null) {
            if (preferences.getTargetGender() != null && athlete.gender() != null && preferences.getTargetGender().equalsIgnoreCase(athlete.gender())) {
                score += 8;
            }
            if (preferences.getMaxAthleteAge() != null && athlete.age() != null && athlete.age() <= preferences.getMaxAthleteAge()) {
                score += 8;
            }
            if (preferences.getMinFINAPoints() != null && athlete.finaPoints() != null && athlete.finaPoints() >= preferences.getMinFINAPoints()) {
                score += 8;
            }
            if (preferences.getGeographicPreference() != null && athlete.clubAffiliation() != null && athlete.clubAffiliation().toLowerCase().contains(preferences.getGeographicPreference().toLowerCase())) {
                score += 7;
            }
        }

        if ("ACTIVE".equalsIgnoreCase(sponsor.status())) {
            score += 5;
        }

        return clampScore(score);
    }

    private AthleteMatch buildMatchEntity(AthleteProfile athleteProfile, SponsorProfile sponsorProfile, AiMatchResponse aiResponse) {
        try {
            return AthleteMatch.builder()
                    .athlete(athleteRepository.findById(athleteProfile.id()).orElse(null))
                    .sponsor(sponsorRepository.findById(sponsorProfile.id()).orElse(null))
                    .athleteName(athleteProfile.name())
                    .athletePhotoUrl(athleteProfile.photoUrl())
                    .discipline(athleteProfile.discipline())
                    .rank(athleteProfile.currentRank())
                    .suggestedSponsor(sponsorProfile.name())
                    .sponsorLogoUrl(sponsorProfile.logoUrl())
                    .matchScore(aiResponse.matchScore())
                    .confidence(aiResponse.confidence())
                    .reason(aiResponse.reason())
                    .keyFactorsJson(objectMapper.writeValueAsString(aiResponse.keyFactors()))
                    .status(MatchStatus.PROPOSED)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize match factors", e);
        }
    }

    @Transactional
    protected void archiveProposedMatches() {
        matchRepository.findByStatus(MatchStatus.PROPOSED).forEach(match -> {
            match.setStatus(MatchStatus.DISMISSED);
            matchRepository.save(match);
        });
    }

    @Transactional
    protected void archiveProposedMatchesForSponsors(List<Sponsor> sponsors) {
        Set<Long> sponsorIds = sponsors.stream().map(Sponsor::getId).collect(Collectors.toSet());
        matchRepository.findByStatus(MatchStatus.PROPOSED).stream()
                .filter(match -> match.getSponsor() != null && sponsorIds.contains(match.getSponsor().getId()))
                .forEach(match -> {
                    match.setStatus(MatchStatus.DISMISSED);
                    matchRepository.save(match);
                });
    }

    private void ensureGenerationAllowed(Long sponsorId) {
        Instant now = Instant.now();
        Instant lastGeneration = sponsorGenerationTimestamps.get(sponsorId);
        if (lastGeneration != null && Duration.between(lastGeneration, now).compareTo(RATE_LIMIT_WINDOW) < 0) {
            throw new RateLimitExceededException("Matching generation is limited to once per minute for each sponsor.");
        }
        sponsorGenerationTimestamps.put(sponsorId, now);
    }

    private AthleteMatchDTO mapToDTO(AthleteMatch match) {
        return new AthleteMatchDTO(
                match.getId(),
                match.getAthlete() != null ? match.getAthlete().getId() : null,
                match.getSponsor() != null ? match.getSponsor().getId() : null,
                match.getAthleteName(),
                match.getAthletePhotoUrl(),
                match.getDiscipline(),
                match.getRank(),
                match.getSuggestedSponsor(),
                match.getSponsorLogoUrl(),
                match.getMatchScore(),
                match.getScoreColor(),
                match.getConfidence(),
                match.getReason(),
                parseKeyFactors(match.getKeyFactorsJson()),
                match.getStatus() != null ? match.getStatus().name() : null
        );
    }

    private List<String> parseKeyFactors(String keyFactorsJson) {
        if (keyFactorsJson == null || keyFactorsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(keyFactorsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of(keyFactorsJson);
        }
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private record CandidateMatch(AthleteProfile athleteProfile, int ruleScore) {}

    private record ResultSummary(String eventName, String recordTime, Integer rankPosition, Integer points) {}

    private record AthleteProfile(
            Long id,
            String name,
            Integer age,
            String discipline,
            Integer currentRank,
            Integer finaPoints,
            List<ResultSummary> recentResults,
            String licenseStatus,
            String clubAffiliation,
            String performanceTrend,
            String gender,
            String nationality,
            String photoUrl
    ) {}

    private record SponsorProfile(
            Long id,
            String name,
            String tier,
            Double budget,
            String status,
            List<String> preferredDisciplines,
            List<String> historicalDisciplines,
            Integer athletesCount,
            Integer minRankPosition,
            Integer maxAthleteAge,
            String targetGender,
            Integer minFINAPoints,
            String geographicPreference,
            Double contractValueRangeMin,
            Double contractValueRangeMax,
            String logoUrl
    ) {}

    private record AiMatchResponse(Integer matchScore, String reason, String confidence, List<String> keyFactors) {}
}