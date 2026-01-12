package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.OperatingHoursNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.OperatingHoursJpaRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class OperatingHoursRepositoryAdapter implements OperatingHoursRepositoryPort {

  private final OperatingHoursJpaRepository operatingHoursJpaRepository;
  private final OperatingHoursMapper operatingHoursMapper;

  public OperatingHoursRepositoryAdapter(
      OperatingHoursJpaRepository operatingHoursJpaRepository,
      OperatingHoursMapper operatingHoursMapper) {
    this.operatingHoursJpaRepository = operatingHoursJpaRepository;
    this.operatingHoursMapper = operatingHoursMapper;
  }

  @Override
  public OperatingHours save(OperatingHours operatingHours) {
    OperatingHoursEntity entity = operatingHoursMapper.toEntity(operatingHours);
    OperatingHoursEntity savedEntity = operatingHoursJpaRepository.save(entity);
    return operatingHoursMapper.toDomain(savedEntity);
  }

  @Override
  public List<OperatingHours> findByDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    List<OperatingHoursEntity> entities =
        operatingHoursJpaRepository.findByDaysOfWeekIn(daysOfWeek);
    return entities.stream().map(operatingHoursMapper::toDomain).toList();
  }

  @Override
  public OperatingHours findByIdOrElseThrow(UUID id) {
    return operatingHoursJpaRepository
        .findById(id)
        .map(operatingHoursMapper::toDomain)
        .orElseThrow(OperatingHoursNotFoundException::new);
  }

  @Override
  public List<OperatingHours> findAll(Specification<OperatingHoursEntity> spec) {
    List<OperatingHoursEntity> entities = operatingHoursJpaRepository.findAll(spec);
    return entities.stream().map(operatingHoursMapper::toDomain).toList();
  }


}
