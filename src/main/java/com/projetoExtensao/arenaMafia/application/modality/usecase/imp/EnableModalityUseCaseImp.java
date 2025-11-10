package com.projetoExtensao.arenaMafia.application.modality.usecase.imp;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.usecase.EnableModalityUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Transactional
public class EnableModalityUseCaseImp implements EnableModalityUseCase {

  private final ModalityRepositoryPort modalityRepositoryPort;

  public EnableModalityUseCaseImp(ModalityRepositoryPort modalityRepositoryPort) {
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public void execute(UUID hourId) {
    Modality modality = modalityRepositoryPort.findByIdOrElseThrow(hourId);
    modality.enable();
    modalityRepositoryPort.save(modality);
  }
}
