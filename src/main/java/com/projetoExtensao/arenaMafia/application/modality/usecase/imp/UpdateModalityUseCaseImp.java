package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.UpdateModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ModalityAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateModalityUseCaseImp implements UpdateModalityUseCase {

  private final ModalityRepositoryPort modalityRepositoryPort;

  public UpdateModalityUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public Modality execute(UUID id, String newName) {
    Modality modality = modalityRepositoryPort.findByIdOrElseThrow(id);

    if (!modality.getName().equalsIgnoreCase(newName)) {
      validateIfModalityNameAlreadyExists(newName);
      modality.updateName(newName);
    }
    return modalityRepositoryPort.save(modality);
  }

  private void validateIfModalityNameAlreadyExists(String name) {
    if (modalityRepositoryPort.existsByName(name)) {
      throw new ModalityAlreadyExistsException();
    }
  }
}
