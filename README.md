# springai-starter

This project is a small starting point for learning Spring Boot and Spring AI.

## What it does

- Starts a Spring Boot web app
- Exposes `GET /hello`
- Returns `Hello, world!`
- Exposes `GET /ask`
- Sends your prompt to an OpenAI model through Spring AI

## Run it

```bash
mvn spring-boot:run
```

Then open:

- `http://localhost:8080/hello`
- `http://localhost:8080/ask?message=What%20is%20Spring%20AI`

## Configure your API key

Set your OpenAI API key before starting the app:

```bash
export OPENAI_API_KEY=your_api_key_here
export SPRING_AI_MODEL_CHAT=openai
```

The app reads these values through:

- `spring.ai.model.chat=${SPRING_AI_MODEL_CHAT:none}`
- `spring.ai.openai.api-key=${OPENAI_API_KEY:}`

## Run tests

```bash
mvn test
```

## How it is wired

- `HelloController` handles the basic `/hello` endpoint
- `AiController` handles the `/ask` endpoint
- `SpringAiService` uses Spring AI's `ChatClient` to call the model
