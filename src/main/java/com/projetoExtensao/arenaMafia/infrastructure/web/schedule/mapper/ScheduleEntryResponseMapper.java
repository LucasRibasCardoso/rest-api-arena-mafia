package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.domain.exception.badRequest.UnsupportedScheduleEntryTypeException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper principal que coordena a conversão de diferentes tipos de ScheduleEntry para seus
 * respectivos DTOs de resposta. Utiliza o Strategy Pattern para delegar a conversão aos mappers
 * específicos.
 *
 * <p>As estratégias são auto-registradas via injeção de dependência do Spring, eliminando a
 * necessidade de registro manual ao adicionar novos tipos de ScheduleEntry.
 */
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

  /**
   * Converte uma entidade de domínio ScheduleEntry para seu DTO de resposta correspondente.
   *
   * @param scheduleEntry a entidade de domínio a ser convertida
   * @return o DTO de resposta correspondente
   * @throws IllegalArgumentException se não houver mapper registrado para o tipo
   */
  public ScheduleEntryResponseDto toResponseDto(ScheduleEntry scheduleEntry) {
    return getStrategy(scheduleEntry).toDto(scheduleEntry);
  }

  /**
   * Obtém a estratégia de mapeamento apropriada para o tipo de ScheduleEntry.
   *
   * @param scheduleEntry a entidade de domínio
   * @return a estratégia de mapeamento correspondente
   * @throws UnsupportedScheduleEntryTypeException se não houver mapper registrado para o tipo
   */
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
