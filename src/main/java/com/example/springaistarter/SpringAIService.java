package com.example.springaistarter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

@Service
public class SpringAIService implements AIService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ChatMemory chatMemory;
    private final NBATools nbaTools;
    private final List<McpSyncClient> mcpSyncClients;
    private final Environment environment;

    public SpringAIService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ChatMemory chatMemory,
            NBATools nbaTools,
            ObjectProvider<List<McpSyncClient>> mcpSyncClientsProvider,
            Environment environment) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.chatMemory = chatMemory;
        this.nbaTools = nbaTools;
        this.mcpSyncClients = mcpSyncClientsProvider.getIfAvailable(ArrayList::new);
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

    @Override
    public NBAChatResponse chatAboutNBA(String conversationId, String message) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        String configurationError = getConfigurationError();
        if (configurationError != null) {
            return new NBAChatResponse(normalizedConversationId, configurationError);
        }

        String answer = createChatClient().prompt()
                .advisors(advisorSpec -> advisorSpec
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .param(ChatMemory.CONVERSATION_ID, normalizedConversationId))
                .tools(nbaTools)
                .system("""
                        You are an NBA assistant for a Spring AI demo application.
                        Keep continuity across the conversation and answer follow-up questions using prior context when helpful.
                        Use generally known basketball knowledge and avoid pretending to know breaking news, injuries, or live stats.
                        Use the available NBA tool when the user asks for quick facts, player summaries, or comparisons involving known players.
                        If the user asks about a player or team from earlier in the conversation, use the remembered context naturally.
                        """)
                .user(message)
                .call()
                .content();

        return new NBAChatResponse(normalizedConversationId, answer);
    }

    @Override
    public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName) {
        String normalizedPlayerName = normalizePlayerName(playerName);
        String recentGamesText = fetchRecentGamesFromMCP(normalizedPlayerName);
        List<String> recentGames = extractRecentGames(recentGamesText);

        String configurationError = getConfigurationError();
        if (configurationError != null) {
            return new NBAMCPRecentGamesResponse(
                    normalizedPlayerName,
                    "local-mcp-server",
                    "get_recent_games_summary",
                    recentGames,
                    configurationError);
        }

        String summary = createChatClient().prompt()
                .system("""
                        You are summarizing recent NBA game data that was fetched through a local MCP server.
                        Write a short, clear recap in 2-3 sentences.
                        Only use the supplied game data.
                        If the supplied data says the player is unavailable, explain that clearly and do not invent any games.
                        """)
                .user(user -> user
                        .text("""
                                Player: {playerName}
                                Recent game data from the MCP server:
                                {recentGames}

                                Summarize the player's recent form, touching on scoring, playmaking, rebounding, and any simple trend you can infer.
                                """)
                        .param("playerName", normalizedPlayerName)
                        .param("recentGames", recentGamesText))
                .call()
                .content();

        return new NBAMCPRecentGamesResponse(
                normalizedPlayerName,
                "local-mcp-server",
                "get_recent_games_summary",
                recentGames,
                summary);
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

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "demo-conversation";
        }
        return conversationId;
    }

    private String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return "Stephen Curry";
        }
        return playerName.trim();
    }

    private String fetchRecentGamesFromMCP(String playerName) {
        McpSyncClient mcpClient = getMCPClient();
        if (mcpClient == null) {
            return "The local MCP client is not configured.";
        }

        if (!mcpClient.isInitialized()) {
            mcpClient.initialize();
        }

        McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest("get_recent_games_summary", Map.of("playerName", playerName)));

        return extractText(result);
    }

    private McpSyncClient getMCPClient() {
        if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
            return null;
        }
        return mcpSyncClients.getFirst();
    }

    private String extractText(McpSchema.CallToolResult result) {
        if (result == null) {
            return "The MCP server did not return a response.";
        }

        String text = result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining("\n"));

        if (!text.isBlank()) {
            return normalizeMCPText(text);
        }

        if (result.structuredContent() != null) {
            return normalizeMCPText(String.valueOf(result.structuredContent()));
        }

        return "The MCP server returned an empty response.";
    }

    private List<String> extractRecentGames(String recentGamesText) {
        if (recentGamesText == null || recentGamesText.isBlank()) {
            return List.of();
        }

        String normalizedText = recentGamesText.replace("\r", " ").replace('\n', ' ').trim();
        int headerIndex = normalizedText.toLowerCase().indexOf("recent games:");
        String gameLines = headerIndex >= 0
                ? normalizedText.substring(headerIndex + "recent games:".length()).trim()
                : normalizedText;

        return List.of(gameLines.split("\\s+-\\s+"))
                .stream()
                .map(String::trim)
                .map(line -> line.startsWith("- ") ? line.substring(2) : line)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String normalizeMCPText(String rawText) {
        if (rawText == null) {
            return "";
        }

        String normalized = rawText.trim()
                .replace("\\n", "\n")
                .replace("\\\"", "\"");

        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() >= 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        return normalized.trim();
    }
}
