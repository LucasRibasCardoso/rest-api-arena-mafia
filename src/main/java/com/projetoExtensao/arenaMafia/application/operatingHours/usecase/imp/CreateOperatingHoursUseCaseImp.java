package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.CreateOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateOperatingHoursRequestDto;
import jakarta.transaction.Transactional;
import java.util.List;
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
    var dayOfWeek = request.dayOfWeek();
    var timeInterval = request.timeInterval();

    List<OperatingHours> existingHours = operatingHoursRepository.findByDayOfWeek(dayOfWeek);

    OperatingHours newOperatingHours = OperatingHours.create(dayOfWeek, timeInterval);
    validateNoOverlapWithExistingHours(existingHours, newOperatingHours);

    return operatingHoursRepository.save(newOperatingHours);
  }

  private void validateNoOverlapWithExistingHours(
      List<OperatingHours> existingHours, OperatingHours newOperatingHours) {

    for (OperatingHours existing : existingHours) {
      newOperatingHours.validateNoOverlapWithSameDay(existing);
    }
  }
}
