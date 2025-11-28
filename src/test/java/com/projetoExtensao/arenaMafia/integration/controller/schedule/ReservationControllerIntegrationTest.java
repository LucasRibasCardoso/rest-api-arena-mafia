package com.projetoExtensao.arenaMafia.integration.controller.schedule;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.request.CreateReservationRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.ScheduleEntryResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para ReservationController")
public class ReservationControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private ScheduleEntryRepositoryPort scheduleEntryRepository;
  private RequestSpecification specification;
  private String accessToken;
  private User defaultUser;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/users/me/reservations")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    defaultUser = mockPersistUser();
    AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokens.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/users/me/reservations")
  class CreateReservationTests {

    @Nested
    @DisplayName("Cenários de sucesso - 201 Created")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve criar uma nova reserva com sucesso")
      void shouldCreateNewReservationSuccessfully() {
        // Arrange
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Court 1", modality);
        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0));
        var request =
            new CreateReservationRequestDto(
                modality.getId(), court.getId(), reservationDate, timeInterval);

        // Act & Assert
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
                .as(ScheduleEntryResponseDto.class);

        // Verifications
        ScheduleEntry savedEntry = scheduleEntryRepository.findByIdOrElseThrow(response.id());

        assertThat(savedEntry.getId()).isEqualTo(response.id());
        assertThat(savedEntry.getCourtId()).isEqualTo(response.courtId());
        assertThat(savedEntry.getDateTimeSlot().date()).isEqualTo(response.date());
        assertThat(savedEntry.getDateTimeSlot().timeInterval().startTime())
            .isEqualTo(response.timeInterval().startTime());
        assertThat(savedEntry.getDateTimeSlot().timeInterval().endTime())
            .isEqualTo(response.timeInterval().endTime());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("ID da modalidade não informado")
      void shouldReturn400WhenModalityIdNotProvided() {
        // Arrange

        Court court = mockPersistCourt("Court 1", mockPersistModality("Futebol"));
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0));

        CreateReservationRequestDto request =
            new CreateReservationRequestDto(
                null, court.getId(), LocalDate.now().plusDays(1), timeInterval);

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
        ErrorCode errorCode = ErrorCode.RESERVATION_MODALITY_ID_REQUIRED;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("modalityId")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("ID da quadra não informado")
      void shouldReturn400WhenCourtIdNotProvided() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0));

        CreateReservationRequestDto request =
            new CreateReservationRequestDto(
                modality.getId(), null, LocalDate.now().plusDays(1), timeInterval);

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
        ErrorCode errorCode = ErrorCode.RESERVATION_COURT_ID_REQUIRED;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("courtId")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Data da reserva não informada")
      void shouldReturn400WhenReservationDateNotProvided() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Court 1", modality);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(9, 0));

        CreateReservationRequestDto request =
            new CreateReservationRequestDto(modality.getId(), court.getId(), null, timeInterval);

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
        ErrorCode errorCode = ErrorCode.RESERVATION_DATE_REQUIRED;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("date")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidTimeIntervalProvider
      @DisplayName("Intervalo de tempo inválido")
      void shouldReturn400WhenInvalidTimeInterval(
          LocalTime startTime, LocalTime endTime, String expectedErrorCode) {
        // Arrange
        Modality modality = mockPersistModality("Volei");
        Court court = mockPersistCourt("Court B", modality);

        Map<String, Object> timeIntervalMap = new HashMap<>();
        timeIntervalMap.put("startTime", startTime);
        timeIntervalMap.put("endTime", endTime);

        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("modalityId", modality.getId());
        jsonRequest.put("courtId", court.getId());
        jsonRequest.put("date", LocalDate.now().plusDays(1));
        jsonRequest.put("timeInterval", timeIntervalMap);

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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("A quadra informada não existe")
      void shouldReturn404WhenCourtNotFound() {
        // Arrange
        Modality modality = mockPersistModality("Tennis");
        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        UUID courtId = UUID.randomUUID();
        var request =
            new CreateReservationRequestDto(
                modality.getId(), courtId, reservationDate, timeInterval);

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

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class ConflictScenarios {

      @Test
      @DisplayName("Tenta criar uma reserva em um horário já ocupado")
      void shouldReturn409WhenCreatingReservationInOccupiedTimeSlot() {
        // Arrange
        Modality modality = mockPersistModality("Soccer");
        Court court = mockPersistCourt("Court A", modality);
        LocalDate reservationDate = LocalDate.now().plusDays(2);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(15, 0), LocalTime.of(16, 0));
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            reservationDate,
            timeInterval,
            BigDecimal.valueOf(50.00),
            defaultUser.getId());

        var request =
            new CreateReservationRequestDto(
                modality.getId(), court.getId(), reservationDate, timeInterval);

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

        ErrorCode errorCode = ErrorCode.SCHEDULE_ENTRY_NOT_AVAILABLE;

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/users/me/reservations/{reservationId}")
  class GetReservationByIdTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar uma reserva existente pelo ID")
      void shouldReturnExistingReservationById() {
        // Arrange
        Modality modality = mockPersistModality("Padel");
        Court court = mockPersistCourt("Court X", modality);
        LocalDate reservationDate = LocalDate.now().plusDays(3);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0));
        ScheduleEntry savedEntry =
            mockPersistReservationByUser(
                modality.getId(),
                court.getId(),
                reservationDate,
                timeInterval,
                BigDecimal.valueOf(75.00),
                defaultUser.getId());

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{reservationId}", savedEntry.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(ScheduleEntryResponseDto.class);

        assertThat(response.id()).isEqualTo(savedEntry.getId());
        assertThat(response.courtId()).isEqualTo(savedEntry.getCourtId());
        assertThat(response.date()).isEqualTo(savedEntry.getDateTimeSlot().date());
        assertThat(response.timeInterval().startTime())
            .isEqualTo(savedEntry.getDateTimeSlot().timeInterval().startTime());
        assertThat(response.timeInterval().endTime())
            .isEqualTo(savedEntry.getDateTimeSlot().timeInterval().endTime());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("ID da reserva em formato inválido")
      void shouldReturn400WhenReservationIdIsInInvalidFormat() {
        // Arrange
        String reservationId = "invalid-uuid-format";

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{reservationId}", reservationId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations/" + reservationId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 403 Forbidden")
    class ForbiddenScenarios {

      @Test
      @DisplayName("Tenta acessar uma reserva que pertence a outro usuário")
      void shouldReturn403WhenAccessingReservationOfAnotherUser() {
        // Arrange
        Modality modality = mockPersistModality("Hockey");
        Court court = mockPersistCourt("Court Z", modality);
        LocalDate reservationDate = LocalDate.now().plusDays(4);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));
        User anotherUser = mockPersistUser("Test", "Test User", "+5511999999999", "testuser");
        ScheduleEntry savedEntry =
            mockPersistReservationByUser(
                modality.getId(),
                court.getId(),
                reservationDate,
                timeInterval,
                BigDecimal.valueOf(60.00),
                anotherUser.getId());

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{reservationId}", savedEntry.getId())
                .then()
                .statusCode(403)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.RESERVATION_ACCESS_DENIED;

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations/" + savedEntry.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("Reserva não encontrada pelo ID")
      void shouldReturn404WhenReservationNotFoundById() {
        // Arrange
        UUID reservationId = UUID.randomUUID();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get("/{reservationId}", reservationId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.SCHEDULE_ENTRY_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/users/me/reservations/" + reservationId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
