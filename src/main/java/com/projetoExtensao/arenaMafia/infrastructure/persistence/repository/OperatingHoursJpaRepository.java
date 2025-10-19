package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OperatingHoursJpaRepository
    extends JpaRepository<OperatingHoursEntity, UUID>,
        JpaSpecificationExecutor<OperatingHoursEntity> {

  List<OperatingHoursEntity> findByDayOfWeek(DayOfWeek dayOfWeek);
}
