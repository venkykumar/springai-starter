package com.example.springaistarter;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ask")
    public String ask(
            @RequestParam(defaultValue = "Tell me a short joke about Java.") String message,
            @RequestParam(required = false) String model) {
        return aiService.ask(message, model);
    }

    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(
            @RequestParam(defaultValue = "Tell me a short joke about Java.") String message,
            @RequestParam(required = false) String model) {
        return aiService.streamAsk(message, model);
    }

    @GetMapping("/vision/describe")
    public String describeImage(
            @RequestParam String imageUrl,
            @RequestParam(defaultValue = "Describe this image in detail.") String prompt,
            @RequestParam(required = false) String model) {
        return aiService.describeImage(imageUrl, prompt, model);
    }

    @GetMapping("/nba/player-profile")
    public NBAPlayerProfile playerProfile(
            @RequestParam(defaultValue = "Stephen Curry") String playerName,
            @RequestParam(required = false) String model) {
        return aiService.generateNBAPlayerProfile(playerName, model);
    }

    @GetMapping("/nba/chat")
    public NBAChatResponse nbaChat(
            @RequestParam(defaultValue = "demo-conversation") String conversationId,
            @RequestParam(defaultValue = "Tell me about Stephen Curry.") String message,
            @RequestParam(required = false) String model) {
        return aiService.chatAboutNBA(conversationId, message, model);
    }

    @GetMapping("/nba/mcp/recent-games-summary")
    public NBAMCPRecentGamesResponse nbaMCPRecentGamesSummary(
            @RequestParam(defaultValue = "Stephen Curry") String playerName,
            @RequestParam(required = false) String model) {
        return aiService.summarizeRecentGamesWithMCP(playerName, model);
    }

    @GetMapping("/nba/ask")
    public NBARagResponse ragAsk(
            @RequestParam(defaultValue = "Who holds the NBA record for most career three-pointers?") String question,
            @RequestParam(required = false) String model) {
        return aiService.askWithRAG(question, model);
    }
}