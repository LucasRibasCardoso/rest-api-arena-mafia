package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.gateway.OperatingHoursPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;

import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.PreviewOperatingHoursDisableUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PreviewOperatingHoursDisableUseCaseImp implements PreviewOperatingHoursDisableUseCase {

  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final OperatingHoursRepositoryPort operatingHoursRepository;
  private final ScheduleEntryEnrichmentService scheduleEntryEnrichmentService;
  private final OperatingHoursPreviewCachePort operatingHoursPreviewCachePort;

  public PreviewOperatingHoursDisableUseCaseImp(
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      OperatingHoursRepositoryPort operatingHoursRepository,
      ScheduleEntryEnrichmentService scheduleEntryEnrichmentService,
      OperatingHoursPreviewCachePort operatingHoursPreviewCachePort) {
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.operatingHoursRepository = operatingHoursRepository;
    this.scheduleEntryEnrichmentService = scheduleEntryEnrichmentService;
    this.operatingHoursPreviewCachePort = operatingHoursPreviewCachePort;
  }


  @Override
  public OperatingHoursDisablePreview execute(UUID adminId, UUID operatingHoursId) {
    OperatingHours operatingHours = validateCourtExistsAndIsActive(operatingHoursId);

    List<ScheduleEntry> affectedSchedules = fetchAffectedSchedules(operatingHours);
    ScheduleEntriesEnrichedResult enrichedAffectedSchedules = enrichScheduleEntries(affectedSchedules);

    return buildAndSaveCachePreview(enrichedAffectedSchedules, adminId, operatingHours);
  }

  /**
   * Valida se o horário de funcionamento existe e está ativo.
   * @param operatingHoursId ID do horário de funcionamento.
   * @return O horário de funcionamento validado.
   */
  private OperatingHours validateCourtExistsAndIsActive(UUID operatingHoursId) {
    OperatingHours operatingHours = operatingHoursRepository.findByIdOrElseThrow(operatingHoursId);

    if (!operatingHours.isActive()) {
      throw new OperatingHoursStatusConflictException(ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
    }

    return operatingHours;
  }

  /**
   * Buscar os agendamentos afetados.
   *
   * @param operatingHours Horário de funcionamento.
   * @return Lista de agendamentos afetados.
   */
  private List<ScheduleEntry> fetchAffectedSchedules(OperatingHours operatingHours) {
    return scheduleEntryRepository.findAllActiveSchedulesFromTodayByDaysOfWeekAndTimeInterval(
            operatingHours.getDaysOfWeek(),
            operatingHours.getTimeInterval()
    );
  }

  /**
   * Enriquecer os agendamentos afetados.
   *
   * @param affectedSchedules Lista de agendamentos afetados.
   * @return Resultado dos agendamentos enriquecidos.
   */
  private ScheduleEntriesEnrichedResult enrichScheduleEntries(List<ScheduleEntry> affectedSchedules) {
    return scheduleEntryEnrichmentService.enrichScheduleEntries(affectedSchedules);
  }

  /**
   * Construir e salvar o preview em cache.
   *
   * @param enrichedAffectedSchedulesResult Resultado dos agendamentos enriquecidos.
   * @param adminId ID do administrador.
   * @param operatingHours Horário de funcionamento.
   * @return Preview do desativamento do horário de funcionamento.
   */
  private OperatingHoursDisablePreview buildAndSaveCachePreview(
          ScheduleEntriesEnrichedResult enrichedAffectedSchedulesResult,
          UUID adminId,
          OperatingHours operatingHours) {

    List<ReservationDetail> reservations = enrichedAffectedSchedulesResult.enrichedReservations();
    List<BlockedTimeDetail> blockedTimes = enrichedAffectedSchedulesResult.enrichedBlockedTimes();

    List<ReservationDetail> inProgressReservations = reservations.stream()
            .filter(ReservationDetail::isInProgress)
            .toList();

    List<ReservationDetail> reservationsToCancel = reservations.stream()
            .filter(reservation -> !reservation.isInProgress())
            .toList();

    int usersAffected = countDistinctUsers(reservations);
    String previewKey = operatingHoursPreviewCachePort.generateKey(adminId);

    OperatingHoursDisablePreview preview = new OperatingHoursDisablePreview(
            previewKey,
            operatingHours.getId(),
            usersAffected,
            blockedTimes.size(),
            reservationsToCancel.size(),
            blockedTimes,
            reservationsToCancel,
            inProgressReservations
    );

    operatingHoursPreviewCachePort.save(previewKey, preview);
    return preview;
  }

  /**
   * Conta o número de usuários distintos em uma lista de reservas.
   * @param reservations Lista de detalhes de reservas.
   * @return O número de usuários distintos.
   */
  private int countDistinctUsers(List<ReservationDetail> reservations) {
    return (int) reservations.stream()
            .map(ReservationDetail::userId)
            .distinct()
            .count();
  }
}
