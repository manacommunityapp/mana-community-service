package com.manacommunity.platform.sqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wires AWS SDK v2 {@link SqsClient} only when {@code app.sqs.enabled=true}.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SqsProperties.class)
public class SqsConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.sqs", name = "enabled", havingValue = "true")
    public SqsClient sqsClient(SqsProperties props) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(props.getRegion()));

        if (isValidCredential(props.getAccessKey()) && isValidCredential(props.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(props.getEndpoint()));
            log.info("Configured SqsClient with custom endpoint={}", props.getEndpoint());
        } else {
            log.info("Configured SqsClient for region={}", props.getRegion());
        }
        return builder.build();
    }

    static String resolveQueueUrl(
            SqsClient sqsClient,
            ConcurrentHashMap<String, String> cache,
            String queueNameOrUrl) {
        if (queueNameOrUrl.startsWith("https://") || queueNameOrUrl.startsWith("http://")) {
            return queueNameOrUrl;
        }
        return cache.computeIfAbsent(queueNameOrUrl, name ->
                sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()).queueUrl());
    }

    private static boolean isValidCredential(String val) {
        return val != null && !val.isBlank() && !val.startsWith("YOUR_AWS_");
    }
}
