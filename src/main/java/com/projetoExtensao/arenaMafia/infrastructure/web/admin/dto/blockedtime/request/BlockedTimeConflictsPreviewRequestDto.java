package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator.ValidConditionalTimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator.ValidDateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de request para preview de conflitos de bloqueios de horário.
 *
 * <p><b>Regras:</b>
 * <ul>
 *   <li>Para bloqueio único: startDate = endDate</li>
 *   <li>Para bloqueio recorrente: endDate >= startDate</li>
 *   <li>startDate deve ser hoje ou uma data futura (não permite datas passadas)</li>
 *   <li>Se isFullDay = true: timeInterval pode ser null (será calculado baseado nos OperatingHours)</li>
 *   <li>Se isFullDay = false: timeInterval é obrigatório</li>
 *   <li>selectedDaysOfWeek é opcional: se null/vazio, bloqueia todos os dias no intervalo; se preenchido, bloqueia apenas os dias da semana selecionados</li>
 * </ul>
 *
 * <p><b>Casos de uso:</b>
 * <ul>
 *   <li>Bloqueio pontual: startDate = endDate, selectedDaysOfWeek = null</li>
 *   <li>Bloqueio consecutivo: startDate != endDate, selectedDaysOfWeek = null (ex: férias, reforma)</li>
 *   <li>Bloqueio recorrente semanal: startDate != endDate, selectedDaysOfWeek = [TUESDAY, THURSDAY] (ex: aulas regulares)</li>
 * </ul>
 */
@ValidDateRange
@ValidConditionalTimeInterval
public record BlockedTimeConflictsPreviewRequestDto(
    @NotNull(message = "BLOCKED_TIME_COURT_IDS_REQUIRED")
    @Size(min = 1, max = 20, message = "BLOCKED_TIME_COURT_IDS_SIZE_INVALID")
    List<UUID> courtIds,

    @NotNull(message = "BLOCKED_TIME_START_DATE_REQUIRED")
    LocalDate startDate,

    @NotNull(message = "BLOCKED_TIME_END_DATE_REQUIRED")
    LocalDate endDate,

    @Valid
    TimeInterval timeInterval,

    @NotNull(message = "BLOCKED_TIME_IS_FULL_DAY_REQUIRED")
    Boolean isFullDay,

    Set<DayOfWeek> selectedDaysOfWeek) {}


