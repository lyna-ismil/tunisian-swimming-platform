package com.ftn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.dto.MatchEvaluationResponseDTO;
import com.ftn.platform.entity.*;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SmartMatchingService {

    private static final String SYSTEM_PROMPT = """
            You are the FTN Smart Sponsorship Analyst. Your job is to evaluate how well a specific athlete fits a specific sponsor.
            
            ATHLETE PROFILE:
            %s
            
            SPONSOR PROFILE:
            %s
            
            SPONSOR PREFERENCES:
            %s
            
            RULE-BASED SCORE: %d
            
            Respond ONLY with valid JSON in this exact structure:
            {
              "matchScore": [0-100, can differ slightly from rule-based score based on nuance],
              "verdict": "Strong Fit" | "Moderate Fit" | "Poor Fit",
              "confidence": "HIGH" | "MEDIUM" | "LOW",
              "explanation": "2-3 sentences",
              "strengths": ["3-5 bullet points"],
              "weaknesses": ["2-4 bullet points, or empty array"],
              "potentialROI": {
                "projectedValue": [number],
                "currency": "TND",
                "explanation": "2 sentences"
              },
              "audienceOverlap": {
                "score": [0-100],
                "explanation": "2 sentences"
              },
              "keyFactors": [
                {"name": "...", "score": 0-100, "weight": 0.0}
              ]
            }
            
            Guidelines:
            - Be specific. Reference actual athlete achievements and sponsor goals.
            - If you lack data, infer reasonably and flag confidence as MEDIUM or LOW.
            - Use numbers and percentages where possible.
            - Keep explanations concise but insightful.
            - Strengths should outweigh weaknesses for scores >75.
            """;

    private final AthleteRepository athleteRepository;
    private final RankingRepository rankingRepository;
    private final CompetitionResultRepository resultRepository;
    private final LicenseRepository licenseRepository;
    private final SponsorRepository sponsorRepository;
    private final SponsorPreferencesRepository sponsorPreferencesRepository;
    private final AthleteMatchRepository matchRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    private final String apiKey;
    private final String model;
    private final String groqUrl;
    private final WebClient groqClient;

    public SmartMatchingService(
            AthleteRepository athleteRepository,
            RankingRepository rankingRepository,
            CompetitionResultRepository resultRepository,
            LicenseRepository licenseRepository,
            SponsorRepository sponsorRepository,
            SponsorPreferencesRepository sponsorPreferencesRepository,
            AthleteMatchRepository matchRepository,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model,
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String groqUrl) {
        this.athleteRepository = athleteRepository;
        this.rankingRepository = rankingRepository;
        this.resultRepository = resultRepository;
        this.licenseRepository = licenseRepository;
        this.sponsorRepository = sponsorRepository;
        this.sponsorPreferencesRepository = sponsorPreferencesRepository;
        this.matchRepository = matchRepository;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.groqUrl = groqUrl;
        this.groqClient = webClientBuilder.baseUrl(groqUrl).build();
    }

    public List<AthleteProfile> getAthletePool(String discipline, Integer ageMin, Integer ageMax, Integer rankMax, String gender, String club) {
        return buildAthleteProfiles().stream()
                .filter(a -> discipline == null || discipline.equalsIgnoreCase(a.discipline()))
                .filter(a -> ageMin == null || (a.age() != null && a.age() >= ageMin))
                .filter(a -> ageMax == null || (a.age() != null && a.age() <= ageMax))
                .filter(a -> rankMax == null || (a.currentRank() != null && a.currentRank() <= rankMax))
                .filter(a -> gender == null || gender.equalsIgnoreCase(a.gender()))
                .filter(a -> club == null || (a.clubAffiliation() != null && a.clubAffiliation().toLowerCase().contains(club.toLowerCase())))
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchEvaluationResponseDTO evaluateAthleteForSponsor(Long sponsorId, Long athleteId) {
        List<AthleteMatch> existingMatches = matchRepository.findBySponsorId(sponsorId);
        AthleteMatch existingMatch = existingMatches.stream()
                .filter(m -> m.getAthlete() != null && m.getAthlete().getId().equals(athleteId))
                .findFirst()
                .orElse(null);

        if (existingMatch != null && existingMatch.getEvaluationDate() != null) {
            if (existingMatch.getEvaluationDate().isAfter(LocalDateTime.now().minusHours(24))) {
                return mapToResponseDTO(existingMatch);
            }
        }

        Sponsor sponsor = sponsorRepository.findById(sponsorId)
                .orElseThrow(() -> new EntityNotFoundException("Sponsor not found"));
        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(() -> new EntityNotFoundException("Athlete not found"));

        SponsorPreferences preferences = sponsorPreferencesRepository.findBySponsorId(sponsorId).orElse(null);
        SponsorProfile sponsorProfile = buildSponsorProfile(sponsor, preferences);
        AthleteProfile athleteProfile = buildAthleteProfile(athlete);
        
        int ruleScore = calculateFallbackScore(athleteProfile, sponsorProfile, preferences);
        
        // Note: AI call is inside @Transactional. Consider extracting out to avoid holding DB connections.
        AiMatchResponse aiResponse = generateAiMatch(athleteProfile, sponsorProfile, preferences, ruleScore);
        if (aiResponse == null) {
            aiResponse = buildFallbackResponse(ruleScore, athleteProfile, sponsorProfile);
        }

        AthleteMatch match = existingMatch != null ? existingMatch : new AthleteMatch();
        match.setAthlete(athlete);
        match.setSponsor(sponsor);
        match.setAthleteName(athleteProfile.name());
        match.setAthletePhotoUrl(athleteProfile.photoUrl());
        match.setDiscipline(athleteProfile.discipline());
        match.setRank(athleteProfile.currentRank());
        match.setSuggestedSponsor(sponsorProfile.name());
        match.setSponsorLogoUrl(sponsorProfile.logoUrl());
        if (existingMatch == null) {
            match.setStatus(MatchStatus.PROPOSED);
        }
        
        match.setMatchScore(aiResponse.matchScore());
        match.setVerdict(aiResponse.verdict());
        match.setConfidence(aiResponse.confidence());
        match.setReason(aiResponse.explanation());
        try {
            match.setStrengthsJson(objectMapper.writeValueAsString(aiResponse.strengths()));
            match.setWeaknessesJson(objectMapper.writeValueAsString(aiResponse.weaknesses()));
            match.setPotentialRoiText(objectMapper.writeValueAsString(aiResponse.potentialROI()));
            match.setAudienceOverlapText(objectMapper.writeValueAsString(aiResponse.audienceOverlap()));
            match.setKeyFactorsJson(objectMapper.writeValueAsString(aiResponse.keyFactors()));
        } catch (JsonProcessingException e) {
            log.error("Error serializing match evaluation to JSON", e);
        }
        match.setEvaluationDate(LocalDateTime.now());
        
        match = matchRepository.save(match);
        return mapToResponseDTO(match);
    }

    private MatchEvaluationResponseDTO mapToResponseDTO(AthleteMatch m) {
        return new MatchEvaluationResponseDTO(
                m.getId(),
                m.getSponsor() != null ? m.getSponsor().getId() : null,
                m.getAthlete() != null ? m.getAthlete().getId() : null,
                m.getAthleteName(),
                m.getAthletePhotoUrl(),
                m.getSuggestedSponsor(),
                m.getSponsorLogoUrl(),
                m.getMatchScore(),
                m.getVerdict(),
                m.getConfidence(),
                m.getReason(),
                parseStringList(m.getStrengthsJson()),
                parseStringList(m.getWeaknessesJson()),
                parseMap(m.getPotentialRoiText()),
                parseMap(m.getAudienceOverlapText()),
                parseMapList(m.getKeyFactorsJson()),
                m.getEvaluationDate()
        );
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<Map<String, Object>> parseMapList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (Exception e) {
            return List.of();
        }
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
        if (results.isEmpty()) return null;
        return results.get(0).getEventName();
    }

    private String calculateTrend(List<CompetitionResult> results, List<Ranking> rankings) {
        List<Integer> points = new ArrayList<>();
        rankings.stream().limit(3).map(Ranking::getPoints).filter(Objects::nonNull).forEach(points::add);
        if (points.size() < 2) {
            results.stream().limit(3).map(CompetitionResult::getPoints).filter(Objects::nonNull).forEach(points::add);
        }

        if (points.size() < 2) return "stable";

        int latest = points.get(0);
        int previous = points.get(1);
        if (latest > previous + 10) return "improving";
        if (latest < previous - 10) return "declining";
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

    private AiMatchResponse generateAiMatch(AthleteProfile athleteProfile, SponsorProfile sponsorProfile, SponsorPreferences preferences, int ruleScore) {
        try {
            log.debug("Preparing Groq API request for model: {}, Athlete: {}, Sponsor: {}", model, athleteProfile.name(), sponsorProfile.name());

            String prompt = String.format(SYSTEM_PROMPT, 
                    objectMapper.writeValueAsString(athleteProfile), 
                    objectMapper.writeValueAsString(sponsorProfile), 
                    preferences != null ? objectMapper.writeValueAsString(preferences) : "{}",
                    ruleScore);

            Map<String, Object> systemMessage = Map.of("role", "system", "content", prompt);
            Map<String, Object> userMessage = Map.of(
                    "role", "user",
                    "content", "Analyze this match and return the JSON evaluation."
            );

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 500);

            JsonNode response = groqClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> reactor.core.publisher.Mono.error(new RuntimeException("API Error: " + clientResponse.statusCode() + ", Body: " + errorBody))))
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(30));

            if (response == null) {
                return null;
            }

            String content = response.path("choices").path(0).path("message").path("content").asText();
            return parseAiResponse(content, ruleScore);
        } catch (Exception ex) {
            log.error("Groq matching request failed for athlete {} and sponsor {}", athleteProfile.name(), sponsorProfile.name(), ex);
            return null;
        }
    }

    private AiMatchResponse parseAiResponse(String content, int ruleScore) {
        try {
            String json = extractJson(content);
            JsonNode node = objectMapper.readTree(json);
            
            List<String> strengths = new ArrayList<>();
            if (node.has("strengths") && node.get("strengths").isArray()) {
                node.get("strengths").forEach(item -> strengths.add(item.asText()));
            }
            
            List<String> weaknesses = new ArrayList<>();
            if (node.has("weaknesses") && node.get("weaknesses").isArray()) {
                node.get("weaknesses").forEach(item -> weaknesses.add(item.asText()));
            }

            Map<String, Object> potentialROI = new java.util.HashMap<>();
            if (node.has("potentialROI") && node.get("potentialROI").isObject()) {
                potentialROI = objectMapper.convertValue(node.get("potentialROI"), Map.class);
            }
            if (!potentialROI.containsKey("projectedValue")) {
                potentialROI.put("projectedValue", 0);
                potentialROI.put("currency", "TND");
                potentialROI.put("explanation", "ROI calculation unavailable from AI model.");
            }
            
            Map<String, Object> audienceOverlap = new java.util.HashMap<>();
            if (node.has("audienceOverlap") && node.get("audienceOverlap").isObject()) {
                audienceOverlap = objectMapper.convertValue(node.get("audienceOverlap"), Map.class);
            }
            if (!audienceOverlap.containsKey("score")) {
                audienceOverlap.put("score", 0);
                audienceOverlap.put("explanation", "Audience overlap score unavailable from AI model.");
            }
            
            List<Map<String, Object>> keyFactors = new ArrayList<>();
            if (node.has("keyFactors") && node.get("keyFactors").isArray()) {
                for (JsonNode factorNode : node.get("keyFactors")) {
                    keyFactors.add(objectMapper.convertValue(factorNode, Map.class));
                }
            }
            
            return new AiMatchResponse(
                    clampScore(node.path("matchScore").asInt(ruleScore)),
                    node.path("verdict").asText("Moderate Fit"),
                    node.path("confidence").asText("MEDIUM"),
                    node.path("explanation").asText("Compatibility is based on ranking, discipline fit, and sponsor needs."),
                    strengths,
                    weaknesses,
                    potentialROI,
                    audienceOverlap,
                    keyFactors
            );
        } catch (Exception ex) {
            log.warn("Failed to parse Groq response: {}", content, ex);
            return null;
        }
    }

    private String extractJson(String content) {
        if (content == null || content.isBlank()) return "{}";
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private AiMatchResponse buildFallbackResponse(int score, AthleteProfile athlete, SponsorProfile sponsorProfile) {
        int disciplineAlignment = athlete.discipline() != null && sponsorProfile.preferredDisciplines().stream().anyMatch(d -> d.equalsIgnoreCase(athlete.discipline())) ? 95 : 55;
        int performanceTrend = switch (Objects.toString(athlete.performanceTrend(), "stable").toLowerCase()) {
            case "improving" -> 88;
            case "declining" -> 45;
            default -> 68;
        };

        int brandImageFit = Math.max(40, Math.min(95, (athlete.finaPoints() != null ? athlete.finaPoints() / 12 : 55)));
        int geographicRelevance = (sponsorProfile.geographicPreference() != null && athlete.clubAffiliation() != null
            && athlete.clubAffiliation().toLowerCase().contains(sponsorProfile.geographicPreference().toLowerCase())) ? 85 : 60;
        int socialMediaReach = Math.max(35, Math.min(90, (athlete.finaPoints() != null ? athlete.finaPoints() / 14 : 50)));

        long podiumCount = athlete.recentResults() == null ? 0 : athlete.recentResults().stream()
            .filter(r -> r.rankPosition() != null && r.rankPosition() <= 3)
            .count();
        int historicalSuccess = (int) Math.max(30, Math.min(95, 40 + podiumCount * 15));

        double visibilityMultiplier = 1.0 + (Math.max(0, performanceTrend - 50) / 100.0);
        double sponsorValue = sponsorProfile.budget() != null ? sponsorProfile.budget() : 0.0;
        double projectedValue = ((podiumCount > 0 ? podiumCount : 1) * sponsorValue * visibilityMultiplier) / 100.0;

        int audienceScore = Math.max(20, Math.min(95, (disciplineAlignment + geographicRelevance + socialMediaReach) / 3));

        String explanation = String.format(
            "Algorithmic evaluation indicates a %s fit with strong discipline alignment (%d/100) and performance trend (%d/100). Estimated ROI is based on recent podium performance and sponsor value.",
            score >= 90 ? "high" : score >= 75 ? "moderate" : "developing",
            disciplineAlignment,
            performanceTrend
        );

        List<String> strengths = new ArrayList<>();
        if (disciplineAlignment >= 80) strengths.add("Strong discipline alignment with sponsor priorities.");
        if (performanceTrend >= 75) strengths.add("Recent performances indicate upward momentum.");
        if (historicalSuccess >= 70) strengths.add("Competition history supports visibility and conversion potential.");
        if (strengths.isEmpty()) strengths.add("Foundational compatibility with room to improve via targeted activation.");

        List<String> weaknesses = new ArrayList<>();
        if (geographicRelevance < 65) weaknesses.add("Geographic relevance is moderate for current sponsor market focus.");
        if (socialMediaReach < 60) weaknesses.add("Digital reach indicators are limited compared to top performers.");
        if (weaknesses.isEmpty()) weaknesses.add("No critical weaknesses detected in the current data snapshot.");

        String verdict = score >= 90 ? "Strong Fit" : score >= 75 ? "Moderate Fit" : "Emerging Fit";
        String confidence = score >= 90 ? "HIGH" : score >= 75 ? "MEDIUM" : "LOW";

        List<Map<String, Object>> factors = List.of(
            Map.of("name", "Discipline Alignment", "score", disciplineAlignment, "weight", 0.24),
            Map.of("name", "Performance Trend", "score", performanceTrend, "weight", 0.20),
            Map.of("name", "Brand Image Fit", "score", brandImageFit, "weight", 0.16),
            Map.of("name", "Geographic Relevance", "score", geographicRelevance, "weight", 0.12),
            Map.of("name", "Social Media Reach", "score", socialMediaReach, "weight", 0.12),
            Map.of("name", "Historical Success", "score", historicalSuccess, "weight", 0.16)
        );

        return new AiMatchResponse(
            score,
            verdict,
            confidence,
            explanation,
            strengths,
            weaknesses,
            Map.of(
                "projectedValue", Math.round(projectedValue),
                "currency", "TND",
                "explanation", "Potential ROI computed from podium history, sponsor value, and visibility multiplier."
            ),
            Map.of(
                "score", audienceScore,
                "explanation", "Audience overlap combines discipline alignment, geographic relevance, and digital reach."
            ),
            factors
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

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    public record ResultSummary(String eventName, String recordTime, Integer rankPosition, Integer points) {}

    public record AthleteProfile(
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

    private record AiMatchResponse(
            Integer matchScore, 
            String verdict, 
            String confidence, 
            String explanation, 
            List<String> strengths, 
            List<String> weaknesses, 
            Map<String, Object> potentialROI, 
            Map<String, Object> audienceOverlap, 
            List<Map<String, Object>> keyFactors) {}
}