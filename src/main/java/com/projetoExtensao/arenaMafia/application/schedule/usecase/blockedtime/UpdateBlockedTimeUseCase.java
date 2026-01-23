package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeUpdateRequestDto;
import java.util.List;
import java.util.UUID;

public interface UpdateBlockedTimeUseCase {
  List<BlockedTimeDetail> execute(UUID blockedTimeId, BlockedTimeUpdateRequestDto requestDto);
}
