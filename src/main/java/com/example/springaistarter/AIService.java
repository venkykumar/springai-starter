package com.example.springaistarter;

public interface AIService {

    String ask(String message);

    NBAPlayerProfile generateNBAPlayerProfile(String playerName);

    NBAChatResponse chatAboutNBA(String conversationId, String message);

    NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName);
}
