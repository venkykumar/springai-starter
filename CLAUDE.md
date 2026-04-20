# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean install

# Run (requires env vars below)
mvn spring-boot:run

# All tests
mvn test

# Single test class
mvn test -Dtest=AIControllerTest

# Single test method
mvn test -Dtest=AIControllerTest#nbaPlayerProfile_ReturnsStructuredOutput
```

## Required Environment Variables

```bash
export SPRING_AI_MODEL_CHAT=openai
export OPENAI_API_KEY=sk-...
```

Without these, AI endpoints return informational error messages instead of failing — handled in `SpringAIService.getConfigurationError()`.

## Architecture

Single Spring Boot app (Java 25, Spring Boot 3.5.x, Spring AI 1.1.x) that demos five Spring AI features in sequence.

**Request flow:** `AIController` → `AIService` (interface) → `SpringAIService` (all logic lives here, ~273 lines)

**The five patterns demonstrated:**

| Endpoint | Pattern | Key API |
|---|---|---|
| `GET /ask` | Guided chat | `ChatClient.prompt().system(...).user(...).call()` |
| `GET /nba/player-profile` | Structured output | `.entity(NBAPlayerProfile.class)` |
| `GET /nba/chat` | Conversation memory | `MessageChatMemoryAdvisor` + per-`conversationId` `ChatMemory` |
| `GET /nba/chat` | Tool calling | `@Tool` on `NBATools.getPlayerQuickFacts()`, wired via `.tools(nbaTools)` |
| `GET /nba/mcp/recent-games-summary` | MCP | App exposes itself as MCP server on `/mcp`; `McpSyncClient` calls it back |
| `GET /nba/ask` | RAG | Similarity search on `SimpleVectorStore` → context chunks → grounded answer |

**RAG architecture:** `RAGConfiguration` creates a `SimpleVectorStore` bean (`@ConditionalOnBean(EmbeddingModel.class)`), reads `src/main/resources/nba-knowledge-base.txt` via `TextReader`, chunks it with `TokenTextSplitter`, and calls `store.add()` at startup (wrapped in try-catch so a missing API key doesn't prevent boot). `SpringAIService.askWithRAG()` calls `vectorStore.similaritySearch(question)`, passes the top chunks as `{context}` in a user message, and returns the answer alongside `retrievedChunks` in `NBARagResponse`.

**MCP architecture:** The app is simultaneously an MCP server (`spring-ai-starter-mcp-server-webmvc`, Streamable HTTP) and an MCP client. `MCPServerConfiguration` registers `NBAMCPTools` methods via `MethodToolCallbackProvider`. The client calls `http://localhost:{port}/mcp`.

**Memory:** In-memory only (no persistence). `ChatMemory` bean is shared; conversations are keyed by `conversationId` query param.

**Models:** Java records — `NBAPlayerProfile`, `NBAChatResponse`, `NBAMCPRecentGamesResponse`. Spring AI maps structured output directly into these.

## Testing Approach

Tests use standalone MockMvc (no `@SpringBootTest`) with a mock `AIService` implementation — no real AI calls. Three test classes: `HelloControllerTest`, `AIControllerTest`, `NBAMCPToolsTest`. JSON assertions use `.content().json(expected)`.

## Configuration

All config in `src/main/resources/application.properties`. Model is `gpt-4o-mini` at temperature 0.7. MCP client initialization is deferred (`spring.ai.mcp.client.initialized=false`) and done programmatically in `SpringAIService`.

## UI

`src/main/resources/static/index.html` — browser playground served at `GET /` that exercises all endpoints interactively.