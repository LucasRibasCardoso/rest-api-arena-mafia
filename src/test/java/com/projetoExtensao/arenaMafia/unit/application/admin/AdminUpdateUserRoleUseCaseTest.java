package com.projetoExtensao.arenaMafia.unit.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.projetoExtensao.arenaMafia.application.admin.usecase.users.imp.AdminUpdateUserRoleUseCaseImp;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateOwnRoleException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateRoleOfUnverifiedUserException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para AdminUpdateUserRoleUseCase")
public class AdminUpdateUserRoleUseCaseTest {

  @Mock private UserRepositoryPort userRepositoryPort;
  @InjectMocks private AdminUpdateUserRoleUseCaseImp adminUpdateUserRoleUseCase;

  @Test
  @DisplayName("Deve atualizar a permissão do usuário de 'USER' para 'ADMIN'")
  void execute_ShouldUpdateUserRole_WhenValidInput() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    RoleEnum newRole = RoleEnum.ROLE_ADMIN;
    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withRole(RoleEnum.ROLE_USER)
            .withStatus(AccountStatus.ACTIVE)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act
    adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole);

    // Assert
    assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
  }

  @Test
  @DisplayName("Deve atualizar a permissão do usuário de 'ADMIN' para 'USER'")
  void execute_ShouldUpdateUserRoleFromAdminToUser_WhenValidInput() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    RoleEnum newRole = RoleEnum.ROLE_USER;
    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withRole(RoleEnum.ROLE_ADMIN)
            .withStatus(AccountStatus.ACTIVE)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act
    adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole);

    // Assert
    assertThat(user.getRole()).isEqualTo(RoleEnum.ROLE_USER);
  }

  @Test
  @DisplayName("Deve lançar ForbiddenException quando admin tentar atualizar sua própria permissão")
  void execute_ShouldThrowForbiddenException_WhenAdminTriesToUpdateOwnRole() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = authenticatedAdminId;
    RoleEnum newRole = RoleEnum.ROLE_ADMIN;

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole))
        .isInstanceOf(AdminCannotUpdateOwnRoleException.class)
        .satisfies(
            ex -> {
              var forbiddenException = (AdminCannotUpdateOwnRoleException) ex;
              assertThat(forbiddenException.getErrorCode())
                  .isEqualTo(ErrorCode.ADMIN_CANNOT_UPDATE_OWN_ROLE);
            });
  }

  @Test
  @DisplayName("Deve lançar UserNotFoundException quando o usuário alvo não for encontrado")
  void execute_ShouldThrowUserNotFoundException_WhenTargetUserNotFound() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    RoleEnum newRole = RoleEnum.ROLE_ADMIN;

    doThrow(new UserNotFoundException()).when(userRepositoryPort).findByIdOrElseThrow(targetUserId);

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException userNotFoundException = (UserNotFoundException) ex;
              assertThat(userNotFoundException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });
  }

  @Test
  @DisplayName(
      "Deve lançar AdminCannotUpdateRoleOfUnverifiedUserException quando tentar atualizar permissão de usuário com status PENDING_VERIFICATION")
  void execute_ShouldThrowException_WhenTargetUserIsPendingVerification() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    RoleEnum newRole = RoleEnum.ROLE_ADMIN;
    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withRole(RoleEnum.ROLE_USER)
            .withStatus(
                com.projetoExtensao
                    .arenaMafia
                    .domain
                    .model
                    .enums
                    .AccountStatus
                    .PENDING_VERIFICATION)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole))
        .isInstanceOf(AdminCannotUpdateRoleOfUnverifiedUserException.class)
        .satisfies(
            ex -> {
              var exception = (AdminCannotUpdateRoleOfUnverifiedUserException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ADMIN_CANNOT_UPDATE_ROLE_OF_UNVERIFIED_USER);
            });
  }

  @Test
  @DisplayName("Deve lançar AccountStatusForbiddenException quando a permissão for inválida")
  void execute_ShouldThrowException_WhenRoleIsInvalid() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    RoleEnum newRole = RoleEnum.ROLE_MODERATOR; // ROLE_MODERATOR não seja permitido

    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withRole(RoleEnum.ROLE_USER)
            .withStatus(AccountStatus.ACTIVE)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              var exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ROLE);
            });
  }

  @ParameterizedTest
  @EnumSource(
      value = RoleEnum.class,
      names = {"ROLE_ADMIN", "ROLE_USER"})
  @DisplayName("Deve lançar AccountStatusConflictException quando a permissão for a mesma")
  void execute_ShouldThrowException_WhenRoleIsTheSame(RoleEnum newRole) {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();

    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withRole(newRole)
            .withStatus(AccountStatus.ACTIVE)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    ErrorCode errorCode;
    switch (newRole) {
      case ROLE_ADMIN -> errorCode = ErrorCode.USER_ALREADY_ADMIN;
      case ROLE_USER -> errorCode = ErrorCode.USER_ALREADY_USER;
      default -> errorCode = ErrorCode.ACCOUNT_STATE_CONFLICT;
    }

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserRoleUseCase.execute(authenticatedAdminId, targetUserId, newRole))
        .isInstanceOf(AccountStatusConflictException.class)
        .satisfies(
            ex -> {
              var exception = (AccountStatusConflictException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            });
  }
}
