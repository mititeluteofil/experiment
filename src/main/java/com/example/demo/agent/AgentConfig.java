package com.example.demo.agent;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    private final BankingTools bankingTools;

    @Value("${agent.memory-window:20}")
    private int memoryWindow;

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(memoryWindow)
                .build();
    }

    // AnthropicChatModel is auto-configured by langchain4j-anthropic-spring-boot-starter
    @Bean
    public AgentAssistant agentAssistant(AnthropicChatModel model, ChatMemoryProvider chatMemoryProvider) {
        return AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(model)
                .tools(bankingTools)
                .chatMemoryProvider(chatMemoryProvider)
                .build();
    }
}
