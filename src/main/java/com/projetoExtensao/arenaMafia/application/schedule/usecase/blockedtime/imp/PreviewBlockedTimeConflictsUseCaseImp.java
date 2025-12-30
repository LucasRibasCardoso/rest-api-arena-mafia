package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.domain.dto.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.domain.dto.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.domain.dto.ReservationDetail;
import com.projetoExtensao.arenaMafia.domain.dto.ScheduleDetail;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidBlockDateException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PreviewBlockedTimeConflictsUseCaseImp implements PreviewBlockedTimeConflictsUseCase {

  private final ScheduleEntryEnrichmentService enrichmentService;
  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final OperatingHoursRepositoryPort operatingHourRepository;
  private final BlockedTimePreviewCachePort blockedTimePreviewCachePort;

  public PreviewBlockedTimeConflictsUseCaseImp(
          ScheduleEntryEnrichmentService enrichmentService,
          ScheduleEntryRepositoryPort scheduleEntryRepository,
      OperatingHoursRepositoryPort operatingHourRepository,
      BlockedTimePreviewCachePort blockedTimePreviewCachePort
      ) {
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.operatingHourRepository = operatingHourRepository;
    this.enrichmentService = enrichmentService;
    this.blockedTimePreviewCachePort = blockedTimePreviewCachePort;
  }

  @Override
  public BlockedTimeConflictsPreview execute(BlockedTimeConflictsPreviewRequestDto request, UUID adminId) {
    TimeInterval searchInterval = calculateOperatingInterval(request);

    List<ScheduleEntry> conflicts = scheduleEntryRepository.findConflicts(
            request.courtIds(),
            request.startDate(),
            request.endDate(),
            searchInterval);

    if (conflicts.isEmpty()) {
      return new BlockedTimeConflictsPreview(null, 0, 0, 0, List.of(), List.of());
    }

    return enrichmentConflicts(conflicts, adminId);
  }

  /**
   * Enriquece os conflitos encontrados com detalhes adicionais, calcula estatísticas
   * e salva o resultado no cache.
   *
   * @param conflicts lista de conflitos encontrados
   * @param adminId ID do administrador que está gerando o preview
   * @return DTO de domínio com detalhes dos conflitos, estatísticas e chave do cache
   */
  private BlockedTimeConflictsPreview enrichmentConflicts(List<ScheduleEntry> conflicts, UUID adminId) {
    // Enriquecer todos os conflitos de uma vez usando polimorfismo
    List<ScheduleDetail> enrichedConflicts = enrichmentService.enrichScheduleEntries(conflicts);

    // Separar por tipo após enriquecimento
    List<ReservationDetail> enrichedReservations =
        enrichedConflicts.stream()
            .filter(detail -> detail instanceof ReservationDetail)
            .map(detail -> (ReservationDetail) detail)
            .toList();

    List<BlockedTimeDetail> enrichedBlockedTimes =
        enrichedConflicts.stream()
            .filter(detail -> detail instanceof BlockedTimeDetail)
            .map(detail -> (BlockedTimeDetail) detail)
            .toList();

    // Calcular estatísticas
    int usersAffected =
        (int)
            enrichedReservations.stream()
                .map(ReservationDetail::reservationId)
                .distinct()
                .count();

    String previewKey = blockedTimePreviewCachePort.generateKey(adminId);

    // Criar o preview COM a chave já definida
    BlockedTimeConflictsPreview preview = new BlockedTimeConflictsPreview(
        previewKey,
        enrichedBlockedTimes.size(),
        usersAffected,
        enrichedReservations.size(),
        enrichedBlockedTimes,
        enrichedReservations);

    blockedTimePreviewCachePort.save(previewKey, preview);

    return preview;
  }

  /**
   * Calcula o intervalo de tempo de funcionamento com base no atributo isFullDay da requisição
   *
   * @param request requisição de visualização de conflitos de tempo bloqueado
   * @return intervalo de tempo de funcionamento
   */
  private TimeInterval calculateOperatingInterval(BlockedTimeConflictsPreviewRequestDto request) {
    if (!request.isFullDay()) {
      return new TimeInterval(request.timeInterval().startTime(), request.timeInterval().endTime());
    }

    Set<DayOfWeek> dayOfWeeks = getDaysOfWeekInRange(request.startDate(), request.endDate());
    List<OperatingHours> hoursList = operatingHourRepository.findByDaysOfWeek(dayOfWeeks);

    if (hoursList.isEmpty()) {
      throw new InvalidBlockDateException(ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND);
    }

    LocalTime minStart = calculateMinStart(hoursList);
    LocalTime maxEnd = calculateMaxEnd(hoursList);
    return new TimeInterval(minStart, maxEnd);
  }

  /**
   * Converter as datas num conjunto de dias da semana
   *
   * @param startDate data de início
   * @param endDate data de fim
   * @return conjunto de dias da semana
   */
  private Set<DayOfWeek> getDaysOfWeekInRange(LocalDate startDate, LocalDate endDate) {
    return startDate
        .datesUntil(endDate.plusDays(1))
        .map(DayOfWeek::convertToDayOfWeek)
        .collect(Collectors.toSet());
  }

  /**
   * Calcula o horário de início mínimo a partir da lista de horários de funcionamento
   *
   * @param hoursList lista de horários de funcionamento
   * @return horário de início mínimo
   */
  private LocalTime calculateMinStart(List<OperatingHours> hoursList) {
    return hoursList.stream()
        .map(oh -> oh.getTimeInterval().startTime())
        .min(LocalTime::compareTo)
        .orElse(LocalTime.MIN);
  }

  /**
   * Calcula o horário de término máximo a partir da lista de horários de funcionamento
   *
   * @param hoursList lista de horários de funcionamento
   * @return horário de término máximo
   */
  private LocalTime calculateMaxEnd(List<OperatingHours> hoursList) {
    return hoursList.stream()
        .map(oh -> oh.getTimeInterval().endTime())
        .max(LocalTime::compareTo)
        .orElse(LocalTime.MAX);
  }
}
