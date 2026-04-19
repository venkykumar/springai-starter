package com.example.helloworld;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiControllerTest {

    @Test
    void askEndpointReturnsServiceResponse() throws Exception {
        AiService aiService = message -> "AI response for: " + message;
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AiController(aiService)).build();

        mockMvc.perform(get("/ask").param("message", "What is Spring AI?"))
                .andExpect(status().isOk())
                .andExpect(content().string("AI response for: What is Spring AI?"));
    }
}
