package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AdminAgendaController")
public class AdminAgendaControllerIntegrationTest extends WebIntegrationTestConfig {

  private static final String BASE_PATH = "/api/admin/agenda";
  private static final BigDecimal DEFAULT_RESERVATION_PRICE = BigDecimal.valueOf(50);

  private RequestSpecification specification;
  private String accessToken;
  private UUID adminId;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath(BASE_PATH)
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    User admin = mockPersistAdminUser();
    adminId = admin.getId();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/agenda")
  class GetAgendaTests {

    @Nested
    @DisplayName("Cenários de Sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar agenda com slots disponíveis quando não há reservas")
      void shouldReturnAgendaWithAvailableSlotsWhenNoReservations() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        LocalDate date = LocalDate.now().plusDays(1);

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // Verificar primeiro slot disponível
        String firstType = jsonPath.getString("[0].type");
        assertThat(firstType).isEqualTo("AVAILABLE_SLOT");

        UUID firstCourtId = UUID.fromString(jsonPath.getString("[0].courtId"));
        assertThat(firstCourtId).isEqualTo(court.getId());

        String firstCourtName = jsonPath.getString("[0].courtName");
        assertThat(firstCourtName).isEqualTo(court.getName());
      }

      @Test
      @DisplayName("Deve retornar agenda com reservas quando existem agendamentos")
      void shouldReturnAgendaWithReservationsWhenSchedulesExist() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");
        LocalDate date = LocalDate.now().plusDays(1);
        TimeInterval reservationInterval =
            new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            date,
            reservationInterval,
            DEFAULT_RESERVATION_PRICE,
            user.getId());

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // Verificar se existe pelo menos um item do tipo SCHEDULE_DETAIL (reserva)
        List<String> types = jsonPath.getList("type", String.class);
        assertThat(types).contains("SCHEDULE_DETAIL");
      }

      @Test
      @DisplayName("Deve retornar agenda com bloqueios quando existem bloqueios de horário")
      void shouldReturnAgendaWithBlockedTimesWhenBlockedTimesExist() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        LocalDate date = LocalDate.now().plusDays(1);
        TimeInterval blockedInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

        mockPersistBlockedTimeSpecific(court.getId(), date, blockedInterval, "Manutenção", adminId);

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // Verificar se existe pelo menos um item do tipo SCHEDULE_DETAIL (bloqueio)
        List<String> types = jsonPath.getList("type", String.class);
        assertThat(types).contains("SCHEDULE_DETAIL");
      }

      @Test
      @DisplayName("Deve retornar agenda filtrada por quadra específica")
      void shouldReturnAgendaFilteredByCourtId() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court1 = mockPersistCourt("Quadra 1", modality);
        mockPersistCourt("Quadra 2", modality);
        LocalDate date = LocalDate.now().plusDays(1);

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .queryParam("courtId", court1.getId().toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // Todos os slots disponíveis devem ser da quadra 1
        List<String> courtIds =
            jsonPath.getList("findAll { it.type == 'AVAILABLE_SLOT' }.courtId", String.class);
        assertThat(courtIds).allMatch(id -> id.equals(court1.getId().toString()));
      }

      @Test
      @DisplayName("Deve retornar agenda com múltiplas quadras quando courtId não é informado")
      void shouldReturnAgendaWithMultipleCourtsWhenCourtIdNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court1 = mockPersistCourt("Quadra 1", modality);
        Court court2 = mockPersistCourt("Quadra 2", modality);
        LocalDate date = LocalDate.now().plusDays(1);

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // Deve haver slots de ambas as quadras
        List<String> courtIds =
            jsonPath.getList("findAll { it.type == 'AVAILABLE_SLOT' }.courtId", String.class);
        List<String> distinctCourtIds = courtIds.stream().distinct().toList();

        assertThat(distinctCourtIds).contains(court1.getId().toString(), court2.getId().toString());
      }

      @Test
      @DisplayName("Deve retornar agenda com preço correto nos slots disponíveis")
      void shouldReturnAgendaWithCorrectPriceInAvailableSlots() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        mockPersistCourt("Quadra 1", modality);
        LocalDate date = LocalDate.now().plusDays(1);

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        List<?> items = jsonPath.getList("$");
        assertThat(items).isNotEmpty();

        // O primeiro slot disponível deve ter preço
        Float firstPrice = jsonPath.getFloat("find { it.type == 'AVAILABLE_SLOT' }.price");
        assertThat(firstPrice).isNotNull();
        assertThat(firstPrice).isPositive();
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("Deve retornar 400 quando parâmetro date não é informado")
      void shouldReturn400WhenDateParameterNotProvided() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo(BASE_PATH);
      }

      @Test
      @DisplayName("Deve retornar 400 quando formato de date é inválido")
      void shouldReturn400WhenDateFormatIsInvalid() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", "invalid-date")
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo(BASE_PATH);
      }

      @Test
      @DisplayName("Deve retornar 400 quando formato de courtId é inválido")
      void shouldReturn400WhenCourtIdFormatIsInvalid() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .queryParam("courtId", "invalid-uuid")
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo(BASE_PATH);
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("Deve retornar 404 quando courtId não existe")
      void shouldReturn404WhenCourtIdNotFound() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        LocalDate date = LocalDate.now().plusDays(1);
        UUID nonExistentCourtId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("date", date.toString())
                .queryParam("courtId", nonExistentCourtId.toString())
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(response, 404, BASE_PATH, ErrorCode.COURT_NOT_FOUND);
      }
    }
  }
}
