package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedTimeRepositoryPort {

  BlockedTime save(BlockedTime blockedTime);

  List<BlockedTime> saveAll(List<BlockedTime> blockedTimes);

  Optional<BlockedTime> findById(UUID id);

  BlockedTime findByIdOrElseThrow(UUID id);

  List<BlockedTime> findAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId);

  List<BlockedTime> findByCourtIdAndDateRange(UUID courtId, LocalDate startDate, LocalDate endDate);

  List<BlockedTime> findByCourtIdsAndDateRange(
      List<UUID> courtIds, LocalDate startDate, LocalDate endDate);

  void deleteAllByIds(List<UUID> ids);

  void deleteByRecurringBlockedTimeId(UUID recurringBlockedTimeId);

  boolean existsActiveBlockedTimeByCourtIdAndDate(UUID courtId, LocalDate date);
}
