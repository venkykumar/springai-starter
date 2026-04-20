package com.example.springaistarter;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIControllerTest {

    @Test
    void askEndpointReturnsServiceResponse() throws Exception {
        AIService aiService = new AIService() {
            @Override
            public String ask(String message, String model) {
                return "AI response for: " + message;
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
                return new NBAPlayerProfile(playerName, "Golden State Warriors", "Point Guard",
                        java.util.List.of("Shooting", "Ball handling"), "High-movement scorer",
                        "One of the most recognizable guards in the NBA.");
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
                return new NBAChatResponse(conversationId, "NBA response for: " + message);
            }

            @Override
            public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
                return new NBAMCPRecentGamesResponse(playerName, "local-mcp-server", "get_recent_games_summary",
                        java.util.List.of("2026-04-17 vs Lakers: 31 points, 8 assists."), "Recent games summary");
            }

            @Override
            public NBARagResponse askWithRAG(String question, String model) {
                return new NBARagResponse(question, "RAG answer", java.util.List.of());
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AIController(aiService)).build();

        mockMvc.perform(get("/ask").param("message", "What is Spring AI?"))
                .andExpect(status().isOk())
                .andExpect(content().string("AI response for: What is Spring AI?"));
    }

    @Test
    void nbaPlayerProfileEndpointReturnsStructuredResponse() throws Exception {
        AIService aiService = new AIService() {
            @Override
            public String ask(String message, String model) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
                return new NBAPlayerProfile(playerName, "Golden State Warriors", "Point Guard",
                        java.util.List.of("3-point shooting", "Off-ball movement"), "Elite perimeter creator",
                        "A dynamic offensive engine and all-time great shooter.");
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
                return new NBAChatResponse(conversationId, "unused");
            }

            @Override
            public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
                return null;
            }

            @Override
            public NBARagResponse askWithRAG(String question, String model) {
                return new NBARagResponse(question, "unused", java.util.List.of());
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AIController(aiService)).build();

        mockMvc.perform(get("/nba/player-profile").param("playerName", "Stephen Curry"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "playerName": "Stephen Curry",
                          "team": "Golden State Warriors",
                          "position": "Point Guard",
                          "strengths": ["3-point shooting", "Off-ball movement"],
                          "playingStyle": "Elite perimeter creator",
                          "summary": "A dynamic offensive engine and all-time great shooter."
                        }
                        """));
    }

    @Test
    void nbaChatEndpointReturnsConversationAwareResponse() throws Exception {
        AIService aiService = new AIService() {
            @Override
            public String ask(String message, String model) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
                return null;
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
                return new NBAChatResponse(conversationId, "Steph Curry is an elite shooter.");
            }

            @Override
            public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
                return null;
            }

            @Override
            public NBARagResponse askWithRAG(String question, String model) {
                return new NBARagResponse(question, "unused", java.util.List.of());
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AIController(aiService)).build();

        mockMvc.perform(get("/nba/chat")
                        .param("conversationId", "warriors-thread")
                        .param("message", "Tell me about Steph Curry"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "conversationId": "warriors-thread",
                          "answer": "Steph Curry is an elite shooter."
                        }
                        """));
    }

    @Test
    void nbaMCPRecentGamesEndpointReturnsStructuredResponse() throws Exception {
        AIService aiService = new AIService() {
            @Override
            public String ask(String message, String model) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
                return null;
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
                return null;
            }

            @Override
            public NBARagResponse askWithRAG(String question, String model) {
                return new NBARagResponse(question, "unused", java.util.List.of());
            }

            @Override
            public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
                return new NBAMCPRecentGamesResponse(
                        playerName,
                        "local-mcp-server",
                        "get_recent_games_summary",
                        java.util.List.of(
                                "2026-04-17 vs Lakers: 31 points, 5 rebounds, 8 assists in a 118-109 win.",
                                "2026-04-14 vs Clippers: 27 points, 4 rebounds, 6 assists in a 111-115 loss."),
                        "Stephen Curry has been scoring efficiently while still creating offense for teammates.");
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AIController(aiService)).build();

        mockMvc.perform(get("/nba/mcp/recent-games-summary").param("playerName", "Stephen Curry"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "playerName": "Stephen Curry",
                          "source": "local-mcp-server",
                          "toolName": "get_recent_games_summary",
                          "recentGames": [
                            "2026-04-17 vs Lakers: 31 points, 5 rebounds, 8 assists in a 118-109 win.",
                            "2026-04-14 vs Clippers: 27 points, 4 rebounds, 6 assists in a 111-115 loss."
                          ],
                          "summary": "Stephen Curry has been scoring efficiently while still creating offense for teammates."
                        }
                        """));
    }

    @Test
    void ragAskEndpointReturnsStructuredResponse() throws Exception {
        AIService aiService = new AIService() {
            @Override
            public String ask(String message, String model) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
                return null;
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
                return null;
            }

            @Override
            public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
                return null;
            }

            @Override
            public NBARagResponse askWithRAG(String question, String model) {
                return new NBARagResponse(
                        question,
                        "Stephen Curry holds the NBA record for most career three-pointers made.",
                        java.util.List.of(
                                "Stephen Curry holds the NBA record for the most career three-pointers made.",
                                "Stephen Curry set the single-season three-point record in 2015-16 by making 402 three-pointers."));
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AIController(aiService)).build();

        mockMvc.perform(get("/nba/ask")
                        .param("question", "Who has the most three-pointers in NBA history?"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "question": "Who has the most three-pointers in NBA history?",
                          "answer": "Stephen Curry holds the NBA record for most career three-pointers made.",
                          "retrievedChunks": [
                            "Stephen Curry holds the NBA record for the most career three-pointers made.",
                            "Stephen Curry set the single-season three-point record in 2015-16 by making 402 three-pointers."
                          ]
                        }
                        """));
    }
}