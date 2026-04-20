package com.example.springaistarter;

import reactor.core.publisher.Flux;

public interface AIService {

    String ask(String message, String model);

    Flux<String> streamAsk(String message, String model);

    String describeImage(String imageUrl, String prompt, String model);

    NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model);

    NBAChatResponse chatAboutNBA(String conversationId, String message, String model);

    NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model);

    NBARagResponse askWithRAG(String question, String model);
}
