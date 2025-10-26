package com.projetoExtensao.arenaMafia.infrastructure.persistence.repository;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperatingHoursJpaRepository
    extends JpaRepository<OperatingHoursEntity, UUID>,
        JpaSpecificationExecutor<OperatingHoursEntity> {

  @Query(
      "SELECT DISTINCT oh FROM OperatingHoursEntity oh "
          + "JOIN oh.daysOfWeek d "
          + "WHERE d IN :daysOfWeek AND oh.isActive = true")
  List<OperatingHoursEntity> findByDaysOfWeekIn(@Param("daysOfWeek") Set<DayOfWeek> daysOfWeek);
}
