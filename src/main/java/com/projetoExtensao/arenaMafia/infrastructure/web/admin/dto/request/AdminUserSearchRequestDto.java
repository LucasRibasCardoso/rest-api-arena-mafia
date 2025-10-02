package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AdminUserSearchRequestDto(
    @Size(max = 100, message = "TERM_TOO_LONG") String term,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtStart,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAtEnd,
    AccountStatus status,
    RoleEnum role) {}
