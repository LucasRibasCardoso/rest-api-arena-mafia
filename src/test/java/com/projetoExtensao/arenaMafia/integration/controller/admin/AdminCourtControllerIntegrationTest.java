package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.court.port.gateway.CourtPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PreviewNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CourtDisableConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.CreateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.request.UpdateCourtRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response.AdminCourtResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.court.response.CourtDisablePreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.court.InvalidCourtNameProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Testes de integração para AdminCourtController")
public class AdminCourtControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private ReservationRepositoryPort reservationRepositoryPort;
  @Autowired private BlockedTimeRepositoryPort blockedTimeRepositoryPort;
  @Autowired private CourtPreviewCachePort courtDisablePreviewCachePort;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CourtRepositoryPort courtRepository;
  private RequestSpecification specification;
  private String accessToken;
  private UUID adminId;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/courts")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    User admin = mockPersistAdminUser();
    adminId = admin.getId();

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
  @DisplayName("Testes para a funcionalidade de desabilitação de quadras")
  class DisableCourtTests {

    @Nested
    @DisplayName("Testes para o endpoint POST /api/admin/courts/{courtId}/preview-disable")
    class PreviewDisableCourtTests {

      @Nested
      @DisplayName("Cenários de sucesso - 200 OK")
      class SuccessScenarios {

        @Test
        @DisplayName("Deve criar um preview de desabilitação de quadra sem agendamentos afetados")
        void previewDisable_shouldReturn200AndPreviewDisableCourtSuccessfully() {
          // Arrange
          Court court = mockPersistCourt("Quadra 1", mockPersistModality("Volei"));

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", court.getId())
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(CourtDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotNull();
          assertThat(response.courtId()).isEqualTo(court.getId());
          assertThat(response.courtName()).isEqualTo(court.getName());
          assertThat(response.usersAffectedCount()).isZero();
          assertThat(response.blockedTimesAffectedCount()).isZero();
          assertThat(response.reservationsAffectedCount()).isZero();

          Optional<CourtDisablePreview> previewOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewOpt).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewOpt.get());
        }

        @Test
        @DisplayName("Deve criar um preview de desabilitação de quadra com agendamentos afetados")
        void previewDisable_shouldReturn200AndPreviewDisableCourtWithAffectedBookings() {
          // Arrange
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user =
              mockPersistUser("test_username", "Username Test", "+55992052149", "password123");

          // Cria agendamentos que serão afetados
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().minusDays(1),
              new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0)),
              BigDecimal.valueOf(50),
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().plusDays(1),
              new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0)),
              BigDecimal.valueOf(50),
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().plusDays(365),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              BigDecimal.valueOf(80),
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().plusDays(5),
              new TimeInterval(LocalTime.of(9, 0), LocalTime.of(10, 0)),
              BigDecimal.valueOf(80),
              user.getId());
          mockPersistBlockedTimeSpecific(
              court.getId(),
              LocalDate.now().plusDays(9),
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(12, 0)),
              "Feriado Municipal",
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", court.getId())
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(CourtDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotNull();
          assertThat(response.courtId()).isEqualTo(court.getId());
          assertThat(response.courtName()).isEqualTo(court.getName());
          assertThat(response.usersAffectedCount()).isEqualTo(2);
          assertThat(response.blockedTimesAffectedCount()).isEqualTo(1);
          assertThat(response.reservationsAffectedCount()).isEqualTo(3);

          Optional<CourtDisablePreview> previewOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewOpt).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewOpt.get());
        }

        @Test
        @DisplayName(
            "Deve criar um preview de desabilitação de quadra com agendamentos afetados e reservas"
                + " em andamento")
        void previewDisable_shouldReturn200AndPreviewDisableCourtWithOngoingReservations() {
          // Arrange
          Modality modality = mockPersistModality("Futebol");
          Court court = mockPersistCourt("Quadra 1", modality);

          TimeInterval inProgressInterval =
              new TimeInterval(
                  normalizeToValidMinutes(LocalTime.now().minusMinutes(30)),
                  normalizeToValidMinutes(LocalTime.now().plusMinutes(30)));

          // Cria agendamentos que serão afetados, incluindo uma reserva em andamento
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now(),
              inProgressInterval,
              BigDecimal.valueOf(60),
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().plusDays(5),
              new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
              BigDecimal.valueOf(70),
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", court.getId())
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(CourtDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotNull();
          assertThat(response.courtId()).isEqualTo(court.getId());
          assertThat(response.courtName()).isEqualTo(court.getName());
          assertThat(response.usersAffectedCount()).isEqualTo(1);
          assertThat(response.blockedTimesAffectedCount()).isZero();
          assertThat(response.reservationsAffectedCount()).isEqualTo(1);
          assertThat(response.inProgressReservations().size()).isEqualTo(1);

          Optional<CourtDisablePreview> previewOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewOpt).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewOpt.get());
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 400 Bad Request")
      class BadRequestScenarios {

        @Test
        @DisplayName("Tenta criar um preview de desabilitação de quadra utilizando um ID inválido")
        void previewDisable_shouldReturn400BadRequest_whenCourtIdIsInvalid() {
          // Arrange
          String invalidCourtId = "invalid-uuid";

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", invalidCourtId)
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          String expectedPath = "/api/admin/courts/" + invalidCourtId + "/preview-disable";

          assertBusinessError(response, 400, expectedPath, ErrorCode.INVALID_REQUEST_PARAMETER);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 404 Not Found")
      class NotFoundScenarios {

        @Test
        @DisplayName("Tenta criar um preview de desabilitação de quadra que não existe")
        void previewDisable_shouldReturn404NotFound_whenCourtDoesNotExist() {
          // Arrange
          UUID nonExistentCourtId = UUID.randomUUID();

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", nonExistentCourtId)
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          String expectedPath = "/api/admin/courts/" + nonExistentCourtId + "/preview-disable";
          assertBusinessError(response, 404, expectedPath, ErrorCode.COURT_NOT_FOUND);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 409 Conflict")
      class ConflictScenarios {

        @Test
        @DisplayName("Tenta criar um preview de desabilitação de quadra que já está desabilitada")
        void previewDisable_shouldReturn409Conflict_whenCourtIsAlreadyDisabled() {
          // Arrange
          Court court = mockPersistCourt("Quadra 1", mockPersistModality("Volei"));

          // Desativa a quadra
          court.disable();
          courtRepository.save(court);

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{courtId}/preview-disable", court.getId())
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          String expectedPath = "/api/admin/courts/" + court.getId() + "/preview-disable";
          assertBusinessError(response, 409, expectedPath, ErrorCode.COURT_ALREADY_DISABLED);
        }
      }
    }

    @Nested
    @DisplayName("Teste para o endpoint POST /api/admin/courts/{courtId}/confirm-disable")
    class ConfirmDisableCourtTests {

      private static final String PATH_CONFIRM_DISABLE = "/api/admin/courts/confirm-disable";

      @Nested
      @DisplayName("Cenários de sucesso - 200 OK")
      class SuccessScenarios {

        @Test
        @DisplayName(
            "Deve confirmar a desabilitação de uma quadra com sucesso sem agendamentos afetados")
        void confirmDisable_shouldReturn200AndDisableCourtSuccessfully_withoutAffectedBookings() {
          // Arrange
          Court court = mockPersistCourt("Quadra 1", mockPersistModality("Volei"));
          String previewKey = createPreviewAndGetKey(court.getId());

          var request = new CourtDisableConfirmRequestDto(previewKey, "Desativação da Quadra");

          // Act
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .body(request)
              .post("/confirm-disable")
              .then()
              .statusCode(204);

          // Assert
          Court disabledCourt = courtRepository.findById(court.getId()).orElseThrow();
          assertThat(disabledCourt.isActive()).isFalse();

          Optional<CourtDisablePreview> previewOpt = getPreviewSavedFromCache(previewKey);
          assertThat(previewOpt).isNotPresent();
        }

        @Test
        @DisplayName(
            "Deve confirmar a desabilitação de uma quadra com sucesso com agendamentos afetados")
        void confirmDisable_shouldReturn200AndDisableCourtSuccessfully_withAffectedBookings() {
          // Arrange
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra 1", modality);

          // Cria reserva que será afetada
          Reservation affectedReservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  court.getId(),
                  LocalDate.now().plusDays(1),
                  new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                  BigDecimal.valueOf(50),
                  adminId);

          // Cria reserva em andamento que não deve ser afetada
          Reservation inProgressReservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  court.getId(),
                  LocalDate.now(),
                  new TimeInterval(
                      normalizeToValidMinutes(LocalTime.now().minusMinutes(30)),
                      normalizeToValidMinutes(LocalTime.now().plusMinutes(30))),
                  BigDecimal.valueOf(50),
                  adminId);

          // Cria blocked time que será afetado
          BlockedTime blockedTime =
              mockPersistBlockedTimeSpecific(
                  court.getId(),
                  LocalDate.now().plusDays(2),
                  new TimeInterval(LocalTime.of(10, 0), LocalTime.of(12, 0)),
                  "Manutenção",
                  adminId);

          String previewKey = createPreviewAndGetKey(court.getId());

          var request = new CourtDisableConfirmRequestDto(previewKey, "Desativação da Quadra");

          // Act
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .body(request)
              .post("/confirm-disable")
              .then()
              .log()
              .all()
              .statusCode(204);

          // Assert
          Court disabledCourt = courtRepository.findById(court.getId()).orElseThrow();
          assertThat(disabledCourt.isActive()).isFalse();

          Optional<CourtDisablePreview> previewOpt = getPreviewSavedFromCache(previewKey);
          assertThat(previewOpt).isNotPresent();

          // Verifica se a reserva afetada foi cancelada
          Reservation canceledReservation =
              reservationRepositoryPort.findByIdOrElseThrow(affectedReservation.getId());
          assertThat(canceledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

          // Verifica se a reserva em andamento não foi afetada
          Reservation ongoingReservation =
              reservationRepositoryPort.findByIdOrElseThrow(inProgressReservation.getId());
          assertThat(ongoingReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

          // Verifica se o blocked time afetado foi removido
          Optional<BlockedTime> removedBlockedTimeOpt =
              blockedTimeRepositoryPort.findById(blockedTime.getId());
          assertThat(removedBlockedTimeOpt).isNotPresent();
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 400 Bad Request")
      class BadRequestScenarios {

        @Test
        @DisplayName("Tenta confirmar a desabilitação de uma quadra sem informar a previewKey")
        void confirmDisable_shouldReturn400BadRequest_whenPreviewKeyIsMissing() {
          // Arrange
          var request = new CourtDisableConfirmRequestDto(null, "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertValidationError(
              response, PATH_CONFIRM_DISABLE, "previewKey", ErrorCode.PREVIEW_KEY_REQUIRED);
        }

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra informando uma previewKey inválida")
        void confirmDisable_shouldReturn400BadRequest_whenPreviewKeyIsInvalid() {
          // Arrange
          var request =
              new CourtDisableConfirmRequestDto("invalid-preview-key", "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertBusinessError(response, 400, PATH_CONFIRM_DISABLE, ErrorCode.PREVIEW_KEY_INVALID);
        }

        @Test
        @DisplayName("Tenta confirmar a desabilitação de uma quadra sem informar o motivo")
        void confirmDisable_shouldReturn400BadRequest_whenReasonIsMissing() {
          // Arrange
          String previewKey = courtDisablePreviewCachePort.generateKey(adminId);
          var request = new CourtDisableConfirmRequestDto(previewKey, null);

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertValidationError(
              response,
              PATH_CONFIRM_DISABLE,
              "description",
              ErrorCode.COURT_DISABLE_DESCRIPTION_REQUIRED);
        }

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra com motivo inválido (muito curto)")
        void confirmDisable_shouldReturn400BadRequest_whenReasonIsInvalid() {
          // Arrange
          String previewKey = courtDisablePreviewCachePort.generateKey(adminId);
          var request = new CourtDisableConfirmRequestDto(previewKey, "AB");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertValidationError(
              response,
              PATH_CONFIRM_DISABLE,
              "description",
              ErrorCode.COURT_DISABLE_DESCRIPTION_INVALID_LENGTH);
        }

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra com motivo inválido (muito longo)")
        void confirmDisable_shouldReturn400BadRequest_whenReasonIsTooLong() {
          // Arrange
          String previewKey = courtDisablePreviewCachePort.generateKey(adminId);
          String longReason = "A".repeat(501);
          var request = new CourtDisableConfirmRequestDto(previewKey, longReason);

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertValidationError(
              response,
              PATH_CONFIRM_DISABLE,
              "description",
              ErrorCode.COURT_DISABLE_DESCRIPTION_INVALID_LENGTH);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 403 Forbidden")
      class ForbiddenScenarios {

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra com a previewKey de outro administrador")
        void confirmDisable_shouldReturn403Forbidden_whenUsingAnotherAdminPreviewKey() {
          // Arrange
          User otherAdmin = mockPersistOtherAdminUser();
          AuthTokensTest otherAdminTokens = mockLogin(otherAdmin.getUsername(), defaultPassword);
          String otherAdminAccessToken = "Bearer " + otherAdminTokens.accessToken();

          Court court = mockPersistCourt("Quadra 1", mockPersistModality("Volei"));
          String previewKey = createPreviewAndGetKey(court.getId());

          var request = new CourtDisableConfirmRequestDto(previewKey, "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", otherAdminAccessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(403)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertBusinessError(
              response, 403, PATH_CONFIRM_DISABLE, ErrorCode.PREVIEW_KEY_OWNERSHIP_INVALID);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 404 Not Found")
      class NotFoundScenarios {

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra utilizando um previewKey expirada")
        void confirmDisable_shouldReturn404NotFound_whenPreviewKeyIsExpired() {
          // Arrange
          String expiredPreviewKey = courtDisablePreviewCachePort.generateKey(adminId);

          var request =
              new CourtDisableConfirmRequestDto(expiredPreviewKey, "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertBusinessError(response, 404, PATH_CONFIRM_DISABLE, ErrorCode.PREVIEW_NOT_FOUND);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 409 Conflict")
      class ConflictScenarios {

        @Test
        @DisplayName(
            "Tenta confirmar a desabilitação de uma quadra mas o preview está desatualizado")
        void confirmDisable_shouldReturn409Conflict_whenPreviewIsOutdated() {
          // Arrange
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra 1", modality);

          // Cria o preview de desabilitação
          String previewKey = createPreviewAndGetKey(court.getId());

          // Adiciona uma reserva que será afetada após a criação do preview
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().plusDays(2),
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              BigDecimal.valueOf(50),
              adminId);

          var request = new CourtDisableConfirmRequestDto(previewKey, "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertBusinessError(response, 409, PATH_CONFIRM_DISABLE, ErrorCode.PREVIEW_DATA_STALE);
        }

        @Test
        @DisplayName("Tenta confirmar a desabilitação para um quadra que já foi desabilitada")
        void confirmDisable_shouldReturn409_whenCourtIsAlreadyDisabled() {
          // Arrange
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra 1", modality);

          // Cria o preview de desabilitação
          String previewKey = createPreviewAndGetKey(court.getId());

          // Desabilita a quadra antes de confirmar
          court.disable();
          courtRepository.save(court);

          var request = new CourtDisableConfirmRequestDto(previewKey, "Desativação da Quadra");

          // Act & Assert
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .body(request)
                  .post("/confirm-disable")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          assertBusinessError(
              response, 409, PATH_CONFIRM_DISABLE, ErrorCode.COURT_ALREADY_DISABLED);
        }
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

  private void assertResponsePreviewMatchesPreviewSaved(
      CourtDisablePreviewResponseDto response, CourtDisablePreview previewSaved) {
    assertThat(response.courtId()).isEqualTo(previewSaved.courtId());
    assertThat(response.courtName()).isEqualTo(previewSaved.courtName());
    assertThat(response.usersAffectedCount()).isEqualTo(previewSaved.usersAffectedCount());
    assertThat(response.blockedTimesAffectedCount())
        .isEqualTo(previewSaved.blockedTimesAffectedCount());
    assertThat(response.reservationsAffectedCount())
        .isEqualTo(previewSaved.reservationsAffectedCount());

    assertThat(response.affectedBlockedTimes().size())
        .isEqualTo(previewSaved.affectedBlockedTimes().size());
    assertThat(response.affectedReservations().size())
        .isEqualTo(previewSaved.affectedReservations().size());
    assertThat(response.inProgressReservations().size())
        .isEqualTo(previewSaved.inProgressReservations().size());
  }

  private String createPreviewAndGetKey(UUID courtId) {
    var response =
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .post("/{courtId}/preview-disable", courtId)
            .then()
            .statusCode(200)
            .extract()
            .as(CourtDisablePreviewResponseDto.class);

    return response.previewKey();
  }

  private Optional<CourtDisablePreview> getPreviewSavedFromCache(String previewKey) {
    try {
      CourtDisablePreview previewSaved =
          courtDisablePreviewCachePort.getPreviewOrElseThrow(previewKey, adminId);
      return Optional.of(previewSaved);
    } catch (PreviewNotFoundException e) {
      return Optional.empty();
    }
  }
}
