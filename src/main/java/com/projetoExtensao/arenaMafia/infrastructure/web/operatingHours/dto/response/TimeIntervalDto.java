package com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record TimeIntervalDto(
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime) {}
