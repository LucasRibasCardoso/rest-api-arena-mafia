package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.gateway.CourtPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import com.projetoExtensao.arenaMafia.application.court.usecase.ConfirmCourtDisableUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PreviewStaleException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CourtDisableConfirmRequestDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmCourtDisableUseCaseImp implements ConfirmCourtDisableUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ReservationRepositoryPort reservationRepositoryPort;
  private final BlockedTimeRepositoryPort blockedTimeRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final CourtPreviewCachePort courtDisablePreviewCachePort;
  private final ReservationBatchCancellationService reservationBatchCancellationService;

  public ConfirmCourtDisableUseCaseImp(
      CourtRepositoryPort courtRepositoryPort,
      ReservationRepositoryPort reservationRepositoryPort,
      BlockedTimeRepositoryPort blockedTimeRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      CourtPreviewCachePort courtDisablePreviewCachePort,
      ReservationBatchCancellationService reservationBatchCancellationService) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.blockedTimeRepositoryPort = blockedTimeRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.courtDisablePreviewCachePort = courtDisablePreviewCachePort;
    this.reservationBatchCancellationService = reservationBatchCancellationService;
  }

  public void execute(UUID adminId, CourtDisableConfirmRequestDto request) {
    CourtDisablePreview preview =
        courtDisablePreviewCachePort.getPreviewOrElseThrow(request.previewKey(), adminId);
    validatePreviewIsNotStale(preview);

    Court court = validateCourtExistsAndActive(preview.courtId());

    cancelAffectedReservations(preview.affectedReservations(), request.description(), adminId);
    deleteAffectedBlockedTimes(preview.affectedBlockedTimes());

    court.disable();
    courtRepositoryPort.save(court);

    courtDisablePreviewCachePort.delete(request.previewKey());
  }

  /**
   * Validar se o preview de desativação da quadra não está desatualizado.
   *
   * @param preview O preview de desativação da quadra.
   * @throws PreviewStaleException se o preview estiver desatualizado.
   */
  private void validatePreviewIsNotStale(CourtDisablePreview preview) {
    List<ScheduleEntry> currentSchedules =
        scheduleEntryRepositoryPort.findAllActiveSchedulesByCourtIdFromToday(preview.courtId());

    Set<UUID> currentIds =
        currentSchedules.stream().map(ScheduleEntry::getId).collect(Collectors.toSet());

    Set<UUID> previewIds =
        Stream.of(
                preview.affectedReservations().stream().map(ReservationDetail::reservationId),
                preview.affectedBlockedTimes().stream().map(BlockedTimeDetail::blockedTimeId),
                preview.inProgressReservations().stream().map(ReservationDetail::reservationId))
            .flatMap(s -> s)
            .collect(Collectors.toSet());

    if (!currentIds.equals(previewIds)) {
      throw new PreviewStaleException();
    }
  }

  /**
   * Cancela as reservas afetadas pela desativação da quadra.
   *
   * @param reservations As reservas afetadas.
   * @param description A descrição da desativação.
   * @param adminId Identificador do administrador que está cancelando as reservas
   */
  private void cancelAffectedReservations(List<ReservationDetail> reservations, String description, UUID adminId) {
    List<UUID> reservationIdsToCancel =
        reservations.stream()
            .filter(detail -> !detail.isInProgress())
            .map(ReservationDetail::reservationId)
            .toList();

    if (reservationIdsToCancel.isEmpty()) {
      return;
    }

    List<Reservation> reservationsToCancel = reservationRepositoryPort.findAllFutureReservationsByIds(reservationIdsToCancel);

    String cancellationReason = String.format("Quadra desativada: %s", description);
    reservationBatchCancellationService.cancelReservationsInBatchByAdmin(reservationsToCancel, cancellationReason, adminId);
  }

  /**
   * Deletar os horários bloqueados afetados pela desativação da quadra.
   *
   * @param blockedTimes Os horários bloqueados afetados.
   */
  private void deleteAffectedBlockedTimes(List<BlockedTimeDetail> blockedTimes) {
    List<UUID> blockedTimeIdsToDelete =
        blockedTimes.stream().map(BlockedTimeDetail::blockedTimeId).toList();

    if (blockedTimeIdsToDelete.isEmpty()) {
      return;
    }

    blockedTimeRepositoryPort.deleteAllByIds(blockedTimeIdsToDelete);
  }

  /**
   * Valida se a quadra existe e está ativa.
   *
   * @param courtId O ID da quadra.
   * @return A quadra válida.
   * @throws CourtNotFoundException se a quadra não existir
   * @throws CourtStatusConflictException se a quadra já estiver desativada
   */
  private Court validateCourtExistsAndActive(UUID courtId) {
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);

    if (!court.isActive()) {
      throw new CourtStatusConflictException(ErrorCode.COURT_ALREADY_DISABLED);
    }

    return court;
  }
}
