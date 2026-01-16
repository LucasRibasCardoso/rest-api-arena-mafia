package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourtJpaRepository
    extends JpaRepository<CourtEntity, UUID>, JpaSpecificationExecutor<CourtEntity> {

  boolean existsByName(String name);

  Optional<CourtEntity> findByName(String name);

  @Query(
      """
          SELECT c FROM CourtEntity c
          JOIN c.modalities m
          WHERE m.id = :modalityId
          AND c.isActive = true
          """)
  List<CourtEntity> findActiveCourtsByModalityId(@Param("modalityId") UUID modalityId);
}
