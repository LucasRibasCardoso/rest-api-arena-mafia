package com.projetoExtensao.arenaMafia.application.court.port;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public interface CourtRepositoryPort {

  Court save(Court court);

  List<Court> findAll(Specification<CourtEntity> spec);

  Optional<Court> findById(UUID id);

  Court findByIdOrElseThrow(UUID id);

  Optional<Court> findByName(String name);

  void delete(Court court);

  boolean existsByName(String name);
}
