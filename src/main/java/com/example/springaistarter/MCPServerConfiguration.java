package com.example.springaistarter;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCPServerConfiguration {

    @Bean
    ToolCallbackProvider nbaMCPToolCallbackProvider(NBAMCPTools nbaMCPTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(nbaMCPTools)
                .build();
    }
}
