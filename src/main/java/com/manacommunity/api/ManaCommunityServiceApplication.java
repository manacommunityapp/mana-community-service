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

import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Spring AI targets Boot 3.x/4.x and references components like
// RestClientAutoConfiguration / RetryListener that cause startup issues.
// Exclude all AI auto-configs to prevent eager introspection failures.
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = "com.manacommunity")
@EnableJpaRepositories(basePackages = "com.manacommunity")
@SpringBootApplication(
    scanBasePackages = "com.manacommunity",
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class,
    exclude = {
        OllamaApiAutoConfiguration.class,
        OllamaChatAutoConfiguration.class,
        OllamaEmbeddingAutoConfiguration.class,
        ChatClientAutoConfiguration.class,
        ChatMemoryAutoConfiguration.class,
        ToolCallingAutoConfiguration.class,
        ChatObservationAutoConfiguration.class,
        EmbeddingObservationAutoConfiguration.class,
        SpringAiRetryAutoConfiguration.class,
    },
    excludeName = {
        "org.springframework.ai.retry.autoconfigure.SpringAiRetryAutoConfiguration",
        "org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration"
    }
)
public class ManaCommunityServiceApplication {

    @jakarta.annotation.PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ManaCommunityServiceApplication.class);
        app.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        app.run(args);
    }

}
