package com.ftn.platform.controller;

import com.ftn.platform.dto.*;
import com.ftn.platform.service.DbChurnPredictionService;
import com.ftn.platform.service.MLServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/churn")
@RequiredArgsConstructor
@Slf4j
public class ChurnPredictionController {

    private final MLServiceClient mlClient;
    private final DbChurnPredictionService dbChurnService;

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDTO> health() {
        HealthResponseDTO h = mlClient.getHealth();
        log.info("ML health check: status={}, modelLoaded={}", h.getStatus(), h.getModelLoaded());
        return ResponseEntity.ok(h);
    }

    @PostMapping("/predict")
    public ResponseEntity<ChurnPredictionDTO> predict(@RequestBody SponsorFeaturesDTO features) {
        return ResponseEntity.ok(mlClient.predict(features));
    }

    @PostMapping("/predict/batch")
    public ResponseEntity<BatchPredictionResponseDTO> predictBatch(@RequestBody BatchPredictionRequestDTO request) {
        return ResponseEntity.ok(mlClient.predictBatch(request));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ModelMetricsDTO> metrics() {
        return ResponseEntity.ok(mlClient.getMetrics());
    }

    @GetMapping("/feature-importance")
    public ResponseEntity<List<FeatureImportanceItemDTO>> featureImportance() {
        return ResponseEntity.ok(mlClient.getFeatureImportance());
    }

    @GetMapping("/at-risk")
    public ResponseEntity<List<AtRiskSponsorDTO>> atRisk(
            @RequestParam(defaultValue = "0.3") Double threshold,
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(dbChurnService.getAtRiskSponsors(threshold, tier, status));
    }

    @GetMapping("/sponsor/{sponsorId}/risk")
    public ResponseEntity<ChurnPredictionDTO> sponsorRisk(@PathVariable String sponsorId) {
        return ResponseEntity.ok(mlClient.getSponsorRisk(sponsorId));
    }

    @PostMapping("/test-model")
    public ResponseEntity<ChurnPredictionDTO> testModel() {
        return ResponseEntity.ok(mlClient.runSmokeTestPrediction());
    }
}
