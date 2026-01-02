package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindAllOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.specification.OperatingHoursSpecification;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindAllOperatingHoursUseCaseImp implements FindAllOperatingHoursUseCase {

  private final OperatingHoursRepositoryPort operatingHoursRepository;

  public FindAllOperatingHoursUseCaseImp(OperatingHoursRepositoryPort operatingHoursRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
  }

  @Override
  public List<OperatingHours> execute(Boolean isActive) {
    var specification = OperatingHoursSpecification.byActiveStatus(isActive);
    return operatingHoursRepository.findAll(specification);
  }
}
