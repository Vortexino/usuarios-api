package com.uni.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
@Profile("lambda")
public class SnsConfig {

    @Bean
    public SnsClient snsClient(Environment env) {
        String region = env.getProperty("app.s3.region", "us-east-2");
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }
}