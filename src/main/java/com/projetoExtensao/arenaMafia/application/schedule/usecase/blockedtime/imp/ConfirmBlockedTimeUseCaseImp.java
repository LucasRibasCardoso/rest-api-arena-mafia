package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.result.BatchCancellationResult;
import com.projetoExtensao.arenaMafia.application.schedule.result.ConfirmBlockedTimeResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.BlockedTimeDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.ConfirmBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConfirmBlockedTimeUseCaseImp implements ConfirmBlockedTimeUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmBlockedTimeUseCaseImp.class);

  private final BlockedTimePreviewCachePort previewCachePort;
  private final BlockedTimeRepositoryPort blockedTimeRepository;
  private final ReservationRepositoryPort reservationRepository;
  private final ReservationBatchCancellationService batchCancellationService;
  private final BlockedTimeDateCalculationService dateCalculationService;

  public ConfirmBlockedTimeUseCaseImp(
      BlockedTimePreviewCachePort previewCachePort,
      BlockedTimeRepositoryPort blockedTimeRepository,
      ReservationRepositoryPort reservationRepository,
      ReservationBatchCancellationService batchCancellationService,
      BlockedTimeDateCalculationService dateCalculationService) {
    this.previewCachePort = previewCachePort;
    this.blockedTimeRepository = blockedTimeRepository;
    this.reservationRepository = reservationRepository;
    this.batchCancellationService = batchCancellationService;
    this.dateCalculationService = dateCalculationService;
  }

  @Override
  public ConfirmBlockedTimeResult execute(UUID adminId, BlockedTimeConfirmRequestDto request) {
    String previewKey = request.previewKey();
    String description = request.description();

    BlockedTimeConflictsPreview preview = previewCachePort.getPreviewOrElseThrow(previewKey);
    previewCachePort.validateKeyOwnership(previewKey, adminId);

    int reservationsCancelled = cancelConflictingReservations(preview, description);
    int blockedTimesCancelled = removeConflictingBlockedTimes(preview);

    List<UUID> blockedTimesCreated = createBlockedTimes(preview, description, adminId);

    previewCachePort.delete(previewKey);

    return new ConfirmBlockedTimeResult(
        blockedTimesCreated,
        blockedTimesCreated.size(),
        reservationsCancelled,
        blockedTimesCancelled,
        preview.usersAffected());
  }

  /**
   * Cancela as reservas conflitantes, excluindo as que estão em andamento.
   *
   * @param preview Preview contendo as reservas conflitantes
   * @param description Descrição do bloqueio (usada como motivo do cancelamento)
   * @return Quantidade de reservas canceladas
   */
  private int cancelConflictingReservations(BlockedTimeConflictsPreview preview, String description) {

    // Busca as entidades Reservation completas baseado nos IDs dos Details
    List<UUID> reservationIds = preview.conflictingReservations().stream()
        .map(ReservationDetail::reservationId)
        .collect(Collectors.toList());

    List<Reservation> allReservations = reservationRepository.findAllByIds(reservationIds);

    // Filtra apenas as que NÃO estão em andamento
    Set<UUID> inProgressIds = preview.inProgressReservations().stream()
        .map(ReservationDetail::reservationId)
        .collect(Collectors.toSet());

    List<Reservation> reservationsToCancel = allReservations.stream()
        .filter(r -> !inProgressIds.contains(r.getId()))
        .collect(Collectors.toList());

    if (reservationsToCancel.isEmpty()) {
      return 0;
    }

    String cancellationReason = String.format("Bloqueio de horário criado: %s", description);

    BatchCancellationResult result =
        batchCancellationService.cancelReservationsInBatch(reservationsToCancel, cancellationReason);

    if (result.hasFailures()) {
      LOGGER.warn(
          "Algumas reservas falharam ao cancelar: {}",
          result.failedReservationIds());
    }

    return result.successCount();
  }

  /**
   * Remove os BlockedTimes conflitantes em batch.
   *
   * @param preview Preview contendo os bloqueios conflitantes
   * @return Quantidade de bloqueios removidos
   */
  private int removeConflictingBlockedTimes(BlockedTimeConflictsPreview preview) {
    List<BlockedTimeDetail> blockedTimesToRemove = preview.conflictingBlockedTimes();

    if (blockedTimesToRemove.isEmpty()) {
      LOGGER.debug("Nenhum BlockedTime para remover");
      return 0;
    }

    List<UUID> idsToRemove = blockedTimesToRemove.stream()
        .map(BlockedTimeDetail::blockedTimeId)
        .toList();

    blockedTimeRepository.deleteAllByIds(idsToRemove);

    LOGGER.debug("Removidos {} BlockedTime(s) conflitantes", idsToRemove.size());
    return idsToRemove.size();
  }

  /**
   * Cria os novos BlockedTime(s) baseado nos dados do preview.
   *
   * <p>Para cada combinação de quadra e data aplicável, cria um BlockedTime:
   * <ul>
   *   <li>Se isFullDay = true, calcula o timeInterval baseado nos horários de funcionamento
   *   <li>Se isFullDay = false, usa o timeInterval informado na requisição
   *   <li>Se múltiplos bloqueios são criados, gera um recurringBlockedTimeId para agrupá-los
   * </ul>
   *
   * @param preview Preview contendo os dados de criação
   * @param description Descrição do bloqueio
   * @param adminId ID do admin que está criando os bloqueios
   * @return Lista de IDs dos BlockedTimes criados
   */
  private List<UUID> createBlockedTimes(
      BlockedTimeConflictsPreview preview, String description, UUID adminId) {

    var request = preview.request();

    Set<DayOfWeek> effectiveDaysOfWeek = dateCalculationService.resolveEffectiveDaysOfWeek(
        request.selectedDaysOfWeek(),
        request.startDate(),
        request.endDate());

    List<LocalDate> applicableDates = dateCalculationService.calculateApplicableDates(
        request.startDate(),
        request.endDate(),
        request.selectedDaysOfWeek());

    TimeInterval timeInterval = dateCalculationService.calculateSearchInterval(
        request.isFullDay(),
        request.timeInterval(),
        effectiveDaysOfWeek);

    int totalBlockedTimes = request.courtIds().size() * applicableDates.size();
    UUID recurringBlockedTimeId = totalBlockedTimes > 1 ? UUID.randomUUID() : null;

    LOGGER.debug(
        "Criando {} BlockedTime(s) para {} quadra(s) em {} data(s). RecurringId: {}",
        totalBlockedTimes,
        request.courtIds().size(),
        applicableDates.size(),
        recurringBlockedTimeId);

    List<UUID> createdIds = new ArrayList<>();

    for (UUID courtId : request.courtIds()) {
      for (LocalDate date : applicableDates) {
        BlockedTime blockedTime = createBlockedTime(
            courtId,
            date,
            timeInterval,
            request.isFullDay(),
            description,
            adminId,
            recurringBlockedTimeId);

        BlockedTime saved = blockedTimeRepository.save(blockedTime);
        createdIds.add(saved.getId());
      }
    }

    LOGGER.info("Criados {} BlockedTime(s)", createdIds.size());
    return createdIds;
  }

  /**
   * Cria um BlockedTime individual usando o factory method apropriado.
   *
   * <p>Escolhe o factory method baseado no contexto:
   * <ul>
   *   <li>Se há recurringBlockedTimeId: usa createRecurring (operação em lote)
   *   <li>Se isFullDay = true e sem recorrência: usa createFullDay
   *   <li>Se isFullDay = false e sem recorrência: usa createSpecificTime
   * </ul>
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

    if (recurringBlockedTimeId != null) {
      return BlockedTime.createRecurring(
          courtId,
          dateTimeSlot,
          description,
          adminId,
          isFullDay,
          recurringBlockedTimeId);
    }

    if (isFullDay) {
      return BlockedTime.createFullDay(courtId, dateTimeSlot, description, adminId);
    }

    return BlockedTime.createSpecificTime(courtId, dateTimeSlot, description, adminId);
  }
}
