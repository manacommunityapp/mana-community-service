package com.manacommunity.api.media.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed binding for the {@code app.media} configuration block.
 * Keeps all media-service settings in one place and makes them injectable
 * without scattering @Value annotations across classes.
 */
@Configuration
@ConfigurationProperties(prefix = "app.media")
@Getter
@Setter
public class MediaProperties {

    private S3Props s3 = new S3Props();
    private CloudFrontProps cloudfront = new CloudFrontProps();
    private LimitProps limits = new LimitProps();
    private ImageProps image = new ImageProps();

    @Getter @Setter
    public static class S3Props {
        private String bucket = "manacommunityhub";
        private String region = "ap-south-1";
        private String accessKey;
        private String secretKey;
        private int presignedPutExpiryMinutes = 15;
        private int presignedGetExpiryMinutes = 60;
    }

    @Getter @Setter
    public static class CloudFrontProps {
        /** When set, CloudFront URLs are returned instead of presigned S3 GET URLs. */
        private String domain = "";
    }

    @Getter @Setter
    public static class LimitProps {
        private int maxImageSizeMb = 20;
        private int maxVideoSizeMb = 500;
        private int maxDocumentSizeMb = 50;
    }

    @Getter @Setter
    public static class ImageProps {
        private int thumbnailWidth = 400;
        private int mediumWidth = 1920;
    }
}
