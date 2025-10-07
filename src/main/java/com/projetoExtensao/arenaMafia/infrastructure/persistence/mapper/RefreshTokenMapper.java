package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, RefreshTokenVOMapper.class})
public abstract class RefreshTokenMapper {

  @Autowired protected UserMapper userMapper;

  public abstract RefreshTokenEntity toEntity(RefreshToken domain);

  public RefreshToken toDomain(RefreshTokenEntity entity) {
    if (entity == null) {
      return null;
    }

    var userDomain = userMapper.toDomain(entity.getUser());
    var tokenVO = RefreshTokenVO.fromString(entity.getToken());

    return RefreshToken.reconstitute(
        entity.getId(), tokenVO, entity.getExpiryDate(), userDomain, entity.getCreatedAt());
  }
}
