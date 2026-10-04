package com.uni.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("lambda")
public class S3Config {

    @Bean
    public S3Client s3Client(Environment env) {
        String region = env.getProperty("app.s3.region", "us-east-2");
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}