package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.scheduler.SchedulerClient;

@Configuration
public class AwsSchedulerConfig {

  @Value("${spring.cloud.aws.region.static}")
  private String region;

  @Value("${app.aws.scheduler.endpoint:}")
  private String endpoint;

  @Bean
  public SchedulerClient schedulerClient() {
    var builder =
        SchedulerClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(Region.of(region));

    if (endpoint != null && !endpoint.isBlank()) {
      builder.endpointOverride(URI.create(endpoint));
    }

    return builder.build();
  }
}
