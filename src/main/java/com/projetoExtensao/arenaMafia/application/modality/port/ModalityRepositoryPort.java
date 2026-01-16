package com.projetoExtensao.arenaMafia.application.modality.port;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public interface ModalityRepositoryPort {
  boolean existsByName(String name);

  boolean existsCourtsByModalityId(UUID modalityId);

  Optional<Modality> findById(UUID id);

  Modality findByIdOrElseThrow(UUID id);

  Optional<Modality> findByName(String name);

  List<Modality> findAll(Specification<ModalityEntity> specification);

  List<Modality> findAllByIds(Set<UUID> ids);

  Modality save(Modality modality);
}
