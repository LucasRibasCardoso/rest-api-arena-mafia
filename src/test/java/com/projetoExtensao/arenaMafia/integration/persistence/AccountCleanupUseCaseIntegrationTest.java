package com.projetoExtensao.arenaMafia.integration.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.application.user.usecase.cleanup.imp.AccountCleanupUseCaseImp;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Testes de integração para AccountCleanupUseCase")
public class AccountCleanupUseCaseIntegrationTest extends BaseTestContainersConfig {

  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;
  @Autowired private UserRepositoryPort userRepositoryPort;
  @Autowired private ReservationRepositoryPort reservationRepository;
  @Autowired private AccountCleanupUseCaseImp accountCleanupUseCase;

  @Nested
  @DisplayName("Testes para executeCleanupOfPendingAccounts")
  class CleanupPendingAccountsTests {

    @Test
    @DisplayName("Deve deletar usuários pendentes e seus tokens quando encontrados")
    void shouldDeletePendingUsersAndTokens_whenAccountsAreFound() {
      // Arrange
      User userToDelete =
          mockPersistUser(
              "userToDelete",
              "+551100000001",
              AccountStatus.PENDING_VERIFICATION,
              Instant.now().minus(25, ChronoUnit.HOURS),
              Instant.now().minus(25, ChronoUnit.HOURS));
      RefreshToken token = refreshTokenRepository.save(RefreshToken.create(7L, userToDelete));

      User mockRecentPendingUser =
          mockPersistUser(
              "mockRecentPendingUser",
              "+551100000002",
              AccountStatus.PENDING_VERIFICATION,
              Instant.now().minus(1, ChronoUnit.HOURS),
              Instant.now().minus(1, ChronoUnit.HOURS));

      User mockActivedUser = mockPersistUser();

      // Act
      accountCleanupUseCase.executeCleanupOfPendingAccounts();

      // Assert
      assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();
      assertThat(refreshTokenRepository.findByToken(token.getToken())).isEmpty();

      assertThat(userRepositoryPort.findById(mockRecentPendingUser.getId())).isPresent();
      assertThat(userRepositoryPort.findById(mockActivedUser.getId())).isPresent();
    }

    @Test
    @DisplayName("Não deve deletar nenhum usuário quando não há contas pendentes elegíveis")
    void shouldNotDeleteAnyUser_whenNoPendingAccountsEligible() {
      // Arrange
      User mockRecentPendingUser =
          mockPersistUser(
              "mockRecentPendingUser",
              "+551100000001",
              AccountStatus.PENDING_VERIFICATION,
              Instant.now().minus(1, ChronoUnit.HOURS),
              Instant.now().minus(1, ChronoUnit.HOURS));

      User mockActivedUser = mockPersistUser();

      // Act
      accountCleanupUseCase.executeCleanupOfPendingAccounts();

      // Assert
      assertThat(userRepositoryPort.findById(mockRecentPendingUser.getId())).isPresent();
      assertThat(userRepositoryPort.findById(mockActivedUser.getId())).isPresent();
    }
  }

  @Nested
  @DisplayName("Testes para executeCleanupOfDisabledAccounts")
  class CleanupDisabledAccountsTests {

    @Test
    @DisplayName("Deve deletar usuários desativados e seus tokens quando encontrados")
    void shouldDeleteDisabledUsersAndTokens_whenAccountsAreFound() {
      // Arrange
      mockPersistSystemUser(); // Necessário para o cleanup de contas desativadas

      User userToDelete =
          mockPersistUser(
              "userToDelete",
              "+551100000001",
              AccountStatus.DISABLED,
              Instant.now().minus(10, ChronoUnit.DAYS),
              Instant.now().minus(8, ChronoUnit.DAYS));
      RefreshToken token = refreshTokenRepository.save(RefreshToken.create(7L, userToDelete));

      User mockRecentDisabledUser =
          mockPersistUser(
              "mockRecentDisabledUser",
              "+551100000002",
              AccountStatus.DISABLED,
              Instant.now().minus(1, ChronoUnit.DAYS),
              Instant.now().minus(1, ChronoUnit.DAYS));

      User mockActivedUser = mockPersistUser();

      // Act
      accountCleanupUseCase.executeCleanupOfDisabledAccounts();

      // Assert
      assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();
      assertThat(refreshTokenRepository.findByToken(token.getToken())).isEmpty();

      assertThat(userRepositoryPort.findById(mockRecentDisabledUser.getId())).isPresent();
      assertThat(userRepositoryPort.findById(mockActivedUser.getId())).isPresent();
    }

    @Test
    @DisplayName("Deve migrar reservas passadas para o system user ao deletar conta desativada")
    void shouldMigrateReservationsToSystemUser_whenDeletingDisabledAccount() {
      // Arrange
      User systemUser = mockPersistSystemUser(); // Necessário para migração de reservas

      Modality modality = mockPersistModality("Futebol");
      Court court = mockPersistCourt("Quadra 1", modality);

      User userToDelete =
          mockPersistUser(
              "userToDelete",
              "+551100000001",
              AccountStatus.DISABLED,
              Instant.now().minus(10, ChronoUnit.DAYS),
              Instant.now().minus(8, ChronoUnit.DAYS));

      // Criar reservas passadas (histórico) para o usuário
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
      LocalDate pastDate = LocalDate.now().minusDays(5);

      Reservation pastReservation1 =
          mockPersistReservationByUserWithStatus(
              modality.getId(),
              court.getId(),
              pastDate,
              timeInterval,
              BigDecimal.valueOf(100),
              userToDelete.getId(),
              ReservationStatus.COMPLETED);

      Reservation pastReservation2 =
          mockPersistReservationByUserWithStatus(
              modality.getId(),
              court.getId(),
              pastDate.minusDays(1),
              timeInterval,
              BigDecimal.valueOf(100),
              userToDelete.getId(),
              ReservationStatus.CANCELLED);

      // Act
      accountCleanupUseCase.executeCleanupOfDisabledAccounts();

      // Assert - Usuário deletado
      assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();

      // Assert - Reservas migradas para system user
      Reservation migratedReservation1 =
          reservationRepository.findByIdOrElseThrow(pastReservation1.getId());
      Reservation migratedReservation2 =
          reservationRepository.findByIdOrElseThrow(pastReservation2.getId());

      assertThat(migratedReservation1.getUserId()).isEqualTo(systemUser.getId());
      assertThat(migratedReservation2.getUserId()).isEqualTo(systemUser.getId());

      // Assert - Status das reservas preservado
      assertThat(migratedReservation1.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
      assertThat(migratedReservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("Deve deletar conta desativada sem reservas")
    void shouldDeleteDisabledAccount_whenNoReservationsExist() {
      // Arrange
      mockPersistSystemUser(); // Necessário para o cleanup de contas desativadas

      User userToDelete =
          mockPersistUser(
              "userToDelete",
              "+551100000001",
              AccountStatus.DISABLED,
              Instant.now().minus(10, ChronoUnit.DAYS),
              Instant.now().minus(8, ChronoUnit.DAYS));

      // Act
      accountCleanupUseCase.executeCleanupOfDisabledAccounts();

      // Assert
      assertThat(userRepositoryPort.findById(userToDelete.getId())).isEmpty();
    }

    @Test
    @DisplayName("Não deve deletar usuários desativados recentemente (menos de 7 dias)")
    void shouldNotDeleteRecentlyDisabledUsers() {
      // Arrange
      mockPersistSystemUser(); // Necessário para o cleanup de contas desativadas

      User mockRecentDisabledUser =
          mockPersistUser(
              "mockRecentDisabledUser",
              "+551100000001",
              AccountStatus.DISABLED,
              Instant.now().minus(3, ChronoUnit.DAYS),
              Instant.now().minus(3, ChronoUnit.DAYS));

      // Act
      accountCleanupUseCase.executeCleanupOfDisabledAccounts();

      // Assert
      assertThat(userRepositoryPort.findById(mockRecentDisabledUser.getId())).isPresent();
    }
  }
}
