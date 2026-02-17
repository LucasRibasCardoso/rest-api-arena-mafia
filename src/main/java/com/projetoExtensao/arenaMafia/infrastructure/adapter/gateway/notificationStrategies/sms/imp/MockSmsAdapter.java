package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.imp;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.SmsProvider;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.SmsStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockSmsAdapter implements SmsStrategy {

  private static final Logger logger = LoggerFactory.getLogger(MockSmsAdapter.class);

  @Override
  public SmsProvider getProvider() {
    return SmsProvider.MOCK;
  }

  @Override
  public void send(String phoneNumber, String message) {
    logger.info("--- SIMULANDO ENVIO DE SMS ---");
    logger.info("Para: {}", phoneNumber);
    logger.info("Mensagem: {}", message);
    logger.info("-----------------------------");
  }
}
