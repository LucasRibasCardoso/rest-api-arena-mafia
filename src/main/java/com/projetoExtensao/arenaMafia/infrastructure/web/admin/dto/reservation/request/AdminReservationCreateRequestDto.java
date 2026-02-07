package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator.ValidConditionalSelectedDaysOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.validator.ValidDateRange;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@ValidDateRange
@ValidConditionalSelectedDaysOfWeek
public record AdminReservationCreateRequestDto(

        @NotBlank(message = "PHONE_REQUIRED")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "PHONE_INVALID_FORMAT")
        String userPhone,

        @NotNull(message = "RESERVATION_COURT_ID_REQUIRED")
        UUID courtId,

        @NotNull(message = "RESERVATION_MODALITY_ID_REQUIRED")
        UUID modalityId,

        @NotNull(message = "RESERVATION_START_DATE_REQUIRED")
        LocalDate startDate,

        @NotNull(message = "RESERVATION_END_DATE_REQUIRED")
        LocalDate endDate,

        @NotNull(message = "RESERVATION_TIME_INTERVAL_REQUIRED")
        @Valid
        TimeInterval timeInterval,

        Set<DayOfWeek> selectedDaysOfWeek
) {}
