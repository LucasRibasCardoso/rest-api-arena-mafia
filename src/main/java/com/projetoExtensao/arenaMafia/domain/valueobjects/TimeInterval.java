package com.projetoExtensao.arenaMafia.domain.valueobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOffsetMinutesException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidTimeIntervalException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.TimeIntervalOverlapException;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import java.time.Duration;
import java.time.LocalTime;

public record TimeInterval(
    @JsonProperty("startTime") @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonProperty("endTime") @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

  public TimeInterval {
    if (startTime == null || endTime == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }

    if (startTime.equals(endTime)) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_SAME_TIME);
    }

    try {
      OffsetMinutes.fromValue(startTime.getMinute());
      OffsetMinutes.fromValue(endTime.getMinute());
    } catch (InvalidOffsetMinutesException e) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_INVALID_MINUTES);
    }

    validateDuration(startTime, endTime);
  }

  /**
   * Valida se este intervalo de tempo não se sobrepõe a outro intervalo fornecido. Considera
   * intervalos que atravessam a meia-noite.
   *
   * @param other O outro intervalo de tempo a ser comparado.
   * @throws InvalidTimeIntervalException se o outro intervalo for nulo.
   * @throws TimeIntervalOverlapException se os intervalos se sobrepõem.
   */
  public void validateNoOverlapWith(TimeInterval other) {
    if (other == null) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_REQUIRED);
    }

    boolean overlaps = checkOverlap(this, other);

    if (overlaps) {
      throw new TimeIntervalOverlapException(ErrorCode.TIME_INTERVAL_OVERLAP);
    }
  }

  /**
   * Verifica se um horário específico está contido dentro deste intervalo de tempo. O intervalo é
   * considerado inclusivo no início e exclusivo no fim [startTime, endTime). Considera intervalos
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
      // Se atravessa a meia-noite: o horário está contido se for >= startTime OU < endTime
      return !timeToTest.isBefore(startTime) || timeToTest.isBefore(endTime);
    } else {
      // Não atravessa a meia-noite: validação normal
      return !timeToTest.isBefore(startTime) && timeToTest.isBefore(endTime);
    }
  }

  /**
   * Verifica se este intervalo se sobrepõe a outro. Wrapper público seguro para o metodo privado
   * checkOverlap.
   */
  public boolean overlaps(TimeInterval other) {
    if (other == null) {
      return false;
    }
    return checkOverlap(this, other);
  }

  /**
   * Verifica se outro intervalo está completamente contido dentro deste intervalo.
   *
   * <p>Um intervalo está contido se seu horário de início está dentro deste intervalo
   * (usando a semântica de {@link #contains(LocalTime)}) e seu horário de término
   * não ultrapassa o endTime deste intervalo.
   *
   * <p>O endTime do intervalo interno pode coincidir com o endTime deste intervalo
   * (limite inclusivo no fim para esta verificação).
   *
   * <p>Considera intervalos que atravessam a meia-noite.
   *
   * <p>Exemplo: [08:00-12:00] contém [09:00-11:00] e [08:00-12:00], mas não contém [07:00-11:00].
   *
   * @param other O intervalo a ser verificado se está contido.
   * @return true se o intervalo fornecido está completamente contido neste intervalo.
   */
  public boolean containsInterval(TimeInterval other) {
    if (other == null) {
      return false;
    }

    // O startTime do inner deve estar contido neste intervalo
    if (!this.contains(other.startTime())) {
      return false;
    }

    // Se os endTimes são iguais, está contido
    if (other.endTime().equals(this.endTime)) {
      return true;
    }

    // Se o inner atravessa meia-noite mas o outer não, impossível conter
    if (other.crossesMidnight() && !this.crossesMidnight()) {
      return false;
    }

    // Verifica se o endTime do inner não ultrapassa o endTime do outer
    return this.contains(other.endTime()) || other.endTime().equals(this.endTime);
  }

  private void validateDuration(LocalTime startTime, LocalTime endTime) {
    long durationInMinutes = calculateDurationInMinutes(startTime, endTime);

    if (durationInMinutes >= 24 * 60) {
      throw new InvalidTimeIntervalException(ErrorCode.TIME_INTERVAL_EXCEEDS_24_HOURS);
    }
  }

  private boolean crossesMidnight() {
    return endTime.isBefore(startTime);
  }

  private long calculateDurationInMinutes(LocalTime startTime, LocalTime endTime) {
    if (endTime.isAfter(startTime)) {
      return Duration.between(startTime, endTime).toMinutes();
    } else {
      long minutesUntilMidnight = Duration.between(startTime, LocalTime.MAX).toMinutes() + 1;
      long minutesFromMidnight = Duration.between(LocalTime.MIN, endTime).toMinutes();
      return minutesUntilMidnight + minutesFromMidnight;
    }
  }

  private boolean checkOverlap(TimeInterval interval1, TimeInterval interval2) {
    boolean interval1CrossesMidnight = interval1.crossesMidnight();
    boolean interval2CrossesMidnight = interval2.crossesMidnight();

    // Ambos os intervalos não atravessam a meia-noite
    if (!interval1CrossesMidnight && !interval2CrossesMidnight) {
      return (interval1.startTime.isBefore(interval2.endTime))
          && (interval2.startTime.isBefore(interval1.endTime));
    }

    // Apenas o primeiro intervalo atravessa a meia-noite
    if (interval1CrossesMidnight && !interval2CrossesMidnight) {
      return !(interval2.startTime().isAfter(interval1.endTime())
          && interval2.endTime().isBefore(interval1.startTime()));
    }

    // Apenas o segundo intervalo atravessa a meia-noite
    if (!interval1CrossesMidnight) {
      return !(interval1.startTime.isAfter(interval2.endTime)
          && interval1.endTime.isBefore(interval2.startTime));
    }

    // Ambos os intervalos atravessam a meia-noite e sempre se sobrepõem
    return true;
  }
}
