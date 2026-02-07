package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import java.util.UUID;

public interface DeleteBlockedTimeUseCase {

  void execute(UUID blockedTimeId, Boolean deleteAllRecurring);
}
