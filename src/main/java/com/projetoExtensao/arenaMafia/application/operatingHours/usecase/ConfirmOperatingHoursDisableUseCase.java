package com.projetoExtensao.arenaMafia.application.operatingHours.usecase;

import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.OperatingHoursDisableConfirmRequestDto;

import java.util.UUID;

public interface ConfirmOperatingHoursDisableUseCase {
  void execute(UUID adminId, OperatingHoursDisableConfirmRequestDto requestDto);
}
