package com.example.helloworld;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SpringAiService implements AiService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment environment;

    public SpringAiService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, Environment environment) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.environment = environment;
    }

    @Override
    public String ask(String message) {
        String chatModel = environment.getProperty("spring.ai.model.chat", "none");
        if (!"openai".equalsIgnoreCase(chatModel)) {
            return "Enable Spring AI by setting SPRING_AI_MODEL_CHAT=openai and OPENAI_API_KEY.";
        }

        String apiKey = environment.getProperty("spring.ai.openai.api-key");
        if (apiKey == null || apiKey.isBlank()) {
            return "Set OPENAI_API_KEY before calling /ask.";
        }

        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            return "Spring AI chat client is not available. Check your Spring AI configuration.";
        }

        return chatClientBuilder.build()
                .prompt()
                .user(message)
                .call()
                .content();
    }
}
