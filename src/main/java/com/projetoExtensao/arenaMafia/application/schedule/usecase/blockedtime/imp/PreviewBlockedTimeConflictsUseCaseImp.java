package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.service.BlockedTimeDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PreviewBlockedTimeConflictsUseCaseImp implements PreviewBlockedTimeConflictsUseCase {

  private final ScheduleEntryEnrichmentService enrichmentService;
  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final BlockedTimePreviewCachePort blockedTimePreviewCachePort;
  private final BlockedTimeDateCalculationService dateCalculationService;

  public PreviewBlockedTimeConflictsUseCaseImp(
      ScheduleEntryEnrichmentService enrichmentService,
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      BlockedTimePreviewCachePort blockedTimePreviewCachePort,
      BlockedTimeDateCalculationService dateCalculationService) {
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.enrichmentService = enrichmentService;
    this.blockedTimePreviewCachePort = blockedTimePreviewCachePort;
    this.dateCalculationService = dateCalculationService;
  }

  @Override
  public BlockedTimeConflictsPreview execute(BlockedTimeConflictsPreviewRequestDto request, UUID adminId) {
    Set<DayOfWeek> effectiveDaysOfWeek = resolveAndValidateDaysOfWeek(request);

    List<LocalDate> applicableDates = calculateAndValidateDates(request, effectiveDaysOfWeek);
    if (applicableDates.isEmpty()) {
      return createEmptyPreview(request);
    }

    TimeInterval searchInterval = calculateSearchInterval(request, effectiveDaysOfWeek);

    List<ScheduleEntry> conflicts = findConflicts(request, searchInterval, effectiveDaysOfWeek);
    if (conflicts.isEmpty()) {
      return createEmptyPreview(request);
    }

    return buildAndCachePreview(conflicts, request, adminId);
  }

  /**
   * Resolve e valida os dias da semana efetivos.
   *
   * @param request request do preview
   * @return conjunto de dias da semana efetivos
   */
  private Set<DayOfWeek> resolveAndValidateDaysOfWeek(BlockedTimeConflictsPreviewRequestDto request) {
    Set<DayOfWeek> effectiveDaysOfWeek = dateCalculationService.resolveEffectiveDaysOfWeek(
        request.selectedDaysOfWeek(),
        request.startDate(),
        request.endDate());

    dateCalculationService.validateDaysHaveOperatingHours(effectiveDaysOfWeek);

    return effectiveDaysOfWeek;
  }

  /**
   * Calcula as datas aplicáveis e valida o limite de ocorrências.
   */
  private List<LocalDate> calculateAndValidateDates(
      BlockedTimeConflictsPreviewRequestDto request,
      Set<DayOfWeek> effectiveDaysOfWeek) {

    List<LocalDate> applicableDates = dateCalculationService.calculateApplicableDates(
        request.startDate(),
        request.endDate(),
        effectiveDaysOfWeek);

    if (!applicableDates.isEmpty()) {
      dateCalculationService.validateOccurrencesLimit(request.courtIds().size(), applicableDates.size());
    }

    return applicableDates;
  }

  /**
   * Calcula o intervalo de tempo para busca de conflitos.
   */
  private TimeInterval calculateSearchInterval(
      BlockedTimeConflictsPreviewRequestDto request,
      Set<DayOfWeek> effectiveDaysOfWeek) {

    return dateCalculationService.calculateSearchInterval(
        request.isFullDay(),
        request.timeInterval(),
        effectiveDaysOfWeek);
  }

  /**
   * Busca os conflitos no repositório.
   */
  private List<ScheduleEntry> findConflicts(
      BlockedTimeConflictsPreviewRequestDto request,
      TimeInterval searchInterval,
      Set<DayOfWeek> effectiveDaysOfWeek) {

    return scheduleEntryRepository.findConflicts(
        request.courtIds(),
        request.startDate(),
        request.endDate(),
        searchInterval,
        effectiveDaysOfWeek);
  }

  /**
   * Constrói o preview enriquecido e salva em cache.
   */
  private BlockedTimeConflictsPreview buildAndCachePreview(
      List<ScheduleEntry> conflicts,
      BlockedTimeConflictsPreviewRequestDto request,
      UUID adminId) {

    List<ScheduleDetail> enrichedConflicts = enrichmentService.enrichScheduleEntries(conflicts);

    List<ReservationDetail> allReservations = filterReservations(enrichedConflicts);
    List<BlockedTimeDetail> blockedTimes = filterBlockedTimes(enrichedConflicts);
    List<ReservationDetail> inProgressReservations = filterInProgressReservations(allReservations);
    List<ReservationDetail> reservationsToCancel = excludeInProgressReservations(allReservations, inProgressReservations);

    int usersAffected = countDistinctUsers(reservationsToCancel);
    String previewKey = blockedTimePreviewCachePort.generateKey(adminId);

    BlockedTimeConflictsPreview preview = new BlockedTimeConflictsPreview(
        previewKey,
        usersAffected,
        blockedTimes.size(),
        reservationsToCancel.size(),
        blockedTimes,
        reservationsToCancel,
        inProgressReservations,
        request);

    blockedTimePreviewCachePort.save(previewKey, preview);

    return preview;
  }

  /**
   * Filtra apenas as reservas da lista de detalhes enriquecidos.
   */
  private List<ReservationDetail> filterReservations(List<ScheduleDetail> enrichedConflicts) {
    return enrichedConflicts.stream()
        .filter(ReservationDetail.class::isInstance)
        .map(ReservationDetail.class::cast)
        .toList();
  }

  /**
   * Filtra apenas os bloqueios da lista de detalhes enriquecidos.
   */
  private List<BlockedTimeDetail> filterBlockedTimes(List<ScheduleDetail> enrichedConflicts) {
    return enrichedConflicts.stream()
        .filter(BlockedTimeDetail.class::isInstance)
        .map(BlockedTimeDetail.class::cast)
        .toList();
  }

  /**
   * Filtra as reservas que estão em andamento.
   */
  private List<ReservationDetail> filterInProgressReservations(List<ReservationDetail> reservations) {
    return reservations.stream()
        .filter(this::isInProgress)
        .toList();
  }

  /**
   * Remove as reservas em andamento da lista de reservas a cancelar.
   */
  private List<ReservationDetail> excludeInProgressReservations(
      List<ReservationDetail> allReservations,
      List<ReservationDetail> inProgressReservations) {

    return allReservations.stream()
        .filter(reservation -> !inProgressReservations.contains(reservation))
        .toList();
  }

  /**
   * Conta o número de usuários distintos afetados.
   */
  private int countDistinctUsers(List<ReservationDetail> reservations) {
    return (int) reservations.stream()
        .map(ReservationDetail::userId)
        .distinct()
        .count();
  }

  /**
   * Verifica se a reserva está em andamento com base na data e horário atuais.
   */
  private boolean isInProgress(ReservationDetail reservation) {
    LocalDateTime now = LocalDateTime.now();

    LocalTime startTime = reservation.timeInterval().startTime();
    LocalTime endTime = reservation.timeInterval().endTime();

    LocalDateTime startDateTime = LocalDateTime.of(reservation.date(), startTime);
    LocalDateTime endDateTime = calculateEndDateTime(reservation.date(), startTime, endTime);

    return !now.isBefore(startDateTime) && now.isBefore(endDateTime);
  }

  /**
   * Calcula o DateTime de término considerando intervalos que atravessam meia-noite.
   */
  private LocalDateTime calculateEndDateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
    if (endTime.isBefore(startTime)) {
      return LocalDateTime.of(date.plusDays(1), endTime);
    }
    return LocalDateTime.of(date, endTime);
  }

  /**
   * Cria um preview vazio quando não há conflitos.
   */
  private BlockedTimeConflictsPreview createEmptyPreview(BlockedTimeConflictsPreviewRequestDto request) {
    return new BlockedTimeConflictsPreview(
        null,
        0,
        0,
        0,
        List.of(),
        List.of(),
        List.of(),
        request);
  }
}
