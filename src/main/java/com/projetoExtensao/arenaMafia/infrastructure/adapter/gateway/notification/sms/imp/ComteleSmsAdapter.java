package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.imp;

import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsProvider;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms.SmsStrategy;
import com.projetoExtensao.arenaMafia.infrastructure.utils.PhoneFormatterUtils;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("prod")
public class ComteleSmsAdapter implements SmsStrategy {

  private static final Logger logger = LoggerFactory.getLogger(ComteleSmsAdapter.class);

  private final String apiUrl;
  private final String apiKey;
  private final RestClient restClient;

  public ComteleSmsAdapter(
      @Value("${comtele.api-url}") String apiUrl,
      @Value("${comtele.api-key}") String apiKey,
      RestClient.Builder restClientBuilder) {
    this.apiUrl = apiUrl;
    this.apiKey = apiKey;
    this.restClient = restClientBuilder.build();
  }

  @Override
  public SmsProvider getProvider() {
    return SmsProvider.COMTELE;
  }

  @Override
  public void sendMessage(String phone, String message) {
    String phoneFormatted = phone != null ? phone.replaceAll("\\D", "") : "";

    if (phoneFormatted.isEmpty()) {
      logger.warn("Tentativa de envio de SMS para número nulo ou vazio.");
      return;
    }

    var payload = Map.of("Receivers", List.of(phoneFormatted), "Content", message);

    try {
      restClient
          .post()
          .uri(apiUrl + "/send")
          .header("auth-key", apiKey)
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();

      logger.info(
          "SMS enviado com sucesso para {}", PhoneFormatterUtils.maskPhoneNumber(phoneFormatted));

    } catch (RestClientException ex) {
      logger.error(
          "Erro ao enviar SMS para {}: {}",
          PhoneFormatterUtils.maskPhoneNumber(phoneFormatted),
          ex.getMessage());
    }
  }
}
