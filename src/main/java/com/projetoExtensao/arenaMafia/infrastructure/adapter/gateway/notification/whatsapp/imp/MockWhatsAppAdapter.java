package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp.imp;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp.WhatsAppProvider;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp.WhatsAppStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "test"})
public class MockWhatsAppAdapter implements WhatsAppStrategy {

  private static final Logger logger = LoggerFactory.getLogger(MockWhatsAppAdapter.class);

  @Override
  public WhatsAppProvider getProvider() {
    return WhatsAppProvider.MOCK;
  }

  @Override
  public void sendMessage(String phoneNumber, String message) {
    logger.info("--- SIMULANDO ENVIO DE MENSAGEM VIA WHATSAPP ---");
    logger.info("Para: {}", phoneNumber);
    logger.info("Mensagem: {}", message);
    logger.info("-------------------------------------------------");
  }
}
