package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.gateway.CourtDisablePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import com.projetoExtensao.arenaMafia.application.court.usecase.PreviewCourtDisableUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PreviewCourtDisableUseCaseImp implements PreviewCourtDisableUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final CourtDisablePreviewCachePort courtDisablePreviewCachePort;
  private final ScheduleEntryEnrichmentService scheduleEntryEnrichmentService;

  public PreviewCourtDisableUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      CourtDisablePreviewCachePort courtDisablePreviewCachePort,
      ScheduleEntryEnrichmentService scheduleEntryEnrichmentService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.courtDisablePreviewCachePort = courtDisablePreviewCachePort;
    this.scheduleEntryEnrichmentService = scheduleEntryEnrichmentService;
  }

  @Override
  public CourtDisablePreview execute(UUID courtId, UUID adminId) {
    Court court = validateCourtExistsAndIsActive(courtId);

    List<ScheduleEntry> affectedSchedules = fetchAffectedSchedules(courtId);
    ScheduleEntriesEnrichedResult enrichedAffectedSchedules = enrichScheduleEntries(affectedSchedules);

    return buildAndSaveCachePreview(enrichedAffectedSchedules, adminId, court);
  }

  /**
   * Valida se a quadra existe e está ativa.
   * @param courtId ID da quadra.
   * @return A quadra validada.
   */
  private Court validateCourtExistsAndIsActive(UUID courtId) {
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);

    if (!court.isActive()) {
      throw new CourtStatusConflictException(ErrorCode.COURT_ALREADY_DISABLED);
    }
    return court;
  }

  /**
   * Busca todos os agendamentos ativos futuros de uma quadra.
   * @param courtId ID da quadra.
   * @return Lista de agendamentos afetados.
   */
  private List<ScheduleEntry> fetchAffectedSchedules(UUID courtId) {
    LocalDate today = LocalDate.now();
    return scheduleEntryRepositoryPort.findAllActiveSchedulesByCourtIdAfterDate(courtId, today);
  }

  /**
   * Enriquecer os agendamentos com informações adicionais.
   * @param scheduleEntries Lista de agendamentos a serem enriquecidos.
   * @return Resultado do enriquecimento dos agendamentos.
   */
  private ScheduleEntriesEnrichedResult enrichScheduleEntries(List<ScheduleEntry> scheduleEntries) {
    return scheduleEntryEnrichmentService.enrichScheduleEntries(scheduleEntries);
  }

  /**
   * Constrói e salva em cache a pré-visualização de desativação da quadra.
   * @param enrichedAffectedSchedulesResult Agendamentos afetados enriquecidos.
   * @param adminId ID do administrador que está solicitando a pré-visualização.
   * @param court A quadra que está sendo desativada.
   * @return A pré-visualização de desativação da quadra.
   */
  private CourtDisablePreview buildAndSaveCachePreview(
          ScheduleEntriesEnrichedResult enrichedAffectedSchedulesResult,
          UUID adminId,
          Court court
  ) {
    List<ReservationDetail> reservations = enrichedAffectedSchedulesResult.enrichedReservations();
    List<BlockedTimeDetail> blockedTimes = enrichedAffectedSchedulesResult.enrichedBlockedTimes();

    List<ReservationDetail> inProgressReservations = reservations.stream()
            .filter(ReservationDetail::isInProgress)
            .toList();

    List<ReservationDetail> reservationsToCancel = reservations.stream()
            .filter(reservation -> !reservation.isInProgress())
            .toList();


    int usersAffected = countDistinctUsers(reservations);
    String previewKey = courtDisablePreviewCachePort.generateKey(adminId);

    CourtDisablePreview preview =
        new CourtDisablePreview(
            previewKey,
            court.getId(),
            court.getName(),
            usersAffected,
            blockedTimes.size(),
            reservationsToCancel.size(),
            blockedTimes,
            reservationsToCancel,
            inProgressReservations
        );

    courtDisablePreviewCachePort.save(previewKey, preview);
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
