package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.imp;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.SmsProvider;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms.SmsStrategy;
import com.projetoExtensao.arenaMafia.infrastructure.utils.PhoneFormatterUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "AWS")
public class AwsSmsAdapter implements SmsStrategy {

  private static final Logger logger = LoggerFactory.getLogger(AwsSmsAdapter.class);
  private final SnsClient snsClient;

  public AwsSmsAdapter() {
    this.snsClient = SnsClient.create();
  }

  @Override
  public SmsProvider getProvider() {
    return SmsProvider.AWS;
  }

  @Override
  public void send(String phoneNumber, String message) {
    try {
      var request = PublishRequest.builder().message(message).phoneNumber(phoneNumber).build();

      var response = snsClient.publish(request);
      logger.debug("SMS enviado com sucesso via AWS SNS. MessageId: {}", response.messageId());

    } catch (SnsException e) {
      logger.error(
          "Erro ao enviar SMS para {}: {}",
          PhoneFormatterUtils.maskPhoneNumber(phoneNumber),
          e.awsErrorDetails().errorMessage());
    }
  }

  @PreDestroy
  public void closeClient() {
    if (snsClient != null) {
      snsClient.close();
      logger.info("AWS SNS Client fechado");
    }
  }
}
