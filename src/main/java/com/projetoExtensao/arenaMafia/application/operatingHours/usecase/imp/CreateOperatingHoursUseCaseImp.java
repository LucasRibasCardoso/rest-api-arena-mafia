package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.CreateOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.CreateOperatingHoursRequestDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CreateOperatingHoursUseCaseImp implements CreateOperatingHoursUseCase {

  private final OperatingHoursRepositoryPort operatingHoursRepository;

  public CreateOperatingHoursUseCaseImp(OperatingHoursRepositoryPort operatingHoursRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
  }

  @Override
  public OperatingHours execute(CreateOperatingHoursRequestDto request) {
    var daysOfWeek = request.daysOfWeek();
    var timeInterval = request.timeInterval();

    OperatingHours newOperatingHours = OperatingHours.create(daysOfWeek, timeInterval);

    operatingHoursRepository
        .findByDaysOfWeek(daysOfWeek)
        .forEach(newOperatingHours::validateNoOverlapWithSameDay);

    return operatingHoursRepository.save(newOperatingHours);
  }
}
