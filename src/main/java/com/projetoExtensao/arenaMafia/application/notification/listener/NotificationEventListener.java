package com.projetoExtensao.arenaMafia.application.notification.listener;

import com.projetoExtensao.arenaMafia.application.notification.event.*;
import com.projetoExtensao.arenaMafia.application.notification.gateway.OtpPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;
import com.projetoExtensao.arenaMafia.application.notification.gateway.WhatsAppPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.OtpCode;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

  private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

  private final OtpPort otpPort;
  private final SmsPort smsPort;
  private final WhatsAppPort whatsAppPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public NotificationEventListener(
      OtpPort otpPort,
      SmsPort smsPort,
      WhatsAppPort whatsAppPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.otpPort = otpPort;
    this.smsPort = smsPort;
    this.whatsAppPort = whatsAppPort;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onOtpVerificationRequired(OnVerificationRequiredEvent eventData) {
    try {
      User user = eventData.user();
      String recipientPhone = eventData.getRecipientPhone();

      OtpCode otpCode = otpPort.generateOtpCode(user.getId());
      String message = buildOtpVerificationMessage(otpCode);

      smsPort.send(recipientPhone, message);

    } catch (Exception e) {
      logger.error("Falha ao processar o evento de verificação OTP via SMS: {}", e.getMessage());
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onScheduleCreated(OnReservationCreatedEvent eventData) {
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

    } catch (Exception e) {
      logger.error("Falha ao processar evento de criação de reserva: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCancelledByUser(OnReservationCancelledByUserEvent eventData) {
    try {
      Reservation reservation = eventData.reservation();

      String message = buildReservationCancellationMessage(eventData.username(), reservation);
      smsPort.send(eventData.userPhone(), message);

    } catch (Exception e) {
      logger.error("Falha ao processar evento de cancelamento de reserva: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationCancelledByAdmin(OnReservationCancelledByAdminEvent eventData) {
    try {
      Reservation reservation = eventData.reservation();

      String message =
          buildReservationCancellationByAdminMessage(
              eventData.username(), reservation, eventData.adminReason());
      smsPort.send(eventData.userPhone(), message);

    } catch (Exception e) {
      logger.error(
          "Falha ao processar evento de cancelamento de reserva por admin: {}", e.getMessage(), e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecurringReservationCreateByAdmin(
      OnRecurringReservationCreatedByAdminEvent eventData) {
    try {
      String message = buildRecurringReservationCreateByAdminMessage(eventData);
      smsPort.send(eventData.userPhone(), message);

    } catch (Exception e) {
      logger.error(
          "Falha ao processar evento de criação de reservas recorrentes por admin: {}",
          e.getMessage(),
          e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRecurringReservationCancelledByAdmin(
      OnRecurringReservationCancelledByAdminEvent eventData) {
    try {
      String message = buildRecurringReservationCancellationByAdminMessage(eventData);
      smsPort.send(eventData.userPhone(), message);

    } catch (Exception e) {
      logger.error(
          "Falha ao processar evento de cancelamento de reservas recorrentes por admin: {}",
          e.getMessage(),
          e);
    }
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onReservationsCancelledByAdmin(OnReservationsCancelledByAdminEvent eventData) {
    try {
      String message = buildReservationsCancelledByAdminMessage(eventData);
      smsPort.send(eventData.userPhone(), message);

    } catch (Exception e) {
      logger.error(
          "Falha ao processar evento de cancelamento em lote de reservas por admin: {}",
          e.getMessage(),
          e);
    }
  }

  //  ================================ Mensagens de notificação ================================

  private String buildOtpVerificationMessage(OtpCode otpCode) {
    return """
        Arena Máfia - Código de Verificação

        Seu código de verificação é: %s

        ⚠️ Não compartilhe este código com ninguém.
        O código expira em 5 minutos."""
        .formatted(otpCode);
  }

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

  private String buildReservationCancellationByAdminMessage(
      String username, Reservation reservation, String adminReason) {
    String date = reservation.getDateTimeSlot().date().toString();
    String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
    String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();

    return """
        Arena Máfia - Reserva cancelada pela administração

        Olá %s,
        Sua reserva para o dia %s, no horário de %s às %s, foi cancelada pela administração.

        Motivo: %s

        Código da reserva: %s

        Para mais informações, entre em contato com nossa central."""
        .formatted(username, date, startTime, endTime, adminReason, reservation.getId());
  }

  private String buildRecurringReservationCreateByAdminMessage(
      OnRecurringReservationCreatedByAdminEvent eventData) {
    List<Reservation> reservations = eventData.reservations();

    LocalDate startDate = extractStartDate(reservations);
    LocalDate endDate = extractEndDate(reservations);
    TimeInterval timeInterval = extractTimeInterval(reservations);
    UUID recurringId = extractRecurringId(reservations);
    String daysOfWeek = buildDaysOfWeek(eventData.daysOfWeek());
    BigDecimal minPrice = extractMinPrice(reservations);
    BigDecimal maxPrice = extractMaxPrice(reservations);

    // Formatar preço (usar minPrice se todos iguais, senão range)
    String priceText =
        minPrice.equals(maxPrice) ? minPrice.toString() : minPrice + " - " + maxPrice;

    return """
           Arena Máfia - Reservas Confirmadas!

           Olá %s, suas reservas recorrentes foram confirmadas!

           Data de início: %s
           Data de término: %s
           Dias da semana: %s
           Horário: %s - %s
           Preço: R$ %s

           Código das reservas: %s
           """
        .formatted(
            eventData.username(),
            startDate,
            endDate,
            daysOfWeek,
            timeInterval.startTime(),
            timeInterval.endTime(),
            priceText,
            recurringId);
  }

  private String buildRecurringReservationCancellationByAdminMessage(
      OnRecurringReservationCancelledByAdminEvent eventData) {
    List<Reservation> reservations = eventData.reservations();

    LocalDate startDate = extractStartDate(reservations);
    LocalDate endDate = extractEndDate(reservations);
    TimeInterval timeInterval = extractTimeInterval(reservations);
    UUID recurringId = extractRecurringId(reservations);
    String daysOfWeek = buildDaysOfWeek(eventData.daysOfWeek());

    return """
           Arena Máfia - Reservas Canceladas!

           Olá %s, suas reservas recorrentes foram canceladas pela administração.

           Período: %s a %s
           Dias: %s
           Horário: %s - %s

           Motivo: %s

           Código: %s
           """
        .formatted(
            eventData.username(),
            startDate,
            endDate,
            daysOfWeek,
            timeInterval.startTime(),
            timeInterval.endTime(),
            eventData.adminReason(),
            recurringId);
  }

  private String buildReservationsCancelledByAdminMessage(
      OnReservationsCancelledByAdminEvent eventData) {
    List<Reservation> reservations = eventData.reservations();

    StringBuilder reservationsList = new StringBuilder();
    for (Reservation reservation : reservations) {
      String date = reservation.getDateTimeSlot().date().toString();
      String startTime = reservation.getDateTimeSlot().timeInterval().startTime().toString();
      String endTime = reservation.getDateTimeSlot().timeInterval().endTime().toString();
      reservationsList.append(String.format("- %s, %s-%s\n", date, startTime, endTime));
    }

    return """
           Arena Máfia - Reservas Canceladas!

           Olá %s, suas reservas foram canceladas pela administração.

           Reservas canceladas:
           %s
           Motivo: %s

           Para mais informações, entre em contato com nossa central.
           """
        .formatted(
            eventData.username(), reservationsList.toString().trim(), eventData.adminReason());
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

  private BigDecimal extractMinPrice(List<Reservation> reservations) {
    return reservations.stream()
        .map(Reservation::getPrice)
        .min(BigDecimal::compareTo)
        .orElse(getDefaultPrice());
  }

  private BigDecimal extractMaxPrice(List<Reservation> reservations) {
    return reservations.stream()
        .map(Reservation::getPrice)
        .max(BigDecimal::compareTo)
        .orElse(getDefaultPrice());
  }

  private BigDecimal getDefaultPrice() {
    return priceRuleRepositoryPort.findDefaultRuleOrElseThrow().getPrice();
  }
}
