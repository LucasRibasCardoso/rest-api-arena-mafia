package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.result.ConfirmBlockedTimeResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.ConfirmBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnBlockedTimeCreatedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnBlockedTimeDeletedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PreviewStaleException;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmBlockedTimeUseCaseImp implements ConfirmBlockedTimeUseCase {

  private final BlockedTimePreviewCachePort previewCachePort;
  private final BlockedTimeRepositoryPort blockedTimeRepository;
  private final ReservationRepositoryPort reservationRepository;
  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final CourtRepositoryPort courtRepository;
  private final ReservationBatchCancellationService reservationBatchCancellationService;
  private final ScheduleDateCalculationService dateCalculationService;
  private final ApplicationEventPublisher eventPublisher;

  public ConfirmBlockedTimeUseCaseImp(
      BlockedTimePreviewCachePort previewCachePort,
      BlockedTimeRepositoryPort blockedTimeRepository,
      ReservationRepositoryPort reservationRepository,
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      CourtRepositoryPort courtRepository,
      ReservationBatchCancellationService reservationBatchCancellationService,
      ScheduleDateCalculationService dateCalculationService,
      ApplicationEventPublisher eventPublisher) {
    this.previewCachePort = previewCachePort;
    this.blockedTimeRepository = blockedTimeRepository;
    this.reservationRepository = reservationRepository;
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.courtRepository = courtRepository;
    this.reservationBatchCancellationService = reservationBatchCancellationService;
    this.dateCalculationService = dateCalculationService;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public ConfirmBlockedTimeResult execute(UUID adminId, BlockedTimeConfirmRequestDto request) {
    // Recupera o preview salvo no cache
    BlockedTimeConflictsPreview preview =
        previewCachePort.getPreviewOrElseThrow(request.previewKey(), adminId);

    // Valida se todas as quadras existem e estão ativas
    courtRepository.validateAllExistAndActive(preview.request().courtIds());

    // Calcula os dias da semana efetivos e o intervalo de horários que será utilizado nos bloqueios
    Set<DayOfWeek> effectiveDaysOfWeek =
        dateCalculationService.resolveEffectiveDaysOfWeekWithOccurrencesValidation(
            preview.request().selectedDaysOfWeek(),
            preview.request().startDate(),
            preview.request().endDate(),
            preview.request().courtIds().size());
    TimeInterval searchInterval =
        dateCalculationService.calculateSearchInterval(
            preview.request().isFullDay(), preview.request().timeInterval(), effectiveDaysOfWeek);

    // Valida se o preview não está desatualizado
    validatePreviewIsNotStale(preview, effectiveDaysOfWeek, searchInterval);

    // Processa os conflitos: cancela reservas e remove bloqueios conflitantes
    int reservationsCancelled =
        cancelConflictingReservations(
            preview.conflictingReservations(), request.description(), adminId);
    int blockedTimesCancelled = removeConflictingBlockedTimes(preview.conflictingBlockedTimes());

    // Cria e salva os novos BlockedTimes
    List<BlockedTime> blockedTimesCreated =
        createAndSaveBlockedTime(
            preview.request(), effectiveDaysOfWeek, searchInterval, adminId, request.description());

    // Deleta o preview do cache, garantindo que não possa ser reutilizado
    previewCachePort.delete(request.previewKey());

    // Agenda a deleção dos BlockedTimes criados para o momento em que eles terminarem
    blockedTimesCreated.forEach(
        bt -> eventPublisher.publishEvent(new OnBlockedTimeCreatedScheduleTaskEvent(bt)));

    // Extrair IDs dos BlockedTimes criados
    List<UUID> blockedTimesCreateIds =
        blockedTimesCreated.stream().map(BlockedTime::getId).toList();

    return new ConfirmBlockedTimeResult(
        blockedTimesCreateIds,
        blockedTimesCreated.size(),
        reservationsCancelled,
        blockedTimesCancelled,
        preview.usersAffected());
  }

  /**
   * Valida se o preview armazenado no cache ainda é válido, comparando os conflitos atuais com os
   * conflitos registrados no preview.
   *
   * @param preview Preview armazenado no cache
   * @param effectiveDaysOfWeek Dias da semana usado na busca por conflitos
   * @param searchInterval Intervalo de tempo usado na busca por conflitos
   * @throws PreviewStaleException se o preview estiver desatualizado
   */
  private void validatePreviewIsNotStale(
      BlockedTimeConflictsPreview preview,
      Set<DayOfWeek> effectiveDaysOfWeek,
      TimeInterval searchInterval) {

    var previewRequest = preview.request();
    List<ScheduleEntry> currentConflicts =
        scheduleEntryRepository.findAllActiveSchedulesConflicts(
            previewRequest.courtIds(),
            previewRequest.startDate(),
            previewRequest.endDate(),
            searchInterval,
            effectiveDaysOfWeek);

    Set<UUID> currentIds =
        currentConflicts.stream().map(ScheduleEntry::getId).collect(Collectors.toSet());

    Set<UUID> previewIds =
        Stream.of(
                preview.conflictingReservations().stream().map(ReservationDetail::reservationId),
                preview.conflictingBlockedTimes().stream().map(BlockedTimeDetail::blockedTimeId),
                preview.inProgressReservations().stream().map(ReservationDetail::reservationId))
            .flatMap(s -> s)
            .collect(Collectors.toSet());

    if (!currentIds.equals(previewIds)) {
      throw new PreviewStaleException();
    }
  }

  /**
   * Cancela as reservas conflitantes.
   *
   * <p>Reservas que entraram em andamento entre o preview e a confirmação são automaticamente
   * filtradas e não são canceladas.
   *
   * @param conflictingReservations Lista de reservas conflitantes a serem canceladas
   * @param description Descrição do bloqueio (usada como motivo do cancelamento)
   * @param adminId ID do administrador responsável pela criação dos bloqueios
   * @return Quantidade de reservas canceladas com sucesso
   */
  private int cancelConflictingReservations(
      List<ReservationDetail> conflictingReservations, String description, UUID adminId) {
    List<UUID> reservationIdsToCancel =
        conflictingReservations.stream()
            .filter(detail -> !detail.isInProgress())
            .map(ReservationDetail::reservationId)
            .toList();

    if (reservationIdsToCancel.isEmpty()) {
      return 0;
    }

    List<Reservation> reservationsToCancel =
        reservationRepository.findAllFutureReservationsByIds(reservationIdsToCancel);

    String cancellationReason = String.format("Bloqueio de horário criado: %s", description);
    return reservationBatchCancellationService.cancelReservationsInBatchByAdmin(
        reservationsToCancel, cancellationReason, adminId);
  }

  /**
   * Remove os BlockedTimes conflitantes em batch.
   *
   * @param blockedTimesToRemove Lista de BlockedTimes a serem removidos
   * @return Quantidade de bloqueios removidos
   */
  private int removeConflictingBlockedTimes(List<BlockedTimeDetail> blockedTimesToRemove) {
    List<UUID> blockedTimeIdsToDelete =
        blockedTimesToRemove.stream().map(BlockedTimeDetail::blockedTimeId).toList();

    if (blockedTimeIdsToDelete.isEmpty()) {
      return 0;
    }

    blockedTimeIdsToDelete.forEach(
        bt -> eventPublisher.publishEvent(new OnBlockedTimeDeletedScheduleTaskEvent(bt)));

    blockedTimeRepository.deleteAllByIds(blockedTimeIdsToDelete);
    return blockedTimeIdsToDelete.size();
  }

  /**
   * Cria os novos BlockedTime(s) baseado nos dados do preview.
   *
   * @param previewRequest Dados da request salvo no cache durante geração do preview
   * @param effectiveDaysOfWeek Dias da semana efetivos para os bloqueios
   * @param timeInterval Intervalo de tempo a ser usado para os bloqueios
   * @param adminId Id do administrador responsável pela criação dos bloqueios
   * @return Lista de BlockedTimes criados
   */
  private List<BlockedTime> createAndSaveBlockedTime(
      BlockedTimeConflictsPreviewRequestDto previewRequest,
      Set<DayOfWeek> effectiveDaysOfWeek,
      TimeInterval timeInterval,
      UUID adminId,
      String description) {

    List<LocalDate> applicableDates =
        dateCalculationService.calculateApplicableDates(
            previewRequest.startDate(), previewRequest.endDate(), effectiveDaysOfWeek);

    UUID recurringBlockedTimeId =
        createRecurringIdIfNeeded(previewRequest.courtIds(), applicableDates);

    List<BlockedTime> blockedTimesToCreate = new ArrayList<>();

    for (UUID courtId : previewRequest.courtIds()) {
      for (LocalDate date : applicableDates) {
        BlockedTime blockedTime =
            createBlockedTime(
                courtId,
                date,
                timeInterval,
                previewRequest.isFullDay(),
                description,
                adminId,
                recurringBlockedTimeId);

        blockedTimesToCreate.add(blockedTime);
      }
    }

    return blockedTimeRepository.saveAll(blockedTimesToCreate);
  }

  /**
   * Cria um BlockedTime usando o factory method apropriado.
   *
   * @param courtId ID da quadra
   * @param date Data do bloqueio
   * @param timeInterval Intervalo de tempo calculado
   * @param isFullDay Se é bloqueio de dia inteiro
   * @param description Descrição do bloqueio
   * @param adminId ID do admin que está criando o bloqueio
   * @param recurringBlockedTimeId ID de recorrência (null para bloqueios únicos)
   * @return BlockedTime criado
   */
  private BlockedTime createBlockedTime(
      UUID courtId,
      LocalDate date,
      TimeInterval timeInterval,
      boolean isFullDay,
      String description,
      UUID adminId,
      UUID recurringBlockedTimeId) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);

    // Cria um BlockedTime recorrente
    if (recurringBlockedTimeId != null) {
      return BlockedTime.createRecurring(
          courtId, dateTimeSlot, description, adminId, isFullDay, recurringBlockedTimeId);
    }

    // Cria um BlockedTime para dia inteiro
    if (isFullDay) {
      return BlockedTime.createFullDay(courtId, dateTimeSlot, description, adminId);
    }

    // Cria um BlockedTime para horário específico
    return BlockedTime.createSpecificTime(courtId, dateTimeSlot, description, adminId);
  }

  /**
   * Gera um recurringBlockedTimeId se múltiplos bloqueios forem criados.
   *
   * @param courtIds Lista de IDs das quadras
   * @param applicableDates Lista de datas aplicáveis
   * @return UUID do recurringBlockedTimeId ou null se for um único bloqueio
   */
  private UUID createRecurringIdIfNeeded(List<UUID> courtIds, List<LocalDate> applicableDates) {
    int totalBlockedTimes = courtIds.size() * applicableDates.size();
    return totalBlockedTimes > 1 ? UUID.randomUUID() : null;
  }
}
