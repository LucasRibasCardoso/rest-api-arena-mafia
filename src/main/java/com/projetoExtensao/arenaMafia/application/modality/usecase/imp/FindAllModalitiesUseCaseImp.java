package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.FindAllModalitiesUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;

import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.ModalitySpecification;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.PriceRuleSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllModalitiesUseCaseImp implements FindAllModalitiesUseCase {

  private final ModalityRepositoryPort modalityRepositoryPort;

  public FindAllModalitiesUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public List<Modality> execute(Boolean isActive) {
    var specification = ModalitySpecification.byActiveStatus(isActive);
    return modalityRepositoryPort.findAll(specification);
  }
}
