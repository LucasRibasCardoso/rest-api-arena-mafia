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
      validateIfModalityAlreadyExists(newName, modality.getId());
      modality.updateName(newName);
    }
    return modalityRepositoryPort.save(modality);
  }

  private void validateIfModalityAlreadyExists(String name, UUID currentModalityId) {
    modalityRepositoryPort
        .findByName(name)
        .ifPresent(
            existingModality -> {
              if (!existingModality.getId().equals(currentModalityId)) {
                throw new ModalityAlreadyExistsException();
              }
            });
  }
}
