package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.consumer;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsClient;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.dto.NotificationDto;
import com.projetoExtensao.arenaMafia.infrastructure.utils.PhoneFormatterUtils;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppFallbackConsumer {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppFallbackConsumer.class);
  private final SmsClient smsPort;

  public WhatsAppFallbackConsumer(SmsClient smsPort) {
    this.smsPort = smsPort;
  }

  @SqsListener("${app.queue.whatsapp-transactional-dlq}")
  public void recover(NotificationDto notificationDto) {
    String maskedPhone = PhoneFormatterUtils.maskPhoneNumber(notificationDto.phone());
    logger.warn("FALLBACK: WhatsApp falhou 3x. Enviando SMS backup para {}", maskedPhone);
    try {
      smsPort.sendMessage(notificationDto.phone(), notificationDto.content());
    } catch (Exception e) {
      logger.error("Erro fatal ao tentar enviar SMS de fallback (descartado): {}", e.getMessage());
    }
  }
}
