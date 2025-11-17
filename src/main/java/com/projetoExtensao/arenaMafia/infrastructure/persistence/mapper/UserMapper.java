package com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ObjectFactory;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserEntity toEntity(User user);

  User toDomain(UserEntity entity);

  @ObjectFactory
  default User createUser(UserEntity entity) {
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
