package com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response;

import java.util.List;
import java.util.UUID;

public record BlockedTimeConfirmResponseDto(
    List<UUID> blockedTimesCreatedSuccessfully,
    int totalBlockedTimesCreated,
    int reservationsCancelled,
    int blockedTimesCancelled,
    int usersAffected) {}
