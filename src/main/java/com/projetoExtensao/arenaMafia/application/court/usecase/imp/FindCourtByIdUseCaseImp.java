package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.FindCourtByIdUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindCourtByIdUseCaseImp implements FindCourtByIdUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ModalityRepositoryPort modalityRepositoryPort;

  public FindCourtByIdUseCaseImp(
      CourtRepositoryPort courtRepositoryPort, ModalityRepositoryPort modalityRepositoryPort) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public CourtWithModalities execute(UUID courtId) {
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);
    List<Modality> modalities = modalityRepositoryPort.findAllByIds(court.getModalityIds());
    return new CourtWithModalities(court, modalities);
  }
}
