package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.dto.NotificationDto;
import com.projetoExtensao.arenaMafia.infrastructure.utils.PhoneFormatterUtils;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationProducer {

  private static final Logger logger = LoggerFactory.getLogger(NotificationProducer.class);

  private final String smsQueue;
  private final String whatsappQueue;
  private final SqsTemplate sqsTemplate;
  private final ObjectMapper objectMapper;

  public NotificationProducer(
      @Value("${app.queue.sms}") String smsQueue,
      @Value("${app.queue.whatsapp}") String whatsappQueue,
      SqsTemplate sqsTemplate,
      ObjectMapper objectMapper) {
    this.smsQueue = smsQueue;
    this.whatsappQueue = whatsappQueue;
    this.sqsTemplate = sqsTemplate;
    this.objectMapper = objectMapper;
  }

  public void sendSms(String phone, String content) {
    String payload = toJson(new NotificationDto(phone, content));
    sqsTemplate.send(to -> to.queue(smsQueue).payload(payload));
    logger.info("Adicionado na fila de notificação SMS: {}", PhoneFormatterUtils.maskPhoneNumber(phone));
  }

  public void sendWhatsapp(String phone, String content) {
    String payload = toJson(new NotificationDto(phone, content));
    sqsTemplate.send(to -> to.queue(whatsappQueue).payload(payload));
    logger.info("Adicionado na fila de notificação WhatsApp: {}", PhoneFormatterUtils.maskPhoneNumber(phone));
  }

  private String toJson(NotificationDto dto) {
    try {
      return objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar NotificationDto para JSON", e);
    }
  }
}
