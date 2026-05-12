package com.projetoExtensao.arenaMafia.unit.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("Testes Unitários para UserMapper")
class UserMapperTest {

  private UserMapper userMapper;

  @BeforeEach
  void setup() {
    userMapper = Mappers.getMapper(UserMapper.class);
  }

  @Nested
  @DisplayName("Testes para o método toEntity()")
  class ToEntityTests {

    @Test
    @DisplayName("Deve converter User para UserEntity com sucesso")
    void toEntity_shouldConvertUserToUserEntity_successfully() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "joao_silva",
              "João da Silva",
              "+5511999999999",
              "hashedPassword123",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(userId);
      assertThat(entity.getUsername()).isEqualTo("joao_silva");
      assertThat(entity.getFullName()).isEqualTo("João da Silva");
      assertThat(entity.getPhone()).isEqualTo("+5511999999999");
      assertThat(entity.getPasswordHash()).isEqualTo("hashedPassword123");
      assertThat(entity.getStatus()).isEqualTo(AccountStatus.ACTIVE);
      assertThat(entity.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(entity.getCreatedAt()).isEqualTo(now);
      assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando User for null")
    void toEntity_shouldReturnNull_whenUserIsNull() {
      // Arrange
      User user = null;

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNull();
    }

    @Test
    @DisplayName("Deve converter User com status PENDING_VERIFICATION para UserEntity")
    void toEntity_shouldConvertUserWithPendingVerificationStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "maria_santos",
              "Maria Santos",
              "+5511988888888",
              "hashedPassword456",
              AccountStatus.PENDING_VERIFICATION,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("Deve converter User com role ADMIN para UserEntity")
    void toEntity_shouldConvertUserWithAdminRole() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "admin_user",
              "Admin User",
              "+5511977777777",
              "hashedPassword789",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_ADMIN,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Deve converter User com status LOCKED para UserEntity")
    void toEntity_shouldConvertUserWithLockedStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "locked_user",
              "Locked User",
              "+5511966666666",
              "hashedPassword000",
              AccountStatus.LOCKED,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    @DisplayName("Deve converter User com status DISABLED para UserEntity")
    void toEntity_shouldConvertUserWithDisabledStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "disabled_user",
              "Disabled User",
              "+5511955555555",
              "hashedPassword111",
              AccountStatus.DISABLED,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getStatus()).isEqualTo(AccountStatus.DISABLED);
    }

    @Test
    @DisplayName("Deve converter User com role MODERATOR para UserEntity")
    void toEntity_shouldConvertUserWithModeratorRole() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "moderator_user",
              "Moderator User",
              "+5511944444444",
              "hashedPassword222",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_MODERATOR,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(user);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getRole()).isEqualTo(RoleEnum.ROLE_MODERATOR);
    }
  }

  @Nested
  @DisplayName("Testes para o método toDomain()")
  class ToDomainTests {

    @Test
    @DisplayName("Deve converter UserEntity para User com sucesso")
    void toDomain_shouldConvertUserEntityToUser_successfully() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("joao_silva");
      entity.setFullName("João da Silva");
      entity.setPhone("+5511999999999");
      entity.setPasswordHash("hashedPassword123");
      entity.setStatus(AccountStatus.ACTIVE);
      entity.setRole(RoleEnum.ROLE_USER);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getId()).isEqualTo(userId);
      assertThat(user.getUsername()).isEqualTo("joao_silva");
      assertThat(user.getFullName()).isEqualTo("João da Silva");
      assertThat(user.getPhone()).isEqualTo("+5511999999999");
      assertThat(user.getPasswordHash()).isEqualTo("hashedPassword123");
      assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      assertThat(user.getCreatedAt()).isEqualTo(now);
      assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve retornar null quando UserEntity for null")
    void toDomain_shouldReturnNull_whenUserEntityIsNull() {
      // Arrange
      UserEntity entity = null;

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNull();
    }

    @Test
    @DisplayName("Deve converter UserEntity com status PENDING_VERIFICATION para User")
    void toDomain_shouldConvertUserEntityWithPendingVerificationStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("maria_santos");
      entity.setFullName("Maria Santos");
      entity.setPhone("+5511988888888");
      entity.setPasswordHash("hashedPassword456");
      entity.setStatus(AccountStatus.PENDING_VERIFICATION);
      entity.setRole(RoleEnum.ROLE_USER);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("Deve converter UserEntity com role ADMIN para User")
    void toDomain_shouldConvertUserEntityWithAdminRole() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("admin_user");
      entity.setFullName("Admin User");
      entity.setPhone("+5511977777777");
      entity.setPasswordHash("hashedPassword789");
      entity.setStatus(AccountStatus.ACTIVE);
      entity.setRole(RoleEnum.ROLE_ADMIN);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Deve converter UserEntity com status LOCKED para User")
    void toDomain_shouldConvertUserEntityWithLockedStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("locked_user");
      entity.setFullName("Locked User");
      entity.setPhone("+5511966666666");
      entity.setPasswordHash("hashedPassword000");
      entity.setStatus(AccountStatus.LOCKED);
      entity.setRole(RoleEnum.ROLE_USER);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    }

    @Test
    @DisplayName("Deve converter UserEntity com status DISABLED para User")
    void toDomain_shouldConvertUserEntityWithDisabledStatus() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("disabled_user");
      entity.setFullName("Disabled User");
      entity.setPhone("+5511955555555");
      entity.setPasswordHash("hashedPassword111");
      entity.setStatus(AccountStatus.DISABLED);
      entity.setRole(RoleEnum.ROLE_USER);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
    }

    @Test
    @DisplayName("Deve converter UserEntity com role MODERATOR para User")
    void toDomain_shouldConvertUserEntityWithModeratorRole() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity entity = new UserEntity();
      entity.setId(userId);
      entity.setUsername("moderator_user");
      entity.setFullName("Moderator User");
      entity.setPhone("+5511944444444");
      entity.setPasswordHash("hashedPassword222");
      entity.setStatus(AccountStatus.ACTIVE);
      entity.setRole(RoleEnum.ROLE_MODERATOR);
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(entity);

      // Assert
      assertThat(user).isNotNull();
      assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_MODERATOR);
    }
  }

  @Nested
  @DisplayName("Testes de conversão bidirecional")
  class BidirectionalConversionTests {

    @Test
    @DisplayName("Deve manter os mesmos dados após conversão bidirecional User -> Entity -> User")
    void shouldMaintainDataAfterBidirectionalConversion_userToEntityToUser() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User originalUser =
          User.reconstitute(
              userId,
              "test_user",
              "Test User",
              "+5511933333333",
              "hashedPassword999",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserEntity entity = userMapper.toEntity(originalUser);
      User convertedUser = userMapper.toDomain(entity);

      // Assert
      assertThat(convertedUser).isNotNull();
      assertThat(convertedUser.getId()).isEqualTo(originalUser.getId());
      assertThat(convertedUser.getUsername()).isEqualTo(originalUser.getUsername());
      assertThat(convertedUser.getFullName()).isEqualTo(originalUser.getFullName());
      assertThat(convertedUser.getPhone()).isEqualTo(originalUser.getPhone());
      assertThat(convertedUser.getPasswordHash()).isEqualTo(originalUser.getPasswordHash());
      assertThat(convertedUser.getStatus()).isEqualTo(originalUser.getStatus());
      assertThat(convertedUser.getRole()).isEqualTo(originalUser.getRole());
      assertThat(convertedUser.getCreatedAt()).isEqualTo(originalUser.getCreatedAt());
      assertThat(convertedUser.getUpdatedAt()).isEqualTo(originalUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve manter os mesmos dados após conversão bidirecional Entity -> User -> Entity")
    void shouldMaintainDataAfterBidirectionalConversion_entityToUserToEntity() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      UserEntity originalEntity = new UserEntity();
      originalEntity.setId(userId);
      originalEntity.setUsername("test_user");
      originalEntity.setFullName("Test User");
      originalEntity.setPhone("+5511933333333");
      originalEntity.setPasswordHash("hashedPassword999");
      originalEntity.setStatus(AccountStatus.ACTIVE);
      originalEntity.setRole(RoleEnum.ROLE_USER);
      originalEntity.setCreatedAt(now);
      originalEntity.setUpdatedAt(now);

      // Act
      User user = userMapper.toDomain(originalEntity);
      UserEntity convertedEntity = userMapper.toEntity(user);

      // Assert
      assertThat(convertedEntity).isNotNull();
      assertThat(convertedEntity.getId()).isEqualTo(originalEntity.getId());
      assertThat(convertedEntity.getUsername()).isEqualTo(originalEntity.getUsername());
      assertThat(convertedEntity.getFullName()).isEqualTo(originalEntity.getFullName());
      assertThat(convertedEntity.getPhone()).isEqualTo(originalEntity.getPhone());
      assertThat(convertedEntity.getPasswordHash()).isEqualTo(originalEntity.getPasswordHash());
      assertThat(convertedEntity.getStatus()).isEqualTo(originalEntity.getStatus());
      assertThat(convertedEntity.getRole()).isEqualTo(originalEntity.getRole());
      assertThat(convertedEntity.getCreatedAt()).isEqualTo(originalEntity.getCreatedAt());
      assertThat(convertedEntity.getUpdatedAt()).isEqualTo(originalEntity.getUpdatedAt());
    }
  }
}
