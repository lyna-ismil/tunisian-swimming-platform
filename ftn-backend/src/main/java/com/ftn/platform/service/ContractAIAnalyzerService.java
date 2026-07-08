package com.ftn.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.dto.AIAnalysisResultDTO;
import com.ftn.platform.entity.ContractClause;
import com.ftn.platform.entity.SponsorContract;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.ContractClauseRepository;
import com.ftn.platform.repository.SponsorContractRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContractAIAnalyzerService {

    private final String apiKey;
    private final String model;
    private final WebClient groqClient;
    private final ObjectMapper objectMapper;
    private final SponsorContractRepository contractRepository;
    private final ContractClauseRepository clauseRepository;
    private final FileStorageService fileStorageService;

    public ContractAIAnalyzerService(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            SponsorContractRepository contractRepository,
            ContractClauseRepository clauseRepository,
            FileStorageService fileStorageService,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model,
            @Value("${groq.api.url:https://api.groq.com/openai/v1}") String groqUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.contractRepository = contractRepository;
        this.clauseRepository = clauseRepository;
        this.fileStorageService = fileStorageService;
        this.groqClient = webClientBuilder.baseUrl(groqUrl).build();
    }

    @Transactional
    public AIAnalysisResultDTO analyzeContract(Long contractId) {
        SponsorContract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new EntityNotFoundException("Contract not found"));

        if (contract.getDocumentUrl() == null) {
            throw new IllegalStateException("No document uploaded for this contract");
        }

        // 1. Download PDF
        byte[] pdfBytes = fileStorageService.download(contract.getDocumentUrl());

        // 2. Extract text from PDF
        String extractedText = extractTextFromPdf(pdfBytes);

        // 3. Call AI
        String aiJsonResponse = callGroqApi(extractedText);

        // 4. Parse response
        AIAnalysisResultDTO result = parseAIResponse(aiJsonResponse);

        // 5. Save summary to contract
        contract.setAiSummary(result.contractSummary());
        contractRepository.save(contract);

        // 6. Save clauses
        clauseRepository.deleteByContractId(contractId);
        if (result.clauses() != null) {
            for (AIAnalysisResultDTO.ExtractedClause clause : result.clauses()) {
                ContractClause entity = ContractClause.builder()
                        .contract(contract)
                        .clauseType(clause.type())
                        .extractedText(clause.text())
                        .aiInterpretation(clause.interpretation())
                        .pageNumber(clause.page())
                        .confidenceScore(BigDecimal.valueOf(95.0))
                        .build();
                clauseRepository.save(entity);
            }
        }

        return result;
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try {
            PDDocument document = Loader.loadPDF(pdfBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF", e);
            return "Unable to extract text from the uploaded PDF document.";
        }
    }

    private String callGroqApi(String contractText) {
        String truncatedText = contractText.substring(0, Math.min(contractText.length(), 15000));

        String prompt = "You are a legal contract analyst for the Tunisian Swimming Federation.\n" +
                "Analyze this sponsorship contract and respond ONLY with valid JSON.\n\n" +
                "Extract:\n" +
                "1. contractType: one of \"ATHLETE\", \"COMPETITION\", \"PFE\", or \"GENERAL\" — based on the contract subject\n" +
                "2. contractSummary: Brief 2-3 sentence overview\n" +
                "3. parties: Array of party names\n" +
                "4. financialValue: { \"amount\": number, \"currency\": \"TND\" }\n" +
                "5. duration: { \"start\": \"YYYY-MM-DD\", \"end\": \"YYYY-MM-DD\", \"autoRenewal\": boolean }\n" +
                "6. paymentTerms: String description\n" +
                "7. deliverables: Array of strings\n" +
                "8. exclusivity: { \"exists\": boolean, \"sector\": \"string\" }\n" +
                "9. terminationClause: String description\n" +
                "10. risks: Array of risk strings\n" +
                "11. keyDates: Array of date strings\n" +
                "12. clauses: Array of { \"type\": string, \"text\": string, \"interpretation\": string, \"page\": number }\n\n" +
                "Contract text:\n" + truncatedText;

        try {
            Map<String, Object> systemMsg = Map.of("role", "system", "content", "You are an expert legal contract analyst.");
            Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(systemMsg, userMsg));
            requestBody.put("temperature", 0.2);

            JsonNode response = groqClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(60));

            if (response != null) {
                String content = response.path("choices").path(0).path("message").path("content").asText();
                return extractJson(content);
            }
        } catch (Exception ex) {
            log.error("AI contract analysis failed", ex);
        }

        return getFallbackResponse();
    }

    private AIAnalysisResultDTO parseAIResponse(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            // Parse financial value
            AIAnalysisResultDTO.FinancialValue financialValue = null;
            if (node.has("financialValue")) {
                JsonNode fv = node.get("financialValue");
                financialValue = new AIAnalysisResultDTO.FinancialValue(
                        fv.has("amount") ? fv.get("amount").decimalValue() : BigDecimal.ZERO,
                        fv.has("currency") ? fv.get("currency").asText() : "TND"
                );
            }

            // Parse duration
            AIAnalysisResultDTO.DurationInfo duration = null;
            if (node.has("duration")) {
                JsonNode d = node.get("duration");
                duration = new AIAnalysisResultDTO.DurationInfo(
                        d.has("start") && !d.get("start").asText().isBlank() ? LocalDate.parse(d.get("start").asText()) : null,
                        d.has("end") && !d.get("end").asText().isBlank() ? LocalDate.parse(d.get("end").asText()) : null,
                        d.has("autoRenewal") && d.get("autoRenewal").asBoolean()
                );
            }

            // Parse exclusivity
            AIAnalysisResultDTO.ExclusivityInfo exclusivity = null;
            if (node.has("exclusivity")) {
                JsonNode ex = node.get("exclusivity");
                exclusivity = new AIAnalysisResultDTO.ExclusivityInfo(
                        ex.has("exists") && ex.get("exists").asBoolean(),
                        ex.has("sector") ? ex.get("sector").asText() : ""
                );
            }

            // Parse clauses
            List<AIAnalysisResultDTO.ExtractedClause> clauses = new ArrayList<>();
            if (node.has("clauses") && node.get("clauses").isArray()) {
                for (JsonNode cl : node.get("clauses")) {
                    clauses.add(new AIAnalysisResultDTO.ExtractedClause(
                            cl.has("type") ? cl.get("type").asText() : "OTHER",
                            cl.has("text") ? cl.get("text").asText() : "",
                            cl.has("interpretation") ? cl.get("interpretation").asText() : "",
                            cl.has("page") ? cl.get("page").asInt() : null
                    ));
                }
            }

            return new AIAnalysisResultDTO(
                    node.has("contractSummary") ? node.get("contractSummary").asText() : "",
                    parseStringList(node, "parties"),
                    financialValue,
                    duration,
                    node.has("paymentTerms") ? node.get("paymentTerms").asText() : "",
                    parseStringList(node, "deliverables"),
                    exclusivity,
                    node.has("terminationClause") ? node.get("terminationClause").asText() : "",
                    parseStringList(node, "risks"),
                    parseStringList(node, "keyDates"),
                    clauses
            );
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            return new AIAnalysisResultDTO(
                    "Failed to parse AI analysis. Please try again.",
                    List.of(), null, null, "", List.of(), null, "", List.of(), List.of(), List.of()
            );
        }
    }

    private List<String> parseStringList(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || !node.get(fieldName).isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode item : node.get(fieldName)) {
            result.add(item.asText());
        }
        return result;
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

    private String getFallbackResponse() {
        return "{\n" +
                "  \"contractSummary\": \"AI analysis could not be completed. Please review the document manually.\",\n" +
                "  \"parties\": [],\n" +
                "  \"financialValue\": {\"amount\": 0, \"currency\": \"TND\"},\n" +
                "  \"duration\": {\"start\": null, \"end\": null, \"autoRenewal\": false},\n" +
                "  \"paymentTerms\": \"Not available\",\n" +
                "  \"deliverables\": [],\n" +
                "  \"exclusivity\": {\"exists\": false, \"sector\": \"\"},\n" +
                "  \"terminationClause\": \"Not available\",\n" +
                "  \"risks\": [\"AI analysis unavailable — manual review required\"],\n" +
                "  \"keyDates\": [],\n" +
                "  \"clauses\": []\n" +
                "}";
    }

    public Map<String, Object> extractContractMetadata(String pdfText) {
        try {
            String json = callGroqApi(pdfText);
            if (json == null || json.isBlank()) return Map.of();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Failed to extract contract metadata", e);
            return Map.of();
        }
    }
}
