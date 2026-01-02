package com.projetoExtensao.arenaMafia.application.schedule.result;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record BatchCancellationResult(
    int totalReservations, int successCount, int failureCount, List<UUID> failedReservationIds) {

  /**
   * Cria um resultado vazio para quando não há reservas a processar.
   *
   * @return BatchCancellationResult com todos os contadores zerados
   */
  public static BatchCancellationResult empty() {
    return new BatchCancellationResult(0, 0, 0, Collections.emptyList());
  }

  /**
   * Verifica se todos os cancelamentos foram bem-sucedidos.
   *
   * @return true se não houve falhas, false caso contrário
   */
  public boolean isFullySuccessful() {
    return failureCount == 0;
  }

  /**
   * Verifica se houve alguma falha durante o processamento.
   *
   * @return true se houve pelo menos uma falha, false caso contrário
   */
  public boolean hasFailures() {
    return failureCount > 0;
  }

  /**
   * Calcula a taxa de sucesso do cancelamento em lote.
   *
   * @return Percentual de sucesso (0.0 a 100.0)
   */
  public double getSuccessRate() {
    if (totalReservations == 0) return 0.0;
    return ((double) successCount / totalReservations) * 100.0;
  }
}
