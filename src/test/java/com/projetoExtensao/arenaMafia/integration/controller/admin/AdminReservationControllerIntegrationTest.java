package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.reservation.request.AdminReservationCreateRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.ReservationDetailResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.dayOfWeek.InvalidDaysOfWeekProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.common.mapper.TypeRef;
import io.restassured.path.json.JsonPath;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AdminReservationController")
public class AdminReservationControllerIntegrationTest extends WebIntegrationTestConfig {

  private static final String BASE_PATH = "/api/admin/reservations";
  private static final BigDecimal DEFAULT_RESERVATION_PRICE = BigDecimal.valueOf(50);

  @Autowired private ReservationRepositoryPort reservationRepository;
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
  @DisplayName("Testes para o endpoint GET /api/admin/reservations")
  class SearchReservations {

    @Nested
    @DisplayName("Cenários de Sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve retornar 200 OK e lista de reservas paginada com valores padrão")
      void shouldReturn200WithDefaultPaginationValues() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            futureDate,
            timeInterval,
            DEFAULT_RESERVATION_PRICE,
            user.getId());
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            futureDate.plusDays(1),
            timeInterval,
            DEFAULT_RESERVATION_PRICE,
            user.getId());

        // Act
        String responseBody =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        // Assert
        JsonPath jsonPath = new JsonPath(responseBody);
        assertThat(jsonPath.getInt("size")).isEqualTo(20);
        assertThat(jsonPath.getInt("number")).isEqualTo(0);
        assertThat(jsonPath.getInt("totalElements")).isGreaterThanOrEqualTo(2);
        assertThat(jsonPath.getList("content").size()).isGreaterThanOrEqualTo(2);
      }

      @Nested
      @DisplayName("Testes de paginação")
      class PaginationTests {

        @Test
        @DisplayName("Deve retornar 200 OK e lista de reservas paginada")
        void shouldReturn200AndPaginatedListOfReservations() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          for (int i = 0; i < 6; i++) {
            mockPersistReservationByUser(
                modality.getId(),
                court.getId(),
                futureDate.plusDays(i),
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId());
          }

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("size", 2)
                  .queryParam("page", 1)
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          assertThat(jsonPath.getInt("size")).isEqualTo(2);
          assertThat(jsonPath.getInt("number")).isEqualTo(1);
          assertThat(jsonPath.getInt("totalElements")).isGreaterThanOrEqualTo(6);
          assertThat(jsonPath.getList("content").size()).isEqualTo(2);
        }
      }

      @Nested
      @DisplayName("Testes de busca por termo (searchTerm)")
      class SearchTermTests {

        @Test
        @DisplayName("Deve retornar 200 OK e reservas encontradas pelo username do cliente")
        void shouldReturn200AndReservationsFoundByUsername() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user1 = mockPersistUser("joao_silva", "João da Silva", "+5511999001111", "123456");
          User user2 = mockPersistUser("maria_santos", "Maria Santos", "+5511999002222", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user1.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(1),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user2.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("searchTerm", "joao_silva")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).hasSize(1);
          assertThat(reservations.getFirst().username()).isEqualTo("joao_silva");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e reservas encontradas pelo nome completo do cliente")
        void shouldReturn200AndReservationsFoundByFullName() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user1 = mockPersistUser("joao_silva", "João da Silva", "+5511999001111", "123456");
          User user2 = mockPersistUser("maria_santos", "Maria Santos", "+5511999002222", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user1.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(1),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user2.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("searchTerm", "João da Silva")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).hasSize(1);
          assertThat(reservations.getFirst().fullName()).isEqualTo("João da Silva");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e reservas encontradas pelo telefone do cliente")
        void shouldReturn200AndReservationsFoundByPhone() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user1 = mockPersistUser("joao_silva", "João da Silva", "+5511999001111", "123456");
          User user2 = mockPersistUser("maria_santos", "Maria Santos", "+5511999002222", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user1.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(1),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user2.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("searchTerm", "999001111")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).hasSize(1);
          assertThat(reservations.getFirst().userPhone()).isEqualTo("+5511999001111");
        }
      }

      @Nested
      @DisplayName("Testes de filtros")
      class FilterTests {

        @ParameterizedTest
        @EnumSource(
            value = ReservationStatus.class,
            names = {"CONFIRMED", "CANCELLED", "COMPLETED"})
        @DisplayName("Deve retornar 200 OK e reservas filtradas por status")
        void shouldReturn200AndReservationsFilteredByStatus(ReservationStatus status) {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUserWithStatus(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId(),
              status);

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("status", status.name())
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).isNotEmpty();
          assertThat(reservations.stream().allMatch(r -> r.status().equals(status))).isTrue();
        }

        @Test
        @DisplayName("Deve retornar 200 OK e reservas filtradas por userId")
        void shouldReturn200AndReservationsFilteredByUserId() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user1 = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");
          User user2 = mockPersistUser("client2", "Cliente 2", "+5511999002222", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user1.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(1),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user2.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(2),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user1.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("userId", user1.getId().toString())
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).hasSize(2);
          assertThat(reservations.stream().allMatch(r -> r.userId().equals(user1.getId())))
              .isTrue();
        }

        @Test
        @DisplayName("Deve retornar 200 OK e reservas filtradas por intervalo de datas")
        void shouldReturn200AndReservationsFilteredByDateRange() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

          LocalDate baseDate = LocalDate.now().plusDays(5);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              baseDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              baseDate.plusDays(2),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              baseDate.plusDays(10),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());

          LocalDate endDate = baseDate.plusDays(5);

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("startDate", baseDate.toString())
                  .queryParam("endDate", endDate.toString())
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).hasSize(2);
        }
      }

      @Nested
      @DisplayName("Testes de ordenação")
      class SortingTests {

        @Test
        @DisplayName("Deve retornar 200 OK e reservas ordenadas por data DESC (padrão)")
        void shouldReturn200AndReservationsOrderedByCreatedAtDescByDefault() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(3),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(1),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          assertThat(jsonPath.getList("content")).isNotEmpty();
        }

        @Test
        @DisplayName("Deve retornar 200 OK e reservas ordenadas por data ASC")
        void shouldReturn200AndReservationsOrderedByDateAsc() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          mockPersistDefaultPriceRule();
          Modality modality = mockPersistModality("Beach Tennis");
          Court court = mockPersistCourt("Quadra 1", modality);
          User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

          LocalDate futureDate = LocalDate.now().plusDays(10);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(5),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate,
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());
          mockPersistReservationByUser(
              modality.getId(),
              court.getId(),
              futureDate.plusDays(2),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              user.getId());

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("sort", "dateTimeSlot.date,asc")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<ReservationDetailResponseDto> reservations =
              jsonPath.getList("content", ReservationDetailResponseDto.class);

          assertThat(reservations).isNotEmpty();
          assertThat(reservations.getFirst().date()).isEqualTo(futureDate);
        }
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando searchTerm for muito longo")
      void shouldReturn400WhenSearchTermIsTooLong() {
        // Arrange
        String invalidTerm = "a".repeat(101);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("searchTerm", invalidTerm)
                .when()
                .get()
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertValidationError(response, BASE_PATH, "searchTerm", ErrorCode.TERM_TOO_LONG);
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando status for inválido")
      void shouldReturn400WhenStatusIsInvalid() {
        // Arrange
        String invalidStatus = "INVALID_STATUS";

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("status", invalidStatus)
                .when()
                .get()
                .then()
                .log()
                .all()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertValidationError(response, BASE_PATH, "status", ErrorCode.INVALID_REQUEST_PARAMETER);
      }

      @Test
      @DisplayName("Deve retornar 400 Bad Request quando startDate for depois de endDate")
      void shouldReturn400WhenStartDateIsAfterEndDate() {
        // Arrange
        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(5);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("startDate", startDate.toString())
                .queryParam("endDate", endDate.toString())
                .when()
                .get()
                .then()
                .log()
                .all()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(response, 400, BASE_PATH, ErrorCode.START_DATE_AFTER_END_DATE);
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/reservations/{reservationId}/cancel")
  class CancelReservation {

    @Nested
    @DisplayName("Cenários de Sucesso - 204 No Content")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve cancelar uma reserva individual com sucesso")
      void shouldCancelSingleReservationSuccessfully() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reservation reservation =
            mockPersistReservationByUser(
                modality.getId(),
                court.getId(),
                futureDate,
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId());

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .queryParam("cancelAllRecurring", false)
            .when()
            .post("/{reservationId}/cancel", reservation.getId())
            .then()
            .statusCode(204);

        // Assert
        Reservation cancelledReservation = reservationRepository.findByIdOrElseThrow(reservation.getId());
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelledReservation.getCancelledByAdminId()).isEqualTo(adminId);
      }

      @Test
      @DisplayName("Deve cancelar reserva recorrente e todas suas instâncias futuras")
      void shouldCancelRecurringReservationAndAllFutureInstances() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        UUID recurringId = UUID.randomUUID();
        LocalDate futureDate = LocalDate.now().plusDays(7);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        Reservation reservation1 =
            mockPersistRecurringReservation(
                modality.getId(),
                court.getId(),
                futureDate,
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId(),
                adminId,
                recurringId);
        Reservation reservation2 =
            mockPersistRecurringReservation(
                modality.getId(),
                court.getId(),
                futureDate.plusWeeks(1),
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId(),
                adminId,
                recurringId);
        Reservation reservation3 =
            mockPersistRecurringReservation(
                modality.getId(),
                court.getId(),
                futureDate.plusWeeks(2),
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId(),
                adminId,
                recurringId);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .queryParam("cancelAllRecurring", true)
            .when()
            .post("/{reservationId}/cancel", reservation1.getId())
            .then()
            .statusCode(204);

        // Assert
        Reservation cancelled1 = reservationRepository.findByIdOrElseThrow(reservation1.getId());
        Reservation cancelled2 = reservationRepository.findByIdOrElseThrow(reservation2.getId());
        Reservation cancelled3 = reservationRepository.findByIdOrElseThrow(reservation3.getId());

        assertThat(cancelled1.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled2.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelled3.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("Deve retornar 404 Not Found quando reserva não existe")
      void shouldReturn404WhenReservationNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("cancelAllRecurring", false)
                .when()
                .post("/{reservationId}/cancel", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(
            response,
            404,
            BASE_PATH + "/" + nonExistentId + "/cancel",
            ErrorCode.SCHEDULE_ENTRY_NOT_FOUND);
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 409 Conflict")
    class ConflictScenarios {

      @Test
      @DisplayName("Deve retornar 409 Conflict quando reserva já foi cancelada")
      void shouldReturn409WhenReservationAlreadyCancelled() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reservation reservation =
            mockPersistReservationByUserWithStatus(
                modality.getId(),
                court.getId(),
                futureDate,
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId(),
                ReservationStatus.CANCELLED);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("cancelAllRecurring", false)
                .when()
                .post("/{reservationId}/cancel", reservation.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(
            response,
            409,
            BASE_PATH + "/" + reservation.getId() + "/cancel",
            ErrorCode.RESERVATION_ALREADY_CANCELLED);
      }

      @Test
      @DisplayName("Deve retornar 409 Conflict quando reserva já foi concluída")
      void shouldReturn409WhenReservationAlreadyCompleted() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate futureDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        Reservation reservation =
            mockPersistReservationByUserWithStatus(
                modality.getId(),
                court.getId(),
                futureDate,
                timeInterval,
                DEFAULT_RESERVATION_PRICE,
                user.getId(),
                ReservationStatus.COMPLETED);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .queryParam("cancelAllRecurring", false)
                .when()
                .post("/{reservationId}/cancel", reservation.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(
            response,
            409,
            BASE_PATH + "/" + reservation.getId() + "/cancel",
            ErrorCode.RESERVATION_ALREADY_COMPLETED);
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/reservations")
  class CreateReservation {

    @Nested
    @DisplayName("Cenários de Sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Deve criar uma reserva individual com sucesso")
      void shouldCreateSingleReservationSuccessfully() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<List<ReservationDetailResponseDto>>() {});

        // Assert
        assertThat(response).hasSize(1);
        ReservationDetailResponseDto createdReservation = response.getFirst();
        assertThat(createdReservation.userId()).isEqualTo(user.getId());
        assertThat(createdReservation.courtId()).isEqualTo(court.getId());
        assertThat(createdReservation.date()).isEqualTo(reservationDate);
        assertThat(createdReservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
      }

      @Test
      @DisplayName("Deve criar reservas recorrentes com sucesso")
      void shouldCreateRecurringReservationsSuccessfully() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate startDate = nextDayOfWeek(java.time.DayOfWeek.MONDAY);
        LocalDate endDate = startDate.plusWeeks(4);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
        Set<DayOfWeek> selectedDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                startDate,
                endDate,
                timeInterval,
                selectedDays);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .post()
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<List<ReservationDetailResponseDto>>() {});

        // Assert
        assertThat(response).isNotEmpty();
        assertThat(response.stream().allMatch(r -> r.userId().equals(user.getId()))).isTrue();
        assertThat(response.stream().allMatch(r -> r.status().equals(ReservationStatus.CONFIRMED)))
            .isTrue();
        assertThat(response.stream().allMatch(r -> r.recurringReservationId() != null)).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 400 Bad Request")
    class BadRequestScenarios {

      @Test
      @DisplayName("Deve retornar 400 quando telefone não informado")
      void shouldReturn400WhenPhoneNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                null,
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(response, BASE_PATH, "userPhone", ErrorCode.PHONE_REQUIRED);
      }

      @Test
      @DisplayName("Deve retornar 400 quando telefone tem formato inválido")
      void shouldReturn400WhenPhoneHasInvalidFormat() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                "invalid-phone",
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(response, BASE_PATH, "userPhone", ErrorCode.PHONE_INVALID_FORMAT);
      }

      @Test
      @DisplayName("Deve retornar 400 quando o telefone é inválido")
      void shouldReturn400WhenPhoneIsInvalid() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                "+5511111111111",
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

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

        // Assert
        assertBusinessError(response, 400, BASE_PATH, ErrorCode.PHONE_INVALID);
      }

      @Test
      @DisplayName("Deve retornar 400 quando courtId não informado")
      void shouldReturn400WhenCourtIdNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                null,
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "courtId", ErrorCode.RESERVATION_COURT_ID_REQUIRED);
      }

      @Test
      @DisplayName("Deve retornar 400 quando modalityId não informado")
      void shouldReturn400WhenModalityIdNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                null,
                reservationDate,
                reservationDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "modalityId", ErrorCode.RESERVATION_MODALITY_ID_REQUIRED);
      }

      @Test
      @DisplayName("Deve retornar 400 quando startDate não informada")
      void shouldReturn400WhenStartDateNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                null,
                LocalDate.now().plusDays(1),
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "startDate", ErrorCode.RESERVATION_START_DATE_REQUIRED);
      }

      @Test
      @DisplayName("Deve retornar 400 quando endDate não informada")
      void shouldReturn400WhenEndDateNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                LocalDate.now().plusDays(1),
                null,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "endDate", ErrorCode.RESERVATION_END_DATE_REQUIRED);
      }

      @Test
      @DisplayName("Deve retornar 400 quando timeInterval não informado")
      void shouldReturn400WhenTimeIntervalNotProvided() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                null,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "timeInterval", ErrorCode.RESERVATION_TIME_INTERVAL_REQUIRED);
      }

      @InvalidTimeIntervalProvider
      @DisplayName("Deve retornar 400 quando timeInterval é inválido")
      void shouldReturn400WhenTimeIntervalIsInvalid(
          LocalTime startTime, LocalTime endTime, String expectedErrorCode) {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);

        Map<String, Object> timeIntervalMap = new HashMap<>();
        timeIntervalMap.put("startTime", startTime);
        timeIntervalMap.put("endTime", endTime);

        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("userPhone", user.getPhone());
        jsonRequest.put("courtId", court.getId());
        jsonRequest.put("modalityId", modality.getId());
        jsonRequest.put("startDate", reservationDate.toString());
        jsonRequest.put("endDate", reservationDate.toString());
        jsonRequest.put("timeInterval", timeIntervalMap);
        jsonRequest.put("selectedDaysOfWeek", null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "timeInterval", ErrorCode.valueOf(expectedErrorCode));
      }

      @Test
      @DisplayName("Deve retornar 400 quando data está no passado")
      void shouldReturn400WhenDateIsInThePast() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate pastDate = LocalDate.now().minusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                pastDate,
                pastDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "startDate", ErrorCode.BLOCKED_TIME_START_DATE_IN_PAST);
      }

      @Test
      @DisplayName("Deve retornar 400 quando startDate é depois de endDate")
      void shouldReturn400WhenStartDateIsAfterEndDate() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate startDate = LocalDate.now().plusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(5);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                startDate,
                endDate,
                timeInterval,
                null);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "startDate", ErrorCode.BLOCKED_TIME_START_DATE_AFTER_END_DATE);
      }

      @InvalidDaysOfWeekProvider
      @DisplayName("Deve retornar 400 quando o selectedDaysOfWeek for inválido")
      void shouldReturn400WhenSelectedDaysOfWeekIsInvalid(
          String[] invalidDay, String expectedErrorCode) {
        // Arrange
        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("userPhone", "+5511999001111");
        jsonRequest.put("courtId", UUID.randomUUID());
        jsonRequest.put("modalityId", UUID.randomUUID());
        jsonRequest.put("startDate", LocalDate.now());
        jsonRequest.put("endDate", LocalDate.now());
        jsonRequest.put("timeInterval", new TimeInterval(LocalTime.of(9, 0), LocalTime.of(11, 0)));
        jsonRequest.put("selectedDaysOfWeek", invalidDay);

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

        // Assert
        assertValidationError(
            response, BASE_PATH, "selectedDaysOfWeek", ErrorCode.valueOf(expectedErrorCode));
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 404 Not Found")
    class NotFoundScenarios {

      @Test
      @DisplayName("Deve retornar 404 quando usuário com o telefone não existe")
      void shouldReturn404WhenUserWithPhoneNotFound() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                "+5511999999999",
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

        // Act
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

        // Assert
        assertBusinessError(response, 404, BASE_PATH, ErrorCode.USER_NOT_FOUND);
      }

      @Test
      @DisplayName("Deve retornar 404 quando modalidade não existe")
      void shouldReturn404WhenModalityNotFound() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                UUID.randomUUID(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

        // Act
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

        // Assert
        assertBusinessError(response, 404, BASE_PATH, ErrorCode.MODALITY_NOT_FOUND);
      }
    }

    @Nested
    @DisplayName("Cenários de Erro - 409 Conflict")
    class ConflictScenarios {

      @Test
      @DisplayName("Deve retornar 409 quando horário já está ocupado")
      void shouldReturn409WhenTimeSlotIsOccupied() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality = mockPersistModality("Beach Tennis");
        Court court = mockPersistCourt("Quadra 1", modality);
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");
        User otherUser = mockPersistUser("client2", "Cliente 2", "+5511999002222", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        // Create existing reservation
        mockPersistReservationByUser(
            modality.getId(),
            court.getId(),
            reservationDate,
            timeInterval,
            DEFAULT_RESERVATION_PRICE,
            otherUser.getId());

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality.getId(),
                reservationDate,
                reservationDate,
                timeInterval,
                null);

        // Act
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

        // Assert
        assertBusinessError(response, 409, BASE_PATH, ErrorCode.SCHEDULE_ENTRY_NOT_AVAILABLE);
      }

      @Test
      @DisplayName("Deve retornar 409 quando quadra não suporta a modalidade")
      void shouldReturn409WhenCourtDoesNotSupportModality() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        mockPersistDefaultPriceRule();
        Modality modality1 = mockPersistModality("Beach Tennis");
        Modality modality2 = mockPersistModality("Futevôlei");
        Court court = mockPersistCourt("Quadra 1", modality1); // Court only supports Beach Tennis
        User user = mockPersistUser("client1", "Cliente 1", "+5511999001111", "123456");

        LocalDate reservationDate = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

        var request =
            new AdminReservationCreateRequestDto(
                user.getPhone(),
                court.getId(),
                modality2.getId(), // Using different modality
                reservationDate,
                reservationDate,
                timeInterval,
                null);

        // Act
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

        // Assert
        assertBusinessError(response, 409, BASE_PATH, ErrorCode.COURT_NOT_SUPPORTS_MODALITY);
      }
    }
  }
}
