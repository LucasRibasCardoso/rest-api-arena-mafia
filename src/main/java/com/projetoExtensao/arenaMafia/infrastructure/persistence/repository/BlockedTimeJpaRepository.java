package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BlockedTimeJpaRepository
    extends JpaRepository<BlockedTimeEntity, UUID>, JpaSpecificationExecutor<BlockedTimeEntity> {

  List<BlockedTimeEntity> findAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId);

  void deleteAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId);
}
