package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.FindByIdModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindByIdModalityUseCaseImp implements FindByIdModalityUseCase {

  private final ModalityRepositoryPort modalityRepositoryPort;

  public FindByIdModalityUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public Modality execute(UUID modalityId) {
    return modalityRepositoryPort.findByIdOrElseThrow(modalityId);
  }
}
