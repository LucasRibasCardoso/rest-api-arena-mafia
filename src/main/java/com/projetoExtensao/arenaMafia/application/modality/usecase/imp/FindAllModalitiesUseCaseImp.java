package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.FindAllModalitiesUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;
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
  public List<Modality> execute() {
    return modalityRepositoryPort.findAll();
  }
}
