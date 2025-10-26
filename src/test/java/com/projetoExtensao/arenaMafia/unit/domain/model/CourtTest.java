package com.projetoExtensao.arenaMafia.unit.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.CourtModalityRequiredException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.CourtOffsetRequiredException;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidCourtNameException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.CourtStatusConflictException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.unit.config.TestCourtDataProvider;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Testes unitários para entidade Court")
public class CourtTest {

  private final String defaultName = TestCourtDataProvider.defaultName;
  private final String defaultDescription = TestCourtDataProvider.defaultDescription;
  private final OffsetMinutes defaultOffsetMinutes = TestCourtDataProvider.defaultOffsetMinutes;
  private final Set<UUID> defaultModalityIds = TestCourtDataProvider.defaultModalityIds;

  @Nested
  @DisplayName("Testes para os Factory Methods")
  class FactoryMethodTests {

    @Test
    @DisplayName("create() deve criar uma quadra com valores padrão corretos")
    void create_shouldCreateCourtSuccessfully() {
      // Arrange
      Instant startTime = Instant.now();

      // Act
      Court court =
          Court.create(defaultName, defaultDescription, defaultOffsetMinutes, defaultModalityIds);

      // Assert
      assertThat(court).isNotNull();
      assertThat(court.getId()).isNotNull();
      assertThat(court.getName()).isEqualTo(defaultName);
      assertThat(court.getDescription()).isEqualTo(defaultDescription);
      assertThat(court.getOffsetMinutes()).isEqualTo(defaultOffsetMinutes);
      assertThat(court.isActive()).isTrue();
      assertThat(court.getModalityIds()).hasSize(2).containsAll(defaultModalityIds);
      assertThat(court.getCreatedAt()).isAfterOrEqualTo(startTime).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName(
        "reconstitute() deve reconstituir uma quadra com sucesso a partir de dados existentes")
    void reconstitute_shouldRebuildCourtSuccessfully() {
      // Arrange
      UUID id = UUID.randomUUID();
      String name = "Quadra 1";
      String description = "Quadra ao ar livre";
      OffsetMinutes offsetMinutes = OffsetMinutes.THIRTY;
      boolean isActive = false;
      Set<UUID> modalityIds = Set.of(UUID.randomUUID());
      Instant createdAt = Instant.now().minusSeconds(3600);

      // Act
      Court court =
          Court.reconstitute(
              id, name, description, offsetMinutes, isActive, modalityIds, createdAt);

      // Assert
      assertThat(court.getId()).isEqualTo(id);
      assertThat(court.getName()).isEqualTo(name);
      assertThat(court.getDescription()).isEqualTo(description);
      assertThat(court.getOffsetMinutes()).isEqualTo(offsetMinutes);
      assertThat(court.isActive()).isFalse();
      assertThat(court.getModalityIds()).containsExactlyInAnyOrderElementsOf(modalityIds);
      assertThat(court.getCreatedAt()).isEqualTo(createdAt);
    }

    @Nested
    @DisplayName("Cenários de Falha (Validações)")
    class Failure {

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestCourtDataProvider#invalidCourtNameProvider")
      @DisplayName("create() deve lançar InvalidCourtNameException para nomes inválidos")
      void create_shouldThrowException_whenNameIsInvalidUsingProvider(
          String invalidName, ErrorCode errorCode) {
        // Act & Assert
        assertThatThrownBy(
                () ->
                    Court.create(
                        invalidName, defaultDescription, defaultOffsetMinutes, defaultModalityIds))
            .isInstanceOf(InvalidCourtNameException.class)
            .satisfies(
                ex -> {
                  InvalidCourtNameException exception = (InvalidCourtNameException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @ParameterizedTest
      @MethodSource(
          "com.projetoExtensao.arenaMafia.unit.config.TestCourtDataProvider#invalidModalityIdsProvider")
      @DisplayName("create() deve lançar CourtModalityRequiredException para modalityIds inválidos")
      void create_shouldThrowException_whenModalityIdsIsInvalidUsingProvider(
          Set<UUID> invalidModalitiesId, ErrorCode errorCode) {

        // Act & Assert
        assertThatThrownBy(
                () ->
                    Court.create(
                        defaultName, defaultDescription, defaultOffsetMinutes, invalidModalitiesId))
            .isInstanceOf(CourtModalityRequiredException.class)
            .satisfies(
                ex -> {
                  CourtModalityRequiredException exception = (CourtModalityRequiredException) ex;
                  assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                });
      }

      @Test
      @DisplayName("create() deve lançar CourtOffsetRequiredException quando offsetMinutes é null")
      void create_shouldThrowException_whenOffsetMinutesIsNull() {
        // Act & Assert
        assertThatThrownBy(
                () -> Court.create(defaultName, defaultDescription, null, defaultModalityIds))
            .isInstanceOf(CourtOffsetRequiredException.class);
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
      Court court = TestCourtDataProvider.createActiveCourt();
      String newName = "Nova Quadra";

      // Act
      court.updateName(newName);

      // Assert
      assertThat(court.getName()).isEqualTo(newName);
    }

    @Test
    @DisplayName("updateName() deve lançar InvalidCourtNameException para nome inválido")
    void updateName_shouldThrowException_whenNameIsInvalid() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      String invalidName = "AB";

      // Act & Assert
      assertThatThrownBy(() -> court.updateName(invalidName))
          .isInstanceOf(InvalidCourtNameException.class)
          .satisfies(
              ex -> {
                InvalidCourtNameException exception = (InvalidCourtNameException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURT_NAME_INVALID_LENGTH);
              });
    }

    @Test
    @DisplayName("updateName() não deve fazer nada quando o nome é null")
    void updateName_shouldNotUpdate_whenNameIsNull() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      String originalName = court.getName();

      // Act
      court.updateName(null);

      // Assert
      assertThat(court.getName()).isEqualTo(originalName);
    }

    @Test
    @DisplayName("updateDescription() deve alterar a descrição com um valor válido")
    void updateDescription_shouldUpdateDescription_whenValid() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      String newDescription = "Nova descrição da quadra";

      // Act
      court.updateDescription(newDescription);

      // Assert
      assertThat(court.getDescription()).isEqualTo(newDescription);
    }

    @Test
    @DisplayName("updateDescription() não deve fazer nada quando a descrição é null")
    void updateDescription_shouldNotUpdate_whenDescriptionIsNull() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      String originalDescription = court.getDescription();

      // Act
      court.updateDescription(null);

      // Assert
      assertThat(court.getDescription()).isEqualTo(originalDescription);
    }

    @ParameterizedTest
    @EnumSource(OffsetMinutes.class)
    @DisplayName("updateOffsetMinutes() deve alterar o offsetMinutes com um valor válido")
    void updateOffsetMinutes_shouldUpdateOffsetMinutes_whenValid(OffsetMinutes newOffset) {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();

      // Act
      court.updateOffsetMinutes(newOffset);

      // Assert
      assertThat(court.getOffsetMinutes()).isEqualTo(newOffset);
    }

    @Test
    @DisplayName("updateOffsetMinutes() não deve fazer nada quando offsetMinutes é null")
    void updateOffsetMinutes_shouldNotUpdate_whenOffsetMinutesIsNull() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      OffsetMinutes originalOffset = court.getOffsetMinutes();

      // Act
      court.updateOffsetMinutes(null);

      // Assert
      assertThat(court.getOffsetMinutes()).isEqualTo(originalOffset);
    }

    @Test
    @DisplayName("replaceModalityIds() deve substituir os IDs das modalidades com sucesso")
    void replaceModalityIds_shouldReplaceModalityIds_whenValid() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      Set<UUID> newModalityIds = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

      // Act
      court.replaceModalityIds(newModalityIds);

      // Assert
      assertThat(court.getModalityIds())
          .hasSize(3)
          .containsExactlyInAnyOrderElementsOf(newModalityIds);
    }

    @Test
    @DisplayName(
        "replaceModalityIds() deve lançar CourtModalityRequiredException quando modalityIds é null")
    void replaceModalityIds_shouldThrowException_whenModalityIdsIsNull() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();

      // Act & Assert
      assertThatThrownBy(() -> court.replaceModalityIds(null))
          .isInstanceOf(CourtModalityRequiredException.class);
    }

    @Test
    @DisplayName(
        "replaceModalityIds() deve lançar CourtModalityRequiredException quando modalityIds está vazio")
    void replaceModalityIds_shouldThrowException_whenModalityIdsIsEmpty() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();

      // Act & Assert
      assertThatThrownBy(() -> court.replaceModalityIds(Collections.emptySet()))
          .isInstanceOf(CourtModalityRequiredException.class);
    }

    @Test
    @DisplayName("getModalityIds() deve retornar uma cópia imutável")
    void getModalityIds_shouldReturnUnmodifiableSet() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();

      // Act
      Set<UUID> modalityIds = court.getModalityIds();

      // Assert
      assertThatThrownBy(() -> modalityIds.add(UUID.randomUUID()))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("Testes para Gerenciamento de Status (enable/disable)")
  class StatusManagementTests {

    @Test
    @DisplayName("disable() deve desativar uma quadra ativa")
    void disable_shouldDisableCourt_whenCourtIsActive() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      assertThat(court.isActive()).isTrue();

      // Act
      court.disable();

      // Assert
      assertThat(court.isActive()).isFalse();
    }

    @Test
    @DisplayName(
        "disable() deve lançar CourtStatusConflictException quando a quadra já está desativada")
    void disable_shouldThrowException_whenCourtIsAlreadyDisabled() {
      // Arrange
      Court court = TestCourtDataProvider.createDisabledCourt();
      assertThat(court.isActive()).isFalse();

      // Act & Assert
      assertThatThrownBy(court::disable)
          .isInstanceOf(CourtStatusConflictException.class)
          .satisfies(
              ex -> {
                CourtStatusConflictException exception = (CourtStatusConflictException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURT_ALREADY_DISABLED);
              });
    }

    @Test
    @DisplayName("enable() deve ativar uma quadra desativada")
    void enable_shouldEnableCourt_whenCourtIsDisabled() {
      // Arrange
      Court court = TestCourtDataProvider.createDisabledCourt();
      assertThat(court.isActive()).isFalse();

      // Act
      court.enable();

      // Assert
      assertThat(court.isActive()).isTrue();
    }

    @Test
    @DisplayName(
        "enable() deve lançar CourtStatusConflictException quando a quadra já está ativada")
    void enable_shouldThrowException_whenCourtIsAlreadyEnabled() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      assertThat(court.isActive()).isTrue();

      // Act & Assert
      assertThatThrownBy(court::enable)
          .isInstanceOf(CourtStatusConflictException.class)
          .satisfies(
              ex -> {
                CourtStatusConflictException exception = (CourtStatusConflictException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURT_ALREADY_ENABLED);
              });
    }
  }

  @Nested
  @DisplayName("Testes para Métodos de Validação Estáticos")
  class StaticValidationTests {

    @Test
    @DisplayName("validateName() não deve lançar exceção para um nome válido")
    void validateName_shouldNotThrowException_whenNameIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> Court.validateName("Quadra Válida"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("validateName() deve lançar InvalidCourtNameException para nomes inválidos")
    void validateName_shouldThrowException_whenNameIsInvalid(String invalidName) {
      // Act & Assert
      assertThatThrownBy(() -> Court.validateName(invalidName))
          .isInstanceOf(InvalidCourtNameException.class)
          .satisfies(
              ex -> {
                InvalidCourtNameException exception = (InvalidCourtNameException) ex;
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COURT_NAME_REQUIRED);
              });
    }

    @Test
    @DisplayName("validateModalityIds() não deve lançar exceção para modalityIds válidos")
    void validateModalityIds_shouldNotThrowException_whenModalityIdsIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> Court.validateModalityIds(Set.of(UUID.randomUUID())));
    }

    @Test
    @DisplayName(
        "validateModalityIds() deve lançar CourtModalityRequiredException quando modalityIds é null")
    void validateModalityIds_shouldThrowException_whenModalityIdsIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> Court.validateModalityIds(null))
          .isInstanceOf(CourtModalityRequiredException.class);
    }

    @Test
    @DisplayName("validateOffsetMinutes() não deve lançar exceção para offsetMinutes válido")
    void validateOffsetMinutes_shouldNotThrowException_whenOffsetMinutesIsValid() {
      // Act & Assert
      assertDoesNotThrow(() -> Court.validateOffsetMinutes(OffsetMinutes.ZERO));
    }

    @Test
    @DisplayName(
        "validateOffsetMinutes() deve lançar CourtOffsetRequiredException quando offsetMinutes é null")
    void validateOffsetMinutes_shouldThrowException_whenOffsetMinutesIsNull() {
      // Act & Assert
      assertThatThrownBy(() -> Court.validateOffsetMinutes(null))
          .isInstanceOf(CourtOffsetRequiredException.class);
    }
  }

  @Nested
  @DisplayName("Testes de Integração de Cenários Complexos")
  class ComplexScenarioTests {

    @Test
    @DisplayName("Deve permitir múltiplas atualizações sequenciais em uma quadra")
    void shouldAllowMultipleSequentialUpdates() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();

      // Act
      court.updateName("Quadra Atualizada");
      court.updateDescription("Nova descrição");
      court.updateOffsetMinutes(OffsetMinutes.THIRTY);
      Set<UUID> newModalityIds = Set.of(UUID.randomUUID());
      court.replaceModalityIds(newModalityIds);
      court.disable();

      // Assert
      assertThat(court.getName()).isEqualTo("Quadra Atualizada");
      assertThat(court.getDescription()).isEqualTo("Nova descrição");
      assertThat(court.getOffsetMinutes()).isEqualTo(OffsetMinutes.THIRTY);
      assertThat(court.getModalityIds()).containsExactlyInAnyOrderElementsOf(newModalityIds);
      assertThat(court.isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve manter os dados originais após falha de validação em update")
    void shouldKeepOriginalDataAfterValidationFailure() {
      // Arrange
      Court court = TestCourtDataProvider.createActiveCourt();
      String originalName = court.getName();
      Set<UUID> originalModalityIds = new HashSet<>(court.getModalityIds());

      // Act & Assert
      assertThatThrownBy(() -> court.updateName("AB"))
          .isInstanceOf(InvalidCourtNameException.class);
      assertThat(court.getName()).isEqualTo(originalName);

      assertThatThrownBy(() -> court.replaceModalityIds(Collections.emptySet()))
          .isInstanceOf(CourtModalityRequiredException.class);
      assertThat(court.getModalityIds()).containsExactlyInAnyOrderElementsOf(originalModalityIds);
    }

    @Test
    @DisplayName("Deve criar uma quadra com o nome no limite mínimo de caracteres")
    void shouldCreateCourt_withMinimumNameLength() {
      // Arrange
      String minLengthName = "ABC";

      // Act
      Court court =
          Court.create(minLengthName, defaultDescription, defaultOffsetMinutes, defaultModalityIds);

      // Assert
      assertThat(court.getName()).isEqualTo(minLengthName);
    }

    @Test
    @DisplayName("Deve criar uma quadra com o nome no limite máximo de caracteres")
    void shouldCreateCourt_withMaximumNameLength() {
      // Arrange
      String maxLengthName = "A".repeat(100);

      // Act
      Court court =
          Court.create(maxLengthName, defaultDescription, defaultOffsetMinutes, defaultModalityIds);

      // Assert
      assertThat(court.getName()).isEqualTo(maxLengthName);
    }
  }
}
