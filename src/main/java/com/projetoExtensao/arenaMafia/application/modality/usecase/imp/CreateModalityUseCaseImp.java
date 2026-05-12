package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.CreateModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ModalityAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateModalityUseCaseImp implements CreateModalityUseCase {

  private final ModalityRepositoryPort modalityRepositoryPort;

  public CreateModalityUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public Modality execute(String name) {
    validateIfModalityAlreadyExists(name);

    Modality modality = Modality.create(name);
    return modalityRepositoryPort.save(modality);
  }

  private void validateIfModalityAlreadyExists(String name) {
    if (modalityRepositoryPort.existsByName(name)) {
      throw new ModalityAlreadyExistsException();
    }
  }
}
