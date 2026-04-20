package com.example.springaistarter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class RAGConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RAGConfiguration.class);

    @Bean
    @ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.trim().isEmpty()")
    public VectorStore nbaVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        try {
            TextReader reader = new TextReader(new ClassPathResource("nba-knowledge-base.txt"));
            reader.getCustomMetadata().put("source", "nba-knowledge-base");
            List<Document> documents = new TokenTextSplitter().apply(reader.get());
            store.add(documents);
            log.info("NBA knowledge base loaded: {} chunks ingested", documents.size());
        } catch (Exception e) {
            log.warn("NBA knowledge base ingestion failed (check OPENAI_API_KEY): {}", e.getMessage());
        }
        return store;
    }
}