package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.AdminCourtResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.court.InvalidCourtNameProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AdminCourtController")
public class AdminCourtControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private ObjectMapper objectMapper;
  @Autowired private CourtRepositoryPort courtRepository;
  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/courts")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistAdminUser();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endopoint POST /api/admin/courts")
  class CreateCourtTests {

    @Nested
    @DisplayName("Cenários de sucesso - 201 Created")
    class CreateCourtSuccessScenarios {
      @Test
      @DisplayName("Deve criar com sucesso uma quadra com offset 0")
      void create_shouldReturn201_whenCourtIsCreatedSuccessfully() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String name = "Quadra A";
        String description = "Quadra de Futebol";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request = new CreateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .as(AdminCourtResponseDto.class);
        // Assert
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.description()).isEqualTo(description);
        assertThat(response.offsetMinutes()).isEqualTo(offsetMinutes.getValue());
        assertThat(response.isActive()).isTrue();
        assertThat(response.modalities()).hasSize(1);
        assertThat(response.modalities().get(0).id()).isEqualTo(modality.getId());
        assertThat(response.modalities().get(0).name()).isEqualTo(modality.getName());
      }

      @Test
      @DisplayName("Deve criar com sucesso uma quadra com offset 30")
      void create_shouldReturn201_whenCourtIsCreatedSuccessfully_withOffset30() {
        // Arrange
        var modality = mockPersistModality("Basquete");
        String name = "Quadra B";
        String description = "Quadra de Basquete";
        OffsetMinutes offsetMinutes = OffsetMinutes.THIRTY;
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request = new CreateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .as(AdminCourtResponseDto.class);

        // Assert
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.description()).isEqualTo(description);
        assertThat(response.offsetMinutes()).isEqualTo(offsetMinutes.getValue());
        assertThat(response.isActive()).isTrue();
        assertThat(response.modalities()).hasSize(1);
        assertThat(response.modalities().get(0).id()).isEqualTo(modality.getId());
        assertThat(response.modalities().get(0).name()).isEqualTo(modality.getName());
      }

      @Test
      @DisplayName("Deve criar com sucesso uma quadra sem descrição")
      void create_shouldReturn201Created_whenCourtIsCreatedSuccessfully_withoutDescription() {
        // Arrange
        var modality = mockPersistModality("Vôlei");
        String name = "Quadra C";
        String description = null;
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request = new CreateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .as(AdminCourtResponseDto.class);

        // Assert
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.description()).isNull();
        assertThat(response.offsetMinutes()).isEqualTo(offsetMinutes.getValue());
        assertThat(response.isActive()).isTrue();
        assertThat(response.modalities()).hasSize(1);
        assertThat(response.modalities().get(0).id()).isEqualTo(modality.getId());
        assertThat(response.modalities().get(0).name()).isEqualTo(modality.getName());
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 400 Bad Request")
    class CreateCourtFailureScenarios {
      @InvalidCourtNameProvider
      @DisplayName("Tenta criar uma quadra com nome inválido")
      void create_shouldReturn400BadRequest_whenInvalidData(
          String invalidName, String invalidErrorCode) {
        // Arrange
        var modality = mockPersistModality("Tênis");
        String description = "Quadra de Tênis";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request =
            new CreateCourtRequestDto(invalidName, description, offsetMinutes, modalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.valueOf(invalidErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar uma quadra com offsetMinutes inválido")
      void create_shouldReturn400BadRequest_whenOffsetMinutesIsInvalid()
          throws JsonProcessingException {
        // Arrange
        String name = "Quadra A - Atualizada";
        String description = "Quadra de Futebol - Atualizada";
        Set<UUID> modalityIds = Set.of(UUID.randomUUID());
        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        request.put("description", description);
        request.put("offsetMinutes", "45");
        request.put("modalityIds", modalityIds.toString());
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(jsonRequest)
                .when()
                .post()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode error = ErrorCode.OFFSET_MINUTES_INVALID;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("offsetMinutes")
                        && fieldError.errorCode().equals(error.name())
                        && fieldError.developerMessage().equals(error.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar uma quadra com offsetMinutes nulo")
      void create_shouldReturn400BadRequest_whenOffsetMinutesIsNull() {
        // Arrange
        var modality = mockPersistModality("Handebol");
        String name = "Quadra D";
        String description = "Quadra de Handebol";
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request = new CreateCourtRequestDto(name, description, null, modalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("offsetMinutes")
                        && fieldError.errorCode().equals(ErrorCode.OFFSET_MINUTES_REQUIRED.name())
                        && fieldError
                            .developerMessage()
                            .equals(ErrorCode.OFFSET_MINUTES_REQUIRED.getMessage()));
      }

      @ParameterizedTest
      @ValueSource(strings = {"null", "[]"})
      @DisplayName("Tenta criar uma quadra sem informar nenhuma modalidade")
      void create_shouldReturn400BadRequest_whenModalityIdsIsNullOrEmpty(String modalityIdsValue) {
        // Arrange
        String jsonRequest =
            """
            {
              "name": "Quadra D",
              "description": "Quadra de Handebol",
              "offsetMinutes": 0,
              "modalityIds": %s
            }
            """
                .formatted(modalityIdsValue);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(jsonRequest)
                .when()
                .post()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("modalityIds")
                        && fieldError.errorCode().equals(ErrorCode.COURT_MODALITY_REQUIRED.name())
                        && fieldError
                            .developerMessage()
                            .equals(ErrorCode.COURT_MODALITY_REQUIRED.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 404 Not Found")
    class CreateCourtNotFoundScenarios {
      @Test
      @DisplayName("Tenta criar uma quadra quando uma das modalidades não existir")
      void create_shouldReturn404NotFound_whenOneOfTheModalitiesDoesNotExist() {
        // Arrange
        UUID nonExistentModalityId = UUID.randomUUID();
        String name = "Quadra F";
        String description = "Quadra de Rugby";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(nonExistentModalityId);
        var request = new CreateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 409 Conflict")
    class CreateCourtConflictScenarios {
      @Test
      @DisplayName("Tenta criar uma quadra com nome já existente")
      void create_shouldReturn409Conflict_whenCourtWithSameNameAlreadyExists() {
        // Arrange
        var modality = mockPersistModality("Rugby");
        String name = "Quadra E";
        String description = "Quadra de Rugby";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(modality.getId());
        var request = new CreateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        mockPersistCourt(name, description, offsetMinutes, Set.of(modality));

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_ALREADY_EXISTS;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/courts");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PUT /api/admin/courts/{courtId}")
  class UpdateCourtTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class UpdateCourtSuccessScenarios {
      @Test
      @DisplayName("Deve atualizar com sucesso uma quadra")
      void update_shouldReturn200_whenCourtIsUpdatedSuccessfully() {
        // Arrange
        var modality1 = mockPersistModality("Futebol");
        var modality2 = mockPersistModality("Basquete");
        String originalName = "Quadra A";
        String originalDescription = "Quadra de Futebol";
        OffsetMinutes originalOffset = OffsetMinutes.ZERO;
        Court court =
            mockPersistCourt(originalName, originalDescription, originalOffset, Set.of(modality1));

        String updatedName = "Quadra A - Atualizada";
        String updatedDescription = "Quadra de Futebol - Atualizada";
        OffsetMinutes updatedOffset = OffsetMinutes.THIRTY;
        Set<UUID> updatedModalityIds = Set.of(modality1.getId(), modality2.getId());
        var updateRequest =
            new UpdateCourtRequestDto(
                updatedName, updatedDescription, updatedOffset, updatedModalityIds);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(updateRequest)
                .when()
                .put("/{courtId}", court.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(AdminCourtResponseDto.class);

        // Assert
        assertThat(response.name()).isEqualTo(updatedName);
        assertThat(response.description()).isEqualTo(updatedDescription);
        assertThat(response.offsetMinutes()).isEqualTo(updatedOffset.getValue());
        assertThat(response.isActive()).isTrue();
        assertThat(response.modalities()).hasSize(2);
        assertThat(response.modalities())
            .anyMatch(
                modalityResponse ->
                    modalityResponse.id().equals(modality1.getId())
                        && modalityResponse.name().equals(modality1.getName()));
        assertThat(response.modalities())
            .anyMatch(
                modalityResponse ->
                    modalityResponse.id().equals(modality2.getId())
                        && modalityResponse.name().equals(modality2.getName()));
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 400 Bad Request")
    class UpdateCourtFailScenarios {
      @Test
      @DisplayName("Tenta atualizar uma quadra com nome inválido")
      void update_shouldReturn400BadRequest_whenInvalidName() {
        // Arrange
        UUID courtId = UUID.randomUUID();
        String description = "Quadra de Futebol - Atualizada";
        OffsetMinutes offsetMinutes = OffsetMinutes.THIRTY;
        Set<UUID> modalityIds = Set.of(UUID.randomUUID());
        var updateRequest =
            new UpdateCourtRequestDto("AB", description, offsetMinutes, modalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(updateRequest)
                .when()
                .put("/{courtId}", courtId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode error = ErrorCode.COURT_NAME_INVALID_LENGTH;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + courtId);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(error.name())
                        && fieldError.developerMessage().equals(error.getMessage()));
      }

      @Test
      @DisplayName("Tenta atualizar uma quadra sem informar nenhuma modalidade")
      void update_shouldReturn400BadRequest_whenModalityIdsIsEmpty() {
        // Arrange
        UUID courtId = UUID.randomUUID();
        String name = "Quadra A - Atualizada";
        String description = "Quadra de Futebol - Atualizada";
        OffsetMinutes offsetMinutes = OffsetMinutes.THIRTY;
        var updateRequest = new UpdateCourtRequestDto(name, description, offsetMinutes, Set.of());

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(updateRequest)
                .when()
                .put("/{courtId}", courtId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode error = ErrorCode.COURT_MODALITY_REQUIRED;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + courtId);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("modalityIds")
                        && fieldError.errorCode().equals(error.name())
                        && fieldError.developerMessage().equals(error.getMessage()));
      }

      @Test
      @DisplayName("Tenta atualizar uma quadra com offsetMinutes inválido")
      void update_shouldReturn400BadRequest_whenOffsetMinutesIsInvalid()
          throws JsonProcessingException {
        // Arrange
        UUID courtId = UUID.randomUUID();
        String name = "Quadra A - Atualizada";
        String description = "Quadra de Futebol - Atualizada";
        Set<UUID> modalityIds = Set.of(UUID.randomUUID());
        Map<String, String> request = new HashMap<>();
        request.put("name", name);
        request.put("description", description);
        request.put("offsetMinutes", "45");
        request.put("modalityIds", modalityIds.toString());
        String jsonRequest = objectMapper.writeValueAsString(request);
        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(jsonRequest)
                .when()
                .put("/{courtId}", courtId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode error = ErrorCode.OFFSET_MINUTES_INVALID;
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + courtId);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("offsetMinutes")
                        && fieldError.errorCode().equals(error.name())
                        && fieldError.developerMessage().equals(error.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 404 Not Found")
    class UpdateCourtNotFoundScenarios {
      @Test
      @DisplayName("Tenta atualizar uma quadra que não existe")
      void update_shouldReturn404NotFound_whenCourtDoesNotExist() {
        // Arrange
        UUID nonExistentCourtId = UUID.randomUUID();
        String name = "Quadra X";
        String description = "Quadra de Tênis";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Set<UUID> modalityIds = Set.of(UUID.randomUUID());
        var updateRequest =
            new UpdateCourtRequestDto(name, description, offsetMinutes, modalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(updateRequest)
                .when()
                .put("/{courtId}", nonExistentCourtId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + nonExistentCourtId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta atualizar uma quadra quando uma das modalidades não existir")
      void update_shouldReturn404NotFound_whenOneOfTheModalitiesDoesNotExist() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String originalName = "Quadra A";
        String originalDescription = "Quadra de Futebol";
        OffsetMinutes originalOffset = OffsetMinutes.ZERO;
        Court court =
            mockPersistCourt(originalName, originalDescription, originalOffset, Set.of(modality));

        UUID nonExistentModalityId = UUID.randomUUID();
        String newName = "Quadra A - Atualizada";
        String newDescription = "Quadra de Futebol - Atualizada";
        OffsetMinutes newOffset = OffsetMinutes.THIRTY;
        Set<UUID> newModalityIds = Set.of(modality.getId(), nonExistentModalityId);
        var request = new UpdateCourtRequestDto(newName, newDescription, newOffset, newModalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .put("/{courtId}", court.getId())
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + court.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 409 Conflict")
    class UpdateCourtConflictScenarios {
      @Test
      @DisplayName("Tenta atualizar uma quadra com nome já existente")
      void update_shouldReturn409Conflict_whenCourtWithSameNameAlreadyExists() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String existingName = "Quadra Existente";
        String existingDescription = "Quadra de Futebol";
        OffsetMinutes existingOffset = OffsetMinutes.ZERO;
        mockPersistCourt(existingName, existingDescription, existingOffset, Set.of(modality));

        String originalName = "Quadra A";
        String originalDescription = "Quadra de Basquete";
        OffsetMinutes originalOffset = OffsetMinutes.ZERO;
        Court court =
            mockPersistCourt(originalName, originalDescription, originalOffset, Set.of(modality));

        String newDescription = "Nova Descrição";
        OffsetMinutes newOffset = OffsetMinutes.THIRTY;
        Set<UUID> newModalityIds = Set.of(modality.getId());
        var request =
            new UpdateCourtRequestDto(existingName, newDescription, newOffset, newModalityIds);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .put("/{courtId}", court.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_ALREADY_EXISTS;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + court.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/courts/{courtId}/enable")
  class EnableCourtTests {
    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class EnableCourtSuccessScenarios {
      @Test
      @DisplayName("Deve habilitar com sucesso uma quadra")
      void enable_shouldReturn204NoContent_whenCourtIsEnabledSuccessfully() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String name = "Quadra A";
        String description = "Quadra de Futebol";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Court court = mockPersistCourt(name, description, offsetMinutes, Set.of(modality), false);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{courtId}/enable", court.getId())
            .then()
            .statusCode(204);

        Court enabledCourt = courtRepository.findById(court.getId()).orElseThrow();
        assertThat(enabledCourt.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 400 Bad Request")
    class EnableCourtFailScenarios {
      @Test
      @DisplayName("Tenta habilitar uma quadra que não existe")
      void enable_shouldReturn404NotFound_whenCourtDoesNotExist() {
        // Arrange
        UUID nonExistentCourtId = UUID.randomUUID();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{courtId}/enable", nonExistentCourtId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/courts/" + nonExistentCourtId + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 409 Conflict")
    class EnableCourtConflictScenarios {
      @Test
      @DisplayName("Tenta ativar uma quadra que já está ativada")
      void enable_shouldReturn409Conflict_whenCourtIsAlreadyEnabled() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String name = "Quadra A";
        String description = "Quadra de Futebol";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Court court = mockPersistCourt(name, description, offsetMinutes, Set.of(modality), true);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{courtId}/enable", court.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_ALREADY_ENABLED;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + court.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/courts/{courtId}/disable")
  class DisableCourtTests {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class DisableCourtSuccessScenarios {
      @Test
      @DisplayName("Deve desabilitar com sucesso uma quadra")
      void disable_shouldReturn204NoContent_whenCourtIsDisabledSuccessfully() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String name = "Quadra A";
        String description = "Quadra de Futebol";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Court court = mockPersistCourt(name, description, offsetMinutes, Set.of(modality), true);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{courtId}/disable", court.getId())
            .then()
            .statusCode(204);

        Court disabledCourt = courtRepository.findById(court.getId()).orElseThrow();
        assertThat(disabledCourt.isActive()).isFalse();
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 404 Not Found")
    class DisableCourtFailScenarios {
      @Test
      @DisplayName("Tenta desabilitar uma quadra que não existe")
      void disable_shouldReturn404NotFound_whenCourtDoesNotExist() {
        // Arrange
        UUID nonExistentCourtId = UUID.randomUUID();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{courtId}/disable", nonExistentCourtId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/courts/" + nonExistentCourtId + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de falha - 409 Conflict")
    class DisableCourtConflictScenarios {
      @Test
      @DisplayName("Tenta desabilitar uma quadra que já está desabilitada")
      void disable_shouldReturn409Conflict_whenCourtIsAlreadyDisabled() {
        // Arrange
        var modality = mockPersistModality("Futebol");
        String name = "Quadra A";
        String description = "Quadra de Futebol";
        OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
        Court court = mockPersistCourt(name, description, offsetMinutes, Set.of(modality), false);

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{courtId}/disable", court.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_ALREADY_DISABLED;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/courts/" + court.getId() + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/courts")
  class GetCourtTests {

    @Test
    @DisplayName("Deve retornar 200 OK e listar todas as quadras")
    void getAll_shouldReturn200AndListAllCourts() {
      // Arrange
      var modality1 = mockPersistModality("Futebol");
      var modality2 = mockPersistModality("Basquete");

      Court court1 =
          mockPersistCourt("Quadra A", "Quadra de Futebol", OffsetMinutes.ZERO, Set.of(modality1));
      Court court2 =
          mockPersistCourt(
              "Quadra B", "Quadra de Basquete", OffsetMinutes.THIRTY, Set.of(modality2));
      Court court3 =
          mockPersistCourt(
              "Quadra C",
              "Quadra de Futebol e Basquete",
              OffsetMinutes.ZERO,
              Set.of(modality1, modality2));

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(AdminCourtResponseDto[].class);

      // Assert
      assertThat(response).hasSize(3);

      List<AdminCourtResponseDto> responseList = Arrays.asList(response);

      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court1.getId())
                      && courtResponse.name().equals(court1.getName())
                      && courtResponse.description().equals(court1.getDescription())
                      && courtResponse.offsetMinutes() == court1.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court1.isActive()
                      && courtResponse.modalities().size() == 1
                      && courtResponse.modalities().get(0).id().equals(modality1.getId())
                      && courtResponse.modalities().get(0).name().equals(modality1.getName()));

      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court2.getId())
                      && courtResponse.name().equals(court2.getName())
                      && courtResponse.description().equals(court2.getDescription())
                      && courtResponse.offsetMinutes() == court2.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court2.isActive()
                      && courtResponse.modalities().size() == 1
                      && courtResponse.modalities().get(0).id().equals(modality2.getId())
                      && courtResponse.modalities().get(0).name().equals(modality2.getName()));
      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court3.getId())
                      && courtResponse.name().equals(court3.getName())
                      && courtResponse.description().equals(court3.getDescription())
                      && courtResponse.offsetMinutes() == court3.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court3.isActive()
                      && courtResponse.modalities().size() == 2
                      && courtResponse.modalities().stream()
                          .anyMatch(
                              modalityResponse ->
                                  modalityResponse.id().equals(modality1.getId())
                                      && modalityResponse.name().equals(modality1.getName()))
                      && courtResponse.modalities().stream()
                          .anyMatch(
                              modalityResponse ->
                                  modalityResponse.id().equals(modality2.getId())
                                      && modalityResponse.name().equals(modality2.getName())));
    }

    @Test
    @DisplayName("Deve retornar 200 OK e listar todas as quadras ativas")
    void getAll_shouldReturn200AndListAllActiveCourts() {
      // Arrange
      var modality1 = mockPersistModality("Futebol");
      var modality2 = mockPersistModality("Basquete");

      Court court1 =
          mockPersistCourt(
              "Quadra A", "Quadra de Futebol", OffsetMinutes.ZERO, Set.of(modality1), true);
      mockPersistCourt(
          "Quadra B", "Quadra de Basquete", OffsetMinutes.THIRTY, Set.of(modality2), false);
      Court court3 =
          mockPersistCourt(
              "Quadra C",
              "Quadra de Futebol e Basquete",
              OffsetMinutes.ZERO,
              Set.of(modality1, modality2),
              true);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", true)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(AdminCourtResponseDto[].class);

      // Assert
      assertThat(response).hasSize(2);

      List<AdminCourtResponseDto> responseList = Arrays.asList(response);

      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court1.getId())
                      && courtResponse.name().equals(court1.getName())
                      && courtResponse.description().equals(court1.getDescription())
                      && courtResponse.offsetMinutes() == court1.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court1.isActive()
                      && courtResponse.modalities().size() == 1
                      && courtResponse.modalities().get(0).id().equals(modality1.getId())
                      && courtResponse.modalities().get(0).name().equals(modality1.getName()));

      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court3.getId())
                      && courtResponse.name().equals(court3.getName())
                      && courtResponse.description().equals(court3.getDescription())
                      && courtResponse.offsetMinutes() == court3.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court3.isActive()
                      && courtResponse.modalities().size() == 2
                      && courtResponse.modalities().stream()
                          .anyMatch(
                              modalityResponse ->
                                  modalityResponse.id().equals(modality1.getId())
                                      && modalityResponse.name().equals(modality1.getName()))
                      && courtResponse.modalities().stream()
                          .anyMatch(
                              modalityResponse ->
                                  modalityResponse.id().equals(modality2.getId())
                                      && modalityResponse.name().equals(modality2.getName())));
    }

    @Test
    @DisplayName("Deve retornar 200 OK e listar todas as quadras inativas")
    void getAll_shouldReturn200AndListAllInactiveCourts() {
      // Arrange
      var modality1 = mockPersistModality("Futebol");
      var modality2 = mockPersistModality("Basquete");

      mockPersistCourt(
          "Quadra A", "Quadra de Futebol", OffsetMinutes.ZERO, Set.of(modality1), true);
      Court court2 =
          mockPersistCourt(
              "Quadra B", "Quadra de Basquete", OffsetMinutes.THIRTY, Set.of(modality2), false);
      mockPersistCourt(
          "Quadra C",
          "Quadra de Futebol e Basquete",
          OffsetMinutes.ZERO,
          Set.of(modality1, modality2),
          true);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", false)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(AdminCourtResponseDto[].class);

      // Assert
      assertThat(response).hasSize(1);

      List<AdminCourtResponseDto> responseList = Arrays.asList(response);

      assertThat(responseList)
          .anyMatch(
              courtResponse ->
                  courtResponse.id().equals(court2.getId())
                      && courtResponse.name().equals(court2.getName())
                      && courtResponse.description().equals(court2.getDescription())
                      && courtResponse.offsetMinutes() == court2.getOffsetMinutes().getValue()
                      && courtResponse.isActive() == court2.isActive()
                      && courtResponse.modalities().size() == 1
                      && courtResponse.modalities().stream()
                          .anyMatch(
                              modalityResponse ->
                                  modalityResponse.id().equals(modality2.getId())
                                      && modalityResponse.name().equals(modality2.getName())));
    }

    @Test
    @DisplayName("Deve retornar 200 OK e uma lista vazia quando não houver quadras cadastradas")
    void getAll_shouldReturn200AndListAllCourts_whenNoCourtIsRegistered() {

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(AdminCourtResponseDto[].class);

      // Assert
      assertThat(response).isEmpty();
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/courts/{courtId}")
  class GetCourtByIdTests {

    @Test
    @DisplayName("Deve retornar 200 OK e buscar uma quadra pelo ID")
    void getById_shouldReturn200AndFindCourtById() {
      // Arrange
      var modality1 = mockPersistModality("Futebol");
      var modality2 = mockPersistModality("Basquete");
      String name = "Quadra A";
      String description = "Quadra de Futebol e Basquete";
      OffsetMinutes offsetMinutes = OffsetMinutes.ZERO;
      Court court =
          mockPersistCourt(name, description, offsetMinutes, Set.of(modality1, modality2), true);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get("/{courtId}", court.getId())
              .then()
              .statusCode(200)
              .extract()
              .as(AdminCourtResponseDto.class);

      // Assert
      assertThat(response.id()).isEqualTo(court.getId());
      assertThat(response.name()).isEqualTo(court.getName());
      assertThat(response.description()).isEqualTo(court.getDescription());
      assertThat(response.offsetMinutes()).isEqualTo(court.getOffsetMinutes().getValue());
      assertThat(response.isActive()).isEqualTo(court.isActive());
      assertThat(response.modalities()).hasSize(2);
      assertThat(response.modalities())
          .anyMatch(
              modalityResponse ->
                  modalityResponse.id().equals(modality1.getId())
                      && modalityResponse.name().equals(modality1.getName()));
      assertThat(response.modalities())
          .anyMatch(
              modalityResponse ->
                  modalityResponse.id().equals(modality2.getId())
                      && modalityResponse.name().equals(modality2.getName()));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a quadra não existir")
    void getById_shouldReturn404NotFound_whenCourtDoesNotExist() {
      // Arrange
      UUID nonExistentCourtId = UUID.randomUUID();

      // Act & Assert
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get("/{courtId}", nonExistentCourtId)
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/admin/courts/" + nonExistentCourtId);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }
}
