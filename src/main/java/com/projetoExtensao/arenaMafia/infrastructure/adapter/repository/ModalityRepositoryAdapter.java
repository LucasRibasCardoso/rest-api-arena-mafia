package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ModalityJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ModalityRepositoryAdapter implements ModalityRepositoryPort {

  private final ModalityJpaRepository modalityJpaRepository;
  private final ModalityMapper modalityMapper;

  public ModalityRepositoryAdapter(
      ModalityJpaRepository modalityJpaRepository, ModalityMapper modalityMapper) {
    this.modalityJpaRepository = modalityJpaRepository;
    this.modalityMapper = modalityMapper;
  }

  @Override
  public boolean existsByName(String name) {
    return modalityJpaRepository.existsByNameAndIsActiveTrue(name);
  }

  @Override
  public boolean existsCourtsByModalityId(UUID modalityId) {
    return modalityJpaRepository.existsCourtsByModalityId(modalityId);
  }

  @Override
  public Modality save(Modality modality) {
    ModalityEntity entity = modalityMapper.toEntity(modality);
    ModalityEntity savedEntity = modalityJpaRepository.save(entity);
    return modalityMapper.toDomain(savedEntity);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Modality> findById(UUID id) {
    return modalityJpaRepository.findByIdAndIsActiveTrue(id).map(modalityMapper::toDomain);
  }

  @Override
  public Modality findByIdOrElseThrow(UUID id) {
    return modalityJpaRepository
        .findByIdAndIsActiveTrue(id)
        .map(modalityMapper::toDomain)
        .orElseThrow(ModalityNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Modality> findByName(String name) {
    return modalityJpaRepository.findByNameAndIsActiveTrue(name).map(modalityMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Modality> findAll() {
    return modalityJpaRepository.findAllByIsActiveTrue().stream()
        .map(modalityMapper::toDomain)
        .toList();
  }

  @Override
  public List<Modality> findAllByIds(Set<UUID> ids) {
    return modalityJpaRepository.findAllByIdInAndIsActiveTrue(ids).stream()
        .map(modalityMapper::toDomain)
        .toList();
  }
}
