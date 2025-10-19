package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.OperatingHoursEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OperatingHoursMapper {

  OperatingHoursEntity toEntity(OperatingHours operatingHours);

  default OperatingHours toDomain(OperatingHoursEntity entity) {
    if (entity == null) {
      return null;
    }
    return OperatingHours.reconstitute(
        entity.getId(),
        entity.getDayOfWeek(),
        entity.getTimeInterval(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  default OperatingHoursResponseDto toResponseDto(OperatingHours operatingHours) {
    if (operatingHours == null) {
      return null;
    }
    return new OperatingHoursResponseDto(
        operatingHours.getId(),
        operatingHours.getDayOfWeek().getDayName(),
        operatingHours.getTimeInterval().openTime(),
        operatingHours.getTimeInterval().closeTime(),
        operatingHours.isActive());
  }
}
