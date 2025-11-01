package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.operatingHours.ports.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateOperatingHoursRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.dayOfWeek.InvalidDaysOfWeekProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AdminOperatingHoursController")
public class AdminOperatingHoursControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private OperatingHoursRepositoryPort operatingHoursRepository;
  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/operating-hours")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistAdminUser();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/operating-hours")
  class CreateOperatingHoursTest {

    @Nested
    @DisplayName("Cenários de sucesso - 201 Created")
    class CreateOperatingHoursSuccessScenarios {
      @Test
      @DisplayName("Deve criar horário de funcionamento válido para um dia da semana")
      void shouldReturn201_whenCreatingValidOperatingHours() {
        // Arrange
        var daysOfWeek = Set.of(DayOfWeek.MONDAY);
        var timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
        var request = new CreateOperatingHoursRequestDto(daysOfWeek, timeInterval);

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
                .as(OperatingHoursResponseDto.class);

        // Act
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.daysOfWeek()).isEqualTo(daysOfWeek);
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve criar horário de funcionamento válido para múltiplos dias da semana")
      void shouldReturn201_whenCreatingValidOperatingHoursForMultipleDays() {
        // Arrange
        var daysOfWeek = Set.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
        var timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(22, 0));
        var request = new CreateOperatingHoursRequestDto(daysOfWeek, timeInterval);

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
                .as(OperatingHoursResponseDto.class);

        // Act
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.daysOfWeek()).isEqualTo(daysOfWeek);
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve criar horário de funcionamento para todos os dias da semana")
      void shouldReturn201_whenCreatingOperatingHoursForAllDaysOfWeek() {
        // Arrange
        var timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
        var request = new CreateOperatingHoursRequestDto(null, timeInterval);

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
                .as(OperatingHoursResponseDto.class);

        // Act
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.daysOfWeek()).isNull();
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class CreateOperatingHoursBadRequestScenarios {

      @Test
      @DisplayName("Tentar criar um horário de funcionamento com lista de dias da semana vazia")
      void shouldReturn400_whenProvidingEmptyDaysOfWeekList() {
        // Arrange
        var timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
        var request = new CreateOperatingHoursRequestDto(Set.of(), timeInterval);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(400)
                .log()
                .all()
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.DAY_OF_WEEK_EMPTY;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours");
      }

      @InvalidDaysOfWeekProvider
      @DisplayName("Tentar criar um horário de funcionamento com dias da semana inválidos")
      void shouldReturn400_whenProvidingInvalidDaysOfWeek(
          String[] invalidDay, String expectedErrorCode) {
        // Arrange
        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("daysOfWeek", invalidDay);
        jsonRequest.put("timeInterval", Map.of("startTime", "08:00", "endTime", "00:00"));

        // Act
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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours");

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("daysOfWeek")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tentar criar um horário de funcionamento com intervalo de tempo nulo")
      void shouldReturn400_whenProvidingNullTimeInterval() {
        var request = new CreateOperatingHoursRequestDto(Set.of(DayOfWeek.MONDAY), null);

        // Act
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
        ErrorCode errorCode = ErrorCode.TIME_INTERVAL_REQUIRED;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours");

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidTimeIntervalProvider
      @DisplayName("Tentar criar um horário de funcionamento com intervalo de tempo inválido")
      void shouldReturn400_whenProvidingInvalidTimeInterval(
          String startTime, String endTime, String expectedErrorCode) {

        Map<String, Object> timeInterval = new HashMap<>();
        timeInterval.put("startTime", startTime);
        timeInterval.put("endTime", endTime);

        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("daysOfWeek", Set.of("MONDAY", "TUESDAY"));
        jsonRequest.put("timeInterval", timeInterval);

        // Act
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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours");

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class CreateOperatingHoursConflictScenarios {
      @Test
      @DisplayName("Tentar criar um horário de funcionamento que sobrepõe outro existente")
      void shouldReturn409_whenCreatingOverlappingOperatingHours() {
        // Arrange
        var daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        var timeInterval1 = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));
        var request1 = new CreateOperatingHoursRequestDto(daysOfWeek, timeInterval1);

        // Cria o primeiro horário de funcionamento com sucesso
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .body(request1)
            .when()
            .post()
            .then()
            .statusCode(201);

        // Tenta criar um segundo horário que sobrepõe o primeiro
        var daysOfWeek2 = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
        var timeInterval2 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        var request2 = new CreateOperatingHoursRequestDto(daysOfWeek2, timeInterval2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request2)
                .when()
                .post()
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.TIME_INTERVAL_OVERLAP;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours");
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/operating-hours")
  class GetAllOperatingHoursTest {

    @Test
    @DisplayName("Deve retornar 200 OK com lista vazia quando não há horários de funcionamento")
    void shouldReturn200_whenThereAreNoOperatingHours() {
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
              .as(OperatingHoursResponseDto[].class);

      // Assert
      assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar 200 OK com lista de todos os horários de funcionamento")
    void shouldReturn200_whenGettingAllOperatingHours() {
      // Arrange
      mockPersistListOfOperatingHours();

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
              .as(OperatingHoursResponseDto[].class);

      // Assert
      assertThat(response).hasSize(4);
    }

    @Test
    @DisplayName(
        "Deve retornar 200 OK com lista de horários de funcionamento filtrados por isActive=true")
    void shouldReturn200_whenFilteringByActiveStatus() {
      // Arrange
      mockPersistListOfOperatingHours();

      // Act - Busca apenas ativos
      var activeResponse =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", true)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(OperatingHoursResponseDto[].class);

      // Assert
      assertThat(activeResponse).hasSize(3);
      assertThat(activeResponse).allMatch(OperatingHoursResponseDto::isActive);
    }

    @Test
    @DisplayName(
        "Deve retornar 200 OK com lista de horários de funcionamento filtrados por isActive=false")
    void shouldReturn200_whenFilteringByInactiveStatus() {
      // Arrange
      mockPersistListOfOperatingHours();

      // Act
      var inactiveResponse =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", false)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(OperatingHoursResponseDto[].class);

      // Assert
      assertThat(inactiveResponse).hasSize(1);
      assertThat(inactiveResponse).noneMatch(OperatingHoursResponseDto::isActive);
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/operating-hours/{hourId}")
  class GetOperatingHoursByIdTest {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class GetOperatingHoursByIdSuccessScenarios {
      @Test
      @DisplayName("Deve buscar horário de funcionamento por ID válido")
      void shouldReturn200_whenGettingOperatingHoursByValidId() {
        // Arrange
        var operatingHour = mockPersistOperatingHours();
        UUID operatingHourId = operatingHour.getId();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{hourId}", operatingHourId)
                .then()
                .statusCode(200)
                .extract()
                .as(OperatingHoursResponseDto.class);

        // Assert
        assertThat(response.id()).isEqualTo(operatingHourId);
        assertThat(response.daysOfWeek()).containsAll(operatingHour.getDaysOfWeek());
        assertThat(response.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class GetOperatingHoursByIdBadRequestScenarios {

      @Test
      @DisplayName("Tenta buscar um horário com ID inválido")
      void shouldReturn400_whenGettingOperatingHoursWithInvalidIdFormat() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{hourId}", "invalid-uuid")
                .then()
                .statusCode(400)
                .log()
                .all()
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours/invalid-uuid");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class GetOperatingHoursByIdNotFoundScenarios {

      @Test
      @DisplayName("Tenta buscar um horário de funcionamento com ID inexistente")
      void shouldReturn404_whenGettingOperatingHoursByInvalidId() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{hourId}", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours/" + nonExistentId);
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/operating-hours/{hourId}/enable")
  class EnableOperatingHoursTest {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class EnableOperatingHoursSuccessScenarios {
      @Test
      @DisplayName("Deve habilitar um horário de funcionamento desativado")
      void shouldReturn204_whenEnablingDisabledOperatingHours() {
        // Arrange
        var operatingHour = mockPersistDisabledOperatingHours();
        UUID operatingHourId = operatingHour.getId();

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .patch("/{hourId}/enable", operatingHourId)
            .then()
            .statusCode(204);

        // Assert
        var updatedOperatingHour = operatingHoursRepository.findByIdOrElseThrow(operatingHourId);
        assertThat(updatedOperatingHour.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class EnableOperatingHoursBadRequestScenarios {
      @Test
      @DisplayName("Tenta habilitar um horário com ID inválido")
      void shouldReturn400_whenEnablingWithInvalidIdFormat() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/enable", "invalid-uuid")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/operating-hours/invalid-uuid/enable");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class EnableOperatingHoursNotFoundScenarios {
      @Test
      @DisplayName("Tentar habilitar um horário inexistente")
      void shouldReturn404_whenEnablingNonExistentOperatingHours() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/enable", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path())
            .isEqualTo("/api/admin/operating-hours/" + nonExistentId + "/enable");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class EnableOperatingHoursConflictScenarios {
      @Test
      @DisplayName("Tenta habilitar um horário já habilitado")
      void shouldReturn409_whenEnablingAlreadyEnabledOperatingHours() {
        // Arrange
        var operatingHour = mockPersistOperatingHours();
        UUID operatingHourId = operatingHour.getId();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/enable", operatingHourId)
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_ALREADY_ENABLED;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path())
            .isEqualTo("/api/admin/operating-hours/" + operatingHourId + "/enable");
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/operating-hours/{hourId}/disable")
  class DisableOperatingHoursTest {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class DisableOperatingHoursSuccessScenarios {
      @Test
      @DisplayName("Tenta desabilitar um horário de funcionamento ativo")
      void shouldReturn204_whenDisablingActiveOperatingHours() {
        // Arrange
        var operatingHour = mockPersistOperatingHours();
        UUID operatingHourId = operatingHour.getId();

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .patch("/{hourId}/disable", operatingHourId)
            .then()
            .statusCode(204);

        // Assert
        var updatedOperatingHour = operatingHoursRepository.findByIdOrElseThrow(operatingHourId);
        assertThat(updatedOperatingHour.isActive()).isFalse();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class DisableOperatingHoursBadRequestScenarios {
      @Test
      @DisplayName("Tenta desabilitar um horário com ID inválido")
      void shouldReturn400_whenDisablingWithInvalidIdFormat() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/disable", "invalid-uuid")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).contains("/api/admin/operating-hours/invalid-uuid/disable");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NonExistentDisabling {
      @Test
      @DisplayName("Tenta desabilitar um horário inexistente")
      void shouldReturn404NotFound_whenDisablingNonExistentOperatingHours() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/disable", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path())
            .isEqualTo("/api/admin/operating-hours/" + nonExistentId + "/disable");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class ConflictingDisabling {
      @Test
      @DisplayName("Tenta desabilitar um horário já desabilitado")
      void shouldReturn409Conflict_whenDisablingAlreadyDisabledOperatingHours() {
        // Arrange
        var operatingHours = mockPersistDisabledOperatingHours();
        UUID operatingHoursId = operatingHours.getId();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{hourId}/disable", operatingHoursId)
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_ALREADY_DISABLED;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path())
            .isEqualTo("/api/admin/operating-hours/" + operatingHoursId + "/disable");
      }
    }
  }
}
