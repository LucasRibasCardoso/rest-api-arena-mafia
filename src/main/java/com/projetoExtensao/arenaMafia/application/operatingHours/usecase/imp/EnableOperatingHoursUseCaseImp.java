package com.projetoExtensao.arenaMafia.application.operatingHours.usecase.imp;

import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.EnableOperatingHoursUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnableOperatingHoursUseCaseImp implements EnableOperatingHoursUseCase {

  private final OperatingHoursRepositoryPort operatingHoursRepository;

  public EnableOperatingHoursUseCaseImp(OperatingHoursRepositoryPort operatingHoursRepository) {
    this.operatingHoursRepository = operatingHoursRepository;
  }

  @Override
  public void execute(UUID hourId) {
    var operatingHours = operatingHoursRepository.findByIdOrElseThrow(hourId);
    operatingHours.enable();
    operatingHoursRepository.save(operatingHours);
  }
}
