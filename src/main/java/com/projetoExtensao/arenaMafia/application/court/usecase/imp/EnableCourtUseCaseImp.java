package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.EnableCourtUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Court;

@Service
@Transactional
public class EnableCourtUseCaseImp implements EnableCourtUseCase{

    private final CourtRepositoryPort courtRepositoryPort;

    public EnableCourtUseCaseImp (CourtRepositoryPort courtRepositoryPort) {
        this.courtRepositoryPort = courtRepositoryPort;
    }

    @Override
    public void execute(UUID courtId) {
        Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);
        court.enable();
        courtRepositoryPort.save(court);
    }
}
