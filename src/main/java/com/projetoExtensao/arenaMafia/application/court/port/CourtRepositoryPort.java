package com.projetoExtensao.arenaMafia.application.court.port;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import java.util.Optional;
import java.util.UUID;

public interface CourtRepositoryPort {

  Court save(Court court);

  Optional<Court> findById(UUID id);

  Court findByIdOrElseThrow(UUID id);

  void delete(Court court);

  boolean existsByName(String name);
}
