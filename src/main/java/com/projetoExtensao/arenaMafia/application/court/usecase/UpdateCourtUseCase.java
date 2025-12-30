package com.projetoExtensao.arenaMafia.application.court.usecase;

import com.projetoExtensao.arenaMafia.domain.dto.CourtWithModalitiesResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.UpdateCourtRequestDto;
import java.util.UUID;

/**
 * Caso de uso para atualizar uma quadra existente.
 *
 * <p>Permite atualização parcial: apenas os campos fornecidos no request serão atualizados. Os
 * campos não fornecidos (null) manterão seus valores atuais.
 */
public interface UpdateCourtUseCase {
  /**
   * Atualiza uma quadra existente com os dados fornecidos.
   *
   * @param courtId ID da quadra a ser atualizada
   * @param request DTO contendo os campos a serem atualizados (campos null não serão alterados)
   * @return quadra atualizada enriquecida com suas modalidades
   * @throws com.projetoExtensao.arenaMafia.domain.exception.notFound.CourtNotFoundException se a
   *     quadra não for encontrada
   * @throws com.projetoExtensao.arenaMafia.domain.exception.notFound.ModalityNotFoundException se
   *     alguma modalidade especificada não existir
   * @throws com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtAlreadyExistsException se
   *     o novo nome já estiver em uso por outra quadra
   */
  CourtWithModalitiesResult execute(UUID courtId, UpdateCourtRequestDto request);
}
