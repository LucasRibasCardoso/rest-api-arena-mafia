package com.projetoExtensao.arenaMafia.application.schedule.port.gateway;

import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;

import java.util.Optional;
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
   * @param key chave do preview
   * @return preview se encontrado
   * @throws com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException se o preview não for encontrado
   */
  BlockedTimeConflictsPreview getPreviewOrElseThrow(String key);

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

  /**
   * Valida se a chave pertence ao usuário.
   * Lança exceção se a chave for inválida ou não pertencer ao usuário.
   *
   * @param key chave do preview
   * @param userId ID do usuário
   * @throws com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPreviewKeyException se a chave for inválida
   * @throws com.projetoExtensao.arenaMafia.domain.exception.forbidden.ForbiddenException se a chave não pertencer ao usuário
   */
  void validateKeyOwnership(String key, UUID userId);
}
