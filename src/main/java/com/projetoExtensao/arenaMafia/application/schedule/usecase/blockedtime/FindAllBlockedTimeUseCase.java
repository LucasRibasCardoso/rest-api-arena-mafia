package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FindAllBlockedTimeUseCase {
  Page<BlockedTimeDetail> execute(UUID courtId, Pageable pageable);
}
