package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.response;

import java.time.Instant;

public record AdminUserResponseDto(
    String userId,
    String username,
    String fullName,
    String phone,
    String status,
    String role,
    Instant createdAt) {}
