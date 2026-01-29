package com.projetoExtensao.arenaMafia.unit.application.schedule.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.projetoExtensao.arenaMafia.application.notification.event.OnReservationCancelledByAdminEvent;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.service.ReservationBatchCancellationService;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.BatchCancellationFailedException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
  @DisplayName("Testes para cancelReservationsInBatch")
  class CancelReservationsInBatchTests {

    @Nested
    @DisplayName("Cenários de sucesso")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for null")
      void shouldReturnZeroWhenReservationsIsNull() {
        // Act
        int result = service.cancelReservationsInBatch(null, "Motivo", UUID.randomUUID());

        // Assert
        assertThat(result).isZero();
        verifyNoInteractions(reservationRepository, userRepository, eventPublisher);
      }

      @Test
      @DisplayName("Deve retornar 0 quando lista de reservas for vazia")
      void shouldReturnZeroWhenReservationsIsEmpty() {
        // Act
        int result = service.cancelReservationsInBatch(Collections.emptyList(), "Motivo", UUID.randomUUID());

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

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));
        when(reservationRepository.save(any())).thenReturn(reservation);

        // Act
        int result = service.cancelReservationsInBatch(List.of(reservation), "Bloqueio de horário", UUID.randomUUID());

        // Assert
        assertThat(result).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository, times(1)).save(reservation);
        verify(eventPublisher, times(1)).publishEvent(any(OnReservationCancelledByAdminEvent.class));
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

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user1, user2, user3));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = service.cancelReservationsInBatch(reservations, "Manutenção", UUID.randomUUID());

        // Assert
        assertThat(result).isEqualTo(3);
        assertThat(reservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation3.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository, times(3)).save(any(Reservation.class));
        verify(eventPublisher, times(3)).publishEvent(any(OnReservationCancelledByAdminEvent.class));
      }

      @Test
      @DisplayName("Deve publicar evento com dados corretos")
      void shouldPublishEventWithCorrectData() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        User user = createUser(reservation.getUserId());
        String reason = "Bloqueio para manutenção";

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));
        when(reservationRepository.save(any())).thenReturn(reservation);

        ArgumentCaptor<OnReservationCancelledByAdminEvent> eventCaptor =
            ArgumentCaptor.forClass(OnReservationCancelledByAdminEvent.class);

        // Act
        service.cancelReservationsInBatch(List.of(reservation), reason, UUID.randomUUID());

        // Assert
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        OnReservationCancelledByAdminEvent event = eventCaptor.getValue();
        assertThat(event.reservation()).isEqualTo(reservation);
        assertThat(event.username()).isEqualTo(user.getUsername());
        assertThat(event.userPhone()).isEqualTo(user.getPhone());
        assertThat(event.adminReason()).isEqualTo(reason);
      }

      @Test
      @DisplayName("Não deve publicar evento quando usuário não for encontrado")
      void shouldNotPublishEventWhenUserNotFound() {
        // Arrange
        Reservation reservation = createConfirmedReservation();

        when(userRepository.findAllByIds(any())).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any())).thenReturn(reservation);

        // Act
        int result = service.cancelReservationsInBatch(List.of(reservation), "Motivo", UUID.randomUUID());

        // Assert
        assertThat(result).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository, times(1)).save(reservation);
        verify(eventPublisher, never()).publishEvent(any());
      }
    }

    @Nested
    @DisplayName("Cenários de falha")
    class FailureScenarios {

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando uma reserva falhar e NÃO publicar eventos")
      void shouldThrowExceptionWhenOneReservationFailsAndNotPublishEvents() {
        // Arrange
        Reservation validReservation = createConfirmedReservation();
        Reservation alreadyCancelledReservation = createCancelledReservation();

        User user = createUser(validReservation.getUserId());

        List<Reservation> reservations = List.of(validReservation, alreadyCancelledReservation);

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatch(reservations, "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que NENHUM evento foi publicado (evita notificação sem commit)
        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando save falhar e NÃO publicar eventos")
      void shouldThrowExceptionWhenSaveFailsAndNotPublishEvents() {
        // Arrange
        Reservation reservation = createConfirmedReservation();
        User user = createUser(reservation.getUserId());

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user));
        when(reservationRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatch(List.of(reservation), "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que NENHUM evento foi publicado
        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve lançar BatchCancellationFailedException quando múltiplas reservas falharem e NÃO publicar eventos")
      void shouldThrowExceptionWhenMultipleReservationsFailAndNotPublishEvents() {
        // Arrange
        Reservation cancelled1 = createCancelledReservation();
        Reservation cancelled2 = createCancelledReservation();

        List<Reservation> reservations = List.of(cancelled1, cancelled2);

        when(userRepository.findAllByIds(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatch(reservations, "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que NENHUM evento foi publicado
        verify(eventPublisher, never()).publishEvent(any());
      }

      @Test
      @DisplayName("Deve parar na primeira falha (fail-fast) e NÃO publicar eventos")
      void shouldStopAtFirstFailureAndNotPublishEvents() {
        // Arrange
        Reservation validReservation1 = createConfirmedReservation();
        Reservation alreadyCancelledReservation = createCancelledReservation();
        Reservation validReservation2 = createConfirmedReservation();

        User user1 = createUser(validReservation1.getUserId());
        User user2 = createUser(validReservation2.getUserId());

        List<Reservation> reservations = List.of(
            validReservation1,
            alreadyCancelledReservation,
            validReservation2);

        when(userRepository.findAllByIds(any())).thenReturn(List.of(user1, user2));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act & Assert
        assertThatThrownBy(() -> service.cancelReservationsInBatch(reservations, "Motivo", UUID.randomUUID()))
            .isInstanceOf(BatchCancellationFailedException.class);

        // Verifica que apenas a primeira reserva foi processada (fail-fast)
        assertThat(validReservation1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        // A segunda reserva válida NÃO foi processada devido ao fail-fast
        assertThat(validReservation2.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

        // Verifica que save foi chamado apenas para a primeira reserva
        verify(reservationRepository, times(1)).save(any(Reservation.class));

        // Verifica que NENHUM evento foi publicado (garantia de atomicidade)
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

