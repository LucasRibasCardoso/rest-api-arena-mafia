package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

  public abstract UserEntity toEntity(User user);

  public abstract User toDomain(UserEntity entity);

  @ObjectFactory
  public User createUser(UserEntity entity) {
    return User.reconstitute(
        entity.getId(),
        entity.getUsername(),
        entity.getFullName(),
        entity.getPhone(),
        entity.getPasswordHash(),
        entity.getStatus(),
        entity.getRole(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
