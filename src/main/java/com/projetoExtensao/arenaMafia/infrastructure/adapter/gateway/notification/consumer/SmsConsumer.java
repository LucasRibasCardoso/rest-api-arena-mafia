package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.consumer;

import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.dto.NotificationDto;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsConsumer {

  private static final Logger logger = LoggerFactory.getLogger(SmsConsumer.class);
  private final SmsPort smsPort;

  public SmsConsumer(SmsPort smsPort) {
    this.smsPort = smsPort;
  }

  @SqsListener("${app.queue.sms}")
  public void consume(NotificationDto notificationDto) {
    try {
      smsPort.sendMessage(notificationDto.phone(), notificationDto.content());
    } catch (Exception e) {
      logger.error("Erro fatal ao tentar enviar SMS (descartado): {}", e.getMessage());
    }
  }
}
