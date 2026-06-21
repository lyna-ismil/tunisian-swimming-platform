package com.ftn.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ftn.platform.entity.ChatMessage;
import com.ftn.platform.entity.ChatSession;
import com.ftn.platform.entity.Role;
import com.ftn.platform.entity.SystemPrompt;
import com.ftn.platform.repository.ChatMessageRepository;
import com.ftn.platform.repository.ChatSessionRepository;
import com.ftn.platform.repository.SystemPromptRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class GroqChatService {

    public static final String DEFAULT_SYSTEM_PROMPT = "You are the FTN (Fédération Tunisienne de Natation) AI Assistant. You help federation members with: Competition rules and eligibility, Athlete profiles and rankings, License renewal procedures, Club management, Sponsorship information, Navigating the FTN Dashboard platform. You know Tunisian swimming legends: Ahmed Hafnaoui (Olympic gold 400m free), Oussama Mellouli (two Olympic golds), Sarra Lajnef, Rami Chammari. FTN founded 1959. Major clubs: Club Africain, EST, CSS. Sponsors: Speedo (Platinum), Ooredoo (Gold), Arena (Silver), Tunisair (Gold). Respond in the same language as the user (Arabic, French, or English). Be concise, accurate, and helpful. If you don't know something, say so clearly.";

    private final AtomicReference<String> cachedPrompt = new AtomicReference<>(DEFAULT_SYSTEM_PROMPT);

    private final WebClient webClient;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SystemPromptRepository systemPromptRepository;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public GroqChatService(
            WebClient.Builder webClientBuilder,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            SystemPromptRepository systemPromptRepository,
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.api.model}") String model,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://api.groq.com/openai/v1").build();
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.systemPromptRepository = systemPromptRepository;
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadPrompt() {
        refreshPromptCache();
    }

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void refreshPromptCache() {
        systemPromptRepository.findFirstByIsActiveTrueOrderByVersionDesc()
                .ifPresent(p -> cachedPrompt.set(p.getPromptText()));
    }

    @Transactional
    public SystemPrompt updatePrompt(String newPromptText) {
        // deactivate current
        systemPromptRepository.findFirstByIsActiveTrueOrderByVersionDesc()
                .ifPresent(p -> { p.setIsActive(false); systemPromptRepository.save(p); });

        int nextVersion = (int) systemPromptRepository.count() + 1;
        SystemPrompt saved = systemPromptRepository.save(
            SystemPrompt.builder().promptText(newPromptText).version(nextVersion).isActive(true).build()
        );
        cachedPrompt.set(newPromptText);
        return saved;
    }

    public String getActivePromptText() {
        return cachedPrompt.get();
    }

    public Mono<String> chatNonStreaming(Long sessionId, String userMessageContent) {
        ChatSession session = getOrCreateSession(sessionId, userMessageContent);
        
        saveMessage(session, Role.USER, userMessageContent, null, null, null);
        
        List<Map<String, String>> messages = buildContext(session);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    String assistantContent = response.path("choices").path(0).path("message").path("content").asText();
                    Integer promptTokens = response.path("usage").path("prompt_tokens").asInt();
                    Integer completionTokens = response.path("usage").path("completion_tokens").asInt();
                    
                    saveMessage(session, Role.ASSISTANT, assistantContent, model, promptTokens, completionTokens);
                    updateSessionLastActivity(session);
                    
                    return assistantContent;
                });
    }

    public Flux<String> chatStreaming(Long sessionId, String userMessageContent) {
        ChatSession session = getOrCreateSession(sessionId, userMessageContent);
        
        saveMessage(session, Role.USER, userMessageContent, null, null, null);
        
        List<Map<String, String>> messages = buildContext(session);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.equals("[DONE]"))
                .map(chunk -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(chunk);
                        JsonNode delta = jsonNode.path("choices").path(0).path("delta");
                        if (delta.has("content")) {
                            return delta.path("content").asText();
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing streaming chunk", e);
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty())
                .doOnComplete(() -> updateSessionLastActivity(session));
                // Note: The prompt asks to save final message after stream completes. We can do that with reduce or collect
    }
    
    public Flux<String> chatStreamingWithSave(Long sessionId, String userMessageContent) {
        ChatSession session = getOrCreateSession(sessionId, userMessageContent);
        saveMessage(session, Role.USER, userMessageContent, null, null, null);
        List<Map<String, String>> messages = buildContext(session);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> !chunk.equals("[DONE]"))
                .map(chunk -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(chunk);
                        JsonNode delta = jsonNode.path("choices").path(0).path("delta");
                        if (delta.has("content")) {
                            return delta.path("content").asText();
                        }
                    } catch (JsonProcessingException e) {
                        // ignore parsing error on incomplete chunks
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty())
                // Cache the flux so we can return it and also subscribe to it to save the whole string
                .publish()
                .autoConnect(2, disposable -> {
                    // This creates two subscriptions: one for the return value, one for saving to db.
                    // Instead, let's just collect in memory using windowing or do a parallel subscription.
                });
    }

    // A better approach for saving while streaming:
    public Flux<String> getStreamingFlux(Long sessionId, String userMessageContent) {
        ChatSession session = getOrCreateSession(sessionId, userMessageContent);
        saveMessage(session, Role.USER, userMessageContent, null, null, null);
        List<Map<String, String>> messages = buildContext(session);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", true);

        StringBuilder fullResponse = new StringBuilder();

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(chunk -> chunk != null && !chunk.isEmpty() && !chunk.equals("[DONE]"))
                .map(chunk -> {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(chunk);
                        JsonNode delta = jsonNode.path("choices").path(0).path("delta");
                        if (delta.has("content")) {
                            String content = delta.path("content").asText();
                            fullResponse.append(content);
                            return content;
                        }
                    } catch (JsonProcessingException e) {
                        // Ignore
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty())
                .doOnComplete(() -> {
                    saveMessage(session, Role.ASSISTANT, fullResponse.toString(), model, null, null);
                    updateSessionLastActivity(session);
                });
    }

    private ChatSession getOrCreateSession(Long sessionId, String userMessageContent) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId).orElseGet(() -> createNewSession(userMessageContent));
        }
        return createNewSession(userMessageContent);
    }

    private ChatSession createNewSession(String firstMessage) {
        String title = firstMessage.length() > 50 ? firstMessage.substring(0, 50) + "..." : firstMessage;
        ChatSession session = ChatSession.builder()
                .userId(1L) // Hardcoded for single user
                .title(title)
                .build();
        return chatSessionRepository.save(session);
    }

    private void saveMessage(ChatSession session, Role role, String content, String modelUsed, Integer promptTokens, Integer completionTokens) {
        ChatMessage message = ChatMessage.builder()
                .session(session)
                .role(role)
                .content(content)
                .modelUsed(modelUsed)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .timestamp(LocalDateTime.now())
                .build();
        chatMessageRepository.save(message);
    }

    private void updateSessionLastActivity(ChatSession session) {
        session.setLastActivityAt(LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    private List<Map<String, String>> buildContext(ChatSession session) {
        List<Map<String, String>> messages = new ArrayList<>();
        
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", cachedPrompt.get());
        messages.add(systemMsg);
        
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(session.getId());
        
        // get last 10 messages
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            Map<String, String> m = new HashMap<>();
            m.put("role", msg.getRole().name().toLowerCase());
            m.put("content", msg.getContent());
            messages.add(m);
        }
        
        return messages;
    }
}
