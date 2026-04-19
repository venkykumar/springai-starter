package com.example.helloworld;

import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class NBATools {

    private static final Map<String, String> PLAYER_FACTS = Map.of(
            "stephen curry", """
                    Team: Golden State Warriors
                    Position: Point Guard
                    Style: Elite shooter, off-ball mover, and offensive engine
                    Known for: Deep three-point shooting, ball handling, gravity, and late-game shot making
                    """,
            "lebron james", """
                    Team: Los Angeles Lakers
                    Position: Forward
                    Style: Point forward with power, vision, and downhill scoring
                    Known for: Playmaking, transition offense, strength, and longevity
                    """,
            "luka doncic", """
                    Team: Los Angeles Lakers
                    Position: Guard-Forward
                    Style: Ball-dominant creator who controls pace and creates mismatches
                    Known for: Pick-and-roll orchestration, step-back shooting, and passing vision
                    """,
            "nikola jokic", """
                    Team: Denver Nuggets
                    Position: Center
                    Style: Playmaking center with touch, size, and elite decision-making
                    Known for: Passing, post scoring, rebounding, and tempo control
                    """);

    @Tool(description = "Get quick, stable NBA player facts for a known player.")
    public String getPlayerQuickFacts(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "No player name was provided.";
        }

        String facts = PLAYER_FACTS.get(playerName.trim().toLowerCase());
        if (facts == null) {
            return "No quick facts are available for " + playerName
                    + ". Use general NBA knowledge and clearly state when you are answering without tool data.";
        }

        return facts;
    }
}
