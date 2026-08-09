package com.manacommunity.api.media.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Wires the AWS S3 SDK beans used exclusively by the Media Management Service.
 * Credentials are sourced from {@link MediaProperties} (loaded from application.yaml
 * or env vars), keeping them separate from the legacy storage S3 config.
 */
@Configuration
@RequiredArgsConstructor
public class MediaS3Config {

    private final MediaProperties props;

    @Bean(name = "mediaS3Client")
    public S3Client mediaS3Client() {
        return S3Client.builder()
                .region(Region.of(props.getS3().getRegion()))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                props.getS3().getAccessKey(),
                                props.getS3().getSecretKey())))
                .build();
    }

    @Bean(name = "mediaS3Presigner")
    public S3Presigner mediaS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(props.getS3().getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                props.getS3().getAccessKey(),
                                props.getS3().getSecretKey())))
                .build();
    }
}
