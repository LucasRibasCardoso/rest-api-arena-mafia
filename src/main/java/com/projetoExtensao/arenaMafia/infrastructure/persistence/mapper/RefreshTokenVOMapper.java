package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import org.mapstruct.Mapper;

/**
 * Mapper auxiliar para conversão entre RefreshTokenVO e String.
 *
 * <p>Usado por outros mappers através da propriedade 'uses' do MapStruct.
 */
@Mapper(componentModel = "spring")
public interface RefreshTokenVOMapper {

  default String toString(RefreshTokenVO vo) {
    return vo != null ? vo.toString() : null;
  }

  default RefreshTokenVO toRefreshTokenVO(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    return RefreshTokenVO.fromString(token);
  }
}
