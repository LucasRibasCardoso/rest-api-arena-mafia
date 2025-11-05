package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidModalityNameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ModalityStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.unit.config.TestModalityDataProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Testes unitários para entidade Modality")
public class ModalityTest {

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("create() deve criar uma modalidade com valores padrão corretos")
    void create_shouldCreateModalitySuccessfully() {
      // Arrange
      Instant startTime = Instant.now();
      String name = "Futebol";

      // Act
      Modality modality = Modality.create(name);

      // Assert
      assertThat(modality).isNotNull();
      assertThat(modality.getId()).isNotNull();
      assertThat(modality.getName()).isEqualTo(name);
      assertThat(modality.isActive()).isTrue();
      assertThat(modality.getCreatedAt())
          .isAfterOrEqualTo(startTime)
          .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName(
        "reconstitute() deve reconstituir uma modalidade com sucesso a partir de dados existentes")
    void reconstitute_shouldRebuildModalitySuccessfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      String name = "Vôlei";
      boolean isActive = true;
      Instant createdAt = Instant.now().minusSeconds(3600);

      // Act
      Modality modality = Modality.reconstitute(id, name, isActive, createdAt);

      // Assert
      assertThat(modality.getId()).isEqualTo(id);
      assertThat(modality.getName()).isEqualTo(name);
      assertThat(modality.isActive()).isEqualTo(isActive);
      assertThat(modality.getCreatedAt()).isEqualTo(createdAt);
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestModalityDataProvider#invalidModalityNameProvider")
      @DisplayName("create() deve lançar InvalidModalityNameFormatException para nomes inválidos")
      void create_shouldThrowException_whenNameIsInvalidUsingProvider(
          String invalidName, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(() -> Modality.create(invalidName))
            .isInstanceOf(InvalidModalityNameFormatException.class)
            .satisfies(
                ex -> {
                  InvalidModalityNameFormatException exception =
                      (InvalidModalityNameFormatException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @NullAndEmptySource
      @ValueSource(strings = {"  ", "\t", "\n"})
      @DisplayName(
          "create() deve lançar InvalidModalityNameFormatException para nomes vazios ou nulos")
      void create_shouldThrowException_whenNameIsNullOrBlank(String invalidName) {
        // Act & Assert
        assertThatThrownBy(() -> Modality.create(invalidName))
            .isInstanceOf(InvalidModalityNameFormatException.class)
            .satisfies(
                ex -> {
                  InvalidModalityNameFormatException exception =
                      (InvalidModalityNameFormatException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MODALITY_NAME_REQUIRED);
                });
      }

      @Test
      @DisplayName(
          "create() deve lançar InvalidModalityNameFormatException para nomes muito longos")
      void create_shouldThrowException_whenNameIsTooLong() {
        // Arrange
        String longName = "A".repeat(101);

        // Act & Assert
        assertThatThrownBy(() -> Modality.create(longName))
            .isInstanceOf(InvalidModalityNameFormatException.class)
            .satisfies(
                ex -> {
                  InvalidModalityNameFormatException exception =
                      (InvalidModalityNameFormatException) ex;
                  assertThat(exception.getErrorCode())
                      .isEqualTo(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
                });
      }
    }
  }

  @Nested
  @DisplayName("Testes para os Métodos de Atualização (update...)")
  class AttributeUpdateTests {

    @Test
    @DisplayName("updateName() deve alterar o nome com um valor válido")
    void updateName_shouldUpdateName_whenValid() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      String newName = "Basquete";

      // Act
      modality.updateName(newName);

      // Assert
      assertThat(modality.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateName() deve lançar InvalidModalityNameFormatException para nome inválido")
    void updateName_shouldThrowException_whenNameIsInvalid() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      String invalidName = "AB";

      // Act & Assert
      assertThatThrownBy(() -> modality.updateName(invalidName))
          .isInstanceOf(InvalidModalityNameFormatException.class)
          .satisfies(
              ex -> {
                InvalidModalityNameFormatException exception =
                    (InvalidModalityNameFormatException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
              });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("updateName() deve lançar exceção quando o nome é null ou vazio")
    void updateName_shouldThrowException_whenNameIsNullOrEmpty(String invalidName) {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      String originalName = modality.getName();

      // Act & Assert
      assertThatThrownBy(() -> modality.updateName(invalidName))
          .isInstanceOf(InvalidModalityNameFormatException.class);
      assertThat(modality.getName()).isEqualTo(originalName);
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestModalityDataProvider#modalityUpdateScenariosProvider")
    @DisplayName("updateName() deve permitir múltiplas atualizações sequenciais")
    void updateName_shouldAllowSequentialUpdates(String firstName, String secondName) {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();

      // Act
      modality.updateName(firstName);
      assertThat(modality.getName()).isEqualTo(firstName);

      modality.updateName(secondName);

      // Assert
      assertThat(modality.getName()).isEqualTo(secondName);
    }
  }

  @Nested
  @DisplayName("Testes para Soft Delete (disable/enable)")
  class SoftDeleteTests {

    @Test
    @DisplayName("disable() deve desativar uma modalidade ativa")
    void disable_shouldDeactivateActiveModality() {
      // Arrange
      Modality modality = TestModalityDataProvider.createActiveModality();
      assertThat(modality.isActive()).isTrue();

      // Act
      modality.disable();

      // Assert
      assertThat(modality.isActive()).isFalse();
    }

    @Test
    @DisplayName("disable() deve lançar exceção ao tentar desativar modalidade já inativa")
    void disable_shouldThrowException_whenModalityAlreadyDisabled() {
      // Arrange
      Modality modality = TestModalityDataProvider.createInactiveModality();
      assertThat(modality.isActive()).isFalse();

      // Act & Assert
      assertThatThrownBy(modality::disable)
          .isInstanceOf(ModalityStatusConflictException.class)
          .satisfies(
              ex -> {
                ModalityStatusConflictException exception = (ModalityStatusConflictException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MODALITY_ALREADY_DISABLE);
              });
    }

    @Test
    @DisplayName("disable() não deve afetar outros atributos da modalidade")
    void disable_shouldNotAffectOtherAttributes() {
      // Arrange
      Modality modality = TestModalityDataProvider.createActiveModality();
      UUID originalId = modality.getId();
      String originalName = modality.getName();
      Instant originalCreatedAt = modality.getCreatedAt();

      // Act
      modality.disable();

      // Assert
      assertThat(modality.getId()).isEqualTo(originalId);
      assertThat(modality.getName()).isEqualTo(originalName);
      assertThat(modality.getCreatedAt()).isEqualTo(originalCreatedAt);
      assertThat(modality.isActive()).isFalse();
    }
  }

  @Nested
  @DisplayName("Testes para Métodos de Validação Estáticos")
  class StaticValidationTests {

    @Test
    @DisplayName("validateName() não deve lançar exceção para um nome válido")
    void validateName_shouldNotThrowException_whenNameIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> Modality.validateName("Futebol"));
    }

    @ParameterizedTest
    @MethodSource(
        "com.projetoExtensao.arenaMafia.unit.config.TestModalityDataProvider#invalidModalityNameProvider")
    @DisplayName(
        "validateName() deve lançar InvalidModalityNameFormatException para nomes inválidos")
    void validateName_shouldThrowException_whenNameIsInvalid(
        String invalidName, ErrorCode errorCode) {
      // Act & Assert
      assertThatThrownBy(() -> Modality.validateName(invalidName))
          .isInstanceOf(InvalidModalityNameFormatException.class)
          .satisfies(
              ex -> {
                InvalidModalityNameFormatException exception =
                    (InvalidModalityNameFormatException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(errorCode);
              });
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "AB"})
    @DisplayName(
        "validateName() deve lançar InvalidModalityNameFormatException para nomes muito curtos")
    void validateName_shouldThrowException_whenNameIsTooShort(String shortName) {
      // Act & Assert
      assertThatThrownBy(() -> Modality.validateName(shortName))
          .isInstanceOf(InvalidModalityNameFormatException.class)
          .satisfies(
              ex -> {
                InvalidModalityNameFormatException exception =
                    (InvalidModalityNameFormatException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
              });
    }

    @Test
    @DisplayName(
        "validateName() deve lançar InvalidModalityNameFormatException para nomes muito longos")
    void validateName_shouldThrowException_whenNameIsTooLong() {
      // Arrange
      String longName = "A".repeat(101);

      // Act & Assert
      assertThatThrownBy(() -> Modality.validateName(longName))
          .isInstanceOf(InvalidModalityNameFormatException.class)
          .satisfies(
              ex -> {
                InvalidModalityNameFormatException exception =
                    (InvalidModalityNameFormatException) ex;
                assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
              });
    }
  }

  @Nested
  @DisplayName("Testes para equals() e hashCode()")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals() deve retornar true para modalidades com o mesmo ID")
    void equals_shouldReturnTrue_whenModalitiesHaveSameId() {
      // Arrange
      UUID id = UUID.randomUUID();
      Modality modality1 = Modality.reconstitute(id, "Futebol", true, Instant.now());
      Modality modality2 = Modality.reconstitute(id, "Vôlei", true, Instant.now());

      // Act & Assert
      assertThat(modality1).isEqualTo(modality2);
    }

    @Test
    @DisplayName("equals() deve retornar false para modalidades com IDs diferentes")
    void equals_shouldReturnFalse_whenModalitiesHaveDifferentIds() {
      // Arrange
      Modality modality1 = TestModalityDataProvider.createDefaultModality();
      Modality modality2 = TestModalityDataProvider.createDefaultModality();

      // Act & Assert
      assertThat(modality1).isNotEqualTo(modality2);
    }

    @Test
    @DisplayName("hashCode() deve retornar o mesmo valor para modalidades com o mesmo ID")
    void hashCode_shouldReturnSameValue_whenModalitiesHaveSameId() {
      // Arrange
      UUID id = UUID.randomUUID();
      Modality modality1 = Modality.reconstitute(id, "Futebol", true, Instant.now());
      Modality modality2 = Modality.reconstitute(id, "Vôlei", false, Instant.now());

      // Act & Assert
      assertThat(modality1.hashCode()).isEqualTo(modality2.hashCode());
    }

    @Test
    @DisplayName("equals() deve retornar false quando comparado com null")
    void equals_shouldReturnFalse_whenComparedWithNull() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();

      // Act & Assert
      assertThat(modality).isNotEqualTo(null);
    }

    @Test
    @DisplayName("equals() deve retornar false quando comparado com objeto de tipo diferente")
    void equals_shouldReturnFalse_whenComparedWithDifferentType() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      String differentType = "Not a Modality";

      // Act & Assert
      assertThat(modality).isNotEqualTo(differentType);
    }
  }

  @Nested
  @DisplayName("Testes de Integração de Cenários Complexos")
  class ComplexScenarioTests {

    @Test
    @DisplayName("Deve permitir múltiplas atualizações sequenciais em uma modalidade")
    void shouldAllowMultipleSequentialUpdates() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();

      // Act
      modality.updateName("Futebol de Campo");
      assertThat(modality.getName()).isEqualTo("Futebol de Campo");

      modality.updateName("Futebol Society");
      assertThat(modality.getName()).isEqualTo("Futebol Society");

      modality.updateName("Futsal");

      // Assert
      assertThat(modality.getName()).isEqualTo("Futsal");
    }

    @Test
    @DisplayName("Deve manter o nome original após falha de validação em update")
    void shouldKeepOriginalNameAfterValidationFailure() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      String originalName = modality.getName();

      // Act & Assert
      assertThatThrownBy(() -> modality.updateName("AB"))
          .isInstanceOf(InvalidModalityNameFormatException.class);
      assertThat(modality.getName()).isEqualTo(originalName);

      assertThatThrownBy(() -> modality.updateName(""))
          .isInstanceOf(InvalidModalityNameFormatException.class);
      assertThat(modality.getName()).isEqualTo(originalName);
    }

    @Test
    @DisplayName("Deve criar uma modalidade com o nome no limite mínimo de caracteres")
    void shouldCreateModality_withMinimumNameLength() {
      // Arrange
      String minLengthName = "ABC";

      // Act
      Modality modality = Modality.create(minLengthName);

      // Assert
      assertThat(modality.getName()).isEqualTo(minLengthName);
    }

    @Test
    @DisplayName("Deve criar uma modalidade com o nome no limite máximo de caracteres")
    void shouldCreateModality_withMaximumNameLength() {
      // Arrange
      String maxLengthName = "A".repeat(100);

      // Act
      Modality modality = Modality.create(maxLengthName);

      // Assert
      assertThat(modality.getName()).isEqualTo(maxLengthName);
    }

    @Test
    @DisplayName("Deve criar uma modalidade usando ModalityBuilder com customizações")
    void shouldCreateModalityUsingBuilder() {
      // Arrange
      UUID customId = UUID.randomUUID();
      String customName = "Beach Tennis";
      Instant customCreatedAt = Instant.now().minusSeconds(7200);

      // Act
      Modality modality =
          TestModalityDataProvider.ModalityBuilder.defaultModality()
              .withId(customId)
              .withName(customName)
              .withCreatedAt(customCreatedAt)
              .build();

      // Assert
      assertThat(modality.getId()).isEqualTo(customId);
      assertThat(modality.getName()).isEqualTo(customName);
      assertThat(modality.getCreatedAt()).isEqualTo(customCreatedAt);
    }

    @Test
    @DisplayName("Deve criar modalidades com helpers do TestModalityDataProvider")
    void shouldCreateModalitiesUsingHelpers() {
      // Act
      Modality defaultModality = TestModalityDataProvider.createDefaultModality();
      Modality customNameModality = TestModalityDataProvider.createModalityWithName("Tênis");
      UUID customId = UUID.randomUUID();
      Modality customIdModality = TestModalityDataProvider.createModalityWithId(customId);

      // Assert
      assertThat(defaultModality.getName()).isEqualTo(TestModalityDataProvider.defaultName);
      assertThat(customNameModality.getName()).isEqualTo("Tênis");
      assertThat(customIdModality.getId()).isEqualTo(customId);
    }

    @Test
    @DisplayName("Deve preservar o ID e createdAt após atualização do nome")
    void shouldPreserveIdAndCreatedAtAfterNameUpdate() {
      // Arrange
      Modality modality = TestModalityDataProvider.createDefaultModality();
      UUID originalId = modality.getId();
      Instant originalCreatedAt = modality.getCreatedAt();

      // Act
      modality.updateName("Novo Nome");

      // Assert
      assertThat(modality.getId()).isEqualTo(originalId);
      assertThat(modality.getCreatedAt()).isEqualTo(originalCreatedAt);
      assertThat(modality.getName()).isEqualTo("Novo Nome");
    }

    @Test
    @DisplayName("Deve reconstituir modalidade inativa corretamente")
    void shouldReconstituteInactiveModalityCorrectly() {
      // Arrange
      UUID id = UUID.randomUUID();
      String name = "Modalidade Antiga";
      boolean isActive = false;
      Instant createdAt = Instant.now().minusSeconds(86400);

      // Act
      Modality modality = Modality.reconstitute(id, name, isActive, createdAt);

      // Assert
      assertThat(modality.getId()).isEqualTo(id);
      assertThat(modality.getName()).isEqualTo(name);
      assertThat(modality.isActive()).isFalse();
      assertThat(modality.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("Deve criar modalidade com isActive = true por padrão usando Builder")
    void shouldCreateModalityWithDefaultActiveStatus() {
      // Act
      Modality modality =
          TestModalityDataProvider.ModalityBuilder.defaultModality()
              .withName("Nova Modalidade")
              .build();

      // Assert
      assertThat(modality.isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve permitir atualizar nome de modalidade inativa")
    void shouldAllowUpdateNameOfInactiveModality() {
      // Arrange
      Modality modality = TestModalityDataProvider.createInactiveModality();
      String newName = "Nome Atualizado";

      // Act
      modality.updateName(newName);

      // Assert
      assertThat(modality.getName()).isEqualTo(newName);
      assertThat(modality.isActive()).isFalse();
    }
  }
}
