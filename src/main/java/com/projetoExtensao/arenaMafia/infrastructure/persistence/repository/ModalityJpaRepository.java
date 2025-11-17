package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModalityJpaRepository
    extends JpaRepository<ModalityEntity, UUID>, JpaSpecificationExecutor<ModalityEntity> {

  Optional<ModalityEntity> findByName(String name);

  boolean existsByName(String name);

  @Query(
      "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END "
          + "FROM CourtEntity c JOIN c.modalities m WHERE m.id = :modalityId")
  boolean existsCourtsByModalityId(@Param("modalityId") UUID modalityId);
}
