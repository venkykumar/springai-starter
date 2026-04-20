package com.example.springaistarter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

@Service
public class SpringAIService implements AIService {

    private final ObjectProvider<OpenAiChatModel> openAiChatModelProvider;
    private final ObjectProvider<AnthropicChatModel> anthropicChatModelProvider;
    private final ChatMemory chatMemory;
    private final NBATools nbaTools;
    private final List<McpSyncClient> mcpSyncClients;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final Environment environment;

    public SpringAIService(ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
            ObjectProvider<AnthropicChatModel> anthropicChatModelProvider,
            ChatMemory chatMemory,
            NBATools nbaTools,
            ObjectProvider<List<McpSyncClient>> mcpSyncClientsProvider,
            ObjectProvider<VectorStore> vectorStoreProvider,
            Environment environment) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.anthropicChatModelProvider = anthropicChatModelProvider;
        this.chatMemory = chatMemory;
        this.nbaTools = nbaTools;
        this.mcpSyncClients = mcpSyncClientsProvider.getIfAvailable(ArrayList::new);
        this.vectorStoreProvider = vectorStoreProvider;
        this.environment = environment;
    }

    @Override
    public String ask(String message, String model) {
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return configError;
        }

        return createChatClient(resolvedModel).prompt()
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
    public Flux<String> streamAsk(String message, String model) {
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return Flux.just(configError);
        }

        return createChatClient(resolvedModel).prompt()
                .system("""
                        You are a helpful Java, Spring Boot, and Spring AI assistant.
                        Give concise, accurate answers for developers.
                        Prefer practical explanations over vague summaries.
                        If the user asks about Spring AI, explain it as the Spring project for integrating AI models and workflows into Spring applications.
                        Do not mention knowledge cutoffs or speculate about hidden context.
                        """)
                .user(message)
                .stream()
                .content();
    }

    @Override
    public NBAPlayerProfile generateNBAPlayerProfile(String playerName, String model) {
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return new NBAPlayerProfile(playerName, "Unavailable", "Unavailable", List.of(), "Unavailable", configError);
        }

        return createChatClient(resolvedModel).prompt()
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
    public NBAChatResponse chatAboutNBA(String conversationId, String message, String model) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return new NBAChatResponse(normalizedConversationId, configError);
        }

        String answer = createChatClient(resolvedModel).prompt()
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
    public NBAMCPRecentGamesResponse summarizeRecentGamesWithMCP(String playerName, String model) {
        String normalizedPlayerName = normalizePlayerName(playerName);
        String recentGamesText = fetchRecentGamesFromMCP(normalizedPlayerName);
        List<String> recentGames = extractRecentGames(recentGamesText);

        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return new NBAMCPRecentGamesResponse(normalizedPlayerName, "local-mcp-server", "get_recent_games_summary", recentGames, configError);
        }

        String summary = createChatClient(resolvedModel).prompt()
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

        return new NBAMCPRecentGamesResponse(normalizedPlayerName, "local-mcp-server", "get_recent_games_summary", recentGames, summary);
    }

    @Override
    public NBARagResponse askWithRAG(String question, String model) {
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return new NBARagResponse(question, configError, List.of());
        }

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return new NBARagResponse(question,
                    "Vector store not available. Ensure OPENAI_API_KEY is set and the embedding model is enabled.",
                    List.of());
        }

        List<Document> relevantDocs = vectorStore.similaritySearch(question);
        List<String> retrievedChunks = relevantDocs.stream()
                .map(Document::getText)
                .toList();

        String context = String.join("\n\n", retrievedChunks);

        String answer = createChatClient(resolvedModel).prompt()
                .system("""
                        You are an NBA knowledge assistant.
                        Answer the user's question using ONLY the context provided below.
                        If the answer cannot be found in the context, say "I don't have that information in my knowledge base."
                        Do not use any outside knowledge beyond what is provided.
                        """)
                .user(user -> user
                        .text("""
                                Context:
                                {context}

                                Question: {question}
                                """)
                        .param("context", context.isBlank() ? "No relevant context found." : context)
                        .param("question", question))
                .call()
                .content();

        return new NBARagResponse(question, answer, retrievedChunks);
    }

    @Override
    public String describeImage(String imageUrl, String prompt, String model) {
        String resolvedModel = resolveModel(model);
        String configError = getConfigurationError(resolvedModel);
        if (configError != null) {
            return configError;
        }

        String effectivePrompt = (prompt == null || prompt.isBlank()) ? "Describe this image in detail." : prompt;
        MimeType mimeType = detectMimeType(imageUrl);

        try {
            URL url = new URL(imageUrl);
            return createChatClient(resolvedModel).prompt()
                    .user(u -> u.text(effectivePrompt).media(mimeType, url))
                    .call()
                    .content();
        } catch (Exception e) {
            return "Could not process image: " + e.getMessage();
        }
    }

    private MimeType detectMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (lower.contains(".gif")) return MimeTypeUtils.IMAGE_GIF;
        if (lower.contains(".webp")) return MimeType.valueOf("image/webp");
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private String resolveModel(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim().toLowerCase();
        }
        return environment.getProperty("spring.ai.model.chat", "none").toLowerCase();
    }

    private String getConfigurationError(String resolvedModel) {
        return switch (resolvedModel) {
            case "openai" -> {
                String key = environment.getProperty("spring.ai.openai.api-key");
                yield (key == null || key.isBlank()) ? "Set OPENAI_API_KEY before calling AI endpoints." : null;
            }
            case "anthropic" -> {
                String key = environment.getProperty("spring.ai.anthropic.api-key");
                yield (key == null || key.isBlank()) ? "Set ANTHROPIC_API_KEY before calling AI endpoints." : null;
            }
            default -> "Set SPRING_AI_MODEL_CHAT=openai or SPRING_AI_MODEL_CHAT=anthropic, along with the corresponding API key.";
        };
    }

    private ChatClient createChatClient(String resolvedModel) {
        return switch (resolvedModel) {
            case "openai" -> {
                OpenAiChatModel m = openAiChatModelProvider.getIfAvailable();
                if (m == null) throw new IllegalStateException("OpenAI model not configured. Set OPENAI_API_KEY.");
                yield ChatClient.builder(m).build();
            }
            case "anthropic" -> {
                AnthropicChatModel m = anthropicChatModelProvider.getIfAvailable();
                if (m == null) throw new IllegalStateException("Anthropic model not configured. Set ANTHROPIC_API_KEY.");
                yield ChatClient.builder(m).build();
            }
            default -> throw new IllegalStateException("Unsupported model: " + resolvedModel);
        };
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