package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteBlockedTimeUseCaseImp implements DeleteBlockedTimeUseCase {

  private final BlockedTimeRepositoryPort blockedTimeRepositoryPort;

  public DeleteBlockedTimeUseCaseImp(BlockedTimeRepositoryPort blockedTimeRepositoryPort) {
    this.blockedTimeRepositoryPort = blockedTimeRepositoryPort;
  }

  @Override
  public void execute(UUID blockedTimeId, Boolean deleteAllRecurring) {
    BlockedTime blockedTime = blockedTimeRepositoryPort.findByIdOrElseThrow(blockedTimeId);

    if (blockedTime.isRecurring() && deleteAllRecurring) {
      blockedTimeRepositoryPort.deleteAllByRecurringBlockedTimeId(
          blockedTime.getRecurringBlockedTimeId());
    } else {
      blockedTimeRepositoryPort.deleteById(blockedTimeId);
    }
  }
}
