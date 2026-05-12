package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleAvailabilityService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationUseCase;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnReservationCreatedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.ReservationPastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtNotSupportsModalityException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateReservationUseCaseImp implements CreateReservationUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;
  private final ModalityRepositoryPort modalityRepositoryPort;
  private final PriceCalculatorService priceCalculatorService;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ScheduleAvailabilityService scheduleAvailabilityService;

  public CreateReservationUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      ApplicationEventPublisher eventPublisher,
      ModalityRepositoryPort modalityRepositoryPort,
      PriceCalculatorService priceCalculatorService,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ReservationRepositoryPort reservationRepositoryPort,
      ScheduleAvailabilityService scheduleAvailabilityService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.eventPublisher = eventPublisher;
    this.modalityRepositoryPort = modalityRepositoryPort;
    this.priceCalculatorService = priceCalculatorService;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.scheduleAvailabilityService = scheduleAvailabilityService;
  }

  @Override
  public Reservation execute(UUID userId, CreateReservationRequestDto request) {
    // Validações de negócio
    validateReservationDate(request.date());
    validateModalityExists(request.modalityId());
    validateCourtSupportsModality(request.courtId(), request.modalityId());

    DateTimeSlot dateTimeSlot = new DateTimeSlot(request.date(), request.timeInterval());
    validateScheduleAvailability(request.courtId(), dateTimeSlot);


    // Cria e salva a reserva
    Reservation reservation = createAndSaveReservation(request, userId, dateTimeSlot);

    // Publica evento para agendar conclusão da reserva
    eventPublisher.publishEvent(new OnReservationCreatedScheduleTaskEvent(reservation));

    return reservation;
  }


  private Reservation createAndSaveReservation(CreateReservationRequestDto request, UUID userId, DateTimeSlot dateTimeSlot) {
    BigDecimal price = calculatePrice(request);
    var reservation = Reservation.createByUser(request.modalityId(), request.courtId(), userId, price, dateTimeSlot);
    return reservationRepositoryPort.save(reservation);
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
    scheduleAvailabilityService.validateAvailability(courtId, dateTimeSlot.date(), dateTimeSlot.timeInterval());
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
    List<PriceRule> activePriceRules =
        priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));

    return priceCalculatorService.calculatePrice(
        request.timeInterval(), request.date(), activePriceRules);
  }
}
