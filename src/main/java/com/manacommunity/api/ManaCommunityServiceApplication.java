package com.manacommunity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// All Spring AI auto-configuration exclusions are handled via spring.autoconfigure.exclude
// in application.yaml using string-based class names, which safely skips missing or
// non-auto-configuration classes. Do NOT put Spring AI classes in this annotation's
// exclude={} — that requires the class to be a registered @AutoConfiguration, which
// several Spring AI 1.x classes are not (e.g. OllamaApiAutoConfiguration).
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = "com.manacommunity")
@EnableJpaRepositories(basePackages = "com.manacommunity")
@SpringBootApplication(
    scanBasePackages = "com.manacommunity",
    nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
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
