package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourtJpaRepository extends JpaRepository<CourtEntity, UUID> {
  
  boolean existsByName(String name);
}
