package com.projetoExtensao.arenaMafia.application.modality.port;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModalityRepositoryPort {
  boolean existsByName(String name);

  boolean existsCourtsByModalityId(UUID modalityId);

  Optional<Modality> findById(UUID id);

  Modality findByIdOrElseThrow(UUID id);

  Optional<Modality> findByName(String name);

  List<Modality> findAll();

  Modality save(Modality modality);

  void delete(Modality modality);
}
