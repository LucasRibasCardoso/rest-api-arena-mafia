package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCreatedEvent;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.scheduler.DynamicScheduleEntryCompletionScheduler;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleAvailabilityService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.ReservationPastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtNotSupportsModalityException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateReservationUseCaseImp implements CreateReservationUseCase {

  private final PriceCalculatorService priceCalculatorService;
  private final ScheduleAvailabilityService scheduleAvailabilityService;
  private final DynamicScheduleEntryCompletionScheduler completionScheduler;
  private final UserRepositoryPort userRepositoryPort;
  private final CourtRepositoryPort courtRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;
  private final ModalityRepositoryPort modalityRepositoryPort;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ReservationRepositoryPort reservationRepositoryPort;

  public CreateReservationUseCaseImp(
      PriceCalculatorService priceCalculatorService,
      ScheduleAvailabilityService scheduleAvailabilityService,
      DynamicScheduleEntryCompletionScheduler completionScheduler,
      UserRepositoryPort userRepositoryPort,
      CourtRepositoryPort courtRepositoryPort,
      ModalityRepositoryPort modalityRepositoryPort,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ReservationRepositoryPort reservationRepositoryPort,
      ApplicationEventPublisher eventPublisher) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.completionScheduler = completionScheduler;
    this.userRepositoryPort = userRepositoryPort;
    this.courtRepositoryPort = courtRepositoryPort;
    this.scheduleAvailabilityService = scheduleAvailabilityService;
    this.modalityRepositoryPort = modalityRepositoryPort;
    this.priceCalculatorService = priceCalculatorService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Reservation execute(UUID userId, CreateReservationRequestDto request) {
    // Valida se a data da reserva não está no passado
    validateReservationDate(request.date());

    // Busca o usuário que está fazendo a reserva
    User user = userRepositoryPort.findByIdOrElseThrow(userId);

    // Valida se a modalidade existe e se a quadra suporta a modalidade
    validateModalityExists(request.modalityId());
    validateCourtSupportsModality(request.courtId(), request.modalityId());

    // Valida a disponibilidade da quadra para o intervalo solicitado
    DateTimeSlot dateTimeSlot = buildDateTimeSlot(request);
    validateScheduleAvailability(request.courtId(), dateTimeSlot);

    // Calcula o preço da reserva
    BigDecimal price = calculatePrice(request);
    Reservation reservation = saveReservation(request.modalityId(), request.courtId(), userId, price, dateTimeSlot);

    // Envia notificação de confirmação de reserva de forma assíncrona
    publishConfirmationEvent(user, reservation);

    // Agenda a conclusão automática da reserva
    scheduleAutomaticCompletion(reservation);
    return reservation;
  }

  /**
   * Constrói um DateTimeSlot a partir dos dados da requisição.
   *
   * @param request dados da requisição
   * @return DateTimeSlot construído
   */
  private DateTimeSlot buildDateTimeSlot(CreateReservationRequestDto request) {
    return new DateTimeSlot(request.date(), request.timeInterval());
  }

  /**
   * Cria e salva uma reserva no repositório.
   *
   * @param modalityId ID da modalidade
   * @param courtId ID da quadra
   * @param userId ID do usuário
   * @param price Preço da reserva
   * @param dateTimeSlot Intervalo de data e hora da reserva
   * @return Reserva criada
   */
  private Reservation saveReservation(UUID modalityId, UUID courtId, UUID userId, BigDecimal price, DateTimeSlot dateTimeSlot) {
    var reservation = Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
    return reservationRepositoryPort.save(reservation);
  }

  /**
   * Publica um evento de confirmação de reserva para processamento assíncrono de notificações.
   *
   * @param user Usuário que fez a reserva
   * @param reservation Reserva criada
   */
  private void publishConfirmationEvent(User user, Reservation reservation) {
    eventPublisher.publishEvent(new OnReservationCreatedEvent(user.getUsername(), user.getPhone(), reservation));
  }

  /**
   * Agenda a conclusão automática da reserva no momento do seu término.
   *
   * @param reservation Reserva a ser agendada para conclusão automática
   */
  private void scheduleAutomaticCompletion(Reservation reservation) {
    LocalDateTime endDateTime =
        LocalDateTime.of(
            reservation.getDateTimeSlot().date(),
            reservation.getDateTimeSlot().timeInterval().endTime());

    completionScheduler.scheduleReservationCompletion(reservation.getId(), endDateTime);
  }

  /**
   * Valida se a quadra existe, está ativa e suporta a modalidade informada.
   *
   * @param courtId ID da quadra
   * @param modalityId ID da modalidade
   * @throws CourtNotFoundException se a quadra não for encontrada
   * @throws CourtNotSupportsModalityException se a quadra não suportar a modalidade
   */
  private void validateCourtSupportsModality(UUID courtId, UUID modalityId) {
    Court court = courtRepositoryPort.findActiveByIdOrElseThrow(courtId);

    if (!court.getModalityIds().contains(modalityId)) {
      throw new CourtNotSupportsModalityException();
    }
  }

  /**
   * Valida se a modalidade existe.
   *
   * @param modalityId ID da modalidade
   * @throws ModalityNotFoundException se a modalidade não for encontrada
   */
  private void validateModalityExists(UUID modalityId) {
    modalityRepositoryPort.findByIdOrElseThrow(modalityId);
  }

  /**
   * Valida a disponibilidade da quadra para o intervalo de data e hora solicitado.
   *
   * @param courtId ID da quadra
   * @param dateTimeSlot Intervalo de data e hora da reserva
   */
  private void validateScheduleAvailability(UUID courtId, DateTimeSlot dateTimeSlot) {
    scheduleAvailabilityService.validateAvailability(
        courtId, dateTimeSlot.date(), dateTimeSlot.timeInterval());
  }

  /**
   * Valida se a data da reserva não está no passado. Reservas só podem ser feitas para o dia atual
   * ou datas futuras.
   *
   * @param reservationDate data da reserva
   * @throws ReservationPastDateException se a data for anterior à data atual
   */
  private void validateReservationDate(LocalDate reservationDate) {
    LocalDate today = LocalDate.now();

    if (reservationDate.isBefore(today)) {
      throw new ReservationPastDateException();
    }
  }

  /**
   * Calcula o preço da reserva com base nas regras de precificação ativas.
   *
   * @param request dados da reserva
   * @return preço calculado
   */
  private BigDecimal calculatePrice(CreateReservationRequestDto request) {
    List<PriceRule> activePriceRules = priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));

    return priceCalculatorService.calculatePrice(request.timeInterval(), request.date(), activePriceRules);
  }
}
