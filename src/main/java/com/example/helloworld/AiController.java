package com.example.helloworld;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam(defaultValue = "Tell me a short joke about Java.") String message) {
        return aiService.ask(message);
    }

    @GetMapping("/nba/player-profile")
    public NBAPlayerProfile playerProfile(@RequestParam(defaultValue = "Stephen Curry") String playerName) {
        return aiService.generateNBAPlayerProfile(playerName);
    }
}
