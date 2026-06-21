package com.ftn.platform.service;

import com.ftn.platform.dto.ChatResponseDTO;
import com.ftn.platform.dto.SessionDTO;
import com.ftn.platform.entity.ChatSession;
import com.ftn.platform.entity.Suggestion;
import com.ftn.platform.exception.EntityNotFoundException;
import com.ftn.platform.repository.ChatMessageRepository;
import com.ftn.platform.repository.ChatSessionRepository;
import com.ftn.platform.repository.SuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Month;
import java.time.MonthDay;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final GroqChatService groqChatService;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SuggestionRepository suggestionRepository;

    public Mono<String> chatNonStreaming(Long sessionId, String userMessage) {
        return groqChatService.chatNonStreaming(sessionId, userMessage);
    }

    public Flux<String> chatStreaming(Long sessionId, String userMessage) {
        return groqChatService.chatStreaming(sessionId, userMessage);
    }

    public List<SessionDTO> getSessions(Long userId) {
        List<ChatSession> sessions = chatSessionRepository.findByUserIdOrderByLastActivityAtDesc(userId);
        return sessions.stream()
                .map(s -> new SessionDTO(s.getId(), s.getTitle(), s.getCreatedAt(), s.getLastActivityAt(), s.getMessages().size(), s.getSummary(), s.getStatus()))
                .collect(Collectors.toList());
    }

    public List<ChatResponseDTO> getSessionMessages(Long sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId)
                .stream()
                .map(m -> new ChatResponseDTO(m.getId(), m.getRole().name().toLowerCase(), m.getContent(), m.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        chatSessionRepository.deleteById(sessionId);
    }

    @Transactional
    public void clearSessionMessages(Long sessionId) {
        chatMessageRepository.deleteAll(chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId));
    }

    @org.springframework.scheduling.annotation.Async
    @Transactional
    public void summarizeSessionAsync(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null || session.getMessages().isEmpty()) return;

        // Generate summary prompt
        String prompt = "Please provide a concise 1-2 sentence summary of our conversation so far. Do not include greetings.";
        
        groqChatService.chatNonStreaming(sessionId, prompt).subscribe(summary -> {
            session.setSummary(summary);
            session.setStatus("CLOSED");
            chatSessionRepository.save(session);
        });
    }

    public List<String> getSuggestions() {
        List<Suggestion> top = suggestionRepository.findTop4ByOrderByFrequencyDesc();
        if (!top.isEmpty()) {
            return top.stream().map(Suggestion::getText).collect(Collectors.toList());
        }
        return getSeasonalFallback();
    }

    @Transactional
    public void trackSuggestion(Long id) {
        Suggestion s = suggestionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Suggestion not found: " + id));
        s.setFrequency(s.getFrequency() + 1);
        suggestionRepository.save(s);
    }

    private List<String> getSeasonalFallback() {
        MonthDay today = MonthDay.now();
        // Championship season: July–August
        if (today.getMonth() == Month.JUNE || today.getMonth() == Month.JULY) {
            return List.of(
                "What are the qualifying times for the National Summer Championship?",
                "When does the National Summer Championship registration close?",
                "How do I check my FINA points for qualification?",
                "Who are the top-ranked Tunisian swimmers this season?"
            );
        }
        // License renewal season: August–September
        if (today.getMonth() == Month.AUGUST || today.getMonth() == Month.SEPTEMBER) {
            return List.of(
                "How do I renew my swimming license before the season?",
                "What documents are needed for license renewal?",
                "What is the license renewal fee for this year?",
                "How long does license processing take?"
            );
        }
        // Default
        return List.of(
            "What are the qualifying times for the National Summer Championship?",
            "Tell me about Ahmed Hafnaoui's Olympic medals.",
            "How do I renew my swimming license?",
            "Who are the Platinum sponsors of the FTN?"
        );
    }
}
