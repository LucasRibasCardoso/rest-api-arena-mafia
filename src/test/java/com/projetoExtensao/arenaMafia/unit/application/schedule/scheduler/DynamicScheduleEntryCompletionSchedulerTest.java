package com.projetoExtensao.arenaMafia.unit.application.schedule.scheduler;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.scheduler.DynamicScheduleEntryCompletionScheduler;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.DeleteBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.reservation.CompleteReservationUseCase;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.core.Block;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários para DynamicScheduleEntryCompletionScheduler")
class DynamicScheduleEntryCompletionSchedulerTest {

  private static final ZoneId ZONE_ID = ZoneId.of("America/Sao_Paulo");

  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private CompleteReservationUseCase completeReservationUseCase;
  @Mock
  private DeleteBlockedTimeUseCase deleteBlockedTimeUseCase;
  @Mock
  private ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;

  private DynamicScheduleEntryCompletionScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
            new DynamicScheduleEntryCompletionScheduler(
                    taskScheduler, completeReservationUseCase, deleteBlockedTimeUseCase, scheduleEntryRepositoryPort);
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
      scheduler.scheduleReservationCompletion(reservationId, endDateTime);

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
      scheduler.scheduleReservationCompletion(reservationId, endDateTime);

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

      scheduler.scheduleReservationCompletion(reservationId, endDateTime);

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

      scheduler.scheduleReservationCompletion(reservationId, endDateTime);

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

      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
              any(LocalDateTime.class)))
              .thenReturn((List<ScheduleEntry>) (List<?>) confirmedReservations);

      mockSchedulerToReturnFuture();

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert
      verify(scheduleEntryRepositoryPort).findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class));
      verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Não deve agendar nenhuma tarefa quando não há reservas confirmadas")
    void shouldNotScheduleAnyTaskWhenNoConfirmedReservations() {
      // Arrange
      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
              any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert
      verify(scheduleEntryRepositoryPort)
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

      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(
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

  @Nested
  @DisplayName("Testes para scheduleBlockedTimeDeletion")
  class ScheduleBlockedTimeDeletionTests {

    @Test
    @DisplayName("Deve agendar a deleção de um BlockedTime para o horário de término correto")
    void shouldScheduleDeletionForCorrectEndTime() {
      // Arrange
      UUID blockedTimeId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(2);
      Instant expectedInstant = endDateTime.atZone(ZONE_ID).toInstant();

      mockSchedulerToReturnFuture();

      // Act
      scheduler.scheduleBlockedTimeDeletion(blockedTimeId, endDateTime);

      // Assert
      ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
      verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());

      assertThat(instantCaptor.getValue()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("Deve armazenar a tarefa agendada no mapa interno para BlockedTime")
    void shouldStoreScheduledTaskInMapForBlockedTime() {
      // Arrange
      UUID blockedTimeId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      mockSchedulerToReturnFuture();

      // Act
      scheduler.scheduleBlockedTimeDeletion(blockedTimeId, endDateTime);

      // Assert
      verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Deve executar o usecase de deleção quando a tarefa agendada é executada")
    void shouldExecuteDeleteUseCaseWhenScheduledTaskRuns() {
      // Arrange
      UUID blockedTimeId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      doAnswer(invocation -> mock(ScheduledFuture.class))
              .when(taskScheduler)
              .schedule(runnableCaptor.capture(), any(Instant.class));

      scheduler.scheduleBlockedTimeDeletion(blockedTimeId, endDateTime);

      // Act - Simula a execução da tarefa agendada
      runnableCaptor.getValue().run();

      // Assert
      verify(deleteBlockedTimeUseCase).execute(blockedTimeId, true);
    }

    @Test
    @DisplayName("Deve capturar exceção silenciosamente quando o usecase de deleção falha")
    void shouldCatchExceptionWhenDeleteUseCaseFails() {
      // Arrange
      UUID blockedTimeId = UUID.randomUUID();
      LocalDateTime endDateTime = LocalDateTime.now().plusHours(1);

      ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
      doAnswer(invocation -> mock(ScheduledFuture.class))
              .when(taskScheduler)
              .schedule(runnableCaptor.capture(), any(Instant.class));

      doThrow(new RuntimeException("Erro simulado"))
              .when(deleteBlockedTimeUseCase)
              .execute(blockedTimeId, true);

      scheduler.scheduleBlockedTimeDeletion(blockedTimeId, endDateTime);

      // Act & Assert - Não deve lançar exceção
      runnableCaptor.getValue().run();

      verify(deleteBlockedTimeUseCase).execute(blockedTimeId, true);
    }
  }

  @Nested
  @DisplayName("Testes para rescheduleExistingBlockedTimes")
  class RescheduleExistingBlockedTimesTests {

    @Test
    @DisplayName("Deve reagendar todos os BlockedTimes ativos existentes ao iniciar a aplicação")
    void shouldRescheduleAllActiveBlockedTimesOnApplicationStart() {
      // Arrange
      List<BlockedTime> activeBlockedTimes = createActiveBlockedTimes(3);

      when(scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class)))
              .thenReturn(activeBlockedTimes.stream().map(bt -> (com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry) bt).toList());
      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList()); // Para este teste, focar nos expired

      // Act
      scheduler.rescheduleExistingBlockedTimes();

      // Assert
      verify(scheduleEntryRepositoryPort).findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class));
      verify(deleteBlockedTimeUseCase, times(3)).execute(any(UUID.class), eq(true));
    }

    @Test
    @DisplayName("Deve reagendar todos os BlockedTimes confirmados existentes ao iniciar a aplicação")
    void shouldRescheduleAllConfirmedBlockedTimesOnApplicationStart() {
      // Arrange
      List<BlockedTime> confirmedBlockedTimes = createActiveBlockedTimes(3);

      when(scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());
      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class)))
              .thenReturn(confirmedBlockedTimes.stream().map(bt -> (com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry) bt).toList());

      mockSchedulerToReturnFuture();

      // Act
      scheduler.rescheduleExistingBlockedTimes();

      // Assert
      verify(scheduleEntryRepositoryPort).findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class));
      verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Não deve agendar nenhuma tarefa quando não há BlockedTimes confirmados")
    void shouldNotScheduleAnyTaskWhenNoConfirmedBlockedTimes() {
      // Arrange
      when(scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());
      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());

      // Act
      scheduler.rescheduleExistingBlockedTimes();

      // Assert
      verify(scheduleEntryRepositoryPort).findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class));
      verify(scheduleEntryRepositoryPort).findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class));
      verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("Deve agendar cada BlockedTime para seu horário de término correto")
    void shouldScheduleEachBlockedTimeForCorrectEndTime() {
      // Arrange
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
      TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

      BlockedTime blockedTime1 = createBlockedTime(tomorrow, timeInterval1);
      BlockedTime blockedTime2 = createBlockedTime(tomorrow, timeInterval2);

      when(scheduleEntryRepositoryPort.findAllActiveSchedulesEndedBeforeOrEqual(any(LocalDateTime.class)))
              .thenReturn(Collections.emptyList());
      when(scheduleEntryRepositoryPort.findAllConfirmedReservationsWithEndTimeAfter(any(LocalDateTime.class)))
              .thenReturn(List.of(blockedTime1, blockedTime2));

      mockSchedulerToReturnFuture();

      // Act
      scheduler.rescheduleExistingBlockedTimes();

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

  private List<Reservation> createConfirmedReservations(int count) {
    return IntStream.range(0, count)
            .mapToObj(
                    i -> {
                      LocalDate date = LocalDate.now().plusDays(1);
                      TimeInterval timeInterval =
                              new TimeInterval(LocalTime.of(10 + i, 0), LocalTime.of(11 + i, 0));
                      return createReservation(date, timeInterval);
                    })
            .toList();
  }

  private List<BlockedTime> createActiveBlockedTimes(int count) {
    return IntStream.range(0, count)
            .mapToObj(i -> {
              LocalDate date = LocalDate.now().plusDays(1);
              TimeInterval timeInterval =
                      new TimeInterval(LocalTime.of(10 + i, 0), LocalTime.of(11 + i, 0));
              return createBlockedTime(date, timeInterval);
            }).toList();
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

  private BlockedTime createBlockedTime(LocalDate date, TimeInterval timeInterval) {
    return BlockedTime.createSpecificTime(
            UUID.randomUUID(),
            new DateTimeSlot(date, timeInterval),
            "Manutenção na quadra",
            UUID.randomUUID()
    );
  }
}
