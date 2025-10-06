package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.CreateCourtUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtAlreadyExistsException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;
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
  public Court execute(CreateCourtRequestDto request) {
    validateCourtAlreadyExists(request.name());
    validateModalitiesExist(request.modalityIds());
    Court court =
        Court.create(
            request.name(), request.description(), request.offsetMinutes(), request.modalityIds());

    return courtRepository.save(court);
  }

  private void validateModalitiesExist(Set<UUID> modalityIds) {
    List<Modality> foundModalities = modalityRepository.findAllByIds(modalityIds);

    if (foundModalities.size() != modalityIds.size()) {
      throw new ModalityNotFoundException();
    }
  }

  private void validateCourtAlreadyExists(String name) {
    if (courtRepository.existsByName(name)) {
      throw new CourtAlreadyExistsException();
    }
  }
}
