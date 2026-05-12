package com.projetoExtensao.arenaMafia.unit.application.schedule.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationsCancelledByAdminNotificationEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.BatchCancellationFailedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para ReservationBatchCancellationService")
class ReservationBatchCancellationServiceTest {

  @Mock
  private ReservationRepositoryPort reservationRepository;

  @Mock
  private UserRepositoryPort userRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private ReservationBatchCancellationService service;

  @BeforeEach
  void setUp() {
    service = new ReservationBatchCancellationService(
        reservationRepository,
        userRepository,
        eventPublisher);
  }

  @Nested
  @DisplayName("Testes para cancelReservationsInBatchByAdmin")
  class CancelReservationsInBatchByAdminTests {

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for null")
      void shouldReturnZeroWhenReservationsIsNull() {
        // Act
        int result = service.cancelReservationsInBatchByAdmin(null, "Motivo", UUID.randomUUID());

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, userRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for vazia")
      void shouldReturnZeroWhenReservationsIsEmpty() {
        // Act
        int result = service.cancelReservationsInBatchByAdmin(Collections.emptyList(), "Motivo", UUID.randomUUID());

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, userRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve cancelar uma única reserva com sucesso")
      void shouldCancelSingleReservationSuccessfully() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        User user = createUser(reservation.getUserId());
        UUID adminId = UUID.randomUUID();

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));

        // Act
        int result = service.cancelReservationsInBatchByAdmin(List.of(reservation), "Bloqueio de horário", adminId);

        // Assert
        assertThat(result).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledByAdminId()).isEqualTo(adminId);
        verify(reservationRepository, times(1)).saveAll(anyList());
        verify(eventPublisher, times(1)).publishEvent(any(OnReservationsCancelledByAdminNotificationEvent.class));
      }

      @Test
      @DisplayName("Deve cancelar todas as reservas com sucesso")
      void shouldCancelAllReservationsSuccessfully() {
        // Arrange
        Reservation reservation1 = createConfirmedReservation();
        Reservation reservation2 = createConfirmedReservation();
        Reservation reservation3 = createConfirmedReservation();

        User user1 = createUser(reservation1.getUserId());
        User user2 = createUser(reservation2.getUserId());
        User user3 = createUser(reservation3.getUserId());

        List<Reservation> reservations = List.of(reservation1, reservation2, reservation3);
        UUID adminId = UUID.randomUUID();

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user1, user2, user3));

        // Act
        int result = service.cancelReservationsInBatchByAdmin(reservations, "Manutenção", adminId);

        // Assert
        assertThat(result).isEqualTo(3);
        assertThat(reservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation3.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository, times(1)).saveAll(reservations);
        verify(eventPublisher, times(3)).publishEvent(any(OnReservationsCancelledByAdminNotificationEvent.class));
      }

      @Test
      @DisplayName("Deve publicar evento com dados corretos")
      void shouldPublishEventWithCorrectData() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        User user = createUser(reservation.getUserId());
        String reason = "Bloqueio para manutenção";
        UUID adminId = UUID.randomUUID();

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));

        ArgumentCaptor<OnReservationsCancelledByAdminNotificationEvent> eventCaptor =
            ArgumentCaptor.forClass(OnReservationsCancelledByAdminNotificationEvent.class);

        // Act
        service.cancelReservationsInBatchByAdmin(List.of(reservation), reason, adminId);

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        OnReservationsCancelledByAdminNotificationEvent event = eventCaptor.getValue();
        assertThat(event.reservations().getFirst()).isEqualTo(reservation);
        assertThat(event.username()).isEqualTo(user.getUsername());
        assertThat(event.userPhone()).isEqualTo(user.getPhone());
        assertThat(event.adminReason()).isEqualTo(reason);
      }
    }

    @Nested
    @DisplayName("Cenários de falha")
    class FailureScenarios {

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando uma reserva já cancelada e NÃO publicar eventos")
      void shouldThrowExceptionWhenOneReservationFailsAndNotPublishEvents() {
        // Arrange
        Reservation validReservation = createConfirmedReservation();
        Reservation alreadyCancelledReservation = createCancelledReservation();

        User user = createUser(validReservation.getUserId());

        List<Reservation> reservations = List.of(validReservation, alreadyCancelledReservation);

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatchByAdmin(reservations, "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que NENHUM evento foi publicado (evita notificação sem commit)
        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando saveAll falhar e NÃO publicar eventos")
      void shouldThrowExceptionWhenSaveFailsAndNotPublishEvents() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        User user = createUser(reservation.getUserId());

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));
        doThrow(new RuntimeException("Database error")).when(reservationRepository).saveAll(anyList());

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatchByAdmin(List.of(reservation), "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que NENHUM evento foi publicado
        verify(eventPublisher, never()).publishEvent(any());
      }
    }
  }

  @Nested
  @DisplayName("Testes para cancelReservationsDueToAccountDisabled")
  class CancelReservationsDueToAccountDisabledTests {

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for null")
      void shouldReturnZeroWhenReservationsIsNull() {
        // Arrange
        User user = createUser(UUID.randomUUID());

        // Act
        int result = service.cancelReservationsDueToAccountDisabled(null, user);

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for vazia")
      void shouldReturnZeroWhenReservationsIsEmpty() {
        // Arrange
        User user = createUser(UUID.randomUUID());

        // Act
        int result = service.cancelReservationsDueToAccountDisabled(Collections.emptyList(), user);

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve cancelar reservas e notificar usuário sobre desativação de conta")
      void shouldCancelReservationsAndNotifyUser() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Reservation reservation1 = createConfirmedReservationForUser(userId);
        Reservation reservation2 = createConfirmedReservationForUser(userId);
        List<Reservation> reservations = List.of(reservation1, reservation2);

        ArgumentCaptor<OnReservationsCancelledByAdminNotificationEvent> eventCaptor =
            ArgumentCaptor.forClass(OnReservationsCancelledByAdminNotificationEvent.class);

        // Act
        int result = service.cancelReservationsDueToAccountDisabled(reservations, user);

        // Assert
        assertThat(result).isEqualTo(2);
        assertThat(reservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        // Cancelamento por desativação NÃO deve ter adminId
        assertThat(reservation1.getCancelledByAdminId()).isNull();
        assertThat(reservation2.getCancelledByAdminId()).isNull();
        verify(reservationRepository, times(1)).saveAll(reservations);
        // DEVE publicar evento de notificação
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        OnReservationsCancelledByAdminNotificationEvent event = eventCaptor.getValue();
        assertThat(event.username()).isEqualTo(user.getUsername());
        assertThat(event.userPhone()).isEqualTo(user.getPhone());
        assertThat(event.adminReason()).contains("conta foi desativada");
        assertThat(event.reservations()).hasSize(2);
      }
    }

    @Nested
    @DisplayName("Cenários de falha")
    class FailureScenarios {

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando uma reserva já estiver cancelada")
      void shouldThrowExceptionWhenReservationAlreadyCancelled() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Reservation validReservation = createConfirmedReservationForUser(userId);
        Reservation alreadyCancelledReservation = createCancelledReservation();
        List<Reservation> reservations = List.of(validReservation, alreadyCancelledReservation);

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsDueToAccountDisabled(reservations, user))
            .isInstanceOf(BatchCancellationFailedException.class);

        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando saveAll falhar")
      void shouldThrowExceptionWhenSaveAllFails() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = createUser(userId);
        Reservation reservation = createConfirmedReservationForUser(userId);
        doThrow(new RuntimeException("Database error")).when(reservationRepository).saveAll(anyList());

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsDueToAccountDisabled(List.of(reservation), user))
            .isInstanceOf(BatchCancellationFailedException.class);

        verify(eventPublisher, never()).publishEvent(any());
      }
    }
  }

  @Nested
  @DisplayName("Testes para cancelReservationsInBatchSilently")
  class CancelReservationsInBatchSilentlyTests {

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for null")
      void shouldReturnZeroWhenReservationsIsNull() {
        // Act
        int result = service.cancelReservationsInBatchSilently(null);

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for vazia")
      void shouldReturnZeroWhenReservationsIsEmpty() {
        // Act
        int result = service.cancelReservationsInBatchSilently(Collections.emptyList());

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve cancelar reservas silenciosamente sem publicar eventos")
      void shouldCancelReservationsSilentlyWithoutPublishingEvents() {
        // Arrange
        Reservation reservation1 = createConfirmedReservation();
        Reservation reservation2 = createConfirmedReservation();
        List<Reservation> reservations = List.of(reservation1, reservation2);

        // Act
        int result = service.cancelReservationsInBatchSilently(reservations);

        // Assert
        assertThat(result).isEqualTo(2);
        assertThat(reservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        // Cancelamento silencioso NÃO deve ter adminId
        assertThat(reservation1.getCancelledByAdminId()).isNull();
        assertThat(reservation2.getCancelledByAdminId()).isNull();
        verify(reservationRepository, times(1)).saveAll(reservations);
        // NÃO deve publicar eventos
        verify(eventPublisher, never()).publishEvent(any());
        // NÃO deve buscar usuários
        verify(userRepository, never()).findAllByIds(any());
      }
    }

    @Nested
    @DisplayName("Cenários de falha")
    class FailureScenarios {

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando uma reserva já estiver cancelada")
      void shouldThrowExceptionWhenReservationAlreadyCancelled() {
        // Arrange
        Reservation validReservation = createConfirmedReservation();
        Reservation alreadyCancelledReservation = createCancelledReservation();
        List<Reservation> reservations = List.of(validReservation, alreadyCancelledReservation);

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatchSilently(reservations))
            .isInstanceOf(BatchCancellationFailedException.class);

        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando saveAll falhar")
      void shouldThrowExceptionWhenSaveAllFails() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        doThrow(new RuntimeException("Database error")).when(reservationRepository).saveAll(anyList());

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatchSilently(List.of(reservation)))
            .isInstanceOf(BatchCancellationFailedException.class);

        verify(eventPublisher, never()).publishEvent(any());
      }
    }
  }

  // ==================== Métodos auxiliares ====================

  private Reservation createConfirmedReservation() {
    UUID courtId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID modalityId = UUID.randomUUID();
    LocalDate date = LocalDate.now().plusDays(1);
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    BigDecimal price = BigDecimal.valueOf(100);

    return Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
  }

  private Reservation createConfirmedReservationForUser(UUID userId) {
    UUID courtId = UUID.randomUUID();
    UUID modalityId = UUID.randomUUID();
    LocalDate date = LocalDate.now().plusDays(1);
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    BigDecimal price = BigDecimal.valueOf(100);

    return Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
  }

  private Reservation createCancelledReservation() {
    Reservation reservation = createConfirmedReservation();
    reservation.cancel();
    return reservation;
  }

  private User createUser(UUID userId) {
    return User.reconstitute(
        userId,
        "usuario_teste",
        "Nome Completo",
        "+5511999999999",
        "hashedPassword",
        com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus.ACTIVE,
        com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum.ROLE_USER,
        java.time.Instant.now(),
        java.time.Instant.now());
  }
}

