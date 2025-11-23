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
public abstract class PriceRuleMapper {

  public abstract PriceRuleEntity toEntity(PriceRule priceRule);

  @Mapping(target = "daysOfWeek", ignore = true)
  @Mapping(target = "timeInterval", ignore = true)
  public abstract PriceRule toDomain(PriceRuleEntity entity);

  @Mapping(target = "daysOfWeek", expression = "java(getDaysOfWeek(domain.getDaysOfWeek()))")
  @Mapping(
      target = "timeInterval",
      expression = "java(toTimeIntervalDto(domain.getTimeInterval()))")
  @Mapping(target = "isActive", source = "active")
  @Mapping(target = "isDefault", source = "default")
  public abstract PriceRuleResponseDto toDto(PriceRule domain);

  @ObjectFactory
  public PriceRule createPriceRule(PriceRuleEntity entity) {
    return PriceRule.reconstitute(
        entity.getId(),
        entity.getName(),
        getDaysOfWeek(entity.getDaysOfWeek()),
        entity.getTimeInterval(),
        entity.getPrice(),
        entity.getPriority(),
        entity.isDefault(),
        entity.isActive(),
        entity.getCreatedAt());
  }

  public TimeIntervalDto toTimeIntervalDto(TimeInterval timeInterval) {
    if (timeInterval == null) {
      return null;
    }
    return new TimeIntervalDto(timeInterval.startTime(), timeInterval.endTime());
  }

  public Set<DayOfWeek> getDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
    return (daysOfWeek == null || daysOfWeek.isEmpty()) ? null : daysOfWeek;
  }
}
