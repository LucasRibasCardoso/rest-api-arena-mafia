package com.projetoExtensao.arenaMafia.unit.config;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class TestModalityDataProvider {

  private TestModalityDataProvider() {}

  public static final String defaultName = "Futebol";

  public static Modality createDefaultModality() {
    return ModalityBuilder.defaultModality().build();
  }

  public static Modality createModalityWithName(String name) {
    return ModalityBuilder.defaultModality().withName(name).build();
  }

  public static Modality createModalityWithId(UUID id) {
    return ModalityBuilder.defaultModality().withId(id).build();
  }

  public static class ModalityBuilder {
    private UUID id = UUID.randomUUID();
    private String name = defaultName;
    private Instant createdAt = Instant.now();

    public static ModalityBuilder defaultModality() {
      return new ModalityBuilder();
    }

    public ModalityBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public ModalityBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ModalityBuilder withCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Modality build() {
      return Modality.reconstitute(id, name, createdAt);
    }
  }

  public static Stream<Arguments> invalidModalityNameProvider() {
    return Stream.of(
        Arguments.of(null, ErrorCode.MODALITY_NAME_REQUIRED),
        Arguments.of("", ErrorCode.MODALITY_NAME_REQUIRED),
        Arguments.of("AB", ErrorCode.MODALITY_NAME_INVALID_LENGTH),
        Arguments.of("A".repeat(150), ErrorCode.MODALITY_NAME_INVALID_LENGTH));
  }

  public static Stream<Arguments> modalityUpdateScenariosProvider() {
    return Stream.of(
        Arguments.of("Futebol", "Futebol de Campo"),
        Arguments.of("Vôlei", "Vôlei de Praia"),
        Arguments.of("Basquete", "Basquete 3x3"));
  }
}
