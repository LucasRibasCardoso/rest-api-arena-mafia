package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.BlockedTimeEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.BlockedTimeMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.BlockedTimeJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class BlockedTimeRepositoryAdapter implements BlockedTimeRepositoryPort {

  private final BlockedTimeJpaRepository blockedTimeJpaRepository;
  private final BlockedTimeMapper blockedTimeMapper;

  public BlockedTimeRepositoryAdapter(
      BlockedTimeJpaRepository blockedTimeJpaRepository, BlockedTimeMapper blockedTimeMapper) {
    this.blockedTimeJpaRepository = blockedTimeJpaRepository;
    this.blockedTimeMapper = blockedTimeMapper;
  }

  @Override
  public BlockedTime save(BlockedTime blockedTime) {
    BlockedTimeEntity entity = blockedTimeMapper.toEntity(blockedTime);
    BlockedTimeEntity savedEntity = blockedTimeJpaRepository.save(entity);
    return blockedTimeMapper.toDomain(savedEntity);
  }

  @Override
  public List<BlockedTime> saveAll(List<BlockedTime> blockedTimes) {
    List<BlockedTimeEntity> entities =
        blockedTimes.stream().map(blockedTimeMapper::toEntity).toList();

    List<BlockedTimeEntity> savedEntities = blockedTimeJpaRepository.saveAll(entities);

    return savedEntities.stream().map(blockedTimeMapper::toDomain).toList();
  }

  @Override
  public Optional<BlockedTime> findById(UUID id) {
    return blockedTimeJpaRepository.findById(id).map(blockedTimeMapper::toDomain);
  }

  @Override
  public BlockedTime findByIdOrElseThrow(UUID id) {
    return findById(id).orElseThrow(BlockedTimeNotFoundException::new);
  }

  @Override
  public List<BlockedTime> findAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    return blockedTimeJpaRepository.findAllByRecurringBlockedTimeId(recurringBlockedTimeId).stream()
        .map(blockedTimeMapper::toDomain)
        .toList();
  }

  @Override
  public void deleteAllByIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return;
    }
    blockedTimeJpaRepository.deleteAllById(ids);
  }

  @Override
  public Page<BlockedTime> search(Specification<BlockedTimeEntity> spec, Pageable pageable) {
    return blockedTimeJpaRepository.findAll(spec, pageable).map(blockedTimeMapper::toDomain);
  }

  @Override
  public void deleteAllByRecurringBlockedTimeId(UUID recurringBlockedTimeId) {
    blockedTimeJpaRepository.deleteAllByRecurringBlockedTimeId(recurringBlockedTimeId);
  }

  @Override
  public void deleteById(UUID id) {
    blockedTimeJpaRepository.deleteById(id);
  }
}
