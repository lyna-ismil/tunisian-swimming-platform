package com.ftn.platform.controller;

import com.ftn.platform.dto.ChatRequestDTO;
import com.ftn.platform.dto.ChatResponseDTO;
import com.ftn.platform.dto.SessionDTO;
import com.ftn.platform.entity.SystemPrompt;
import com.ftn.platform.repository.ChatConfigRepository;
import com.ftn.platform.service.AiAssistantService;
import com.ftn.platform.service.GroqChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final GroqChatService groqChatService;
    private final ChatConfigRepository chatConfigRepository;

    @PostMapping("/chat")
    public Mono<ChatResponseDTO> chat(@Valid @RequestBody ChatRequestDTO request) {
        return aiAssistantService.chatNonStreaming(request.sessionId(), request.message())
                .map(content -> new ChatResponseDTO(
                        null, "assistant", content, LocalDateTime.now()
                ));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@Valid @RequestBody ChatRequestDTO request) {
        return aiAssistantService.chatStreaming(request.sessionId(), request.message())
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }

    @GetMapping("/sessions")
    public List<SessionDTO> getSessions() {
        return aiAssistantService.getSessions(1L); // Hardcoded user 1 for now
    }

    @GetMapping("/sessions/{id}/messages")
    public List<ChatResponseDTO> getSessionMessages(@PathVariable Long id) {
        return aiAssistantService.getSessionMessages(id);
    }

    @DeleteMapping("/sessions/{id}")
    public void deleteSession(@PathVariable Long id) {
        aiAssistantService.deleteSession(id);
    }

    @PostMapping("/sessions/{id}/clear")
    public void clearSessionMessages(@PathVariable Long id) {
        aiAssistantService.clearSessionMessages(id);
    }

    @PostMapping("/sessions/{id}/close")
    public void closeAndSummarizeSession(@PathVariable Long id) {
        aiAssistantService.summarizeSessionAsync(id);
    }

    @GetMapping("/suggestions")
    public List<String> getSuggestions() {
        return aiAssistantService.getSuggestions();
    }

    @PostMapping("/suggestions/track")
    public void trackSuggestion(@RequestParam Long id) {
        aiAssistantService.trackSuggestion(id);
    }

    @GetMapping("/admin/prompt")
    public Map<String, Object> getActivePrompt() {
        return Map.of("promptText", groqChatService.getActivePromptText());
    }

    @PutMapping("/admin/prompt")
    public SystemPrompt updatePrompt(@RequestBody Map<String, String> body) {
        return groqChatService.updatePrompt(body.get("promptText"));
    }

    @GetMapping("/config/welcome")
    public Map<String, String> getWelcomeMessage() {
        String msg = chatConfigRepository.findById("welcome_message")
                .map(c -> c.getConfigValue())
                .orElse("Hello! How can I help you today?");
        return Map.of("message", msg);
    }
}
