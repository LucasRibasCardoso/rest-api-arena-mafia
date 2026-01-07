package com.projetoExtensao.arenaMafia.application.court.port;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public interface CourtRepositoryPort {

  Court save(Court court);

  List<Court> findAll(Specification<CourtEntity> spec);

  List<Court> findAllByIds(Set<UUID> ids);

  Optional<Court> findById(UUID id);

  List<Court> findActiveCourtsByModalityId(UUID modalityId);

  Court findByIdOrElseThrow(UUID id);

  Optional<Court> findByName(String name);

  boolean existsByName(String name);

  void validateAllExistAndActive(List<UUID> courtIds);
}
