package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidDayOfWeekException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.OperatingHoursStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.unit.config.TestOperatingHoursDataProvider;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Testes unitários para entidade OperatingHours")
public class OperatingHoursTest {

  private final Set<DayOfWeek> defaultDaysOfWeek = TestOperatingHoursDataProvider.defaultDaysOfWeek;
  private final TimeInterval defaultTimeInterval =
      TestOperatingHoursDataProvider.defaultTimeInterval;

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("create() deve criar um horário de funcionamento com valores padrão corretos")
    void create_shouldCreateOperatingHoursSuccessfully() {
      // Arrange
      Instant startTime = Instant.now();

      // Act
      OperatingHours operatingHours = OperatingHours.create(defaultDaysOfWeek, defaultTimeInterval);

      // Assert
      assertThat(operatingHours).isNotNull();
      assertThat(operatingHours.getId()).isNotNull();
      assertThat(operatingHours.getDaysOfWeek()).isEqualTo(defaultDaysOfWeek);
      assertThat(operatingHours.getTimeInterval()).isEqualTo(defaultTimeInterval);
      assertThat(operatingHours.isActive()).isTrue();
      assertThat(operatingHours.getCreatedAt())
          .isAfterOrEqualTo(startTime)
          .isBeforeOrEqualTo(Instant.now());
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    @DisplayName("create() deve criar horários de funcionamento para todos os dias da semana")
    void create_shouldCreateOperatingHoursForAllDaysOfWeek(DayOfWeek dayOfWeek) {
      // Act
      OperatingHours operatingHours = OperatingHours.create(Set.of(dayOfWeek), defaultTimeInterval);

      // Assert
      assertThat(operatingHours).isNotNull();
      assertThat(operatingHours.getDaysOfWeek()).isEqualTo(Set.of(dayOfWeek));
      assertThat(operatingHours.isActive()).isTrue();
    }

    @Test
    @DisplayName(
        "reconstitute() deve reconstituir um horário de funcionamento a partir de dados existentes")
    void reconstitute_shouldRebuildOperatingHoursSuccessfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.FRIDAY);
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(9, 0), LocalTime.of(21, 0));
      boolean isActive = false;
      Instant createdAt = Instant.now().minusSeconds(3600);

      // Act
      OperatingHours operatingHours =
          OperatingHours.reconstitute(id, daysOfWeek, timeInterval, isActive, createdAt);

      // Assert
      assertThat(operatingHours.getId()).isEqualTo(id);
      assertThat(operatingHours.getDaysOfWeek()).isEqualTo(daysOfWeek);
      assertThat(operatingHours.getTimeInterval()).isEqualTo(timeInterval);
      assertThat(operatingHours.isActive()).isFalse();
      assertThat(operatingHours.getCreatedAt()).isEqualTo(createdAt);
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestOperatingHoursDataProvider#invalidDaysOfWeekProvider")
      @DisplayName("create() deve lançar InvalidDayOfWeekException quando dayOfWeek é null")
      void create_shouldThrowException_whenDayOfWeekIsNull(
          Set<DayOfWeek> invalidDaysOfWeek, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(() -> OperatingHours.create(invalidDaysOfWeek, defaultTimeInterval))
            .isInstanceOf(InvalidDayOfWeekException.class)
            .satisfies(
                ex -> {
                  InvalidDayOfWeekException exception = (InvalidDayOfWeekException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestOperatingHoursDataProvider#invalidTimeIntervalProvider")
      @DisplayName("create() deve lançar InvalidTimeIntervalException quando timeInterval é null")
      void create_shouldThrowException_whenTimeIntervalIsNull(
          TimeInterval invalidTimeInterval, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(() -> OperatingHours.create(defaultDaysOfWeek, invalidTimeInterval))
            .isInstanceOf(InvalidTimeIntervalException.class)
            .satisfies(
                ex -> {
                  InvalidTimeIntervalException exception = (InvalidTimeIntervalException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }
    }
  }

  @Nested
  @DisplayName("Testes para Gerenciamento de Status (enable/disable)")
  class StatusManagementTests {

    @Test
    @DisplayName("disable() deve desativar um horário de funcionamento ativo")
    void disable_shouldDisableOperatingHours_whenOperatingHoursIsActive() {
      // Arrange
      OperatingHours operatingHours = TestOperatingHoursDataProvider.createActiveOperatingHours();
      assertThat(operatingHours.isActive()).isTrue();

      // Act
      operatingHours.disable();

      // Assert
      assertThat(operatingHours.isActive()).isFalse();
    }

    @Test
    @DisplayName(
        "disable() deve lançar OperatingHoursStatusConflictException quando já está desativado")
    void disable_shouldThrowException_whenOperatingHoursIsAlreadyDisabled() {
      // Arrange
      OperatingHours operatingHours = TestOperatingHoursDataProvider.createDisabledOperatingHours();
      assertThat(operatingHours.isActive()).isFalse();

      // Act & Assert
      assertThatThrownBy(operatingHours::disable)
          .isInstanceOf(OperatingHoursStatusConflictException.class)
          .satisfies(
              ex -> {
                OperatingHoursStatusConflictException exception =
                    (OperatingHoursStatusConflictException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
              });
    }

    @Test
    @DisplayName("enable() deve ativar um horário de funcionamento desativado")
    void enable_shouldEnableOperatingHours_whenOperatingHoursIsDisabled() {
      // Arrange
      OperatingHours operatingHours = TestOperatingHoursDataProvider.createDisabledOperatingHours();
      assertThat(operatingHours.isActive()).isFalse();

      // Act
      operatingHours.enable();

      // Assert
      assertThat(operatingHours.isActive()).isTrue();
    }

    @Test
    @DisplayName(
        "enable() deve lançar OperatingHoursStatusConflictException quando já está ativado")
    void enable_shouldThrowException_whenOperatingHoursIsAlreadyEnabled() {
      // Arrange
      OperatingHours operatingHours = TestOperatingHoursDataProvider.createActiveOperatingHours();
      assertThat(operatingHours.isActive()).isTrue();

      // Act & Assert
      assertThatThrownBy(operatingHours::enable)
          .isInstanceOf(OperatingHoursStatusConflictException.class)
          .satisfies(
              ex -> {
                OperatingHoursStatusConflictException exception =
                    (OperatingHoursStatusConflictException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.OPERATING_HOURS_ALREADY_ENABLED);
              });
    }
  }

  @Nested
  @DisplayName("Testes para Validação de Sobreposição de Horários")
  class OverlapValidationTests {

    @Test
    @DisplayName(
        "validateNoOverlapWithSameDay() não deve lançar exceção quando horários não se sobrepõem")
    void validateNoOverlapWithSameDay_shouldNotThrowException_whenNoOverlap() {
      // Arrange
      TimeInterval morning = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));
      TimeInterval afternoon = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0));

      OperatingHours morningHours = OperatingHours.create(defaultDaysOfWeek, morning);
      OperatingHours afternoonHours = OperatingHours.create(defaultDaysOfWeek, afternoon);

      // Act & Assert
      assertDoesNotThrow(() -> morningHours.validateNoOverlapWithSameDay(afternoonHours));
    }

    @Test
    @DisplayName(
        "validateNoOverlapWithSameDay() deve lançar exceção quando horários do mesmo dia se"
            + " sobrepõem")
    void validateNoOverlapWithSameDay_shouldThrowException_whenOverlap() {
      // Arrange
      TimeInterval first = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(14, 0));
      TimeInterval second = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(18, 0));

      OperatingHours firstHours = OperatingHours.create(defaultDaysOfWeek, first);
      OperatingHours secondHours = OperatingHours.create(defaultDaysOfWeek, second);

      // Act & Assert
      assertThatThrownBy(() -> firstHours.validateNoOverlapWithSameDay(secondHours))
          .isInstanceOf(InvalidTimeIntervalException.class)
          .satisfies(
              ex -> {
                InvalidTimeIntervalException exception = (InvalidTimeIntervalException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIME_INTERVAL_OVERLAP);
              });
    }

    @Test
    @DisplayName(
        "validateNoOverlapWithSameDay() não deve lançar exceção quando horários são de dias"
            + " diferentes")
    void validateNoOverlapWithSameDay_shouldNotThrowException_whenDifferentDays() {
      // Arrange
      TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0));
      OperatingHours mondayHours = OperatingHours.create(Set.of(DayOfWeek.MONDAY), timeInterval);
      OperatingHours tuesdayHours = OperatingHours.create(Set.of(DayOfWeek.TUESDAY), timeInterval);

      // Act & Assert
      assertDoesNotThrow(() -> mondayHours.validateNoOverlapWithSameDay(tuesdayHours));
    }

    @Test
    @DisplayName("validateNoOverlapWithSameDay() não deve lançar exceção quando other é null")
    void validateNoOverlapWithSameDay_shouldNotThrowException_whenOtherIsNull() {
      // Arrange
      OperatingHours operatingHours = TestOperatingHoursDataProvider.createActiveOperatingHours();

      // Act & Assert
      assertDoesNotThrow(() -> operatingHours.validateNoOverlapWithSameDay(null));
    }

    @Test
    @DisplayName(
        "validateNoOverlapWithSameDay() deve permitir horários adjacentes sem sobreposição")
    void validateNoOverlapWithSameDay_shouldAllowAdjacentTimeIntervals() {
      // Arrange
      TimeInterval morning = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));
      TimeInterval afternoon = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(18, 0));

      OperatingHours morningHours = OperatingHours.create(defaultDaysOfWeek, morning);
      OperatingHours afternoonHours = OperatingHours.create(defaultDaysOfWeek, afternoon);

      // Act & Assert
      assertDoesNotThrow(() -> morningHours.validateNoOverlapWithSameDay(afternoonHours));
    }
  }

  @Nested
  @DisplayName("Testes para Métodos de Validação Estáticos")
  class StaticValidationTests {

    @Test
    @DisplayName("validateDayOfWeek() não deve lançar exceção para dayOfWeek válido")
    void validateDayOfWeek_shouldNotThrowException_whenDayOfWeekIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> OperatingHours.validateDaysOfWeek(defaultDaysOfWeek));
    }

    @Test
    @DisplayName(
        "validateDayOfWeek() deve lançar InvalidDayOfWeekException quando dayOfWeek é null")
    void validateDayOfWeek_shouldThrowException_whenDayOfWeekIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> OperatingHours.validateDaysOfWeek(null))
          .isInstanceOf(InvalidDayOfWeekException.class)
          .satisfies(
              ex -> {
                InvalidDayOfWeekException exception = (InvalidDayOfWeekException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DAY_OF_WEEK_REQUIRED);
              });
    }

    @Test
    @DisplayName("validateTimeInterval() não deve lançar exceção para timeInterval válido")
    void validateTimeInterval_shouldNotThrowException_whenTimeIntervalIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> OperatingHours.validateTimeInterval(defaultTimeInterval));
    }

    @Test
    @DisplayName(
        "validateTimeInterval() deve lançar InvalidTimeIntervalException quando timeInterval é"
            + " null")
    void validateTimeInterval_shouldThrowException_whenTimeIntervalIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> OperatingHours.validateTimeInterval(null))
          .isInstanceOf(InvalidTimeIntervalException.class)
          .satisfies(
              ex -> {
                InvalidTimeIntervalException exception = (InvalidTimeIntervalException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TIME_INTERVAL_REQUIRED);
              });
    }
  }
}
