package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.operatingHours.port.gateway.OperatingHoursPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.preview.OperatingHoursDisablePreview;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PreviewNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.CreateOperatingHoursRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.request.OperatingHoursDisableConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.operatingHours.response.OperatingHoursDisablePreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.dayOfWeek.InvalidDaysOfWeekProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

@DisplayName("Testes de integração para AdminOperatingHoursController")
public class AdminOperatingHoursControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private OperatingHoursPreviewCachePort operatingHoursPreviewCache;
  @Autowired private OperatingHoursRepositoryPort operatingHoursRepository;
  private RequestSpecification specification;
  private String accessToken;
  private User authenticatedUser;
  private UUID adminId;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/operating-hours")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    authenticatedUser = mockPersistAdminUser();
    adminId = authenticatedUser.getId();
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
      @DisplayName(
          "Deve criar horário de funcionamento para todos os dias da semana quando nulo é"
              + " fornecido")
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

      @Test
      @DisplayName(
          "Deve criar horário de funcionamento para todos os dias da semana quando lista vazia é"
              + " fornecida")
      void shouldReturn201_whenProvidingEmptyDaysOfWeekList() {
        // Arrange
        var timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(18, 0));
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
                .statusCode(201)
                .extract()
                .as(OperatingHoursResponseDto.class);

        // Assert
        assertThat(response.daysOfWeek()).isNull();
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class CreateOperatingHoursBadRequestScenarios {

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
  @DisplayName("Testes para a funcionalidade de desabilitação de Horários de Funcionamento")
  class DisableOperatingHoursTest {

    @Nested
    @DisplayName("Testes para o endpoint POST /api/admin/operating-hours/{hourId}/preview-disable")
    class PreviewDisableOperatingHoursTest {

      @Nested
      @DisplayName("Cenários de sucesso - 200 OK")
      class SuccessScenarios {

        @Test
        @DisplayName(
            "Deve criar preview de desativação para um horário sem conflitos com agendamentos"
                + " futuros")
        void shouldReturn200_whenCreatingPreviewForValidOperatingHours() {
          // Arrange
          var operatingHour = mockPersistOperatingHoursAllDays();
          UUID operatingHourId = operatingHour.getId();

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", operatingHourId)
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(OperatingHoursDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotEmpty();
          assertThat(response.operatingHoursId()).isEqualTo(operatingHourId);
          assertThat(response.usersAffectedCount()).isZero();
          assertThat(response.blockedTimesAffectedCount()).isZero();
          assertThat(response.reservationsAffectedCount()).isZero();
          assertThat(response.blockedTimesAffectedCount()).isZero();
          assertThat(response.reservationsAffectedCount()).isZero();
          assertThat(response.inProgressReservations().size()).isZero();

          Optional<OperatingHoursDisablePreview> previewInCache =
              getPreviewFromCache(response.previewKey());
          assertThat(previewInCache).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewInCache.get());
        }

        @Test
        @DisplayName(
            "Deve criar preview de desativação para um horário que o fim é meia noite em ponto  e"
                + " possui agendamentos futuros")
        void shouldReturn200_whenCreatingPreviewForOperatingHoursWithFutureSchedules() {
          // Arrange
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
          var operatingHour = mockPersistOperatingHoursAllDaysWithTimeInterval(timeInterval);
          UUID operatingHourId = operatingHour.getId();

          // Criar agendamentos futuros que serão afetados
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra A", modality);

          mockPersistBlockedTimeSpecific(
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.FRIDAY),
              new TimeInterval(LocalTime.of(22, 0), LocalTime.of(0, 0)),
              "Manutenção",
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.TUESDAY),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);
          // Reserva no passado que não deve ser afetada
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().minusDays(1),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);
          // Reserva em andamento que não deve ser afetada mas deve constar no preview
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now(),
              new TimeInterval(
                  normalizeToValidMinutes(LocalTime.now().minusMinutes(30)),
                  normalizeToValidMinutes(LocalTime.now().plusMinutes(30))),
              BigDecimal.valueOf(80),
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", operatingHourId)
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(OperatingHoursDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotEmpty();
          assertThat(response.operatingHoursId()).isEqualTo(operatingHourId);
          assertThat(response.usersAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.inProgressReservations().size()).isOne();

          Optional<OperatingHoursDisablePreview> previewInCache =
              getPreviewFromCache(response.previewKey());
          assertThat(previewInCache).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewInCache.get());
        }

        @Test
        @DisplayName(
            "Deve criar preview de desativação para um horário que o fim atravessa a meia noite e"
                + " possui agendamentos futuros")
        void
            shouldReturn200_whenCreatingPreviewForOperatingHoursCrossMidnightWithFutureSchedules() {
          // Arrange
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(1, 0));
          var operatingHour = mockPersistOperatingHoursAllDaysWithTimeInterval(timeInterval);
          UUID operatingHourId = operatingHour.getId();

          // Criar agendamentos futuros que serão afetados
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra A", modality);

          mockPersistBlockedTimeSpecific(
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.FRIDAY),
              new TimeInterval(LocalTime.of(22, 0), LocalTime.of(0, 0)),
              "Manutenção",
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.TUESDAY),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);
          // Reserva no passado que não deve ser afetada
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().minusDays(1),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);
          // Reserva em andamento que não deve ser afetada mas deve constar no preview
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now(),
              new TimeInterval(
                  normalizeToValidMinutes(LocalTime.now().minusMinutes(30)),
                  normalizeToValidMinutes(LocalTime.now().plusMinutes(30))),
              BigDecimal.valueOf(80),
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", operatingHourId)
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(OperatingHoursDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotEmpty();
          assertThat(response.operatingHoursId()).isEqualTo(operatingHourId);
          assertThat(response.usersAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.inProgressReservations().size()).isOne();

          Optional<OperatingHoursDisablePreview> previewInCache =
              getPreviewFromCache(response.previewKey());
          assertThat(previewInCache).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewInCache.get());
        }

        @Test
        @DisplayName(
            "Deve criar preview de desativação para um horário que não atravesse a meia noite e"
                + " possui múltiplos agendamentos futuros")
        void shouldReturn200_whenCreatingPreviewForOperatingHoursWithMultipleFutureSchedules() {
          // Arrange
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(23, 0));
          var operatingHour = mockPersistOperatingHoursAllDaysWithTimeInterval(timeInterval);
          UUID operatingHourId = operatingHour.getId();

          // Criar múltiplos agendamentos futuros que serão afetados
          Modality modality = mockPersistModality("Futebol");
          Court court = mockPersistCourt("Quadra B", modality);

          mockPersistBlockedTimeSpecific(
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.WEDNESDAY),
              new TimeInterval(LocalTime.of(18, 0), LocalTime.of(20, 0)),
              "Evento Especial",
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.THURSDAY),
              new TimeInterval(LocalTime.of(16, 0), LocalTime.of(17, 0)),
              new BigDecimal(100),
              adminId);
          // Reserva no passado que não deve ser afetada
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now().minusDays(1),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);
          // Reserva em andamento que não deve ser afetada mas deve constar no preview
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              LocalDate.now(),
              new TimeInterval(
                  normalizeToValidMinutes(LocalTime.now().minusMinutes(30)),
                  normalizeToValidMinutes(LocalTime.now().plusMinutes(30))),
              BigDecimal.valueOf(80),
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", operatingHourId)
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(OperatingHoursDisablePreviewResponseDto.class);

          // Assert
          assertThat(response.previewKey()).isNotEmpty();
          assertThat(response.operatingHoursId()).isEqualTo(operatingHourId);
          assertThat(response.usersAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.blockedTimesAffectedCount()).isOne();
          assertThat(response.reservationsAffectedCount()).isOne();
          assertThat(response.inProgressReservations().size()).isOne();

          Optional<OperatingHoursDisablePreview> previewInCache =
              getPreviewFromCache(response.previewKey());
          assertThat(previewInCache).isPresent();

          assertResponsePreviewMatchesPreviewSaved(response, previewInCache.get());
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 400 Bad Request")
      class BadRequestScenarios {

        @Test
        @DisplayName("Tenta criar preview de desativação com ID inválido")
        void shouldReturn400_whenCreatingPreviewWithInvalidIdFormat() {
          // Arrange
          String invalidId = "invalid-id";

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", invalidId)
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/" + invalidId + "/preview-disable";
          assertBusinessError(response, 400, path, ErrorCode.INVALID_REQUEST_PARAMETER);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 404 Not Found")
      class NotFoundScenario {

        @Test
        @DisplayName("Tenta criar preview de desativação para um horário inexistente")
        void shouldReturn404_whenCreatingPreviewForNonExistentOperatingHours() {
          // Arrange
          UUID nonExistentId = UUID.randomUUID();

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", nonExistentId)
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/" + nonExistentId + "/preview-disable";
          assertBusinessError(response, 404, path, ErrorCode.OPERATING_HOURS_NOT_FOUND);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 409 Conflict")
      class ConflictScenarios {

        @Test
        @DisplayName("Tenta criar preview de desativação para um horário já desativado")
        void shouldReturn409_whenCreatingPreviewForAlreadyDisabledOperatingHours() {
          // Arrange
          var operatingHour = mockPersistDisabledOperatingHours();
          UUID operatingHourId = operatingHour.getId();

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .post("/{operatingHourId}/preview-disable", operatingHourId)
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/" + operatingHourId + "/preview-disable";
          assertBusinessError(response, 409, path, ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
        }
      }
    }

    @Nested
    @DisplayName("Testes para o endpoint POST /api/admin/operating-hours/confirm-disable")
    class ConfirmDisableOperatingHoursTest {

      @Nested
      @DisplayName("Cenários de sucesso - 200 OK")
      class SuccessScenarios {

        @Test
        @DisplayName(
            "Deve confirmar desativação de um horário de funcionamento com preview válido sem"
                + " conflitos")
        void shouldReturn200_whenConfirmingDisableWithValidPreview() {
          // Arrange
          var operatingHour = mockPersistOperatingHoursAllDays();
          UUID operatingHourId = operatingHour.getId();

          String previewKey = createPreviewAndGetKey(operatingHourId);

          var request =
              new OperatingHoursDisableConfirmRequestDto(previewKey, "Desativação temporária");

          // Act
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .body(request)
              .when()
              .post("/confirm-disable")
              .then()
              .statusCode(204);

          // Assert
          var updatedOperatingHour = operatingHoursRepository.findByIdOrElseThrow(operatingHourId);
          assertThat(updatedOperatingHour.isActive()).isFalse();

          Optional<OperatingHoursDisablePreview> previewInCache = getPreviewFromCache(previewKey);
          assertThat(previewInCache).isEmpty();
        }

        @Test
        @DisplayName(
            "Deve confirmar desativação de um horário de funcionamento com preview válido com"
                + " conflitos")
        void shouldReturn200_whenConfirmingDisableWithValidPreviewWithConflicts() {
          // Arrange
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(23, 0));
          var operatingHour = mockPersistOperatingHoursAllDaysWithTimeInterval(timeInterval);
          UUID operatingHourId = operatingHour.getId();

          // Criar múltiplos agendamentos futuros que serão afetados
          Modality modality = mockPersistModality("Futebol");
          Court court = mockPersistCourt("Quadra B", modality);

          mockPersistBlockedTimeSpecific(
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.WEDNESDAY),
              new TimeInterval(LocalTime.of(18, 0), LocalTime.of(20, 0)),
              "Evento Especial",
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.THURSDAY),
              new TimeInterval(LocalTime.of(16, 0), LocalTime.of(17, 0)),
              new BigDecimal(100),
              adminId);

          String previewKey = createPreviewAndGetKey(operatingHourId);

          var request =
              new OperatingHoursDisableConfirmRequestDto(previewKey, "Desativação temporária");

          // Act
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .body(request)
              .when()
              .post("/confirm-disable")
              .then()
              .statusCode(204);

          // Assert
          var updatedOperatingHour = operatingHoursRepository.findByIdOrElseThrow(operatingHourId);
          assertThat(updatedOperatingHour.isActive()).isFalse();

          Optional<OperatingHoursDisablePreview> previewInCache = getPreviewFromCache(previewKey);
          assertThat(previewInCache).isEmpty();
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 400 Bad Request")
      class BadRequestScenarios {

        @Test
        @DisplayName("Tenta confirmar desativação com chave de preview vazia")
        void shouldReturn400_whenConfirmingDisableWithEmptyPreviewKey() {
          // Arrange
          var request = new OperatingHoursDisableConfirmRequestDto("", "Desativação temporária");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertValidationError(response, path, "previewKey", ErrorCode.PREVIEW_KEY_REQUIRED);
        }

        @Test
        @DisplayName("Tenta confirmar desativação com a chave de preview inválida")
        void shouldReturn400_whenConfirmingDisableWithInvalidPreviewKey() {
          // Arrange
          var request =
              new OperatingHoursDisableConfirmRequestDto(
                  "invalid-preview-key", "Desativação temporária");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertBusinessError(response, 400, path, ErrorCode.PREVIEW_KEY_INVALID);
        }

        @Test
        @DisplayName("Tenta confirmar desativação com motivo vazio")
        void shouldReturn400_whenConfirmingDisableWithEmptyReason() {
          // Arrange
          String previewKey = operatingHoursPreviewCache.generateKey(adminId);

          var request = new OperatingHoursDisableConfirmRequestDto(previewKey, "");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertValidationError(
              response,
              path,
              "description",
              ErrorCode.OPERATING_HOURS_DISABLE_DESCRIPTION_REQUIRED);
        }

        @Test
        @DisplayName("Tenta confirmar desativação com motivo muito longo")
        void shouldReturn400_whenConfirmingDisableWithTooLongReason() {
          // Arrange
          String previewKey = operatingHoursPreviewCache.generateKey(adminId);
          String longDescription = "A".repeat(501);

          var request = new OperatingHoursDisableConfirmRequestDto(previewKey, longDescription);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertValidationError(
              response,
              path,
              "description",
              ErrorCode.OPERATING_HOURS_DISABLE_DESCRIPTION_INVALID_LENGTH);
        }

        @Test
        @DisplayName("Tenta confirmar desativação com motivo muito curto")
        void shouldReturn400_whenConfirmingDisableWithTooShortReason() {
          // Arrange
          String previewKey = operatingHoursPreviewCache.generateKey(adminId);
          String shortDescription = "AA";

          var request = new OperatingHoursDisableConfirmRequestDto(previewKey, shortDescription);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertValidationError(
              response,
              path,
              "description",
              ErrorCode.OPERATING_HOURS_DISABLE_DESCRIPTION_INVALID_LENGTH);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 403 Forbidden")
      class ForbiddenScenarios {

        @Test
        @DisplayName("Tenta confirmar desativação com preview pertencente a outro admin")
        void shouldReturn403_whenConfirmingDisableWithPreviewOfAnotherAdmin() {
          // Arrange
          User otherAdmin = mockPersistOtherAdminUser();
          AuthTokensTest otherAdminTokens = mockLogin(otherAdmin.getUsername(), defaultPassword);
          String anotherAdminAccessToken = "Bearer " + otherAdminTokens.accessToken();

          var operatingHour = mockPersistOperatingHoursAllDays();
          UUID operatingHourId = operatingHour.getId();

          String previewKey = createPreviewAndGetKey(operatingHourId);

          var request =
              new OperatingHoursDisableConfirmRequestDto(previewKey, "Desativação temporária");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", anotherAdminAccessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(403)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertBusinessError(response, 403, path, ErrorCode.PREVIEW_KEY_OWNERSHIP_INVALID);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 404 Not Found")
      class NotFoundScenario {

        @Test
        @DisplayName("Tenta confirmar desativação com preview inexistente")
        void shouldReturn404_whenConfirmingDisableWithNonExistentPreview() {
          // Arrange
          String nonExistentPreviewKey = operatingHoursPreviewCache.generateKey(adminId);

          var request =
              new OperatingHoursDisableConfirmRequestDto(
                  nonExistentPreviewKey, "Desativação temporária");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertBusinessError(response, 404, path, ErrorCode.PREVIEW_NOT_FOUND);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 409 Conflict")
      class ConflictScenarios {

        @Test
        @DisplayName(
            "Tenta confirmar desativação com preview desatualizado devido a mudanças nos"
                + " agendamentos")
        void shouldReturn409_whenConfirmingDisableWithStalePreviewDueToScheduleChanges() {
          // Arrange
          var operatingHour = mockPersistOperatingHoursAllDays();
          UUID operatingHourId = operatingHour.getId();

          String previewKey = createPreviewAndGetKey(operatingHourId);
          var request =
              new OperatingHoursDisableConfirmRequestDto(previewKey, "Desativação temporária");

          // Criar um agendamento futuro que tornará o preview desatualizado
          Modality modality = mockPersistModality("Volei");
          Court court = mockPersistCourt("Quadra A", modality);

          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              nextDayOfWeek(java.time.DayOfWeek.TUESDAY),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              new BigDecimal(85),
              adminId);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertBusinessError(response, 409, path, ErrorCode.PREVIEW_DATA_STALE);
        }

        @Test
        @DisplayName("Tenta confirmar desativação com preview de horário já desativado")
        void shouldReturn409_whenConfirmingDisableWithPreviewOfAlreadyDisabledOperatingHours() {
          // Arrange
          var operatingHour = mockPersistOperatingHours();
          UUID operatingHourId = operatingHour.getId();

          String previewKey = createPreviewAndGetKey(operatingHourId);
          var request =
              new OperatingHoursDisableConfirmRequestDto(previewKey, "Desativação temporária");

          operatingHour.disable();
          operatingHoursRepository.save(operatingHour);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/confirm-disable")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          String path = "/api/admin/operating-hours/confirm-disable";
          assertBusinessError(response, 409, path, ErrorCode.OPERATING_HOURS_ALREADY_DISABLED);
        }
      }
    }
  }

  private void assertResponsePreviewMatchesPreviewSaved(
      OperatingHoursDisablePreviewResponseDto previewResponse,
      OperatingHoursDisablePreview previewSaved) {

    assertThat(previewResponse.previewKey()).isEqualTo(previewSaved.previewKey());
    assertThat(previewResponse.operatingHoursId()).isEqualTo(previewSaved.operatingHoursId());
    assertThat(previewResponse.usersAffectedCount()).isEqualTo(previewSaved.usersAffectedCount());
    assertThat(previewResponse.blockedTimesAffectedCount())
        .isEqualTo(previewSaved.blockedTimesAffectedCount());
    assertThat(previewResponse.reservationsAffectedCount())
        .isEqualTo(previewSaved.reservationsAffectedCount());

    assertThat(previewResponse.affectedBlockedTimes().size())
        .isEqualTo(previewSaved.affectedBlockedTimes().size());
    assertThat(previewResponse.affectedReservations().size())
        .isEqualTo(previewSaved.affectedReservations().size());
    assertThat(previewResponse.inProgressReservations().size())
        .isEqualTo(previewSaved.inProgressReservations().size());
  }

  private String createPreviewAndGetKey(UUID operatingHourId) {
    var response =
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .post("/{operatingHourId}/preview-disable", operatingHourId)
            .then()
            .statusCode(200)
            .extract()
            .as(OperatingHoursDisablePreviewResponseDto.class);

    return response.previewKey();
  }

  private Optional<OperatingHoursDisablePreview> getPreviewFromCache(String previewKey) {
    try {
      OperatingHoursDisablePreview preview =
          operatingHoursPreviewCache.getPreviewOrElseThrow(previewKey, adminId);
      return Optional.of(preview);
    } catch (PreviewNotFoundException e) {
      return Optional.empty();
    }
  }
}
