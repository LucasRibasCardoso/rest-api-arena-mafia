package com.projetoExtensao.arenaMafia.integration.controller.schedule;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.AvailableSlotResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AvailableTimesController")
public class AvailableTimesControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/schedules/available-times")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistUser();
    AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokens.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/schedules/available-times")
  class GetAvailableTimes {

    @Nested
    @DisplayName("Cenários de Sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName(
          "Deve retornar lista de horários disponíveis para a modalidade e data especificadas")
      void shouldReturnEmptyListWhenNoAvailableTimes() {
        // Arrange
        Modality modality = mockPersistModality("Soccer");
        mockPersistCourt("Court 1", "description", OffsetMinutes.ZERO, Set.of(modality));
        mockPersistCourt("Court 2", "description", OffsetMinutes.THIRTY, Set.of(modality));
        mockPersistPriceRule();
        mockPersistOperatingHoursAllDays();

        String date = LocalDate.now().toString();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modality.getId())
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(AvailableSlotResponseDto[].class);

        // Assert
        // Quadra 1 (Offset 0): 08:00-09:00, 09:00-10:00, ..., 23:00-00:00 (16 slots)
        // Quadra 2 (Offset 30): 08:30-09:30, 09:30-10:30, ..., 22:30-23:30 (15 slots)
        // Total: 16 + 15 = 31 slots
        assertThat(response).hasSize(31);
      }
    }

    @Nested
    @DisplayName("Cenários de Falha - 400 Bad Request")
    class FailureScenariosBadRequest {

      @Test
      @DisplayName("Parâmetro modalityId ausente")
      void missingModalityIdParameter() {
        // Arrange
        String date = "2024-07-15";

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Parâmetro modalityId inválido")
      void invalidModalityIdParameter() {
        // Arrange
        String modalityId = "invalid-uuid";
        String date = "2024-07-15";

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modalityId)
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Parâmetro date ausente")
      void missingDateParameter() {
        // Arrange
        UUID modalityId = UUID.randomUUID();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modalityId)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Parâmetro date inválido")
      void invalidDateParameter() {
        // Arrange
        UUID modalityId = UUID.randomUUID();
        String date = "01/10/0000"; // Data inválida

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modalityId)
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta consultar horários disponíveis no passado")
      void dateInThePast() {
        // Arrange
        UUID modalityId = UUID.randomUUID();
        String date = LocalDate.now().minusDays(1).toString();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modalityId)
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PAST_DATE_NOT_ALLOWED;

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de Falha - 404 Not Found")
    class FailureScenariosNotFound {

      @Test
      @DisplayName("Nenhuma quadra encontrada para a modalidade especificada")
      void noCourtsFoundForSpecifiedModality() {
        // Arrange
        UUID modalityId = UUID.randomUUID();
        String date = LocalDate.now().toString();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modalityId)
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND_BY_MODALITY;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Nenhum horário de funcionamento encontrado")
      void noOperatingHoursFound() {
        // Arrange
        Modality modality = mockPersistModality("Beach Tennis");
        mockPersistCourt("Court A", modality);
        String date = LocalDate.now().toString();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modality.getId())
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Nenhum horário de funcionamento aplicável encontrado para a data especificada")
      void noApplicableOperatingHoursFoundForSpecifiedDate() {
        // Arrange
        Modality modality = mockPersistModality("Tennis");
        mockPersistCourt("Court B", modality);
        mockPersistPriceRule();
        mockPersistOperatingHours(); // Cria horários apenas para segunda a sexta

        // Encontra o próximo sábado ou domingo
        LocalDate targetDate = LocalDate.now();
        while (targetDate.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
          targetDate = targetDate.plusDays(1);
        }
        String date = targetDate.toString();

        // Act & Assert
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("modalityId", modality.getId())
                .queryParam("date", date)
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .body()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/schedules/available-times");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
