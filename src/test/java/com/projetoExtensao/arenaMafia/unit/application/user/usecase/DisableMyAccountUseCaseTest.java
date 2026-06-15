package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.disable.imp.DisableMyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStatusConflictException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para DisableMyAccountUseCase")
public class DisableMyAccountUseCaseTest {

  @Mock private UserRepositoryPort userRepository;
  @Mock private ReservationRepositoryPort reservationRepository;
  @Mock private ReservationBatchCancellationService reservationBatchCancellationService;
  @InjectMocks private DisableMyAccountUseCaseImp disableMyAccountUseCase;

  @Test
  @DisplayName("Deve desativar a conta do usuário com sucesso e cancelar reservas futuras")
  void shouldDisableUserAccountSuccessfullyAndCancelFutureReservations() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();
    List<Reservation> futureReservations = createFutureReservations(idCurrentUser, 2);

    when(userRepository.findByIdOrElseThrow(idCurrentUser)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(reservationRepository.findAllFutureActiveReservationsByUser(idCurrentUser))
        .thenReturn(futureReservations);

    // Act
    disableMyAccountUseCase.execute(idCurrentUser);

    // Assert
    assertThat(user.isEnabled()).isFalse();
    verify(userRepository, times(1)).findByIdOrElseThrow(idCurrentUser);
    verify(userRepository, times(1)).save(user);
    verify(reservationRepository, times(1)).findAllFutureActiveReservationsByUser(idCurrentUser);
    verify(reservationBatchCancellationService, times(1))
        .cancelReservationsInBatchSilently(futureReservations);
  }

  @Test
  @DisplayName("Deve desativar a conta do usuário sem reservas futuras")
  void shouldDisableUserAccountSuccessfullyWithoutFutureReservations() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();

    when(userRepository.findByIdOrElseThrow(idCurrentUser)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(reservationRepository.findAllFutureActiveReservationsByUser(idCurrentUser))
        .thenReturn(Collections.emptyList());

    // Act
    disableMyAccountUseCase.execute(idCurrentUser);

    // Assert
    assertThat(user.isEnabled()).isFalse();
    verify(userRepository, times(1)).findByIdOrElseThrow(idCurrentUser);
    verify(userRepository, times(1)).save(user);
    verify(reservationRepository, times(1)).findAllFutureActiveReservationsByUser(idCurrentUser);
    verify(reservationBatchCancellationService, times(1))
        .cancelReservationsInBatchSilently(Collections.emptyList());
  }

  @Test
  @DisplayName("Deve lançar exceção quando o usuário não for encontrado")
  void execute_shouldThrowException_whenUserNotFound() {
    // Arrange
    UUID idCurrentUser = UUID.randomUUID();

    doThrow(new UserNotFoundException()).when(userRepository).findByIdOrElseThrow(idCurrentUser);

    // Act & Assert
    assertThatThrownBy(() -> disableMyAccountUseCase.execute(idCurrentUser))
        .isInstanceOf(UserNotFoundException.class)
        .satisfies(
            ex -> {
              UserNotFoundException exception = (UserNotFoundException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(reservationRepository, never()).findAllFutureActiveReservationsByUser(any());
  }

  @Test
  @DisplayName(
      "Deve lançar AccountStatusConflictException quando o status da conta já for DISABLED")
  void execute_shouldThrowAccountStatusConflictException_whenAccountStatusIsNotActive() {
    // Arrange
    User user =
        TestDataProvider.UserBuilder.defaultUser().withStatus(AccountStatus.DISABLED).build();
    UUID idCurrentUser = user.getId();

    when(userRepository.findByIdOrElseThrow(idCurrentUser)).thenReturn(user);

    // Act & Assert
    assertThatThrownBy(() -> disableMyAccountUseCase.execute(idCurrentUser))
        .isInstanceOf(AccountStatusConflictException.class)
        .satisfies(
            ex -> {
              AccountStatusConflictException exception = (AccountStatusConflictException) ex;
              assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_ALREADY_DISABLED);
            });

    verify(userRepository, never()).save(any(User.class));
    verify(reservationRepository, never()).findAllFutureActiveReservationsByUser(any());
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
