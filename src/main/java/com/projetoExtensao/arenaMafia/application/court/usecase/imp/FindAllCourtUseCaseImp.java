package com.projetoExtensao.arenaMafia.application.court.usecase.imp;

import com.projetoExtensao.arenaMafia.domain.dto.CourtWithModalitiesResult;
import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.usecase.FindAllCourtUseCase;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.CourtEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.CourtSpecification;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllCourtUseCaseImp implements FindAllCourtUseCase {

  private final CourtRepositoryPort courtRepositoryPort;
  private final ModalityRepositoryPort modalityRepositoryPort;

  public FindAllCourtUseCaseImp(
      CourtRepositoryPort courtRepositoryPort, ModalityRepositoryPort modalityRepositoryPort) {
    this.courtRepositoryPort = courtRepositoryPort;
    this.modalityRepositoryPort = modalityRepositoryPort;
  }

  @Override
  public List<CourtWithModalitiesResult> execute(Boolean isActive) {
    Specification<CourtEntity> spec = CourtSpecification.byActiveStatus(isActive);
    List<Court> courts = courtRepositoryPort.findAll(spec);

    if (courts.isEmpty()) {
      return List.of();
    }
    Map<UUID, Modality> modalityMap = loadAllModalitiesAsMap(courts);

    return enrichCourtsWithModalities(courts, modalityMap);
  }

  private Map<UUID, Modality> loadAllModalitiesAsMap(List<Court> courts) {
    Set<UUID> allModalityIds = collectAllModalityIds(courts);
    List<Modality> allModalities = modalityRepositoryPort.findAllByIds(allModalityIds);
    return createModalityMap(allModalities);
  }

  private Set<UUID> collectAllModalityIds(List<Court> courts) {
    return courts.stream()
        .flatMap(court -> court.getModalityIds().stream())
        .collect(Collectors.toSet());
  }

  private Map<UUID, Modality> createModalityMap(List<Modality> modalities) {
    return modalities.stream().collect(Collectors.toMap(Modality::getId, modality -> modality));
  }

  private List<CourtWithModalitiesResult> enrichCourtsWithModalities(
      List<Court> courts, Map<UUID, Modality> modalityMap) {

    return courts.stream()
        .map(court -> enrichSingleCourt(court, modalityMap))
        .collect(Collectors.toList());
  }

  private CourtWithModalitiesResult enrichSingleCourt(
      Court court, Map<UUID, Modality> modalityMap) {

    List<Modality> courtModalities = getModalitiesForCourt(court, modalityMap);
    return new CourtWithModalitiesResult(court, courtModalities);
  }

  private List<Modality> getModalitiesForCourt(Court court, Map<UUID, Modality> modalityMap) {
    return court.getModalityIds().stream()
        .map(modalityMap::get)
        .sorted(Comparator.comparing(Modality::getName, String.CASE_INSENSITIVE_ORDER))
        .collect(Collectors.toList());
  }
}
