package com.projetoExtensao.arenaMafia.unit.application.notification.listener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredNotificationEvent;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.listener.NotificationEventListener;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.producer.NotificationProducer;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para NotificationEventListener")
public class NotificationEventListenerTest {

  @Mock private OtpPort otpPort;
  @Mock private NotificationProducer notificationProducer;
  @Mock private PriceRuleRepositoryPort priceRuleRepositoryPort;
  @InjectMocks private NotificationEventListener eventListener;

  private final OtpCode otpCode = OtpCode.generate();

  @Test
  @DisplayName("Deve gerar OTP e enviar SMS para o número atual do usuário")
  void onOtpVerification_shouldGenerateOtpAndSendSms_toCurrentPhone() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();
    OnVerificationRequiredNotificationEvent event = new OnVerificationRequiredNotificationEvent(user);

    when(otpPort.generateOtpCode(userId)).thenReturn(otpCode);

    // Act
    eventListener.onOtpVerification(event);

    // Assert
    verify(otpPort, times(1)).generateOtpCode(userId);

    ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationProducer, times(1)).sendSms(phoneCaptor.capture(), messageCaptor.capture());

    assertEquals(user.getPhone(), phoneCaptor.getValue());
    assertTrue(messageCaptor.getValue().contains(otpCode.toString()));
  }

  @Test
  @DisplayName("Deve gerar OTP e enviar SMS para o novo número de telefone informado")
  void onOtpVerification_shouldGenerateOtpAndSendSms_toNewPhone() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID userId = user.getId();
    String newPhone = "+5511999999999";
    OnVerificationRequiredNotificationEvent event = new OnVerificationRequiredNotificationEvent(user, newPhone);

    when(otpPort.generateOtpCode(userId)).thenReturn(otpCode);

    // Act
    eventListener.onOtpVerification(event);

    // Assert
    verify(otpPort, times(1)).generateOtpCode(userId);

    ArgumentCaptor<String> phoneCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationProducer, times(1)).sendSms(phoneCaptor.capture(), messageCaptor.capture());

    assertEquals(newPhone, phoneCaptor.getValue());
    assertTrue(messageCaptor.getValue().contains(otpCode.toString()));
  }

  @Test
  @DisplayName("Deve propagar exceção se a geração de OTP falhar")
  void onOtpVerification_shouldThrowException_whenOtpGenerationFails() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    OnVerificationRequiredNotificationEvent event = new OnVerificationRequiredNotificationEvent(user);

    when(otpPort.generateOtpCode(user.getId()))
        .thenThrow(new RuntimeException("Falha ao conectar com o Redis"));

    // Act & Assert
    assertThrows(RuntimeException.class, () -> eventListener.onOtpVerification(event));

    verify(notificationProducer, never()).sendSms(anyString(), anyString());
  }
}
