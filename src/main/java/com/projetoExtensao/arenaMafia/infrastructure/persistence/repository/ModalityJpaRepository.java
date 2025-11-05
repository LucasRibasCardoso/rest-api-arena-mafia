package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModalityJpaRepository extends JpaRepository<ModalityEntity, UUID> {

  @Query("SELECT m FROM ModalityEntity m WHERE m.id = :id AND m.isActive = true")
  Optional<ModalityEntity> findByIdAndIsActiveTrue(@Param("id") UUID id);

  @Query("SELECT m FROM ModalityEntity m WHERE m.name = :name AND m.isActive = true")
  Optional<ModalityEntity> findByNameAndIsActiveTrue(@Param("name") String name);

  @Query(
      "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM ModalityEntity m WHERE m.name ="
          + " :name AND m.isActive = true")
  boolean existsByNameAndIsActiveTrue(@Param("name") String name);

  @Query("SELECT m FROM ModalityEntity m WHERE m.isActive = true")
  List<ModalityEntity> findAllByIsActiveTrue();

  @Query("SELECT m FROM ModalityEntity m WHERE m.id IN :ids AND m.isActive = true")
  List<ModalityEntity> findAllByIdInAndIsActiveTrue(@Param("ids") Set<UUID> ids);

  @Query(
      "SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END "
          + "FROM CourtEntity c JOIN c.modalities m WHERE m.id = :modalityId")
  boolean existsCourtsByModalityId(@Param("modalityId") UUID modalityId);
}
