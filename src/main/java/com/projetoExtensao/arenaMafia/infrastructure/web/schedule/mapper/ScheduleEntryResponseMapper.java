package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.UnsupportedScheduleEntryTypeException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ScheduleDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal.ScheduleEntryResponseDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ScheduleEntryResponseMapper {

  private final Map<Class<?>, ScheduleEntryMapperStrategy<?, ?>> entryStrategies;
  private final Map<Class<?>, ScheduleEntryMapperStrategy<?, ?>> detailStrategies;

  public ScheduleEntryResponseMapper(List<ScheduleEntryMapperStrategy<?, ?>> mappers) {
    this.entryStrategies = new HashMap<>();
    this.detailStrategies = new HashMap<>();

    for (ScheduleEntryMapperStrategy<?, ?> mapper : mappers) {
      entryStrategies.put(mapper.getSupportedType(), mapper);
      detailStrategies.put(mapper.getSupportedDetailType(), mapper);
    }
  }

  public ScheduleEntryResponseDto toDto(ScheduleEntry scheduleEntry) {
    return getEntryStrategy(scheduleEntry).toDto(scheduleEntry);
  }

  public ScheduleDetailResponseDto toDetailDto(ScheduleDetail detail) {
    return getDetailStrategy(detail).toDetailDto(detail);
  }

  @SuppressWarnings("unchecked")
  private <T extends ScheduleEntry> ScheduleEntryMapperStrategy<T, ?> getEntryStrategy(T scheduleEntry) {
    ScheduleEntryMapperStrategy<T, ?> strategy =
        (ScheduleEntryMapperStrategy<T, ?>) entryStrategies.get(scheduleEntry.getClass());

    if (strategy == null) {
      throw new UnsupportedScheduleEntryTypeException();
    }

    return strategy;
  }

  @SuppressWarnings("unchecked")
  private <D extends ScheduleDetail> ScheduleEntryMapperStrategy<?, D> getDetailStrategy(D detail) {
    ScheduleEntryMapperStrategy<?, D> strategy =
            (ScheduleEntryMapperStrategy<?, D>) detailStrategies.get(detail.getClass());

    if (strategy == null) {
      throw new UnsupportedScheduleEntryTypeException();
    }

    return strategy;
  }
}
