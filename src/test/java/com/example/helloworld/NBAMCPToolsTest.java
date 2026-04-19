package com.example.helloworld;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NBAMCPToolsTest {

    private final NBAMCPTools nbaMCPTools = new NBAMCPTools();

    @Test
    void recentGamesSummaryReturnsDataForSupportedPlayer() {
        String summary = nbaMCPTools.getRecentGamesSummary("Stephen Curry");

        assertTrue(summary.contains("Stephen Curry"));
        assertTrue(summary.contains("recent games"));
    }

    @Test
    void supportedPlayersListsKnownPlayers() {
        String supportedPlayers = nbaMCPTools.listSupportedPlayers();

        assertTrue(supportedPlayers.contains("Stephen Curry"));
        assertTrue(supportedPlayers.contains("LeBron James"));
    }
}
