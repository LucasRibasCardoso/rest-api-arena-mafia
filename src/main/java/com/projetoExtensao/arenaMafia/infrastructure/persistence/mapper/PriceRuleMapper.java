package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.PriceRuleEntity;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface PriceRuleMapper {

  @Mapping(target = "isActive", source = "active")
  @Mapping(target = "isDefault", source = "default")
  PriceRuleEntity toEntity(PriceRule priceRule);

  PriceRule toDomain(PriceRuleEntity entity);

  @Mapping(
      target = "timeInterval",
      expression = "java(toTimeIntervalDto(domain.getTimeInterval()))")
  @Mapping(target = "daysOfWeek", expression = "java(normalizeDaysOfWeek(domain.getDaysOfWeek()))")
  @Mapping(target = "isActive", source = "active")
  @Mapping(target = "isDefault", source = "default")
  PriceRuleResponseDto toDto(PriceRule domain);

  @ObjectFactory
  default PriceRule createPriceRule(PriceRuleEntity entity) {
    return PriceRule.reconstitute(
        entity.getId(),
        entity.getName(),
        normalizeDaysOfWeek(entity.getDaysOfWeek()),
        entity.getTimeInterval(),
        entity.getPrice(),
        entity.getPriority(),
        entity.isDefault(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  default TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.openTime(), timeInterval.closeTime());
  }

  default Set<DayOfWeek> normalizeDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    return (daysOfWeek == null || daysOfWeek.isEmpty()) ? null : daysOfWeek;
  }
}
