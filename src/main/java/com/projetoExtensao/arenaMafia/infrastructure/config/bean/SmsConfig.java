package com.projetoExtensao.arenaMafia.infrastructure.config.bean;

import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.SmsStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class SmsConfig {

  private static final Logger logger = LoggerFactory.getLogger(SmsConfig.class);

  @Bean
  @Primary
  public SmsPort smsPort(SmsStrategy smsStrategy) {
    logger.info("SMS Provider configurado: {}", smsStrategy.getProvider());
    return smsStrategy;
  }
}

