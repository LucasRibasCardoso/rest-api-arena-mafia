package com.projetoExtensao.arenaMafia.integration.scheduler;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.scheduler.DynamicScheduleEntryCompletionScheduler;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.integration.config.BaseTestContainersConfig;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes de Integração para DynamicReservationCompletionScheduler")
class DynamicScheduleEntryCompletionSchedulerIntegrationTest extends BaseTestContainersConfig {

  @Autowired private DynamicScheduleEntryCompletionScheduler scheduler;
  @Autowired private ScheduleEntryRepositoryPort scheduleEntryRepositoryPort;

  private User defaultUser;
  private Modality modality;
  private Court court;

  @BeforeEach
  void setUp() {
    defaultUser = mockPersistUser();
    modality = mockPersistModality("Beach Tennis");
    court = mockPersistCourt("Quadra Principal", modality);
  }

  @Nested
  @DisplayName("Testes para agendamento dinâmico de conclusão de reservas")
  class DynamicSchedulingTests {

    @Test
    @DisplayName("Deve alterar o status da reserva para COMPLETED após o horário de término")
    void shouldChangeReservationStatusToCompletedAfterEndTime() {
      // Arrange - Usa horários válidos (minutos 00 ou 30)
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
      DateTimeSlot dateTimeSlot = new DateTimeSlot(tomorrow, timeInterval);

      Reservation reservation =
          Reservation.createByUser(
              modality.getId(),
              court.getId(),
              defaultUser.getId(),
              BigDecimal.valueOf(50.00),
              dateTimeSlot);

      ScheduleEntry savedReservation = scheduleEntryRepositoryPort.save(reservation);

      // Act - Agenda a conclusão para 1 segundo no futuro (simula término imediato)
      LocalDateTime scheduledEndTime = LocalDateTime.now().plusSeconds(1);
      scheduler.scheduleReservationCompletion(savedReservation.getId(), scheduledEndTime);

      // Assert - Aguarda até 5 segundos para a tarefa ser executada
      Awaitility.await()
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(500))
          .untilAsserted(
              () -> {
                ScheduleEntry updatedEntry =
                    scheduleEntryRepositoryPort.findByIdOrElseThrow(savedReservation.getId());

                assertThat(updatedEntry).isInstanceOf(Reservation.class);

                Reservation updatedReservation = (Reservation) updatedEntry;
                assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
              });
    }

    @Test
    @DisplayName("Não deve alterar o status de uma reserva já cancelada")
    void shouldNotChangeStatusOfCancelledReservation() {
      // Arrange - Usa horários válidos
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));
      DateTimeSlot dateTimeSlot = new DateTimeSlot(tomorrow, timeInterval);

      Reservation reservation =
          Reservation.createByUser(
              modality.getId(),
              court.getId(),
              defaultUser.getId(),
              BigDecimal.valueOf(50.00),
              dateTimeSlot);

      // Cancela a reserva antes de salvar
      reservation.cancel();
      ScheduleEntry savedReservation = scheduleEntryRepositoryPort.save(reservation);

      // Act - Agenda a conclusão para 1 segundo no futuro
      LocalDateTime scheduledEndTime = LocalDateTime.now().plusSeconds(1);
      scheduler.scheduleReservationCompletion(savedReservation.getId(), scheduledEndTime);

      // Assert - Aguarda e verifica que o status permanece CANCELLED
      Awaitility.await()
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(500))
          .untilAsserted(
              () -> {
                ScheduleEntry updatedEntry =
                    scheduleEntryRepositoryPort.findByIdOrElseThrow(savedReservation.getId());

                assertThat(updatedEntry).isInstanceOf(Reservation.class);

                Reservation updatedReservation = (Reservation) updatedEntry;
                assertThat(updatedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
              });
    }

    @Test
    @DisplayName("Deve agendar múltiplas reservas simultaneamente sem conflitos")
    void shouldScheduleMultipleReservationsWithoutConflicts() {
      // Arrange - Usa horários válidos
      LocalDate tomorrow = LocalDate.now().plusDays(1);

      TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0));
      TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(9, 30), LocalTime.of(10, 30));

      Reservation reservation1 = createAndSaveReservation(tomorrow, timeInterval1);
      Reservation reservation2 = createAndSaveReservation(tomorrow, timeInterval2);

      // Act - Agenda as duas conclusões para momentos próximos
      scheduler.scheduleReservationCompletion(reservation1.getId(), LocalDateTime.now().plusSeconds(1));
      scheduler.scheduleReservationCompletion(reservation2.getId(), LocalDateTime.now().plusSeconds(2));

      // Assert - Aguarda ambas serem concluídas
      Awaitility.await()
          .atMost(Duration.ofSeconds(7))
          .pollInterval(Duration.ofMillis(500))
          .untilAsserted(
              () -> {
                ScheduleEntry entry1 =
                    scheduleEntryRepositoryPort.findByIdOrElseThrow(reservation1.getId());
                ScheduleEntry entry2 =
                    scheduleEntryRepositoryPort.findByIdOrElseThrow(reservation2.getId());

                assertThat(((Reservation) entry1).getStatus())
                    .isEqualTo(ReservationStatus.COMPLETED);
                assertThat(((Reservation) entry2).getStatus())
                    .isEqualTo(ReservationStatus.COMPLETED);
              });
    }
  }

  @Nested
  @DisplayName("Testes para rescheduleExistingReservations")
  class RescheduleExistingReservationsTests {

    @Test
    @DisplayName("Não deve reagendar reservas que já foram concluídas")
    void shouldNotRescheduleAlreadyCompletedReservations() {
      // Arrange
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
      DateTimeSlot dateTimeSlot = new DateTimeSlot(tomorrow, timeInterval);

      Reservation reservation =
          Reservation.createByUser(
              modality.getId(),
              court.getId(),
              defaultUser.getId(),
              BigDecimal.valueOf(50.00),
              dateTimeSlot);

      reservation.complete();
      scheduleEntryRepositoryPort.save(reservation);

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert - Status deve permanecer COMPLETED
      ScheduleEntry entry = scheduleEntryRepositoryPort.findByIdOrElseThrow(reservation.getId());
      assertThat(entry).isNotNull();
      assertThat(((Reservation) entry).getStatus()).isEqualTo(ReservationStatus.COMPLETED);
    }

    @Test
    @DisplayName("Não deve reagendar reservas que já foram canceladas")
    void shouldNotRescheduleCancelledReservations() {
      // Arrange
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(16, 0), LocalTime.of(17, 0));
      DateTimeSlot dateTimeSlot = new DateTimeSlot(tomorrow, timeInterval);

      Reservation reservation =
          Reservation.createByUser(
              modality.getId(),
              court.getId(),
              defaultUser.getId(),
              BigDecimal.valueOf(50.00),
              dateTimeSlot);

      reservation.cancel();
      scheduleEntryRepositoryPort.save(reservation);

      // Act
      scheduler.rescheduleExistingReservations();

      // Assert - Status deve permanecer CANCELLED
      ScheduleEntry entry = scheduleEntryRepositoryPort.findByIdOrElseThrow(reservation.getId());
      assertThat(entry).isNotNull();
      assertThat(((Reservation) entry).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
  }

  // --- Métodos Auxiliares ---
  private Reservation createAndSaveReservation(LocalDate date, TimeInterval timeInterval) {
    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    Reservation reservation =
        Reservation.createByUser(
            modality.getId(),
            court.getId(),
            defaultUser.getId(),
            BigDecimal.valueOf(50.00),
            dateTimeSlot);
    return (Reservation) scheduleEntryRepositoryPort.save(reservation);
  }
}
