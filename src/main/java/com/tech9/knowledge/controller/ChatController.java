
package com.tech9.knowledge.controller;

import com.tech9.knowledge.dto.ChatRequest;
import com.tech9.knowledge.dto.SearchResult;
import com.tech9.knowledge.service.AnthropicService;
import com.tech9.knowledge.service.SemanticSearchService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final SemanticSearchService semanticSearchService;
    private final AnthropicService anthropicService;

    public ChatController(
            SemanticSearchService semanticSearchService,
            AnthropicService anthropicService) {

        this.semanticSearchService = semanticSearchService;
        this.anthropicService = anthropicService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {

        try {
            if (request.getQuestion() == null
                    || request.getQuestion().trim().isEmpty()) {

                return ResponseEntity.badRequest().body(
                        Map.of("error", "Question cannot be empty."));
            }

            int limit = request.getLimit();

            if (limit < 1 || limit > 10) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Limit must be between 1 and 10."));
            }

            List<SearchResult> searchResults =
                    semanticSearchService.search(
                            request.getQuestion(), limit);

            String answer = anthropicService.generateAnswer(
                    request.getQuestion(), searchResults);

            Map<String, Object> response = new HashMap<>();

            response.put("question", request.getQuestion());
            response.put("answer", answer);
            response.put("sources", searchResults);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage()));

        } catch (IllegalStateException exception) {

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    Map.of("error", exception.getMessage()));
        }
    }
}
