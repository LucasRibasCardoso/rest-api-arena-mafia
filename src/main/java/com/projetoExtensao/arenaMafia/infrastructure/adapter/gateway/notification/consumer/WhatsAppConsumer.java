package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.consumer;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.dto.NotificationDto;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp.WhatsAppClient;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppConsumer {

  private static final Logger logger = LoggerFactory.getLogger(WhatsAppConsumer.class);
  private final WhatsAppClient whatsAppPort;

  public WhatsAppConsumer(WhatsAppClient whatsAppPort) {
    this.whatsAppPort = whatsAppPort;
  }

  /**
   * Consumidor de mensagens do AWS SQS para enviar notificações via WhatsApp. Em caso de falha no
   * envio, a mensagem será reprocessada automaticamente pelo SQS, garantindo a entrega eventual.
   *
   * @param notificationDto DTO contendo o número de telefone e o conteúdo da mensagem a ser
   *     enviada.
   */
  @SqsListener("${app.queue.whatsapp-transactional-queue}")
  public void consume(NotificationDto notificationDto) {
    try {
      whatsAppPort.sendMessage(notificationDto.phone(), notificationDto.content());
    } catch (Exception e) {
      logger.warn("Falha temporária no WhatsApp Transacional. O SQS tentará novamente.");
      throw e;
    }
  }
}
