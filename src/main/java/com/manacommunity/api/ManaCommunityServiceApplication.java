package com.manacommunity.api;

import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.chat.memory.autoconfigure.ChatMemoryAutoConfiguration;
import org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration;
import org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Spring AI 1.0.0 targets Boot 3.x and references RestClientAutoConfiguration
// which was relocated in Boot 4. Exclude all AI auto-configs until Spring AI
// releases a Boot-4-compatible version.
@EnableAsync
@EnableScheduling
@SpringBootApplication(exclude = {
    OllamaChatAutoConfiguration.class,
    OllamaEmbeddingAutoConfiguration.class,
    ChatClientAutoConfiguration.class,
    ChatMemoryAutoConfiguration.class,
    ToolCallingAutoConfiguration.class,
    ChatObservationAutoConfiguration.class,
    EmbeddingObservationAutoConfiguration.class,
})
public class ManaCommunityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManaCommunityServiceApplication.class, args);
    }

}
