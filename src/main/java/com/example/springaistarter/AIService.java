package com.example.springaistarter;

public interface AIService {

    String ask(String message, String model);

    NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model);

    NBAChatResponse chatAboutNBA(String conversationId, String message, String model);

    NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model);

    NBARagResponse askWithRAG(String question, String model);
}
