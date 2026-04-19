package com.example.helloworld;

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
            public String ask(String message) {
                return "AI response for: " + message;
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName) {
                return new NBAPlayerProfile(playerName, "Golden State Warriors", "Point Guard",
                        java.util.List.of("Shooting", "Ball handling"), "High-movement scorer",
                        "One of the most recognizable guards in the NBA.");
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message) {
                return new NBAChatResponse(conversationId, "NBA response for: " + message);
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
            public String ask(String message) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName) {
                return new NBAPlayerProfile(playerName, "Golden State Warriors", "Point Guard",
                        java.util.List.of("3-point shooting", "Off-ball movement"), "Elite perimeter creator",
                        "A dynamic offensive engine and all-time great shooter.");
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message) {
                return new NBAChatResponse(conversationId, "unused");
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
            public String ask(String message) {
                return "unused";
            }

            @Override
            public NBAPlayerProfile generateNBAPlayerProfile(String playerName) {
                return null;
            }

            @Override
            public NBAChatResponse chatAboutNBA(String conversationId, String message) {
                return new NBAChatResponse(conversationId, "Steph Curry is an elite shooter.");
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
}
