package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

import com.projetoExtensao.arenaMafia.application.notification.gateway.WhatsAppPort;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp.WhatsAppStrategy;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class WhatsAppConfig {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppConfig.class);

  @Bean
  @Primary
  public WhatsAppPort whatsAppPort(
      List<WhatsAppStrategy> strategies,
      @Value("${app.notification.whatsapp-provider:MOCK}") String providerName) {

    WhatsAppStrategy selectedStrategy =
        strategies.stream()
            .filter(s -> s.getProvider().name().equalsIgnoreCase(providerName))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "ERRO CRÍTICO: Nenhum provedor WhatsApp configurado para: "
                            + providerName));

    logger.info("WhatsApp Provider configurado: {}", selectedStrategy.getProvider());
    return selectedStrategy;
  }
}
