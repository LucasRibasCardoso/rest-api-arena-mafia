package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.DisableCourtUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableCourtUseCaseImp implements DisableCourtUseCase {

  private final CourtRepositoryPort courtRepositoryPort;

  public DisableCourtUseCaseImp(CourtRepositoryPort courtRepositoryPort) {
    this.courtRepositoryPort = courtRepositoryPort;
  }

  @Override
  public void execute(UUID courtId) {
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);
    court.disable();
    courtRepositoryPort.save(court);
  }
}
