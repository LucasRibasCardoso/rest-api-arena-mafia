package com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.imp;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.notification.event.OnScheduleCreatedEvent;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.service.PriceCalculatorService;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.scheduler.DynamicScheduleEntryCompletionScheduler;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleAvailabilityService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CreateReservationByAdminUseCase;
import com.projetoExtensao.arenaMafia.application.user.port.gateway.PhoneValidatorPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidFormatPhoneException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.ReservationPastDateException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtNotSupportsModalityException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ScheduleConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationCreateRequestDto;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateReservationByAdminUseCaseImp implements CreateReservationByAdminUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final PhoneValidatorPort phoneValidatorPort;
  private final CourtRepositoryPort courtRepositoryPort;
  private final ApplicationEventPublisher eventPublisher;
  private final ModalityRepositoryPort modalityRepositoryPort;
  private final PriceCalculatorService priceCalculatorService;
  private final PriceRuleRepositoryPort priceRuleRepositoryPort;
  private final ScheduleEntryEnrichmentService enrichmentService;
  private final ReservationRepositoryPort reservationRepositoryPort;
  private final ScheduleAvailabilityService scheduleAvailabilityService;
  private final DynamicScheduleEntryCompletionScheduler completionScheduler;
  private final ScheduleDateCalculationService scheduleDateCalculationService;

  public CreateReservationByAdminUseCaseImp(
      UserRepositoryPort userRepositoryPort,
      PhoneValidatorPort phoneValidatorPort,
      CourtRepositoryPort courtRepositoryPort,
      ApplicationEventPublisher eventPublisher,
      ModalityRepositoryPort modalityRepositoryPort,
      PriceCalculatorService priceCalculatorService,
      PriceRuleRepositoryPort priceRuleRepositoryPort,
      ScheduleEntryEnrichmentService enrichmentService,
      ReservationRepositoryPort reservationRepositoryPort,
      ScheduleAvailabilityService scheduleAvailabilityService,
      DynamicScheduleEntryCompletionScheduler completionScheduler,
      ScheduleDateCalculationService scheduleDateCalculationService) {
    this.eventPublisher = eventPublisher;
    this.enrichmentService = enrichmentService;
    this.userRepositoryPort = userRepositoryPort;
    this.phoneValidatorPort = phoneValidatorPort;
    this.completionScheduler = completionScheduler;
    this.courtRepositoryPort = courtRepositoryPort;
    this.modalityRepositoryPort = modalityRepositoryPort;
    this.priceCalculatorService = priceCalculatorService;
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.scheduleAvailabilityService = scheduleAvailabilityService;
    this.scheduleDateCalculationService = scheduleDateCalculationService;
  }

  @Override
  public List<ReservationDetail> execute(UUID adminId, AdminReservationCreateRequestDto requestDto) {
    validateReservationDate(requestDto.startDate());
    validateModalityExists(requestDto.modalityId());
    validateCourtSupportsModality(requestDto.courtId(), requestDto.modalityId());

    User costumer = fetchCostumerByPhone(requestDto.userPhone());
    User admin = userRepositoryPort.findByIdOrElseThrow(adminId);

    List<PriceRule> priceRules = fetchAllPriceRules();

    if (isSingleReservation(requestDto)) {
      return List.of(createSingleReservation(admin, costumer, requestDto, priceRules));
    } else {
      return createRecurringReservation(admin, costumer, requestDto, priceRules);
    }
  }

  /**
   * Cria uma reserva individual
   * @param admin Administrador
   * @param costumer Usuário cliente
   * @param request DTO de request
   * @param priceRules Lista de regras de preços
   * @return Reserva detalhada que foi criada
   *
   * @throws OperatingHoursNotFoundException se o dia da reserva contem horário de funcionamento
   * @throws ScheduleConflictException se já houver uma reserva para esse horário
   */
  private ReservationDetail createSingleReservation(
          User admin,
          User costumer,
          AdminReservationCreateRequestDto request,
          List<PriceRule> priceRules) {

    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(request.startDate());
    scheduleDateCalculationService.validateDaysHaveOperatingHours(Set.of(dayOfWeek));
    scheduleAvailabilityService.validateAvailability(request.courtId(), request.startDate(), request.timeInterval());

    DateTimeSlot dateTimeSlot = new DateTimeSlot(request.startDate(), request.timeInterval());
    BigDecimal price = calculateReservationPrice(dateTimeSlot, priceRules);

    Reservation reservation = createAndSaveReservation(admin, costumer, request, dateTimeSlot, price);

    publishConfirmationEvent(costumer, reservation);
    scheduleAutomaticCompletion(reservation);

    return enrichmentService.enrichReservation(reservation);
  }

  /**
   * Criar reservas recorrentes
   * @param admin Administrador
   * @param costumer Usuário cliente
   * @param request DTO de request
   * @param priceRules Lista de regras de preços
   * @return Lista de reservas detalhadas que foram criadas
   */
  private List<ReservationDetail> createRecurringReservation(
          User admin,
          User costumer,
          AdminReservationCreateRequestDto request,
          List<PriceRule> priceRules) {


    int courtsCount = 1; // Número de quadras
    Set<DayOfWeek> effectiveDaysOfWeek =
            scheduleDateCalculationService.resolveEffectiveDaysOfWeekWithOccurrencesValidation(
                    request.selectedDaysOfWeek(),
                    request.startDate(),
                    request.endDate(),
                    courtsCount);

    List<LocalDate> applicableDates = scheduleDateCalculationService.calculateApplicableDates(
            request.startDate(),
            request.endDate(),
            effectiveDaysOfWeek);

    UUID recurringReservationId = UUID.randomUUID();

    List<Reservation> reservations = new ArrayList<>();
    for (LocalDate date : applicableDates) {
      scheduleAvailabilityService.validateAvailability(request.courtId(), date, request.timeInterval());

      DateTimeSlot dateTimeSlot = new DateTimeSlot(date, request.timeInterval());
      BigDecimal price = calculateReservationPrice(dateTimeSlot, priceRules);

      Reservation reservation =
              Reservation.createRecurring(
                      request.modalityId(),
                      request.courtId(),
                      costumer.getId(),
                      admin.getId(),
                      price,
                      dateTimeSlot,
                      recurringReservationId);
      reservations.add(reservation);
    }

    reservationRepositoryPort.saveAll(reservations);

    for (Reservation reservation : reservations) {
      publishConfirmationEvent(costumer, reservation);
      scheduleAutomaticCompletion(reservation);
    }

    ScheduleEntriesEnrichedResult enrichedResult = enrichmentService.enrichScheduleEntries(reservations);
    return enrichedResult.enrichedReservations();
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
   * Valida se a modalidade existe e está ativa
   * @param modalityId ID da modalidade a ser validada
   * @throws ModalityNotFoundException se a modalidade não for encontrada
   */
  private void validateModalityExists(UUID modalityId) {
    modalityRepositoryPort.findActiveByIdOrElseThrow(modalityId);
  }

  /**
   * Valida se a quadra suporta a modalidade informada
   * @param courtId ID da quadra
   * @param modalityId ID da modalidade
   * @throws CourtNotSupportsModalityException se a quadra não suportar a modalidade
   */
  private void validateCourtSupportsModality(UUID courtId, UUID modalityId) {
    Court court = courtRepositoryPort.findActiveByIdOrElseThrow(courtId);

    if (!court.getModalityIds().contains(modalityId)) {
      throw new CourtNotSupportsModalityException();
    }
  }

  /**
   * Busca o usuário cliente pelo número de telefone e garante que a conta do usuário está ativada
   * @param costumerPhone Número de telefone do usuário
   * @return Usuário encontrado
   * @throws UserNotFoundException se o usuário não for encontrado
   * @throws AccountStatusForbiddenException se a conta do usuário não estiver ativada
   * @throws InvalidFormatPhoneException se o número de telefone for inválido
   */
  private User fetchCostumerByPhone(String costumerPhone) {
    String costumerPhoneValid = phoneValidatorPort.formatToE164(costumerPhone);
    User user = userRepositoryPort.findByPhone(costumerPhoneValid).orElseThrow(UserNotFoundException::new);
    user.ensureAccountEnabled();
    return user;
  }

  /**
   * Busca todos as regras de preço ativas
   * @return Lista de regras de preço
   */
  private List<PriceRule> fetchAllPriceRules() {
    return priceRuleRepositoryPort.findAll(PriceRuleSpecification.byActiveStatus(true));
  }
  /**
   * Verifica se a request é para criação de uma reserva individual
   *
   * @param requestDto DTO de request
   * @return true se for uma reserva individual, false se for recorrente
   */
  private boolean isSingleReservation(AdminReservationCreateRequestDto requestDto) {
    return requestDto.startDate().isEqual(requestDto.endDate());
  }

  /**
   * Calcula o preço da reserva baseado nas regras de preços
   * @param dateTimeSlot slot de data de horário da reserva
   * @return Preço calculado
   */
  private BigDecimal calculateReservationPrice(DateTimeSlot dateTimeSlot, List<PriceRule> priceRules) {
    return priceCalculatorService.calculatePrice(dateTimeSlot.timeInterval(), dateTimeSlot.date(), priceRules);
  }

  /**
   * Cria e salva a reserva
   * @param admin Administrador
   * @param costumer Usuário cliente
   * @param request DTO de request
   * @param dateTimeSlot slot de data de horário da reserva
   * @param price Preço da reserva
   * @return Reserva criada
   */
  private Reservation createAndSaveReservation(
          User admin,
          User costumer,
          AdminReservationCreateRequestDto request,
          DateTimeSlot dateTimeSlot,
          BigDecimal price) {

    Reservation reservation = Reservation.createByAdmin(
            request.modalityId(),
            request.courtId(),
            costumer.getId(),
            admin.getId(),
            price,
            dateTimeSlot
    );
    return reservationRepositoryPort.save(reservation);
  }

  /**
   * Publica o evento de notificação informado o usuário sobre a reserva cadastrada
   * @param costumer Cliente usuário
   * @param reservation reserva cadastrada
   */
  private void publishConfirmationEvent(User costumer, Reservation reservation) {
    eventPublisher.publishEvent(new OnScheduleCreatedEvent(costumer.getUsername(), costumer.getPhone(), reservation));
  }

  /**
   * Agenda a conclusão automática da reserva no momento do seu término
   * @param reservation reserva a ser agendada para conclusão automática
   */
  private void scheduleAutomaticCompletion(Reservation reservation) {
    LocalDateTime endDateTime =
            LocalDateTime.of(
                    reservation.getDateTimeSlot().date(),
                    reservation.getDateTimeSlot().timeInterval().endTime());

    completionScheduler.scheduleReservationCompletion(reservation.getId(), endDateTime);
  }
}
