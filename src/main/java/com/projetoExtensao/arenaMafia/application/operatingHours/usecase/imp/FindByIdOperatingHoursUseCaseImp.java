package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindByIdOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FindByIdOperatingHoursUseCaseImp implements FindByIdOperatingHoursUseCase {

  private final OperatingHoursRepositoryPort operatingHoursRepository;

  public FindByIdOperatingHoursUseCaseImp(OperatingHoursRepositoryPort operatingHoursRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
  }

  @Override
  public OperatingHours execute(UUID hourId) {
    return operatingHoursRepository.findByIdOrElseThrow(hourId);
  }
}
