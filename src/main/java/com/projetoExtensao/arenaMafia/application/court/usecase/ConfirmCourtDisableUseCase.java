package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CourtDisableConfirmRequestDto;

import java.util.UUID;

public interface ConfirmCourtDisableUseCase {

  void execute(UUID adminId, CourtDisableConfirmRequestDto requestDto);
}
