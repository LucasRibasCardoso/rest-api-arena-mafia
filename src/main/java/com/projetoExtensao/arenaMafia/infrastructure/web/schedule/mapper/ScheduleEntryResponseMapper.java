package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.UnsupportedScheduleEntryTypeException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ScheduleEntryResponseMapper {

  private final Map<Class<? extends ScheduleEntry>, ScheduleEntryMapperStrategy<?>> strategies;

  public ScheduleEntryResponseMapper(List<ScheduleEntryMapperStrategy<?>> mappers) {
    this.strategies =
        mappers.stream()
            .collect(
                Collectors.toMap(
                    ScheduleEntryMapperStrategy::getSupportedType, Function.identity()));
  }

  public ScheduleEntryResponseDto toDto(ScheduleEntry scheduleEntry) {
    return getStrategy(scheduleEntry).toDto(scheduleEntry);
  }

  @SuppressWarnings("unchecked")
  private <T extends ScheduleEntry> ScheduleEntryMapperStrategy<T> getStrategy(T scheduleEntry) {
    ScheduleEntryMapperStrategy<T> strategy =
        (ScheduleEntryMapperStrategy<T>) strategies.get(scheduleEntry.getClass());

    if (strategy == null) {
      throw new UnsupportedScheduleEntryTypeException();
    }

    return strategy;
  }
}
