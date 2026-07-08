package com.ftn.platform.service;

import com.ftn.platform.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class MLServiceClient {

    private final WebClient webClient;

    public MLServiceClient(@Qualifier("mlWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    private static final ChurnPredictionDTO DEFAULT_PREDICTION = ChurnPredictionDTO.builder()
        .sponsorId("unknown")
        .churnProbability(0.0)
        .willChurn(false)
        .riskLevel("LOW")
        .expectedValueNextTnd(0.0)
        .recommendation("ML service currently unavailable. Retry once the service is online.")
        .topRiskFactors(List.of())
        .modelUsed("unavailable")
        .build();

    private static final ModelMetricsDTO DEFAULT_METRICS = ModelMetricsDTO.builder()
        .aucRoc(0.0)
        .avgPrecision(0.0)
        .precision(0.0)
        .recall(0.0)
        .f1Score(0.0)
        .cvAucMean(0.0)
        .trainingDate("unavailable")
        .datasetSize(0)
        .build();

    private static final HealthResponseDTO OFFLINE_HEALTH = HealthResponseDTO.builder()
        .status("unavailable")
        .modelLoaded(false)
        .modelType("unknown")
        .version("unknown")
        .build();

    private ChurnPredictionDTO fallbackPrediction(String sponsorId) {
        return ChurnPredictionDTO.builder()
            .sponsorId(sponsorId)
            .churnProbability(DEFAULT_PREDICTION.getChurnProbability())
            .willChurn(DEFAULT_PREDICTION.getWillChurn())
            .riskLevel(DEFAULT_PREDICTION.getRiskLevel())
            .expectedValueNextTnd(DEFAULT_PREDICTION.getExpectedValueNextTnd())
            .recommendation(DEFAULT_PREDICTION.getRecommendation())
            .topRiskFactors(DEFAULT_PREDICTION.getTopRiskFactors())
            .modelUsed(DEFAULT_PREDICTION.getModelUsed())
            .build();
    }

    public HealthResponseDTO getHealth() {
        try {
            log.debug("Calling ML /api/v1/health");
            return webClient.get()
                .uri("/api/v1/health")
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /health returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML health check failed: " + resp.statusCode()))))
                .bodyToMono(HealthResponseDTO.class)
                .doOnSuccess(r -> log.debug("ML /health response: {}", r))
                .onErrorResume(e -> {
                    log.error("ML service /health unreachable: {}", e.getMessage());
                    return Mono.just(OFFLINE_HEALTH);
                })
                .block();
        } catch (Exception e) {
            log.error("ML service /health failed: {}", e.getMessage());
            return OFFLINE_HEALTH;
        }
    }

    public ChurnPredictionDTO predict(SponsorFeaturesDTO features) {
        try {
            String sponsorId = features != null ? features.getSponsorId() : "unknown";
            log.debug("Calling ML /api/v1/ml/predict for sponsor {}", sponsorId);
            return webClient.post()
                .uri("/api/v1/ml/predict")
                .bodyValue(features)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /predict returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML predict failed: " + resp.statusCode()))))
                .bodyToMono(ChurnPredictionDTO.class)
                .onErrorResume(e -> {
                    log.error("ML /predict failed: {}", e.getMessage());
                    return Mono.just(fallbackPrediction(sponsorId));
                })
                .block();
        } catch (Exception e) {
            log.error("ML /predict exception: {}", e.getMessage());
            return fallbackPrediction(
                features != null ? features.getSponsorId() : "unknown"
            );
        }
    }

    public BatchPredictionResponseDTO predictBatch(BatchPredictionRequestDTO request) {
        try {
            log.debug("Calling ML /api/v1/ml/predict/batch");
            return webClient.post()
                .uri("/api/v1/ml/predict/batch")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /predict/batch returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML batch predict failed: " + resp.statusCode()))))
                .bodyToMono(BatchPredictionResponseDTO.class)
                .onErrorResume(e -> {
                    log.error("ML /predict/batch failed: {}", e.getMessage());
                    return Mono.just(BatchPredictionResponseDTO.builder()
                        .predictions(List.of())
                        .totalAtRisk(0)
                        .totalValueAtRiskTnd(0.0)
                        .build());
                })
                .block();
        } catch (Exception e) {
            log.error("ML /predict/batch exception: {}", e.getMessage());
            return BatchPredictionResponseDTO.builder()
                .predictions(List.of())
                .totalAtRisk(0)
                .totalValueAtRiskTnd(0.0)
                .build();
        }
    }

    public ModelMetricsDTO getMetrics() {
        try {
            log.debug("Calling ML /api/v1/ml/metrics");
            return webClient.get()
                .uri("/api/v1/ml/metrics")
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /metrics returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML metrics failed: " + resp.statusCode()))))
                .bodyToMono(ModelMetricsDTO.class)
                .onErrorResume(e -> {
                    log.error("ML /metrics failed: {}", e.getMessage());
                    return Mono.just(DEFAULT_METRICS);
                })
                .block();
        } catch (Exception e) {
            log.error("ML /metrics exception: {}", e.getMessage());
            return DEFAULT_METRICS;
        }
    }

    public List<FeatureImportanceItemDTO> getFeatureImportance() {
        try {
            log.debug("Calling ML /api/v1/ml/feature-importance");
            return webClient.get()
                .uri("/api/v1/ml/feature-importance")
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /feature-importance returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML feature importance failed: " + resp.statusCode()))))
                .bodyToMono(new ParameterizedTypeReference<List<FeatureImportanceItemDTO>>() {})
                .onErrorResume(e -> {
                    log.error("ML /feature-importance failed: {}", e.getMessage());
                    return Mono.just(List.of());
                })
                .block();
        } catch (Exception e) {
            log.error("ML /feature-importance exception: {}", e.getMessage());
            return List.of();
        }
    }

    public List<AtRiskSponsorDTO> getAtRiskSponsors(Double threshold, String tier, String status) {
        try {
            log.debug("Calling ML /api/v1/sponsors/at-risk with threshold={}, tier={}, status={}", threshold, tier, status);
            return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/sponsors/at-risk")
                        .queryParam("threshold", threshold);
                    if (tier != null && !tier.isBlank()) {
                        uriBuilder.queryParam("tier", tier);
                    }
                    if (status != null && !status.isBlank()) {
                        uriBuilder.queryParam("status", status);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /at-risk returned {}: {}", resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML at-risk failed: " + resp.statusCode()))))
                .bodyToMono(new ParameterizedTypeReference<List<AtRiskSponsorDTO>>() {})
                .onErrorResume(e -> {
                    log.error("ML /at-risk failed: {}", e.getMessage());
                    return Mono.just(List.of());
                })
                .block();
        } catch (Exception e) {
            log.error("ML /at-risk exception: {}", e.getMessage());
            return List.of();
        }
    }

    public ChurnPredictionDTO getSponsorRisk(String sponsorId) {
        try {
            log.debug("Calling ML /api/v1/sponsors/{}/risk", sponsorId);
            return webClient.get()
                .uri("/api/v1/sponsors/{sponsorId}/risk", sponsorId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                    .doOnNext(body -> log.error("ML /sponsors/{}/risk returned {}: {}", sponsorId, resp.statusCode(), body))
                    .then(Mono.error(new RuntimeException("ML sponsor risk failed: " + resp.statusCode()))))
                .bodyToMono(ChurnPredictionDTO.class)
                .onErrorResume(e -> {
                    log.error("ML /sponsors/{}/risk failed: {}", sponsorId, e.getMessage());
                    return Mono.just(fallbackPrediction(sponsorId));
                })
                .block();
        } catch (Exception e) {
            log.error("ML /sponsors/{}/risk exception: {}", sponsorId, e.getMessage());
            return fallbackPrediction(sponsorId);
        }
    }

    public ChurnPredictionDTO runSmokeTestPrediction() {
        SponsorFeaturesDTO sample = SponsorFeaturesDTO.builder()
            .sponsorId("SMOKE-TEST")
            .tier("GOLD")
            .contractDurationMonths(24)
            .totalValueTnd(50000.0)
            .numPreviousContracts(2)
            .investmentTrendPct(3.0)
            .availablePackages(10)
            .packagesTaken(7)
            .engagementRate(0.75)
            .athletesSponsored(5)
            .athleteTurnoverRate(0.10)
            .competitionsPerYear(4)
            .lastActivityDays(20)
            .daysSinceStart(320)
            .daysUntilEnd(180)
            .status("ACTIVE")
            .build();
        return predict(sample);
    }
}
