package com.projetoExtensao.arenaMafia.domain.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidOffsetMinutesException;

public enum OffsetMinutes {
  ZERO(0),
  THIRTY(30);

  private final int value;

  OffsetMinutes(int value) {
    this.value = value;
  }

  @JsonValue
  public int getValue() {
    return value;
  }

  /**
   * Converte um valor inteiro para o enum correspondente. Usado pelo Jackson para desserialização
   * de JSON.
   *
   * @param value O valor em minutos (0 ou 30)
   * @return O enum correspondente
   * @throws InvalidOffsetMinutesException se o valor não for 0 ou 30
   */
  @JsonCreator
  public static OffsetMinutes fromValue(int value) {
    for (OffsetMinutes offset : OffsetMinutes.values()) {
      if (offset.value == value) {
        return offset;
      }
    }
    throw new InvalidOffsetMinutesException();
  }
}
