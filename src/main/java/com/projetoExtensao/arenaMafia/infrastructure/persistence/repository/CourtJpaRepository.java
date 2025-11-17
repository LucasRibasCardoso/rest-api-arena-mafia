package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourtJpaRepository
    extends JpaRepository<CourtEntity, UUID>, JpaSpecificationExecutor<CourtEntity> {

  boolean existsByName(String name);

  Optional<CourtEntity> findByName(String name);
}
