package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.DefaultPriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.PriceRuleMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.PriceRuleJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class PriceRuleRepositoryAdapter implements PriceRuleRepositoryPort {

  private final PriceRuleMapper priceRuleMapper;
  private final PriceRuleJpaRepository priceRuleJpaRepository;

  public PriceRuleRepositoryAdapter(
      PriceRuleMapper priceRuleMapper, PriceRuleJpaRepository priceRuleJpaRepository) {
    this.priceRuleMapper = priceRuleMapper;
    this.priceRuleJpaRepository = priceRuleJpaRepository;
  }

  @Override
  public PriceRule save(PriceRule priceRule) {
    PriceRuleEntity savedEntity = priceRuleJpaRepository.save(priceRuleMapper.toEntity(priceRule));
    return priceRuleMapper.toDomain(savedEntity);
  }

  @Override
  public PriceRule findByIdOrElseThrow(UUID id) {
    return priceRuleJpaRepository
        .findById(id)
        .map(priceRuleMapper::toDomain)
        .orElseThrow(PriceRuleNotFoundException::new);
  }

  @Override
  public List<PriceRule> findAll(Specification<PriceRuleEntity> spec) {
    return priceRuleJpaRepository.findAll(spec).stream().map(priceRuleMapper::toDomain).toList();
  }

  @Override
  public Optional<PriceRule> findDefaultRule() {
    return priceRuleJpaRepository.findByIsDefaultTrue().map(priceRuleMapper::toDomain);
  }

  @Override
  public PriceRule findDefaultRuleOrElseThrow() {
    return findDefaultRule().orElseThrow(DefaultPriceRuleNotFoundException::new);
  }

  @Override
  public Optional<PriceRule> findByName(String name) {
    return priceRuleJpaRepository.findByNameIgnoreCase(name).map(priceRuleMapper::toDomain);
  }
}
