package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface OperatingHoursMapper {

  OperatingHoursEntity toEntity(OperatingHours operatingHours);

  @Mapping(target = "daysOfWeek", ignore = true)
  OperatingHours toDomain(OperatingHoursEntity entity);

  @Mapping(target = "isActive", expression = "java(operatingHours.isActive())")
  @Mapping(
      target = "timeInterval",
      expression = "java(toTimeIntervalDto(operatingHours.getTimeInterval()))")
  @Mapping(
      target = "daysOfWeek",
      expression = "java(extractDayOfWeekNames(operatingHours.getDaysOfWeek()))")
  OperatingHoursResponseDto toDto(OperatingHours operatingHours);

  @ObjectFactory
  default OperatingHours createOperatingHours(OperatingHoursEntity entity) {
    return OperatingHours.reconstitute(
        entity.getId(),
        entity.getDaysOfWeek(),
        entity.getTimeInterval(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  default Set<String> extractDayOfWeekNames(Set<DayOfWeek> daysOfWeek) {
    if (daysOfWeek == null) {
      return null;
    }
    return daysOfWeek.stream().map(DayOfWeek::getDayName).collect(Collectors.toSet());
  }

  default TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.openTime(), timeInterval.closeTime());
  }
}
