package com.example.helloworld;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SpringAIService implements AIService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment environment;

    public SpringAIService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, Environment environment) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.environment = environment;
    }

    @Override
    public String ask(String message) {
        String configurationError = getConfigurationError();
        if (configurationError != null) {
            return configurationError;
        }

        return createChatClient().prompt()
                .system("""
                        You are a helpful Java, Spring Boot, and Spring AI assistant.
                        Give concise, accurate answers for developers.
                        Prefer practical explanations over vague summaries.
                        If the user asks about Spring AI, explain it as the Spring project for integrating AI models and workflows into Spring applications.
                        Do not mention knowledge cutoffs or speculate about hidden context.
                        """)
                .user(message)
                .call()
                .content();
    }

    @Override
    public NBAPlayerProfile generateNBAPlayerProfile(String playerName) {
        String configurationError = getConfigurationError();
        if (configurationError != null) {
            return new NBAPlayerProfile(
                    playerName,
                    "Unavailable",
                    "Unavailable",
                    List.of(),
                    "Unavailable",
                    configurationError);
        }

        return createChatClient().prompt()
                .user(user -> user
                        .text("""
                                Create a concise NBA player profile for {playerName}.
                                Return structured data only.
                                Use generally known basketball information and avoid time-sensitive stats, recent injuries, or breaking news.
                                Focus on team, position, playing style, strengths, and a short summary.
                                """)
                        .param("playerName", playerName))
                .call()
                .entity(NBAPlayerProfile.class);
    }

    private ChatClient createChatClient() {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new IllegalStateException("Spring AI chat client is not available. Check your Spring AI configuration.");
        }

        return chatClientBuilder.build();
    }

    private String getConfigurationError() {
        String chatModel = environment.getProperty("spring.ai.model.chat", "none");
        if (!"openai".equalsIgnoreCase(chatModel)) {
            return "Enable Spring AI by setting SPRING_AI_MODEL_CHAT=openai and OPENAI_API_KEY.";
        }

        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return "Set OPENAI_API_KEY before calling AI endpoints.";
        }

        return null;
    }
}
