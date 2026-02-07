package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.gateway.OperatingHoursPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.ConfirmOperatingHoursDisableUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.PreviewStaleException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.OperatingHoursDisableConfirmRequestDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ConfirmOperatingHoursDisableUseCaseImp implements ConfirmOperatingHoursDisableUseCase {

  private final ReservationRepositoryPort reservationRepositoryPort;
  private final BlockedTimeRepositoryPort blockedTimeRepositoryPort;
  private final ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;
  private final OperatingHoursRepositoryPort operatingHoursRepositoryPort;
  private final OperatingHoursPreviewCachePort operatingHoursPreviewCachePort;
  private final ReservationBatchCancellationService reservationBatchCancellationService;

  public ConfirmOperatingHoursDisableUseCaseImp(
      ReservationRepositoryPort reservationRepositoryPort,
      BlockedTimeRepositoryPort blockedTimeRepositoryPort,
      ScheduleEntryRepositoryPort scheduleEntryRepositoryPort,
      OperatingHoursRepositoryPort operatingHoursRepositoryPort,
      OperatingHoursPreviewCachePort operatingHoursPreviewCachePort,
      ReservationBatchCancellationService reservationBatchCancellationService) {
    this.reservationRepositoryPort = reservationRepositoryPort;
    this.blockedTimeRepositoryPort = blockedTimeRepositoryPort;
    this.scheduleEntryRepositoryPort = scheduleEntryRepositoryPort;
    this.operatingHoursRepositoryPort = operatingHoursRepositoryPort;
    this.operatingHoursPreviewCachePort = operatingHoursPreviewCachePort;
    this.reservationBatchCancellationService = reservationBatchCancellationService;
  }

  @Override
  public void execute(UUID adminId, OperatingHoursDisableConfirmRequestDto request) {
    OperatingHoursDisablePreview preview = getPreviewFromCache(request.previewKey(), adminId);
    OperatingHours operatingHours =
        validateOperatingHoursExistsAndActive(preview.operatingHoursId());
    validatePreviewIsNotStale(preview, operatingHours);

    cancelAffectedReservations(preview.affectedReservations(), request.description(), adminId);
    deleteAffectedBlockedTimes(preview.affectedBlockedTimes());

    operatingHours.disable();
    operatingHoursRepositoryPort.save(operatingHours);

    operatingHoursPreviewCachePort.delete(request.previewKey());
  }

  /**
   * Buscar o preview no cache
   *
   * @param previewKey Chave do preview
   * @param adminId ID do admin
   * @return Preview do desativamento do horário de funcionamento
   */
  private OperatingHoursDisablePreview getPreviewFromCache(String previewKey, UUID adminId) {
    return operatingHoursPreviewCachePort.getPreviewOrElseThrow(previewKey, adminId);
  }

  /**
   * Buscar o horário de funcionamento e validar se está ativo
   *
   * @param operatingHoursId Identificador do horário de funcionamento
   * @return Horário de funcionamento
   * @throws OperatingHoursNotFoundException se o horário de funcionamento não for encontrado
   * @throws OperatingHoursStatusConflictException se o horário de funcionamento já estiver
   *     desativado
   */
  private OperatingHours validateOperatingHoursExistsAndActive(UUID operatingHoursId) {
    OperatingHours operatingHours =
        operatingHoursRepositoryPort.findByIdOrElseThrow(operatingHoursId);
    if (!operatingHours.isActive()) {
      throw new OperatingHoursStatusConflictException(ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
    }
    return operatingHours;
  }

  /**
   * Validar se o preview não está desatualizado
   *
   * @param preview Preview de desativação do horário de funcionamento
   * @param operatingHours Horário de funcionamento
   * @throws PreviewStaleException se o preview estiver desatualizado
   */
  private void validatePreviewIsNotStale(
      OperatingHoursDisablePreview preview, OperatingHours operatingHours) {
    List<ScheduleEntry> currentSchedules =
        scheduleEntryRepositoryPort.findAllActiveSchedulesFromTodayByDaysOfWeekAndTimeInterval(
            operatingHours.getDaysOfWeek(), operatingHours.getTimeInterval());

    // Extrair os IDs dos horários de funcionamento atuais
    Set<UUID> currentIds =
        currentSchedules.stream().map(ScheduleEntry::getId).collect(Collectors.toSet());

    // Extrair os IDs dos horários de funcionamento do preview
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
}
