package com.projetoExtensao.arenaMafia.unit.application.user.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.disable.imp.DisableMyAccountUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.AccountStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.UserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.unit.config.TestDataProvider;
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
  @InjectMocks private DisableMyAccountUseCaseImp disableMyAccountUseCase;

  @Test
  @DisplayName("Deve desativar a conta do usuário com sucesso")
  void shouldDisableUserAccountSuccessfully() {
    // Arrange
    User user = TestDataProvider.createActiveUser();
    UUID idCurrentUser = user.getId();

    when(userRepository.findByIdOrElseThrow(idCurrentUser)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);

    // Act
    disableMyAccountUseCase.execute(idCurrentUser);

    // Assert
    assertThat(user.isEnabled()).isFalse();
    verify(userRepository, times(1)).findByIdOrElseThrow(idCurrentUser);
    verify(userRepository, times(1)).save(user);
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
  }

  @Test
  @DisplayName("Deve lançar AccountStatusConflictException quando o status da conta já for ACTIVE")
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
  }
}
