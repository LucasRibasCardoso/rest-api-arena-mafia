package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.mapper;

import com.projetoExtensao.arenaMafia.application.schedule.detail.ScheduleDetail;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ScheduleDetailResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleNormal.ScheduleEntryResponseDto;

/**
 * Interface de estratégia para mapear diferentes tipos de ScheduleEntry para seus respectivos DTOs
 * de resposta. Implementa o Strategy Pattern para permitir extensibilidade ao adicionar novos tipos
 * de agendamento.
 *
 * @param <T> o tipo específico de ScheduleEntry que esta estratégia sabe mapear
 * @param <D> o tipo específico de ScheduleDetail que esta estratégia sabe mapear
 */
public interface ScheduleEntryMapperStrategy<T extends ScheduleEntry, D extends ScheduleDetail> {

  /**
   * Converte uma entidade de domínio ScheduleEntry para seu DTO de resposta correspondente.
   *
   * @param scheduleEntry a entidade de domínio a ser convertida
   * @return o DTO de resposta correspondente
   */
  ScheduleEntryResponseDto toDto(T scheduleEntry);

  /**
   * Converte um ScheduleDetail da camada de application para seu DTO de resposta correspondente.
   *
   * @param detail o detalhe enriquecido a ser convertido
   * @return o DTO de resposta correspondente
   */
  ScheduleDetailResponseDto toDetailDto(D detail);

  /**
   * Retorna o tipo de ScheduleEntry que esta estratégia suporta. Utilizado para o auto-registro das
   * estratégias no contexto do Spring.
   *
   * @return a classe do tipo suportado
   */
  Class<T> getSupportedType();

  /**
   * Retorna o tipo de ScheduleDetail que esta estratégia suporta.
   *
   * @return a classe do tipo de detalhe suportado
   */
  Class<D> getSupportedDetailType();
}
