package com.projetoExtensao.arenaMafia.application.notification.listener;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledEvent;
import com.projetoExtensao.arenaMafia.application.notification.event.OnScheduleCreatedEvent;
import com.projetoExtensao.arenaMafia.application.notification.event.OnVerificationRequiredEvent;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {

  private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

  private final SmsPort smsPort;
  private final OtpPort otpPort;

  public NotificationEventListener(SmsPort smsPort, OtpPort otpPort) {
    this.smsPort = smsPort;
    this.otpPort = otpPort;
  }

  /**
   * Listener que processa eventos de verificação de usuário. Gera código OTP e envia notificação
   * SMS ao usuário.
   *
   * @param event evento contendo dados do usuário
   */
  @Async
  @EventListener
  public void onUserRegistration(OnVerificationRequiredEvent event) {
    try {
      User user = event.getUser();
      String recipientPhone = event.getRecipientPhone();

      OtpCode otpCode = otpPort.generateOtpCode(user.getId());
      String message = buildVerificationMessage(otpCode);

      smsPort.send(recipientPhone, message);

      logger.info("SMS de verificação enviado para o usuário: {}", user.getUsername());

    } catch (Exception e) {
      logger.error("Falha ao processar o evento de registro do usuário: {}", e.getMessage(), e);
    }
  }

  /**
   * Listener que processa eventos de criação de agendamento. Envia notificação SMS ao usuário
   * confirmando a reserva.
   *
   * @param eventData evento contendo dados do agendamento
   */
  @Async
  @EventListener
  public void onScheduleCreated(OnScheduleCreatedEvent eventData) {
    try {
      ScheduleEntry scheduleEntry = eventData.scheduleEntry();

      // Apenas envia notificação para reservas
      if (!(scheduleEntry instanceof Reservation reservation)) {
        logger.warn(
            "Tipo de schedule não suportado para notificação: {}",
            scheduleEntry.getClass().getSimpleName());
        return;
      }

      String message = buildReservationConfirmationMessage(eventData.username(), reservation);
      smsPort.send(eventData.userPhone(), message);

      logger.info(
          "SMS de confirmação de reserva enviado para o usuário: {} - Reserva ID: {}",
          eventData.username(),
          reservation.getId());

    } catch (Exception e) {
      logger.error("Falha ao processar evento de criação de reserva: {}", e.getMessage(), e);
    }
  }

  /**
   * Listener que processa eventos de cancelamento de reserva. Envia notificação SMS ao usuário
   * confirmando o cancelamento.
   *
   * @param eventData evento contendo dados do cancelamento
   */
  @Async
  @EventListener
  public void onReservationCancelled(OnReservationCancelledEvent eventData) {
    try {
      Reservation reservation = eventData.reservation();

      String message =
          buildReservationCancellationMessage(eventData.username(), reservation);
      smsPort.send(eventData.userPhone(), message);

      logger.info(
          "SMS de cancelamento de reserva enviado para o usuário: {} - Reserva ID: {}",
          eventData.username(),
          reservation.getId());

    } catch (Exception e) {
      logger.error("Falha ao processar evento de cancelamento de reserva: {}", e.getMessage(), e);
    }
  }

  /**
   * Constrói a mensagem de verificação com código OTP.
   *
   * @param otpCode código de verificação gerado
   * @return mensagem formatada para envio
   */
  private String buildVerificationMessage(OtpCode otpCode) {
    return """
        Arena Máfia - Código de Verificação

        Seu código de verificação é: %s

        ⚠️ Não compartilhe este código com ninguém.
        O código expira em 5 minutos."""
        .formatted(otpCode);
  }

  /**
   * Constrói a mensagem de confirmação de reserva.
   *
   * @param username nome do usuário
   * @param reservation reserva criada
   * @return mensagem formatada para envio
   */
  private String buildReservationConfirmationMessage(String username, Reservation reservation) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();
    String price = reservation.getPrice().toString();

    return """
        Arena Máfia - Reserva confirmada!

        Olá %s,
        Data: %s
        Horário: %s - %s
        Valor: R$ %s

        Código da reserva: %s"""
        .formatted(username, date, startTime, endTime, price, reservation.getId());
  }

  /**
   * Constrói a mensagem de cancelamento de reserva.
   *
   * @param username nome do usuário
   * @param reservation reserva cancelada
   * @return mensagem formatada para envio
   */
  private String buildReservationCancellationMessage(String username, Reservation reservation) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();

    return """
        Arena Máfia - Reserva cancelada

        Olá %s,
        Sua reserva para o dia %s, no horário de %s às %s, foi cancelada com sucesso.

        Código da reserva: %s"""
        .formatted(username, date, startTime, endTime, reservation.getId());
  }
}
