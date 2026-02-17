package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.whatsapp;

import com.projetoExtensao.arenaMafia.application.notification.gateway.WhatsAppPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppAdapter implements WhatsAppPort {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppAdapter.class);

  @Override
  public void send(String phoneNumber, String message) {
    // TODO: Implementar a integração real com um gateway de SMS
    logger.info("--- SIMULANDO ENVIO DE MENSAGEM VIA WHATSAPP ---");
    logger.info("Para: {}", phoneNumber);
    logger.info("Mensagem: {}", message);
    logger.info("-------------------------------------------------");
  }
}
