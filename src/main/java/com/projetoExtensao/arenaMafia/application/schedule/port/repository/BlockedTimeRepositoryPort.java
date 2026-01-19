package com.projetoExtensao.arenaMafia.application.schedule.port.repository;

import com.projetoExtensao.arenaMafia.application.schedule.detail.BlockedTimeDetail;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockedTimeRepositoryPort {

  BlockedTime save(BlockedTime blockedTime);

  List<BlockedTime> saveAll(List<BlockedTime> blockedTimes);

  Optional<BlockedTime> findById(UUID id);

  Page<BlockedTime> search(Specification<BlockedTimeEntity> spec, Pageable pageable);

  BlockedTime findByIdOrElseThrow(UUID id);

  void deleteAllByIds(List<UUID> ids);
}
