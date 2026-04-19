package com.example.helloworld;

import java.util.List;

public record NBAMCPRecentGamesResponse(
        String playerName,
        String source,
        String toolName,
        List<String> recentGames,
        String summary) {
}
