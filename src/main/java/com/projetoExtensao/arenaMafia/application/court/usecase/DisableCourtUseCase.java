package com.projetoExtensao.arenaMafia.application.court.usecase;

import java.util.UUID;

public interface DisableCourtUseCase {

  void execute(UUID courtId);
}
