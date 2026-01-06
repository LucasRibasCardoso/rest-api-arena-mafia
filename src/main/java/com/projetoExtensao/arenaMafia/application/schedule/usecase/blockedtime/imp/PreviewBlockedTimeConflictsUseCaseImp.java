package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.service.BlockedTimeDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidBlockedTimeException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
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
  private final CourtRepositoryPort courtRepository;

  public PreviewBlockedTimeConflictsUseCaseImp(
      ScheduleEntryEnrichmentService enrichmentService,
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      BlockedTimePreviewCachePort blockedTimePreviewCachePort,
      BlockedTimeDateCalculationService dateCalculationService,
      CourtRepositoryPort courtRepository) {
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.enrichmentService = enrichmentService;
    this.blockedTimePreviewCachePort = blockedTimePreviewCachePort;
    this.dateCalculationService = dateCalculationService;
    this.courtRepository = courtRepository;
  }

  @Override
  public BlockedTimeConflictsPreview execute(
      BlockedTimeConflictsPreviewRequestDto request, UUID adminId) {
    validateCourtsExist(request.courtIds());

    Set<DayOfWeek> effectiveDaysOfWeek = resolveAndValidateDaysOfWeek(request);
    TimeInterval searchInterval = calculateSearchInterval(request, effectiveDaysOfWeek);

    List<ScheduleEntry> conflicts = findConflicts(request, searchInterval, effectiveDaysOfWeek);

    return buildAndCachePreview(conflicts, request, adminId);
  }

  private Set<DayOfWeek> resolveAndValidateDaysOfWeek(
      BlockedTimeConflictsPreviewRequestDto request
  ) {
    Set<DayOfWeek> effectiveDaysOfWeek =
        dateCalculationService.resolveEffectiveDaysOfWeekWithOccurrencesValidation(
            request.selectedDaysOfWeek(),
            request.startDate(),
            request.endDate(),
            request.courtIds().size());

    dateCalculationService.validateDaysHaveOperatingHours(effectiveDaysOfWeek);

    return effectiveDaysOfWeek;
  }

  private TimeInterval calculateSearchInterval(
      BlockedTimeConflictsPreviewRequestDto request,
      Set<DayOfWeek> effectiveDaysOfWeek
  ) {

    if (!request.isFullDay() && request.timeInterval() == null) {
      throw new InvalidBlockedTimeException(
          ErrorCode.BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY);
    }

    return dateCalculationService.calculateSearchInterval(
        request.isFullDay(), request.timeInterval(), effectiveDaysOfWeek);
  }

  private List<ScheduleEntry> findConflicts(
      BlockedTimeConflictsPreviewRequestDto request,
      TimeInterval searchInterval,
      Set<DayOfWeek> effectiveDaysOfWeek
  ) {

    return scheduleEntryRepository.findConflicts(
        request.courtIds(),
        request.startDate(),
        request.endDate(),
        searchInterval,
        effectiveDaysOfWeek);
  }

  private BlockedTimeConflictsPreview buildAndCachePreview(
      List<ScheduleEntry> conflicts,
      BlockedTimeConflictsPreviewRequestDto request,
      UUID adminId
  ) {

    List<ScheduleDetail> enrichedConflicts = enrichmentService.enrichScheduleEntries(conflicts);

    List<ReservationDetail> allReservations = filterReservations(enrichedConflicts);
    List<BlockedTimeDetail> blockedTimes = filterBlockedTimes(enrichedConflicts);
    List<ReservationDetail> inProgressReservations = filterInProgressReservations(allReservations);
    List<ReservationDetail> reservationsToCancel =
        excludeInProgressReservations(allReservations, inProgressReservations);

    int usersAffected = countDistinctUsers(reservationsToCancel);
    String previewKey = blockedTimePreviewCachePort.generateKey(adminId);

    BlockedTimeConflictsPreview preview =
        new BlockedTimeConflictsPreview(
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

  private List<ReservationDetail> filterReservations(List<ScheduleDetail> enrichedConflicts) {
    return enrichedConflicts.stream()
        .filter(ReservationDetail.class::isInstance)
        .map(ReservationDetail.class::cast)
        .toList();
  }

  private List<BlockedTimeDetail> filterBlockedTimes(List<ScheduleDetail> enrichedConflicts) {
    return enrichedConflicts.stream()
        .filter(BlockedTimeDetail.class::isInstance)
        .map(BlockedTimeDetail.class::cast)
        .toList();
  }

  private List<ReservationDetail> filterInProgressReservations(List<ReservationDetail> reservations) {
    return reservations.stream().filter(this::isInProgress).toList();
  }

  private List<ReservationDetail> excludeInProgressReservations(
          List<ReservationDetail> allReservations,
          List<ReservationDetail> inProgressReservations
  ) {

    return allReservations.stream()
        .filter(reservation -> !inProgressReservations.contains(reservation))
        .toList();
  }

  private int countDistinctUsers(List<ReservationDetail> reservations) {
    return (int) reservations.stream().map(ReservationDetail::userId).distinct().count();
  }

  private boolean isInProgress(ReservationDetail reservation) {
    LocalDateTime now = LocalDateTime.now();

    LocalTime startTime = reservation.timeInterval().startTime();
    LocalTime endTime = reservation.timeInterval().endTime();

    LocalDateTime startDateTime = LocalDateTime.of(reservation.date(), startTime);
    LocalDateTime endDateTime = calculateEndDateTime(reservation.date(), startTime, endTime);

    return !now.isBefore(startDateTime) && now.isBefore(endDateTime);
  }

  private LocalDateTime calculateEndDateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
    if (endTime.isBefore(startTime)) {
      return LocalDateTime.of(date.plusDays(1), endTime);
    }
    return LocalDateTime.of(date, endTime);
  }

  private void validateCourtsExist(List<UUID> courtIds) {
    Set<UUID> courtIdsSet = Set.copyOf(courtIds);
    List<Court> foundCourts = courtRepository.findAllByIds(courtIdsSet);

    if (foundCourts.size() != courtIdsSet.size()) {
      throw new CourtNotFoundException(ErrorCode.COURT_NOT_FOUND);
    }
  }
}
