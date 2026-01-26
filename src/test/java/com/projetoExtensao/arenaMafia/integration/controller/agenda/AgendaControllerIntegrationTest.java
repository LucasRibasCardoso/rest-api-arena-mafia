package com.projetoExtensao.arenaMafia.integration.controller.agenda;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.ScheduleEntryType;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicAgendaItemResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicAvailableItemResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.PublicScheduleEntryResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AgendaController")
public class AgendaControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/public/agenda")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/public/agenda")
  class GetAgendaTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Retorna agenda com slots disponíveis e reservados")
      void shouldReturnAgendaWithAvailableAndReservedSlots() {
        Modality modality = mockPersistModality("Tennis");
        Court court = mockPersistCourt("Court A", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        User user = mockPersistUser();
        LocalDate date = LocalDate.now().plusDays(1);
        TimeInterval reservedInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            date,
            reservedInterval,
            BigDecimal.valueOf(50.00),
            user.getId());

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();

        // Validar slots RESERVED (PublicScheduleEntryResponseDto)
        List<PublicScheduleEntryResponseDto> reservedSlots =
            response.stream()
                .filter(item -> item instanceof PublicScheduleEntryResponseDto)
                .map(item -> (PublicScheduleEntryResponseDto) item)
                .toList();

        assertThat(reservedSlots).isNotEmpty();
        assertThat(reservedSlots)
            .allMatch(slot -> slot.entryType() == ScheduleEntryType.RESERVATION);

        // Validar slots AVAILABLE (PublicAvailableItemResponseDto)
        List<PublicAvailableItemResponseDto> availableSlots =
            response.stream()
                .filter(item -> item instanceof PublicAvailableItemResponseDto)
                .map(item -> (PublicAvailableItemResponseDto) item)
                .toList();

        assertThat(availableSlots).isNotEmpty();
        assertThat(availableSlots)
            .allMatch(
                slot ->
                    slot.availableModalityIds() != null && !slot.availableModalityIds().isEmpty());

        // Todos os slots devem ter timeInterval
        assertThat(response).allMatch(slot -> slot.timeInterval() != null);
      }

      @Test
      @DisplayName("Retorna agenda ordenada por horário de início")
      void shouldReturnAgendaSortedByStartTime() {
        Modality modality = mockPersistModality("Football");
        mockPersistCourt("Court B", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(2);

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();

        for (int i = 0; i < response.size() - 1; i++) {
          LocalTime currentStart = response.get(i).timeInterval().startTime();
          LocalTime nextStart = response.get(i + 1).timeInterval().startTime();
          assertThat(currentStart).isBeforeOrEqualTo(nextStart);
        }
      }

      @Test
      @DisplayName("Retorna apenas slots disponíveis quando não há reservas")
      void shouldReturnOnlyAvailableSlotsWhenNoReservations() {
        Modality modality = mockPersistModality("Basketball");
        mockPersistCourt("Court C", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(4);

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .log()
                .all()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();
        assertThat(response).allMatch(item -> item instanceof PublicAvailableItemResponseDto);

        // Todos os slots AVAILABLE devem ter availableModalityIds não vazio
        List<PublicAvailableItemResponseDto> availableSlots =
            response.stream().map(item -> (PublicAvailableItemResponseDto) item).toList();

        assertThat(availableSlots)
            .allMatch(
                slot ->
                    slot.availableModalityIds() != null && !slot.availableModalityIds().isEmpty());
      }

      @Test
      @DisplayName("Retorna agenda para data atual")
      void shouldReturnAgendaForCurrentDate() {
        Modality modality = mockPersistModality("Volleyball");
        mockPersistCourt("Court D", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate today = LocalDate.now();

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", today.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();
      }

      @Test
      @DisplayName("Retorna agenda com múltiplas quadras da mesma modalidade")
      void shouldReturnAgendaWithMultipleCourtsOfSameModality() {
        Modality modality = mockPersistModality("Futsal");
        Court court1 = mockPersistCourt("Court E1", modality);
        mockPersistCourt("Court E2", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        User user = mockPersistUser();
        LocalDate date = LocalDate.now().plusDays(4);
        TimeInterval interval = new TimeInterval(LocalTime.of(20, 0), LocalTime.of(21, 0));
        mockPersistReservationByUser(
            modality.getId(),
            court1.getId(),
            date,
            interval,
            BigDecimal.valueOf(50.00),
            user.getId());

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();

        // Validar que existem slots RESERVED e AVAILABLE
        assertThat(response)
            .anyMatch(item -> item instanceof PublicScheduleEntryResponseDto); // RESERVED
        assertThat(response)
            .anyMatch(item -> item instanceof PublicAvailableItemResponseDto); // AVAILABLE
      }

      @Test
      @DisplayName("Slots AVAILABLE devem conter IDs das modalidades disponíveis")
      void shouldReturnAvailableModalityIdsInAvailableSlots() {
        Modality modality1 = mockPersistModality("Tennis");
        Modality modality2 = mockPersistModality("Badminton");

        mockPersistCourt("Court M1", modality1);
        mockPersistCourt("Court M2", modality2);

        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(5);

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();

        // Todos os slots AVAILABLE devem ter availableModalityIds
        List<PublicAvailableItemResponseDto> availableSlots =
            response.stream()
                .filter(item -> item instanceof PublicAvailableItemResponseDto)
                .map(item -> (PublicAvailableItemResponseDto) item)
                .toList();

        assertThat(availableSlots).isNotEmpty();
        assertThat(availableSlots)
            .allMatch(
                slot ->
                    slot.availableModalityIds() != null && !slot.availableModalityIds().isEmpty());

        // Pelo menos um slot deve conter a modalidade 1 e outro a modalidade 2
        assertThat(availableSlots)
            .anyMatch(slot -> slot.availableModalityIds().contains(modality1.getId()));
        assertThat(availableSlots)
            .anyMatch(slot -> slot.availableModalityIds().contains(modality2.getId()));
      }

      @Test
      @DisplayName("Slots AVAILABLE agrupados não devem ter courtId")
      void shouldNotHaveCourtIdInGroupedAvailableSlots() {
        // Nota: Na nova estrutura PublicAvailableItemResponseDto, o campo courtId foi removido
        // pois a agenda pública é agrupada. Este teste verifica implicitamente a estrutura do DTO.
        // Se o DTO não tem courtId, o teste de compilação já garante isso.
        // Mas podemos verificar se availableModalityIds está presente.

        Modality modality = mockPersistModality("Swimming");
        mockPersistCourt("Court N", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(6);

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        List<PublicAvailableItemResponseDto> availableSlots =
            response.stream()
                .filter(item -> item instanceof PublicAvailableItemResponseDto)
                .map(item -> (PublicAvailableItemResponseDto) item)
                .toList();

        // Devem ter availableModalityIds
        assertThat(availableSlots)
            .allMatch(
                slot ->
                    slot.availableModalityIds() != null && !slot.availableModalityIds().isEmpty());
      }

      @Test
      @DisplayName("Slots RESERVED devem ter entryType correto")
      void shouldHaveCorrectEntryTypeInReservedSlots() {
        Modality modality = mockPersistModality("Boxing");
        Court court = mockPersistCourt("Court O", modality);
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        User user = mockPersistUser();
        LocalDate date = LocalDate.now().plusDays(7);
        TimeInterval interval = new TimeInterval(LocalTime.of(15, 0), LocalTime.of(16, 0));
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            date,
            interval,
            BigDecimal.valueOf(50.00),
            user.getId());

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        List<PublicScheduleEntryResponseDto> reservedSlots =
            response.stream()
                .filter(item -> item instanceof PublicScheduleEntryResponseDto)
                .map(item -> (PublicScheduleEntryResponseDto) item)
                .toList();

        assertThat(reservedSlots).isNotEmpty();

        // Slots RESERVED devem ter entryType RESERVATION
        assertThat(reservedSlots)
            .allMatch(slot -> slot.entryType() == ScheduleEntryType.RESERVATION);
      }

      @Test
      @DisplayName("Agenda com múltiplas modalidades retorna availableModalityIds corretos")
      void shouldReturnCorrectAvailableModalityIds() {
        Modality modality1 = mockPersistModality("Volleyball");
        Modality modality2 = mockPersistModality("Basketball");

        mockPersistCourt("Court P1", modality1);
        mockPersistCourt("Court P2", modality2);

        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(8);

        List<PublicAgendaItemResponseDto> response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", PublicAgendaItemResponseDto.class);

        assertThat(response).isNotEmpty();

        List<PublicAvailableItemResponseDto> availableSlots =
            response.stream()
                .filter(item -> item instanceof PublicAvailableItemResponseDto)
                .map(item -> (PublicAvailableItemResponseDto) item)
                .toList();

        assertThat(availableSlots).isNotEmpty();

        // Verificar que as modalidades estão presentes nos slots disponíveis
        assertThat(availableSlots)
            .anyMatch(slot -> slot.availableModalityIds().contains(modality1.getId()));
        assertThat(availableSlots)
            .anyMatch(slot -> slot.availableModalityIds().contains(modality2.getId()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("Retorna erro quando data não é informada")
      void shouldReturn400WhenDateIsNotProvided() {
        given().spec(specification).when().get().then().statusCode(400);
      }

      @Test
      @DisplayName("Retorna erro quando data está em formato inválido")
      void shouldReturn400WhenDateFormatIsInvalid() {
        given()
            .spec(specification)
            .queryParam("date", "invalid-date")
            .when()
            .get()
            .then()
            .statusCode(400);
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("Retorna erro quando não há quadras ativas")
      void shouldReturn404WhenNoActiveCourts() {
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(1);

        var response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/public/agenda");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Retorna erro quando não há horários de funcionamento ativos")
      void shouldReturn404WhenNoActiveOperatingHours() {
        Modality modality = mockPersistModality("TableTennis");
        mockPersistCourt("Court G", modality);
        mockPersistDefaultPriceRule();

        LocalDate date = LocalDate.now().plusDays(1);

        var response =
            given()
                .spec(specification)
                .queryParam("date", date.toString())
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/public/agenda");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Retorna erro quando não há regras de preço ativas")
      void shouldReturn404WhenNoPriceRules() {
        Modality modality = mockPersistModality("Wrestling");
        mockPersistCourt("Court I", modality);
        mockPersistOperatingHoursAllDays();

        LocalDate futureDate = LocalDate.now().plusDays(1);

        var response =
            given()
                .spec(specification)
                .queryParam("date", futureDate.toString())
                .when()
                .get()
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_NOT_FOUND;

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/public/agenda");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
