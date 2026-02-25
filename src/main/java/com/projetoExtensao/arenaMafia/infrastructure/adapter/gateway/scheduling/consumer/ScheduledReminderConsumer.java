package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.consumer;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationReminderNotificationEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ScheduleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.scheduling.dto.ScheduledReminderTaskDto;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class ScheduledReminderConsumer {

  private static final Logger logger = LoggerFactory.getLogger(ScheduledReminderConsumer.class);

  private final UserRepositoryPort userRepositoryPort;
  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  public ScheduledReminderConsumer(
      ApplicationEventPublisher eventPublisher,
      UserRepositoryPort userRepositoryPort,
      ReservationRepositoryPort reservationRepositoryPort) {
    this.eventPublisher = eventPublisher;
    this.userRepositoryPort = userRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
  }

  @SqsListener("${app.queue.whatsapp-reminder-queue}")
  public void consume(ScheduledReminderTaskDto dto) {
    try {
      logger.debug("Despertador da AWS tocou para o lembrete da reserva: {}", dto.reservationId());
      Reservation reservation = reservationRepositoryPort.findByIdOrElseThrow(dto.reservationId());

      if (!reservation.isActive()) {
        logger.info("Lembrete ignorado: A reserva {} não está mais no status CONFIRMED.", dto.reservationId());
        return;
      }

      String userPhone = userRepositoryPort.findByIdOrElseThrow(reservation.getUserId()).getPhone();
      eventPublisher.publishEvent(new OnReservationReminderNotificationEvent(userPhone, reservation));

    } catch (ScheduleNotFoundException e) {
      logger.warn("Reserva {} não encontrada. Lembrete ignorado.", dto.reservationId());
    } catch (Exception e) {
      logger.error("Falha ao processar lembrete {}: {}. Enviando para a Lixeira.", dto.reservationId(), e.getMessage());
      throw e;
    }
  }
}
