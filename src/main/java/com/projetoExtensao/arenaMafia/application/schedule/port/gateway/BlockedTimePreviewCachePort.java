package com.projetoExtensao.arenaMafia.application.schedule.port.gateway;

import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import java.util.UUID;

public interface BlockedTimePreviewCachePort {

  /**
   * Salva o preview de conflitos no cache.
   *
   * @param key chave única do preview
   * @param preview dados do preview
   */
  void save(String key, BlockedTimeConflictsPreview preview);

  /**
   * Busca um preview no cache.
   *
   * @param userId id do usuário
   * @param key chave do preview
   * @return preview se encontrado
   * @throws com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException
   *     se o preview não for encontrado
   */
  BlockedTimeConflictsPreview getPreviewOrElseThrow(String key, UUID userId);

  /**
   * Remove um preview do cache.
   *
   * @param key chave do preview
   */
  void delete(String key);

  /**
   * Gera uma chave única para o preview.
   *
   * @param userId ID do usuário (para segurança)
   * @return chave única
   */
  String generateKey(UUID userId);
}
