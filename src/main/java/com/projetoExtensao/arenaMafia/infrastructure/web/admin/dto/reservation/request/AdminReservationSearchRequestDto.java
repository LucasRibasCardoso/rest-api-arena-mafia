package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AdminReservationSearchRequestDto(
    @Size(max = 100, message = "TERM_TOO_LONG") String searchTerm,
    UUID userId,
    LocalDate startDate,
    LocalDate endDate,
    ReservationStatus status) {}
