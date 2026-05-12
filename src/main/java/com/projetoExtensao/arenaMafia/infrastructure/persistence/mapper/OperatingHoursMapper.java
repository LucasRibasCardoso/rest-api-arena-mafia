package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public abstract class OperatingHoursMapper {

  public abstract OperatingHoursEntity toEntity(OperatingHours operatingHours);

  @Mapping(target = "daysOfWeek", ignore = true)
  @Mapping(target = "timeInterval", ignore = true)
  public abstract OperatingHours toDomain(OperatingHoursEntity entity);

  @Mapping(
      target = "timeInterval",
      expression = "java(toTimeIntervalDto(operatingHours.getTimeInterval()))")
  @Mapping(target = "isActive", source = "active")
  public abstract OperatingHoursResponseDto toDto(OperatingHours operatingHours);

  @ObjectFactory
  public OperatingHours createOperatingHours(OperatingHoursEntity entity) {
    return OperatingHours.reconstitute(
        entity.getId(),
        entity.getDaysOfWeek(),
        entity.getTimeInterval(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  public TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
  }
}
