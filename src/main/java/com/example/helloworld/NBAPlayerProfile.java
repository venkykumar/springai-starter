package com.example.helloworld;

import java.util.List;

public record NBAPlayerProfile(
        String playerName,
        String team,
        String position,
        List<String> strengths,
        String playingStyle,
        String summary) {
}
