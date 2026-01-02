package com.projetoExtensao.arenaMafia.application.schedule.result;

import java.util.List;
import java.util.UUID;

/**
 * Resultado da confirmação de criação de BlockedTime(s).
 *
 * <p>Este record encapsula todas as informações sobre o processo de criação de bloqueios,
 * incluindo estatísticas de bloqueios criados e reservas/bloqueios cancelados.
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
    int usersAffected) {

  /**
   * Verifica se a operação teve impacto (cancelou reservas ou bloqueios).
   *
   * @return true se houve cancelamentos, false caso contrário
   */
  public boolean hadConflicts() {
    return reservationsCancelled > 0 || blockedTimesCancelled > 0;
  }

  /**
   * Verifica se múltiplos bloqueios foram criados (operação em lote).
   *
   * @return true se criou mais de 1 bloqueio, false caso contrário
   */
  public boolean isMultipleBlockedTimes() {
    return totalBlockedTimesCreated > 1;
  }

  /**
   * Calcula o total de itens afetados (reservas + bloqueios cancelados).
   *
   * @return Total de itens cancelados
   */
  public int getTotalCancellations() {
    return reservationsCancelled + blockedTimesCancelled;
  }
}

