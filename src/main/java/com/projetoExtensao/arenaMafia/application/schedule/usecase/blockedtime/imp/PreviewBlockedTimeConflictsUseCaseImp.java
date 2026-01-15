package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.detail.ReservationDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.BlockedTimeDateCalculationService;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PreviewBlockedTimeConflictsUseCaseImp implements PreviewBlockedTimeConflictsUseCase {

  private final ScheduleEntryEnrichmentService scheduleEntryEnrichmentService;
  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final BlockedTimePreviewCachePort blockedTimePreviewCachePort;
  private final BlockedTimeDateCalculationService dateCalculationService;
  private final CourtRepositoryPort courtRepository;

  public PreviewBlockedTimeConflictsUseCaseImp(
      ScheduleEntryEnrichmentService scheduleEntryEnrichmentService,
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      BlockedTimePreviewCachePort blockedTimePreviewCachePort,
      BlockedTimeDateCalculationService dateCalculationService,
      CourtRepositoryPort courtRepository) {
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.scheduleEntryEnrichmentService = scheduleEntryEnrichmentService;
    this.blockedTimePreviewCachePort = blockedTimePreviewCachePort;
    this.dateCalculationService = dateCalculationService;
    this.courtRepository = courtRepository;
  }

  @Override
  public BlockedTimeConflictsPreview execute(
      BlockedTimeConflictsPreviewRequestDto request, UUID adminId) {
    // Valida se todas as quadras existem e estão ativas
    courtRepository.validateAllExistAndActive(request.courtIds());

    // Calcula os dias semanais efetivos considerando as ocorrências e o intervalo de horários
    Set<DayOfWeek> effectiveDaysOfWeek =
        dateCalculationService.resolveEffectiveDaysOfWeekWithOccurrencesValidation(
            request.selectedDaysOfWeek(),
            request.startDate(),
            request.endDate(),
            request.courtIds().size());
    TimeInterval searchInterval =
        dateCalculationService.calculateSearchInterval(
            request.isFullDay(), request.timeInterval(), effectiveDaysOfWeek);

    // Busca os agendamentos conflitantes com o bloqueio proposto no intervalo de datas e horários
    // especificados
    List<ScheduleEntry> conflicts =
        scheduleEntryRepository.findAllActiveSchedulesConflicts(
            request.courtIds(),
            request.startDate(),
            request.endDate(),
            searchInterval,
            effectiveDaysOfWeek);

    // Enriquecer os agendamentos conflitantes com detalhes adicionais
    ScheduleEntriesEnrichedResult enrichedConflicts =
        scheduleEntryEnrichmentService.enrichScheduleEntries(conflicts);

    // Constrói e armazena em cache a pré-visualização dos conflitos de bloqueio de horário
    return buildAndSaveCachePreview(enrichedConflicts, request, adminId);
  }

  /**
   * Constrói e armazena em cache a pré-visualização dos conflitos de bloqueio de horário.
   *
   * @param enrichedConflictsResult Resultado do enriquecimento dos agendamentos conflitantes.
   * @param request Detalhes da solicitação de pré-visualização.
   * @param adminId ID do administrador que está solicitando a pré-visualização.
   * @return A pré-visualização dos conflitos de bloqueio de horário.
   */
  private BlockedTimeConflictsPreview buildAndSaveCachePreview(
      ScheduleEntriesEnrichedResult enrichedConflictsResult,
      BlockedTimeConflictsPreviewRequestDto request,
      UUID adminId) {

    List<ReservationDetail> reservations = enrichedConflictsResult.enrichedReservations();
    List<BlockedTimeDetail> blockedTimes = enrichedConflictsResult.enrichedBlockedTimes();

    List<ReservationDetail> inProgressReservations =
        reservations.stream().filter(ReservationDetail::isInProgress).toList();

    List<ReservationDetail> reservationsToCancel =
        reservations.stream().filter(reservation -> !reservation.isInProgress()).toList();

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

  /**
   * Conta o número de usuários distintos em uma lista de reservas.
   *
   * @param reservations Lista de detalhes de reservas.
   * @return O número de usuários distintos.
   */
  private int countDistinctUsers(List<ReservationDetail> reservations) {
    return (int) reservations.stream().map(ReservationDetail::userId).distinct().count();
  }
}
