package com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail;

import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.TimeIntervalDto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Interface base para DTOs de resposta de detalhes de agendamentos. Utilizada para representar
 * informações enriquecidas de reservas e bloqueios, com dados adicionais como nome da quadra, nome
 * do usuário, etc.
 */
public sealed interface ScheduleDetailResponseDto
    permits ReservationDetailResponseDto, BlockedTimeDetailResponseDto {

  UUID courtId();

  String courtName();

  LocalDate date();

  TimeIntervalDto timeInterval();
}
