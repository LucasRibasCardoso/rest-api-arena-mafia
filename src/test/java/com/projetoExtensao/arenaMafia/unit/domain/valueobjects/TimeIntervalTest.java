package com.projetoExtensao.arenaMafia.unit.domain.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Testes para o Value Object: TimeInterval")
class TimeIntervalTest {

  private static final LocalTime TEN_AM = LocalTime.of(10, 0);
  private static final LocalTime ELEVEN_AM = LocalTime.of(11, 0);
  private static final LocalTime NOON = LocalTime.of(12, 0);

  @Nested
  @DisplayName("Testes do método validateNoOverlap")
  class ValidationChecks {

    @Test
    @DisplayName("Deve lançar exceção quando os intervalos se sobrepõem")
    void validateNoOverlap_shouldThrowException_whenIntervalsOverlap() {
      TimeInterval interval = new TimeInterval(TEN_AM, NOON);
      TimeInterval overlapping = new TimeInterval(ELEVEN_AM, LocalTime.of(13, 30));

      assertThatThrownBy(() -> interval.validateNoOverlapWith(overlapping))
          .isInstanceOf(InvalidTimeIntervalException.class)
          .satisfies(
              throwable ->
                  assertThat(((InvalidTimeIntervalException) throwable).getErrorCode())
                      .isEqualTo(ErrorCode.TIME_INTERVAL_OVERLAP));
    }

    @Test
    @DisplayName("Não deve lançar exceção quando os intervalos não se sobrepõem")
    void validateNoOverlap_shouldNotThrow_whenIntervalsAreSeparated() {
      TimeInterval interval = new TimeInterval(TEN_AM, ELEVEN_AM);
      TimeInterval nonOverlapping = new TimeInterval(LocalTime.of(12, 30), LocalTime.of(14, 0));

      assertThatCode(() -> interval.validateNoOverlapWith(nonOverlapping))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Não deve lançar exceção quando os intervalos são adjacentes")
    void validateNoOverlap_shouldNotThrow_whenIntervalsAreAdjacent() {
      TimeInterval interval = new TimeInterval(TEN_AM, ELEVEN_AM);
      TimeInterval adjacent = new TimeInterval(ELEVEN_AM, NOON);

      assertThatCode(() -> interval.validateNoOverlapWith(adjacent)).doesNotThrowAnyException();
      assertThatCode(() -> adjacent.validateNoOverlapWith(interval)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve lançar exceção quando o intervalo informado for nulo")
    void validateNoOverlap_shouldThrowException_whenOtherIsNull() {
      TimeInterval interval = new TimeInterval(TEN_AM, NOON);

      assertThatThrownBy(() -> interval.validateNoOverlapWith(null))
          .isInstanceOf(InvalidTimeIntervalException.class)
          .satisfies(
              throwable ->
                  assertThat(((InvalidTimeIntervalException) throwable).getErrorCode())
                      .isEqualTo(ErrorCode.TIME_INTERVAL_REQUIRED));
    }
  }
}
