package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

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

  @Bean
  public SchedulerClient schedulerClient() {
    return SchedulerClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.create())
        .region(Region.of(region))
        .build();
  }
}
