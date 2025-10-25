package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface OperatingHoursMapper {

  OperatingHoursEntity toEntity(OperatingHours operatingHours);

  OperatingHours toDomain(OperatingHoursEntity entity);

  @Mapping(target = "isActive", expression = "java(operatingHours.isActive())")
  @Mapping(
      target = "timeInterval",
      expression = "java(toTimeIntervalDto(operatingHours.getTimeInterval()))")
  @Mapping(
      target = "dayOfWeek",
      expression = "java(extractDayOfWeekName(operatingHours.getDayOfWeek()))")
  OperatingHoursResponseDto toDto(OperatingHours operatingHours);

  @ObjectFactory
  default OperatingHours createOperatingHours(OperatingHoursEntity entity) {
    return OperatingHours.reconstitute(
        entity.getId(),
        entity.getDayOfWeek(),
        entity.getTimeInterval(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  default String extractDayOfWeekName(DayOfWeek dayOfWeek) {
    return dayOfWeek != null ? dayOfWeek.getDayName() : null;
  }

  default TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.openTime(), timeInterval.closeTime());
  }
}
