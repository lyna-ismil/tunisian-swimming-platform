package com.ftn.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.entity.CompetitionEvent;
import com.ftn.platform.entity.Sponsor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SponsorshipAIEvaluationService {

    private final String apiKey;
    private final String model;
    private final WebClient groqClient;
    private final ObjectMapper objectMapper;
    private final com.ftn.platform.repository.SponsorEventRepository sponsorEventRepository;

    public SponsorshipAIEvaluationService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            com.ftn.platform.repository.SponsorEventRepository sponsorEventRepository,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model,
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String groqUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.sponsorEventRepository = sponsorEventRepository;
        this.groqClient = webClientBuilder.baseUrl(groqUrl).build();
    }

    public String evaluateSponsorship(Sponsor sponsor, CompetitionEvent event, String packageType, Double amount) {
        String pastSponsors = getPastSponsorsSummary(event);
        String pastEvents = getPastEventsSummary(sponsor);

        String context = String.format(
            "Sponsor: %s (Tier: %s, Total Value: %.2f TND)\n" +
            "Event: %s (Date: %s, Expected Attendance: %s, Location: %s)\n" +
            "Package: %s, Proposed Amount: %.2f TND\n" +
            "Sponsor's past events: %s\n" +
            "Event's past sponsors: %s\n\n" +
            "Respond ONLY with valid JSON in this structure:\n" +
            "{\n" +
            "  \"roiScore\": 85,\n" +
            "  \"brandFitScore\": 92,\n" +
            "  \"riskLevel\": \"LOW\" | \"MEDIUM\" | \"HIGH\",\n" +
            "  \"estimatedReach\": 15000,\n" +
            "  \"recommendation\": \"STRONG_MATCH\" | \"GOOD_MATCH\" | \"NEUTRAL\" | \"WEAK_MATCH\" | \"NOT_RECOMMENDED\",\n" +
            "  \"summary\": \"2-3 sentences max\",\n" +
            "  \"keyFactors\": [\"bullet 1\", \"bullet 2\", \"bullet 3\"]\n" +
            "}",
            sponsor.getName(), sponsor.getTier(), sponsor.getTotalValue() != null ? sponsor.getTotalValue() : 0.0,
            event.getName(), event.getEventDate(), event.getAudience(), event.getLocation(),
            packageType, amount != null ? amount : 0.0,
            pastEvents,
            pastSponsors
        );

        try {
            Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are an expert sports sponsorship AI analyst.");
            Map<String, Object> userMessage = Map.of("role", "user", "content", context);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMessage, userMessage));
            requestBody.put("temperature", 0.3);

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

            if (response != null) {
                String content = response.path("choices").path(0).path("message").path("content").asText();
                return extractJson(content);
            }
        } catch (Exception ex) {
            log.error("AI evaluation failed", ex);
        }
        
        // Fallback
        return "{\n" +
            "  \"roiScore\": 75,\n" +
            "  \"brandFitScore\": 80,\n" +
            "  \"riskLevel\": \"LOW\",\n" +
            "  \"estimatedReach\": 5000,\n" +
            "  \"recommendation\": \"GOOD_MATCH\",\n" +
            "  \"summary\": \"Fallback evaluation due to service interruption. Proceed with normal review.\",\n" +
            "  \"keyFactors\": [\"Basic rule-based fallback\"]\n" +
            "}";
    }

    private String getPastEventsSummary(Sponsor sponsor) {
        List<com.ftn.platform.entity.SponsorEvent> events = sponsorEventRepository.findBySponsorId(sponsor.getId());
        if (events.isEmpty()) return "None";
        return events.stream().map(e -> e.getEvent().getName()).collect(Collectors.joining(", "));
    }

    private String getPastSponsorsSummary(CompetitionEvent event) {
        List<com.ftn.platform.entity.SponsorEvent> events = sponsorEventRepository.findByEventId(event.getId());
        if (events.isEmpty()) return "None";
        return events.stream().map(e -> e.getSponsor().getName()).collect(Collectors.joining(", "));
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
}
