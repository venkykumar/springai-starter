package com.example.springaistarter;

import java.util.List;

public record NBAMCPRecentGamesResponse(
        String playerName,
        String source,
        String toolName,
        List<String> recentGames,
        String summary) {
}
