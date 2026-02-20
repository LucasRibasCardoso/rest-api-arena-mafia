package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.imp;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsProvider;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockSmsAdapter implements SmsStrategy {

  private static final Logger logger = LoggerFactory.getLogger(MockSmsAdapter.class);

  @Override
  public SmsProvider getProvider() {
    return SmsProvider.MOCK;
  }

  @Override
  public void sendMessage(String phoneNumber, String message) {
    logger.info("--- SIMULANDO ENVIO DE SMS ---");
    logger.info("Para: {}", phoneNumber);
    logger.info("Mensagem: {}", message);
    logger.info("-----------------------------");
  }
}
