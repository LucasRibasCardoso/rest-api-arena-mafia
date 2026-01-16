package com.projetoExtensao.arenaMafia.application.schedule.result;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da confirmação de criação de BlockedTime(s).
 *
 * <p>Este record encapsula todas as informações sobre o processo de criação de bloqueios, incluindo
 * estatísticas de bloqueios criados e reservas/bloqueios cancelados.
 *
 * @param blockedTimesCreated IDs dos BlockedTimes criados com sucesso
 * @param totalBlockedTimesCreated Total de bloqueios criados
 * @param reservationsCancelled Total de reservas canceladas
 * @param blockedTimesCancelled Total de bloqueios anteriores cancelados
 * @param usersAffected Total de usuários afetados pelos cancelamentos
 */
public record ConfirmBlockedTimeResult(
    List<UUID> blockedTimesCreated,
    int totalBlockedTimesCreated,
    int reservationsCancelled,
    int blockedTimesCancelled,
    int usersAffected) {}
