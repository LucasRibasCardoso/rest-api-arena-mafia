package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.UpdateCourtUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.UpdateCourtRequestDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCourtUseCaseImp implements UpdateCourtUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ModalityRepositoryPort modalityRepositoryPort;

  public UpdateCourtUseCaseImp(
      CourtRepositoryPort courtRepositoryPort, ModalityRepositoryPort modalityRepositoryPort) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public CourtWithModalities execute(UUID courtId, UpdateCourtRequestDto request) {
    Court court = courtRepositoryPort.findByIdOrElseThrow(courtId);

    updateCourtFields(court, request);
    Court updatedCourt = courtRepositoryPort.save(court);

    List<Modality> modalities = modalityRepositoryPort.findAllByIds(updatedCourt.getModalityIds());
    return new CourtWithModalities(updatedCourt, modalities);
  }

  private void updateCourtFields(Court court, UpdateCourtRequestDto request) {
    if (request.name() != null) {
      validateIfCourtAlreadyExists(request.name(), court.getId());
      court.updateName(request.name());
    }

    court.updateDescription(request.description());
    court.updateOffsetMinutes(request.offsetMinutes());

    if (request.modalityIds() != null && !request.modalityIds().isEmpty()) {
      validateModalitiesExist(request.modalityIds());
      court.replaceModalityIds(request.modalityIds());
    }
  }

  private void validateIfCourtAlreadyExists(String name, UUID currentCourtId) {
    courtRepositoryPort
        .findByName(name)
        .ifPresent(
            existingCourt -> {
              if (!existingCourt.getId().equals(currentCourtId)) {
                throw new CourtAlreadyExistsException();
              }
            });
  }

  private void validateModalitiesExist(Set<UUID> modalityIds) {
    List<Modality> foundModalities = modalityRepositoryPort.findAllByIds(modalityIds);

    if (foundModalities.size() != modalityIds.size()) {
      throw new ModalityNotFoundException();
    }
  }
}
