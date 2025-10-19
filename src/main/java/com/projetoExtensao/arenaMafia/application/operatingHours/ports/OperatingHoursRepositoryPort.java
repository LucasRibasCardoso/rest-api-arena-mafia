package com.projetoExtensao.arenaMafia.application.operatingHours.ports;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public interface OperatingHoursRepositoryPort {

  OperatingHours save(OperatingHours operatingHours);

  List<OperatingHours> findByDayOfWeek(DayOfWeek dayOfWeek);

  OperatingHours findByIdOrElseThrow(UUID id);

  List<OperatingHours> findAll(Specification<OperatingHoursEntity> spec);
}
