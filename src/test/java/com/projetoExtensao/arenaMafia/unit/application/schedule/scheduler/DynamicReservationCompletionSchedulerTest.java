package com.projetoExtensao.arenaMafia.unit.application.schedule.scheduler;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.scheduler.DynamicReservationCompletionScheduler;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
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
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para DynamicReservationCompletionScheduler")
class DynamicReservationCompletionSchedulerTest {

  private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private CompleteReservationUseCase completeReservationUseCase;
  @Mock
  private ReservationRepositoryPort reservationRepositoryPort;

  private DynamicReservationCompletionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
            new DynamicReservationCompletionScheduler(
                    taskScheduler, completeReservationUseCase, reservationRepositoryPort);
  }

  private void mockSchedulerToReturnFuture() {
    doAnswer(invocation -> mock(ScheduledFuture.class))
            .when(taskScheduler)
            .schedule(any(Runnable.class), any(Instant.class));
  }

  @Nested
  @DisplayName("Testes para scheduleCompletion")
  class ScheduleCompletionTests {

    @Test
    @DisplayName("Deve agendar a conclusão de uma reserva para o horário de término correto")
    void shouldScheduleCompletionForCorrectEndTime() {
      // Arrange
      UUID reservationId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(2);
      Instant expectedInstant = endDateTime.atZone(ZONE_ID).toInstant();

      mockSchedulerToReturnFuture();

      // Act
      scheduler.scheduleCompletion(reservationId, endDateTime);

      // Assert
      ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
      verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());

      assertThat(instantCaptor.getValue()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("Deve armazenar a tarefa agendada no mapa interno")
    void shouldStoreScheduledTaskInMap() {
      // Arrange
      UUID reservationId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      mockSchedulerToReturnFuture();

      // Act
      scheduler.scheduleCompletion(reservationId, endDateTime);

      // Assert
      verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Deve executar o usecase de conclusão quando a tarefa agendada é executada")
    void shouldExecuteCompleteUseCaseWhenScheduledTaskRuns() {
      // Arrange
      UUID reservationId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      doAnswer(invocation -> mock(ScheduledFuture.class))
              .when(taskScheduler)
              .schedule(runnableCaptor.capture(), any(Instant.class));

      scheduler.scheduleCompletion(reservationId, endDateTime);

      // Act - Simula a execução da tarefa agendada
      runnableCaptor.getValue().run();

      // Assert
      verify(completeReservationUseCase).execute(reservationId);
    }

    @Test
    @DisplayName("Deve capturar exceção silenciosamente quando o usecase falha")
    void shouldCatchExceptionWhenUseCaseFails() {
      // Arrange
      UUID reservationId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      doAnswer(invocation -> mock(ScheduledFuture.class))
              .when(taskScheduler)
              .schedule(runnableCaptor.capture(), any(Instant.class));

      doThrow(new RuntimeException("Erro simulado"))
              .when(completeReservationUseCase)
              .execute(reservationId);

      scheduler.scheduleCompletion(reservationId, endDateTime);

      // Act & Assert - Não deve lançar exceção
      runnableCaptor.getValue().run();

      verify(completeReservationUseCase).execute(reservationId);
    }
  }

  @Nested
  @DisplayName("Testes para rescheduleExistingReservations")
  class RescheduleExistingReservationsTests {

    @Test
    @DisplayName("Deve reagendar todas as reservas confirmadas existentes ao iniciar a aplicação")
    void shouldRescheduleAllConfirmedReservationsOnApplicationStart() {
      // Arrange
      List<Reservation> confirmedReservations = createConfirmedReservations(3);

      when(reservationRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
              any(LocalDateTime.class)))
              .thenReturn(confirmedReservations);

      mockSchedulerToReturnFuture();

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert
      verify(reservationRepositoryPort)
              .findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class));
      verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Não deve agendar nenhuma tarefa quando não há reservas confirmadas")
    void shouldNotScheduleAnyTaskWhenNoConfirmedReservations() {
      // Arrange
      when(reservationRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
              any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert
      verify(reservationRepositoryPort)
              .findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class));
      verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Deve agendar cada reserva para seu horário de término correto")
    void shouldScheduleEachReservationForCorrectEndTime() {
      // Arrange
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
      TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

      Reservation reservation1 = createReservation(tomorrow, timeInterval1);
      Reservation reservation2 = createReservation(tomorrow, timeInterval2);

      when(reservationRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
              any(LocalDateTime.class)))
              .thenReturn(List.of(reservation1, reservation2));

      mockSchedulerToReturnFuture();

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert
      ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
      verify(taskScheduler, times(2)).schedule(any(Runnable.class), instantCaptor.capture());

      List<Instant> capturedInstants = instantCaptor.getAllValues();
      Instant expectedInstant1 =
              LocalDateTime.of(tomorrow, LocalTime.of(11, 0)).atZone(ZONE_ID).toInstant();
      Instant expectedInstant2 =
              LocalDateTime.of(tomorrow, LocalTime.of(15, 0)).atZone(ZONE_ID).toInstant();

      assertThat(capturedInstants).containsExactlyInAnyOrder(expectedInstant1, expectedInstant2);
    }
  }

  // --- Helper Methods ---

  private List<Reservation> createConfirmedReservations(int count) {
    return java.util.stream.IntStream.range(0, count)
            .mapToObj(
                    i -> {
                      LocalDate date = LocalDate.now().plusDays(1);
                      TimeInterval timeInterval =
                              new TimeInterval(LocalTime.of(10 + i, 0), LocalTime.of(11 + i, 0));
                      return createReservation(date, timeInterval);
                    })
            .toList();
  }

  private Reservation createReservation(LocalDate date, TimeInterval timeInterval) {
    return Reservation.reconstitute(
            UUID.randomUUID(),
            UUID.randomUUID(), // courtId
            UUID.randomUUID(), // modalityId
            UUID.randomUUID(), // userId
            null, // scheduledByAdminId
            null, // canceledByAdminId
            BigDecimal.valueOf(50.00),
            new DateTimeSlot(date, timeInterval),
            ReservationStatus.CONFIRMED,
            null, // recurringReservationId
            Instant.now());
  }
}
