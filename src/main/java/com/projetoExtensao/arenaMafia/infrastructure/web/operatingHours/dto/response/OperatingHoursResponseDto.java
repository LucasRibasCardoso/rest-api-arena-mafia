package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;
import java.util.UUID;

public record OperatingHoursResponseDto(
    UUID id,
    String dayOfWeek,
    @JsonFormat(pattern = "HH:mm") LocalTime openTime,
    @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
    boolean isActive) {}
