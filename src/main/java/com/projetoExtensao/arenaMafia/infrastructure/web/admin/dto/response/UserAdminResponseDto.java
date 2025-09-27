package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response;

import java.time.Instant;

public record UserAdminResponseDto(
    String userId,
    String username,
    String fullName,
    String phone,
    String status,
    String role,
    Instant createdAt) {}
