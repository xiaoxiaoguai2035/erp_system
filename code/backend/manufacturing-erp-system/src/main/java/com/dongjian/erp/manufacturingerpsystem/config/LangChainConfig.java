package com.dongjian.erp.manufacturingerpsystem.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Bean
    @ConditionalOnExpression("${erp.ai.enabled:false} and '${erp.ai.api-key:}' != ''")
    public ChatModel chatModel(AiProperties aiProperties) {
        return OpenAiChatModel.builder()
                .apiKey(aiProperties.getApiKey())
                .baseUrl(aiProperties.getBaseUrl())
                .modelName(aiProperties.getModelName())
                .build();
    }
}
