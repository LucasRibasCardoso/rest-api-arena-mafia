package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request;

import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminUserSearchRequestDto(
    @Size(max = 100, message = "TERM_TOO_LONG") String term,
    LocalDate createdAtStart,
    LocalDate createdAtEnd,
    AccountStatus status,
    RoleEnum role) {}
