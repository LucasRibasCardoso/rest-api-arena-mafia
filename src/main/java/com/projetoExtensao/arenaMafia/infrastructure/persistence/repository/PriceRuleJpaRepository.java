package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PriceRuleJpaRepository
    extends JpaRepository<PriceRuleEntity, UUID>, JpaSpecificationExecutor<PriceRuleEntity> {

  Optional<PriceRuleEntity> findByIsDefaultTrue();

  Optional<PriceRuleEntity> findByNameIgnoreCase(String name);
}
