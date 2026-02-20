package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsStrategy;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SmsConfig {

  private static final Logger logger = LoggerFactory.getLogger(SmsConfig.class);

  @Bean
  @Primary
  public SmsPort smsPort(List<SmsStrategy> strategies, @Value("${app.notification.sms-provider:MOCK}") String providerName) {

    SmsStrategy selectedStrategy =
        strategies.stream()
            .filter(s -> s.getProvider().name().equalsIgnoreCase(providerName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Nenhum provedor de SMS encontrado para: " + providerName));

    logger.info("SMS Provider configurado: {}", selectedStrategy.getProvider());
    return selectedStrategy;
  }
}
