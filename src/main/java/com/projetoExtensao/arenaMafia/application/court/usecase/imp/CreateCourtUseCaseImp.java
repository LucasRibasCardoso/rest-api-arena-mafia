package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.aggregate.CourtWithModalities;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.CreateCourtUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CreateCourtRequestDto;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCourtUseCaseImp implements CreateCourtUseCase {

  private final CourtRepositoryPort courtRepository;
  private final ModalityRepositoryPort modalityRepository;

  public CreateCourtUseCaseImp(
      CourtRepositoryPort courtRepository, ModalityRepositoryPort modalityRepository) {
    this.courtRepository = courtRepository;
    this.modalityRepository = modalityRepository;
  }

  @Override
  public CourtWithModalities execute(CreateCourtRequestDto request) {
    validateCourtAlreadyExists(request.name());
    List<Modality> modalities = validateAndRetrieveModalities(request.modalityIds());

    Court court =
        Court.create(
            request.name(), request.description(), request.offsetMinutes(), request.modalityIds());

    Court savedCourt = courtRepository.save(court);
    return new CourtWithModalities(savedCourt, modalities);
  }

  private List<Modality> validateAndRetrieveModalities(Set<UUID> modalityIds) {
    List<Modality> foundModalities = modalityRepository.findAllByIds(modalityIds);

    if (foundModalities.size() != modalityIds.size()) {
      throw new ModalityNotFoundException();
    }

    return foundModalities;
  }

  private void validateCourtAlreadyExists(String name) {
    if (courtRepository.existsByName(name)) {
      throw new CourtAlreadyExistsException();
    }
  }
}
