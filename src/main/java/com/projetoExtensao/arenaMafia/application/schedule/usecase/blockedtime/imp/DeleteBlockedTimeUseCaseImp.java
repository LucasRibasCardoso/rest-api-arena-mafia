package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.scheduleTask.event.OnBlockedTimeDeletedScheduleTaskEvent;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteBlockedTimeUseCaseImp implements DeleteBlockedTimeUseCase {

  private final ApplicationEventPublisher eventPublisher;
  private final BlockedTimeRepositoryPort blockedTimeRepositoryPort;

  public DeleteBlockedTimeUseCaseImp(
      ApplicationEventPublisher eventPublisher,
      BlockedTimeRepositoryPort blockedTimeRepositoryPort) {
    this.eventPublisher = eventPublisher;
    this.blockedTimeRepositoryPort = blockedTimeRepositoryPort;
  }

  @Override
  public void execute(UUID blockedTimeId, Boolean deleteAllRecurring) {
    BlockedTime blockedTime = blockedTimeRepositoryPort.findByIdOrElseThrow(blockedTimeId);

    if (blockedTime.isRecurring() && deleteAllRecurring) {
      List<BlockedTime> recurringBlockedTimes =
          findAllByRecurringBlockedTimeId(blockedTime.getRecurringBlockedTimeId());
      recurringBlockedTimes.forEach(
          bt -> eventPublisher.publishEvent(new OnBlockedTimeDeletedScheduleTaskEvent(bt.getId())));
      blockedTimeRepositoryPort.deleteAllByRecurringBlockedTimeId(
          blockedTime.getRecurringBlockedTimeId());
    } else {
      eventPublisher.publishEvent(new OnBlockedTimeDeletedScheduleTaskEvent(blockedTimeId));
      blockedTimeRepositoryPort.deleteById(blockedTimeId);
    }
  }

  private List<BlockedTime> findAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    return blockedTimeRepositoryPort.findAllByRecurringBlockedTimeId(recurringBlockedTimeId);
  }
}
