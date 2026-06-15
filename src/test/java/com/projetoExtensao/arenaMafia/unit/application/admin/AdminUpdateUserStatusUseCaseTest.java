package com.projetoExtensao.arenaMafia.unit.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.admin.imp.AdminUpdateUserStatusUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AccountStatusForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateOwnStatusException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.AdminCannotUpdateStatusOfUnverifiedUserException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
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
@DisplayName("Testes unitários para AdminUpdateUserStatusUseCase")
public class AdminUpdateUserStatusUseCaseTest {

  @Mock private UserRepositoryPort userRepositoryPort;
  @Mock private ReservationRepositoryPort reservationRepository;
  @Mock private ReservationBatchCancellationService reservationBatchCancellationService;
  @InjectMocks private AdminUpdateUserStatusUseCaseImp adminUpdateUserStatusUseCase;

  @Test
  @DisplayName("Deve desativar a conta do usuário e cancelar reservas futuras com notificação")
  void
      execute_ShouldUpdateUserStatusAndCancelFutureReservationsWithNotification_WhenDisablingAccount() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.DISABLED;
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.ACTIVE).build();
    List<Reservation> futureReservations = createFutureReservations(targetUserId, 2);

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);
    when(reservationRepository.findAllFutureActiveReservationsByUser(user.getId()))
        .thenReturn(futureReservations);

    // Act
    adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus);

    // Assert
    assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
    verify(reservationRepository, times(1)).findAllFutureActiveReservationsByUser(user.getId());
    verify(reservationBatchCancellationService, times(1))
        .cancelReservationsDueToAccountDisabled(futureReservations, user);
  }

  @Test
  @DisplayName("Deve desativar a conta sem reservas futuras")
  void execute_ShouldUpdateUserStatusWithoutReservations_WhenDisablingAccount() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.DISABLED;
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.ACTIVE).build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);
    when(reservationRepository.findAllFutureActiveReservationsByUser(user.getId()))
        .thenReturn(Collections.emptyList());

    // Act
    adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus);

    // Assert
    assertThat(user.getStatus()).isEqualTo(AccountStatus.DISABLED);
    verify(reservationRepository, times(1)).findAllFutureActiveReservationsByUser(user.getId());
    verify(reservationBatchCancellationService, times(1))
        .cancelReservationsDueToAccountDisabled(Collections.emptyList(), user);
  }

  @Test
  @DisplayName("Deve bloquear a conta do usuário sem cancelar reservas")
  void execute_ShouldBlockUserAccountWithoutCancellingReservations_WhenValidInput() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.LOCKED;
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.ACTIVE).build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act
    adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus);

    // Assert
    assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
    verify(reservationRepository, never()).findAllFutureActiveReservationsByUser(any());
    verify(reservationBatchCancellationService, never())
        .cancelReservationsInBatchSilently(anyList());
  }

  @Test
  @DisplayName("Deve ativar a conta do usuário sem cancelar reservas")
  void execute_ShouldActivateUserAccountWithoutCancellingReservations_WhenValidInput() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.ACTIVE;
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.LOCKED).build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act
    adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus);

    // Assert
    assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    verify(reservationRepository, never()).findAllFutureActiveReservationsByUser(any());
    verify(reservationBatchCancellationService, never())
        .cancelReservationsInBatchSilently(anyList());
  }

  @Test
  @DisplayName(
      "Deve lançar AdminCannotUpdateOwnStatusException quando admin tentar atualizar seu próprio status")
  void execute_ShouldThrowAdminCannotUpdateOwnStatusException_WhenAdminTriesToUpdateOwnStatus() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.DISABLED;

    // Act & Assert
    assertThatThrownBy(
            () ->
                adminUpdateUserStatusUseCase.execute(
                    authenticatedAdminId, authenticatedAdminId, newStatus))
        .isInstanceOf(AdminCannotUpdateOwnStatusException.class)
        .satisfies(
            ex -> {
              var exception = (AdminCannotUpdateOwnStatusException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ADMIN_CANNOT_UPDATE_OWN_STATUS);
            });
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStatusForbiddenException quando tentar atualizar para um status inválido")
  void execute_ShouldThrowAccountStatusForbiddenException_WhenUpdatingToInvalidStatus() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.PENDING_VERIFICATION;
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.ACTIVE).build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act & Assert
    assertThatThrownBy(
            () ->
                adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus))
        .isInstanceOf(AccountStatusForbiddenException.class)
        .satisfies(
            ex -> {
              var exception = (AccountStatusForbiddenException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ACCOUNT_STATUS);
            });
  }

  @Test
  @DisplayName(
      "Deve lançar AdminCannotUpdateStatusOfUnverifiedUserException quando admin tentar atualizar o status de um usuário não verificado")
  void
      execute_ShouldThrowAdminCannotUpdateStatusOfUnverifiedUserException_WhenAdminTriesToUpdateStatusOfUnverifiedUser() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.DISABLED;
    User user =
        TestDataProvider.UserBuilder.defaultUser()
            .withStatus(AccountStatus.PENDING_VERIFICATION)
            .build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    // Act & Assert
    assertThatThrownBy(
            () ->
                adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus))
        .isInstanceOf(AdminCannotUpdateStatusOfUnverifiedUserException.class)
        .satisfies(
            ex -> {
              var exception = (AdminCannotUpdateStatusOfUnverifiedUserException) ex;
              assertThat(exception.getErrorCode())
                  .isEqualTo(ErrorCode.ADMIN_CANNOT_UPDATE_STATUS_OF_UNVERIFIED_USER);
            });
  }

  @Test
  @DisplayName("Deve lançcar UserNotFoundException quando o usuário não for encontrado")
  void execute_ShouldThrowUserNotFoundException_WhenUserNotFound() {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    AccountStatus newStatus = AccountStatus.DISABLED;

    doThrow(new UserNotFoundException()).when(userRepositoryPort).findByIdOrElseThrow(targetUserId);

    // Act & Assert
    assertThatThrownBy(
            () ->
                adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, newStatus))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              var exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });
  }

  @ParameterizedTest
  @EnumSource(
      value = AccountStatus.class,
      names = {"ACTIVE", "DISABLED", "LOCKED"})
  @DisplayName(
      "Deve lençar AccountStatusConflictException quando tentar atualizar para o mesmo status")
  void execute_ShouldThrowAccountStatusConflictException_WhenUpdatingToSameStatus(
      AccountStatus status) {
    // Arrange
    UUID authenticatedAdminId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    User user = TestDataProvider.UserBuilder.defaultUser().withStatus(status).build();

    when(userRepositoryPort.findByIdOrElseThrow(targetUserId)).thenReturn(user);

    ErrorCode errorCode;
    switch (status) {
      case ACTIVE -> errorCode = ErrorCode.ACCOUNT_ALREADY_ACTIVE;
      case DISABLED -> errorCode = ErrorCode.ACCOUNT_ALREADY_DISABLED;
      case LOCKED -> errorCode = ErrorCode.ACCOUNT_ALREADY_LOCKED;
      default -> errorCode = ErrorCode.ACCOUNT_STATE_CONFLICT;
    }

    // Act & Assert
    assertThatThrownBy(
            () -> adminUpdateUserStatusUseCase.execute(authenticatedAdminId, targetUserId, status))
        .isInstanceOf(AccountStatusConflictException.class)
        .satisfies(
            ex -> {
              var exception = (AccountStatusConflictException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            });
  }

  private List<Reservation> createFutureReservations(UUID userId, int count) {
    return java.util.stream.IntStream.range(0, count)
        .mapToObj(
            i ->
                Reservation.createByUser(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    userId,
                    BigDecimal.valueOf(50.00),
                    new DateTimeSlot(
                        LocalDate.now().plusDays(i + 1),
                        new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)))))
        .toList();
  }
}
