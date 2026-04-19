package com.example.helloworld;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class NBAMCPTools {

    private static final Map<String, List<String>> RECENT_GAME_SUMMARIES = Map.of(
            "stephen curry", List.of(
                    "2026-04-17 vs Lakers: 31 points, 5 rebounds, 8 assists in a 118-109 win.",
                    "2026-04-14 vs Clippers: 27 points, 4 rebounds, 6 assists in a 111-115 loss.",
                    "2026-04-11 vs Suns: 36 points, 6 rebounds, 7 assists in a 122-116 win."),
            "lebron james", List.of(
                    "2026-04-17 vs Warriors: 28 points, 9 rebounds, 7 assists in a 109-118 loss.",
                    "2026-04-14 vs Kings: 25 points, 8 rebounds, 11 assists in a 120-112 win.",
                    "2026-04-11 vs Grizzlies: 30 points, 10 rebounds, 8 assists in a 117-110 win."),
            "nikola jokic", List.of(
                    "2026-04-17 vs Timberwolves: 26 points, 13 rebounds, 12 assists in a 114-106 win.",
                    "2026-04-14 vs Thunder: 24 points, 11 rebounds, 9 assists in a 108-113 loss.",
                    "2026-04-11 vs Jazz: 21 points, 14 rebounds, 10 assists in a 125-101 win."));

    @Tool(name = "get_recent_games_summary",
            description = "Get the recent games for a supported NBA player from the local MCP server demo.")
    public String getRecentGamesSummary(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "No player name was provided.";
        }

        String normalizedPlayerName = playerName.trim().toLowerCase();
        List<String> games = RECENT_GAME_SUMMARIES.get(normalizedPlayerName);

        if (games == null) {
            return "No recent game summaries are available for " + playerName
                    + " in the local MCP server demo.";
        }

        return toDisplayName(normalizedPlayerName) + " recent games:\n- " + String.join("\n- ", games);
    }

    @Tool(name = "list_supported_players",
            description = "List the NBA players supported by the local MCP server demo.")
    public String listSupportedPlayers() {
        return "Supported players: Stephen Curry, LeBron James, Nikola Jokic.";
    }

    private String toDisplayName(String normalizedPlayerName) {
        return switch (normalizedPlayerName) {
            case "stephen curry" -> "Stephen Curry";
            case "lebron james" -> "LeBron James";
            case "nikola jokic" -> "Nikola Jokic";
            default -> normalizedPlayerName;
        };
    }
}
