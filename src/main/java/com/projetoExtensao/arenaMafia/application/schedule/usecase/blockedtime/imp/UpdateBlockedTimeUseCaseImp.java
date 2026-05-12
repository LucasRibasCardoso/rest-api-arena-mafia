package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.result.ScheduleEntriesEnrichedResult;
import com.projetoExtensao.arenaMafia.application.schedule.service.ScheduleEntryEnrichmentService;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.UpdateBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeUpdateRequestDto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateBlockedTimeUseCaseImp implements UpdateBlockedTimeUseCase {

  private final ScheduleEntryEnrichmentService enrichmentService;
  private final BlockedTimeRepositoryPort blockedTimeRepositoryPort;

  public UpdateBlockedTimeUseCaseImp(
      ScheduleEntryEnrichmentService enrichmentService,
      BlockedTimeRepositoryPort blockedTimeRepositoryPort) {
    this.enrichmentService = enrichmentService;
    this.blockedTimeRepositoryPort = blockedTimeRepositoryPort;
  }

  @Override
  public List<BlockedTimeDetail> execute(
      UUID blockedTimeId, BlockedTimeUpdateRequestDto requestDto) {
    BlockedTime blockedTime = blockedTimeRepositoryPort.findByIdOrElseThrow(blockedTimeId);

    if (blockedTime.isRecurring() && requestDto.updateAllRecurring()) {
      List<BlockedTime> recurringBlockedTimes =
          fetchRecurringBlockedTimes(blockedTime.getRecurringBlockedTimeId());
      return updateRecurringBlockedTimes(recurringBlockedTimes, requestDto);
    } else {
      BlockedTimeDetail updatedBlockedTime = updateSingleBlockedTime(blockedTime, requestDto);
      return List.of(updatedBlockedTime);
    }
  }

  /**
   * Busca todos os BlockedTime recorrentes através do "recurringBlockedTimeId"
   *
   * @param recurringBlockedTimeId identificador do grupo recorrente de BlockedTime
   * @return Lista de BlockedTime do mesmo grupo
   */
  private List<BlockedTime> fetchRecurringBlockedTimes(UUID recurringBlockedTimeId) {
    return blockedTimeRepositoryPort.findAllByRecurringBlockedTimeId(recurringBlockedTimeId);
  }

  /**
   * Atualiza um único BlockedTime
   *
   * @param blockedTime BlockedTime a ser atualizado
   * @param requestDto DTO de atualização
   * @return BlockedTime atualizado
   */
  private BlockedTimeDetail updateSingleBlockedTime(
      BlockedTime blockedTime, BlockedTimeUpdateRequestDto requestDto) {

    blockedTime.updateDescription(requestDto.description());
    BlockedTime savedBlockedTime = blockedTimeRepositoryPort.save(blockedTime);
    return enrichmentService.enrichBlockedTime(savedBlockedTime);
  }

  /**
   * Atualiza todos os BlockedTime recorrentes
   *
   * @param recurringBlockedTimes Lista de BlockedTime recorrentes
   * @param requestDto DTO de atualização
   * @return Lista de BlockedTime atualizados
   */
  private List<BlockedTimeDetail> updateRecurringBlockedTimes(
      List<BlockedTime> recurringBlockedTimes, BlockedTimeUpdateRequestDto requestDto) {

    recurringBlockedTimes.forEach(
        blockedTime -> blockedTime.updateDescription(requestDto.description()));
    List<BlockedTime> savedRecurringBlockedTimes =
        blockedTimeRepositoryPort.saveAll(recurringBlockedTimes);
    ScheduleEntriesEnrichedResult enrichedResult =
        enrichmentService.enrichScheduleEntries(savedRecurringBlockedTimes);
    return enrichedResult.enrichedBlockedTimes();
  }
}
