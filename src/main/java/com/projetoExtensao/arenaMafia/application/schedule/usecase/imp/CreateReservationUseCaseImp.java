package com.projetoExtensao.arenaMafia.application.schedule.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.notification.event.OnScheduleCreatedEvent;
import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleAvailabilityService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtNotSupportsModalityException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateReservationUseCaseImp implements CreateReservationUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final CourtRepositoryPort courtRepositoryPort;
  private final ModalityRepositoryPort modalityRepositoryPort;
  private final PriceCalculatorService priceCalculatorService;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleAvailabilityService scheduleAvailabilityService;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;

  public CreateReservationUseCaseImp(
      UserRepositoryPort userRepositoryPort,
      CourtRepositoryPort courtRepositoryPort,
      ModalityRepositoryPort modalityRepositoryPort,
      PriceCalculatorService priceCalculatorService,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleAvailabilityService scheduleAvailabilityService,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      ApplicationEventPublisher eventPublisher) {
    this.userRepositoryPort = userRepositoryPort;
    this.courtRepositoryPort = courtRepositoryPort;
    this.modalityRepositoryPort = modalityRepositoryPort;
    this.priceCalculatorService = priceCalculatorService;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.scheduleAvailabilityService = scheduleAvailabilityService;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public ScheduleEntry execute(UUID userId, CreateReservationRequestDto request) {
    User user = userRepositoryPort.findByIdOrElseThrow(userId);

    validateModalityExists(request.modalityId());
    validateCourtSupportsModality(request.courtId(), request.modalityId());

    DateTimeSlot dateTimeSlot = buildDateTimeSlot(request);
    validateScheduleAvailability(request.courtId(), dateTimeSlot);

    BigDecimal price = calculatePrice(request);

    Reservation reservation =
        saveReservation(request.modalityId(), request.courtId(), userId, price, dateTimeSlot);

    sendConfirmationNotification(user, reservation);
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
  private Reservation saveReservation(
      UUID modalityId, UUID courtId, UUID userId, BigDecimal price, DateTimeSlot dateTimeSlot) {
    var reservation = Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
    scheduleEntryRepositoryPort.save(reservation);
    return reservation;
  }

  /**
   * Envia uma notificação de confirmação de reserva para o usuário.
   *
   * @param user Usuário que fez a reserva
   * @param reservation Reserva criada
   */
  private void sendConfirmationNotification(User user, Reservation reservation) {
    eventPublisher.publishEvent(
        new OnScheduleCreatedEvent(user.getUsername(), user.getPhone(), reservation));
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
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);

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
   * Calcula o preço da reserva com base nas regras de precificação ativas.
   *
   * @param request dados da reserva
   * @return preço calculado
   */
  private BigDecimal calculatePrice(CreateReservationRequestDto request) {
    List<PriceRule> activePriceRules =
        priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));

    return priceCalculatorService.calculatePrice(
        request.timeInterval(), request.date(), activePriceRules);
  }
}
