package com.projetoExtensao.arenaMafia.unit.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.AdminUserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.UserAdminResponseDto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

@DisplayName("Testes Unitários para AdminUserMapper")
class AdminUserMapperTest {

  private AdminUserMapper adminUserMapper;

  @BeforeEach
  void setup() {
    adminUserMapper = Mappers.getMapper(AdminUserMapper.class);
  }

  @Nested
  @DisplayName("Testes para o método toDto()")
  class ToDtoTests {

    @Test
    @DisplayName("Deve converter User para UserAdminResponseDto com sucesso")
    void toDto_shouldConvertUserToUserAdminResponseDto_successfully() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.userId()).isEqualTo(userId.toString());
      assertThat(dto.username()).isEqualTo("joao_silva");
      assertThat(dto.fullName()).isEqualTo("João da Silva");
      assertThat(dto.phone()).isEqualTo("+5511999999999");
      assertThat(dto.status()).isEqualTo(AccountStatus.ACTIVE.getValue());
      assertThat(dto.role()).isEqualTo(RoleEnum.ROLE_USER.getValue());
    }

    @Test
    @DisplayName("Deve retornar null quando User for null")
    void toDto_shouldReturnNull_whenUserIsNull() {
      // Arrange
      User user = null;

      // Act
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNull();
    }

    @Test
    @DisplayName("Deve converter User com status PENDING_VERIFICATION para UserAdminResponseDto")
    void toDto_shouldConvertUserWithPendingVerificationStatus() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION.getValue());
    }

    @Test
    @DisplayName("Deve converter User com role ADMIN para UserAdminResponseDto")
    void toDto_shouldConvertUserWithAdminRole() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.role()).isEqualTo(RoleEnum.ROLE_ADMIN.getValue());
    }

    @Test
    @DisplayName("Deve converter User com status LOCKED para UserAdminResponseDto")
    void toDto_shouldConvertUserWithLockedStatus() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo(AccountStatus.LOCKED.getValue());
    }

    @Test
    @DisplayName("Deve converter User com status DISABLED para UserAdminResponseDto")
    void toDto_shouldConvertUserWithDisabledStatus() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo(AccountStatus.DISABLED.getValue());
    }

    @Test
    @DisplayName("Deve converter User com role MODERATOR para UserAdminResponseDto")
    void toDto_shouldConvertUserWithModeratorRole() {
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
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.role()).isEqualTo(RoleEnum.ROLE_MODERATOR.getValue());
    }

    @Test
    @DisplayName("Deve mapear corretamente todos os campos do User para UserAdminResponseDto")
    void toDto_shouldMapAllFieldsCorrectly() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant createdAt = Instant.now().minusSeconds(86400); // 1 dia atrás
      Instant updatedAt = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "complete_user",
              "Complete User Name",
              "+5511922222222",
              "hashedPassword444",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER,
              createdAt,
              updatedAt);

      // Act
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.userId()).isEqualTo(userId.toString());
      assertThat(dto.username()).isEqualTo("complete_user");
      assertThat(dto.fullName()).isEqualTo("Complete User Name");
      assertThat(dto.phone()).isEqualTo("+5511922222222");
      assertThat(dto.status()).isEqualTo(AccountStatus.ACTIVE.getValue());
      assertThat(dto.role()).isEqualTo(RoleEnum.ROLE_USER.getValue());
    }
  }

  @Nested
  @DisplayName("Testes para o método instantToLocalDate()")
  class InstantToLocalDateTests {

    @Test
    @DisplayName("Deve converter Instant para LocalDate com sucesso")
    void instantToLocalDate_shouldConvertInstantToLocalDate_successfully() {
      // Arrange
      Instant instant = Instant.parse("2025-10-15T10:30:00Z");
      LocalDate expectedDate = instant.atZone(ZoneId.of("America/Sao_Paulo")).toLocalDate();

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNotNull();
      assertThat(localDate).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("Deve retornar null quando Instant for null")
    void instantToLocalDate_shouldReturnNull_whenInstantIsNull() {
      // Arrange
      Instant instant = null;

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNull();
    }

    @Test
    @DisplayName("Deve converter Instant para LocalDate usando timezone America/Sao_Paulo")
    void instantToLocalDate_shouldConvertUsingCorrectTimezone() {
      // Arrange
      Instant instant = Instant.parse("2025-01-01T02:59:59Z");
      // Na timezone America/Sao_Paulo (-03:00), isso seria 31/12/2024 às 23:59:59
      LocalDate expectedDate = LocalDate.of(2024, 12, 31);

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNotNull();
      assertThat(localDate).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("Deve converter Instant de meia-noite para LocalDate corretamente")
    void instantToLocalDate_shouldConvertMidnightInstantCorrectly() {
      // Arrange
      Instant instant = Instant.parse("2025-10-02T03:00:00Z"); // Meia-noite em São Paulo
      LocalDate expectedDate = LocalDate.of(2025, 10, 2);

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNotNull();
      assertThat(localDate).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("Deve converter Instant do início do dia para LocalDate")
    void instantToLocalDate_shouldConvertStartOfDayInstant() {
      // Arrange
      Instant instant = Instant.parse("2025-06-15T03:00:00Z");
      LocalDate expectedDate = LocalDate.of(2025, 6, 15);

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNotNull();
      assertThat(localDate).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("Deve converter Instant do final do dia para LocalDate")
    void instantToLocalDate_shouldConvertEndOfDayInstant() {
      // Arrange
      Instant instant = Instant.parse("2025-06-16T02:59:59Z");
      LocalDate expectedDate = LocalDate.of(2025, 6, 15);

      // Act
      LocalDate localDate = adminUserMapper.instantToLocalDate(instant);

      // Assert
      assertThat(localDate).isNotNull();
      assertThat(localDate).isEqualTo(expectedDate);
    }
  }

  @Nested
  @DisplayName("Testes de conversão com diferentes cenários de data")
  class DateConversionScenariosTests {

    @Test
    @DisplayName("Deve converter User criado há 1 ano para UserAdminResponseDto")
    void toDto_shouldConvertUserCreatedOneYearAgo() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant oneYearAgo = Instant.now().minusSeconds(31536000); // 365 dias
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "old_user",
              "Old User",
              "+5511911111111",
              "hashedPassword555",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER,
              oneYearAgo,
              now);

      // Act
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.userId()).isEqualTo(userId.toString());
    }

    @Test
    @DisplayName("Deve converter User recém-criado para UserAdminResponseDto")
    void toDto_shouldConvertNewlyCreatedUser() {
      // Arrange
      UUID userId = UUID.randomUUID();
      Instant now = Instant.now();
      User user =
          User.reconstitute(
              userId,
              "new_user",
              "New User",
              "+5511900000000",
              "hashedPassword666",
              AccountStatus.PENDING_VERIFICATION,
              RoleEnum.ROLE_USER,
              now,
              now);

      // Act
      UserAdminResponseDto dto = adminUserMapper.toDto(user);

      // Assert
      assertThat(dto).isNotNull();
      assertThat(dto.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION.getValue());
    }
  }
}
