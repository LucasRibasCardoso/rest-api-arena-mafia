package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FindAllBlockedTimeUseCase {
  Page<BlockedTimeDetail> execute(UUID courtId, Pageable pageable);
}
