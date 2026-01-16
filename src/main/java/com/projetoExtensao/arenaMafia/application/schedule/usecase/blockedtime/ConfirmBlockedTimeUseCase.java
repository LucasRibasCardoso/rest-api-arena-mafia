package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.result.ConfirmBlockedTimeResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import java.util.UUID;

public interface ConfirmBlockedTimeUseCase {

  ConfirmBlockedTimeResult execute(UUID adminId, BlockedTimeConfirmRequestDto requestDto);
}
