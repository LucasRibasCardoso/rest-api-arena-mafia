package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.CourtMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.CourtJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ModalityJpaRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class CourtRepositoryAdapter implements CourtRepositoryPort {

  private final CourtJpaRepository courtJpaRepository;
  private final ModalityJpaRepository modalityJpaRepository;
  private final CourtMapper courtMapper;

  public CourtRepositoryAdapter(
      CourtJpaRepository courtJpaRepository,
      ModalityJpaRepository modalityJpaRepository,
      CourtMapper courtMapper) {
    this.courtJpaRepository = courtJpaRepository;
    this.modalityJpaRepository = modalityJpaRepository;
    this.courtMapper = courtMapper;
  }

  @Override
  public Court save(Court court) {
    CourtEntity entity = courtMapper.toEntity(court);

    Set<ModalityEntity> managedModalities =
        court.getModalityIds().stream()
            .map(modalityJpaRepository::getReferenceById)
            .collect(Collectors.toSet());

    entity.setModalities(managedModalities);

    CourtEntity savedEntity = courtJpaRepository.save(entity);
    return courtMapper.toDomain(savedEntity);
  }

  public List<Court> findAll(Specification<CourtEntity> spec) {
    return courtJpaRepository.findAll(spec).stream()
        .map(courtMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Court> findActiveCourtsByModalityId(UUID modalityId) {
    return courtJpaRepository.findActiveCourtsByModalityId(modalityId).stream()
        .map(courtMapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<Court> findById(UUID id) {
    return courtJpaRepository.findById(id).map(courtMapper::toDomain);
  }

  @Override
  public Court findByIdOrElseThrow(UUID id) {
    return courtJpaRepository
        .findById(id)
        .map(courtMapper::toDomain)
        .orElseThrow(CourtNotFoundException::new);
  }

  @Override
  public Optional<Court> findByName(String name) {
    return courtJpaRepository.findByName(name).map(courtMapper::toDomain);
  }

  @Override
  public List<Court> findAllByIds(Set<UUID> ids) {
    return courtJpaRepository.findAllById(ids).stream()
        .filter(CourtEntity::isActive)
        .map(courtMapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsByName(String name) {
    return courtJpaRepository.existsByName(name);
  }

  @Override
  public void validateAllExistAndActive(List<UUID> courtIds) {
    List<Court> activeCourts = findAllByIds(new HashSet<>(courtIds));

    if (activeCourts.size() != courtIds.size()) {
      throw new CourtNotFoundException();
    }
  }
}
