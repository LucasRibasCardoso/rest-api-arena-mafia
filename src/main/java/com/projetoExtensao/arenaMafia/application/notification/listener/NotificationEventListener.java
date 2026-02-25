package com.projetoExtensao.arenaMafia.application.notification.listener;

import com.projetoExtensao.arenaMafia.application.notification.event.*;
import com.projetoExtensao.arenaMafia.application.notification.gateway.NotificationPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

  private final OtpPort otpPort;
  private final NotificationPort notificationPort;

  public NotificationEventListener(OtpPort otpPort, NotificationPort notificationPort) {
    this.otpPort = otpPort;
    this.notificationPort = notificationPort;
  }


  /**
   * Envia um SMS para o usuário com um código OTP para verificação de telefone
   * */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOtpVerification(OnVerificationRequiredNotificationEvent eventData) {
    User user = eventData.user();

    OtpCode otpCode = otpPort.generateOtpCode(user.getId());
    String message = buildOtpVerificationMessage(otpCode);

    notificationPort.sendSms(eventData.getRecipientPhone(), message);
  }

  /**
   * Envia uma notificação via WhatsApp para o usuário lembrando-o de uma reserva agendada
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationReminder(OnReservationReminderNotificationEvent eventData) {
    String message = buildReminderMessage(eventData.reservation());
    notificationPort.sendWhatsappMessage(eventData.userPhone(), message);
  }

  /**
   * Envia uma notificação via WhatsApp para o usuário informando que uma reserva foi criada para
   * ele
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCreatedByAdmin(OnReservationCreatedByAdminNotificationEvent eventData) {
    String message = buildReservationCreatedByAdminMessage(eventData.username(), eventData.reservation());
    notificationPort.sendWhatsappMessage(eventData.userPhone(), message);
  }

  /**
   * Envia uma notificação via WhatsApp para o usuário informando que uma reserva foi cancelada pela
   * administração
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCancelledByAdmin(OnReservationCancelledByAdminNotificationEvent eventData) {
    String message =
        buildReservationCancellationByAdminMessage(
            eventData.username(), eventData.reservation(), eventData.adminReason());
    notificationPort.sendWhatsappMessage(eventData.userPhone(), message);
  }

  /**
   * Envia uma notificação via WhatsApp para o usuário informando que suas reservas foram canceladas
   * pela administração
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationsCancelledByAdmin(OnReservationsCancelledByAdminNotificationEvent eventData) {
    String message = buildReservationsCancelledByAdminMessage(eventData);
    notificationPort.sendWhatsappMessage(eventData.userPhone(), message);
  }

  /**
   * Envia uma notificação via WhatsApp para o usuário informando que suas reservas recorrentes
   * foram criadas pela administração
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecurringReservationCreateByAdmin(
      OnRecurringReservationCreatedByAdminNotificationEvent eventData) {
    String message = buildRecurringReservationCreateByAdminMessage(eventData);
    notificationPort.sendWhatsappMessage(eventData.userPhone(), message);
  }

  //  ================================ Mensagens de notificação ================================
  private String buildReminderMessage(Reservation reservation) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();

    return """
        Arena Máfia - Lembrete de Reserva

        Olá! Só passando para lembrar que você tem uma reserva agendada para logo mais.

        Horário: %s - %s
        Data: %s

        Para mais informações, entre em contato com nossa central."""
        .formatted(startTime, endTime, date);
  }

  private String buildOtpVerificationMessage(OtpCode otpCode) {
    return """
        Arena Máfia - Código de Verificação

        Seu código de verificação é: %s

        Não compartilhe este código com ninguém.
        O código expira em 5 minutos."""
        .formatted(otpCode);
  }

  private String buildReservationCreatedByAdminMessage(String username, Reservation reservation) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();
    String price = reservation.getPrice().toString();

    return """
        Arena Máfia - Reserva Confirmada!

        Olá %s, uma reserva foi criada para você pela administração.

        Data: %s
        Horário: %s - %s
        Valor: R$ %s
        Código da reserva: %s
        """
        .formatted(username, date, startTime, endTime, price, reservation.getId());
  }

  private String buildReservationCancellationByAdminMessage(
      String username, Reservation reservation, String adminReason) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();

    return """
        Arena Máfia - Reserva cancelada pela administração

        Olá %s,
        Sua reserva para o dia %s, no horário de %s às %s, foi cancelada pela administração.
        Código da reserva: %s

        Motivo: %s

        Para mais informações, entre em contato com nossa central."""
        .formatted(username, date, startTime, endTime, reservation.getId(), adminReason);
  }

  private String buildRecurringReservationCreateByAdminMessage(
      OnRecurringReservationCreatedByAdminNotificationEvent eventData) {
    List<Reservation> reservations = eventData.reservations();

    LocalDate startDate = extractStartDate(reservations);
    LocalDate endDate = extractEndDate(reservations);
    TimeInterval timeInterval = extractTimeInterval(reservations);
    UUID recurringId = extractRecurringId(reservations);
    String daysOfWeek = buildDaysOfWeek(eventData.daysOfWeek());

    return """
           Arena Máfia - Reservas Confirmadas!

           Olá %s, suas reservas recorrentes foram confirmadas!

           Período: %s a %s
           Dias da semana: %s
           Horário: %s - %s
           Código das reservas: %s
           """
        .formatted(
            eventData.username(),
            startDate,
            endDate,
            daysOfWeek,
            timeInterval.startTime(),
            timeInterval.endTime(),
            recurringId);
  }

  private String buildReservationsCancelledByAdminMessage(
      OnReservationsCancelledByAdminNotificationEvent eventData) {
    List<Reservation> reservations = eventData.reservations();
    int totalReservations = reservations.size();

    LocalDate startDate = extractStartDate(reservations);
    LocalDate endDate = extractEndDate(reservations);

    String periodText =
        startDate.equals(endDate) ? startDate.toString() : startDate + " a " + endDate;

    return """
           Arena Máfia - Reservas Canceladas

           Olá %s, %d reserva(s) foram canceladas pela administração.

           Período afetado: %s
           Motivo: %s

           Para mais informações, entre em contato com nossa central.
           """
        .formatted(eventData.username(), totalReservations, periodText, eventData.adminReason());
  }

  //  ================================ Métodos auxiliares ================================

  private LocalDate extractStartDate(List<Reservation> reservations) {
    return reservations.stream()
        .map(r -> r.getDateTimeSlot().date())
        .min(LocalDate::compareTo)
        .orElse(null);
  }

  private LocalDate extractEndDate(List<Reservation> reservations) {
    return reservations.stream()
        .map(r -> r.getDateTimeSlot().date())
        .max(LocalDate::compareTo)
        .orElse(null);
  }

  private TimeInterval extractTimeInterval(List<Reservation> reservations) {
    return reservations.getFirst().getDateTimeSlot().timeInterval();
  }

  private UUID extractRecurringId(List<Reservation> reservations) {
    return reservations.getFirst().getRecurringReservationId();
  }

  private String buildDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    return daysOfWeek.stream().map(DayOfWeek::getPortugueseName).collect(Collectors.joining(", "));
  }
}
