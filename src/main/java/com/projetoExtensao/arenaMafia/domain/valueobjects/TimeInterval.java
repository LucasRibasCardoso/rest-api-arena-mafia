package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOffsetMinutesException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import java.time.Duration;
import java.time.LocalTime;

public record TimeInterval(
    @JsonProperty("openTime") LocalTime openTime, @JsonProperty("closeTime") LocalTime closeTime) {

  public TimeInterval {
    if (openTime == null || closeTime == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }

    if (openTime.equals(closeTime)) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_SAME_TIME);
    }

    try {
      OffsetMinutes.fromValue(openTime.getMinute());
      OffsetMinutes.fromValue(closeTime.getMinute());
    } catch (InvalidOffsetMinutesException e) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_INVALID_MINUTES);
    }

    validateDuration(openTime, closeTime);
  }

  /**
   * Valida se este intervalo de tempo não se sobrepõe a outro intervalo fornecido. Considera
   * intervalos que atravessam a meia-noite.
   *
   * @param other O outro intervalo de tempo a ser comparado.
   * @throws InvalidTimeIntervalException se o outro intervalo for nulo.
   * @throws InvalidTimeIntervalException se os intervalos se sobrepõem.
   */
  public void validateNoOverlapWith(TimeInterval other) {
    if (other == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }

    boolean overlaps = checkOverlap(this, other);

    if (overlaps) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_OVERLAP);
    }
  }

  /**
   * Verifica se um horário específico está contido dentro deste intervalo de tempo. O intervalo é
   * considerado inclusivo no início e exclusivo no fim [openTime, closeTime). Considera intervalos
   * que atravessam a meia-noite.
   *
   * <p>Exemplo: um intervalo de 09:00 a 10:00 contém 09:00 e 09:30, mas não contém 10:00.
   *
   * <p>Exemplo: um intervalo de 22:00 a 02:00 (atravessa meia-noite) contém 22:00, 23:00, 00:00,
   * 01:00, mas não contém 02:00.
   *
   * @param timeToTest O horário a ser verificado.
   * @return true se o horário estiver dentro do intervalo, false caso contrário.
   */
  public boolean contains(LocalTime timeToTest) {
    if (timeToTest == null) {
      return false;
    }

    if (crossesMidnight()) {
      // Se atravessa a meia-noite: o horário está contido se for >= openTime OU < closeTime
      return !timeToTest.isBefore(openTime) || timeToTest.isBefore(closeTime);
    } else {
      // Não atravessa a meia-noite: validação normal
      return !timeToTest.isBefore(openTime) && timeToTest.isBefore(closeTime);
    }
  }

  /**
   * Valida se a duração do intervalo não excede 24 horas.
   *
   * @param openTime Horário de abertura.
   * @param closeTime Horário de fechamento.
   * @throws InvalidTimeIntervalException se a duração exceder 24 horas.
   */
  private void validateDuration(LocalTime openTime, LocalTime closeTime) {
    long durationInMinutes = calculateDurationInMinutes(openTime, closeTime);

    if (durationInMinutes >= 24 * 60) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_EXCEEDS_24_HOURS);
    }
  }

  /**
   * Calcula a duração do intervalo em minutos, considerando se o intervalo atravessa a meia-noite.
   *
   * @param openTime Horário de abertura.
   * @param closeTime Horário de fechamento.
   * @return Duração do intervalo em minutos.
   */
  private long calculateDurationInMinutes(LocalTime openTime, LocalTime closeTime) {
    if (closeTime.isAfter(openTime)) {
      return Duration.between(openTime, closeTime).toMinutes();
    } else {
      long minutesUntilMidnight = Duration.between(openTime, LocalTime.MAX).toMinutes() + 1;
      long minutesFromMidnight = Duration.between(LocalTime.MIN, closeTime).toMinutes();
      return minutesUntilMidnight + minutesFromMidnight;
    }
  }

  /**
   * Verifica se o intervalo de tempo atravessa a meia-noite.
   *
   * @return true se atravessa a meia-noite, false caso contrário.
   */
  private boolean crossesMidnight() {
    return closeTime.isBefore(openTime);
  }

  /**
   * Verifica se dois intervalos de tempo se sobrepõem, considerando intervalos que atravessam a
   * meia-noite.
   *
   * @param interval1 Intervalo 1
   * @param interval2 Intervalo 2
   * @return true se os intervalos se sobrepõem, false caso contrário.
   */
  private boolean checkOverlap(TimeInterval interval1, TimeInterval interval2) {
    boolean interval1CrossesMidnight = interval1.crossesMidnight();
    boolean interval2CrossesMidnight = interval2.crossesMidnight();

    // Ambos os intervalos não atravessam a meia-noite
    if (!interval1CrossesMidnight && !interval2CrossesMidnight) {
      return (interval1.openTime.isBefore(interval2.closeTime))
          && (interval2.openTime.isBefore(interval1.closeTime));
    }

    // Apenas o primeiro intervalo atravessa a meia-noite
    if (interval1CrossesMidnight && !interval2CrossesMidnight) {
      return !(interval2.openTime().isAfter(interval1.closeTime())
          && interval2.closeTime().isBefore(interval1.openTime()));
    }

    // Apenas o segundo intervalo atravessa a meia-noite
    if (!interval1CrossesMidnight) {
      return !(interval1.openTime.isAfter(interval2.closeTime)
          && interval1.closeTime.isBefore(interval2.openTime));
    }

    // Ambos os intervalos atravessam a meia-noite e sempre se sobrepõem
    return true;
  }
}
