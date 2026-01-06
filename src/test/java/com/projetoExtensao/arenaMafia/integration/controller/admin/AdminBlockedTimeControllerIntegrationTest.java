package com.projetoExtensao.arenaMafia.integration.controller.admin;

import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConflictsPreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.BlockedTime.InvalidListOfCourtIdsProvider;
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
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AdminBlockedTimeController")
public class AdminBlockedTimeControllerIntegrationTest extends WebIntegrationTestConfig {

  private static final BigDecimal DEFAULT_RESERVATION_PRICE = BigDecimal.valueOf(50);
  
  @Autowired private BlockedTimePreviewCachePort blockedTimePreviewCachePort;
  @Autowired private BlockedTimeRepositoryPort blockedTimeRepositoryPort;
  private RequestSpecification specification;
  private String accessToken;
  private UUID adminId;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/blocked-times")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    User admin = mockPersistAdminUser();
    adminId = admin.getId();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/blocked-times/preview-conflicts")
  class PreviewConflictsTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class SuccessScenarios {

      @Nested
      @DisplayName("Bloqueios Pontuais")
      class BlockedTimeSpecificScenarios {
        // ========== Cenários com isFullDay = true ============
        @Test
        @DisplayName("Cria preview de bloqueio para dia todo sem conflitos")
        void shouldCreatePreviewForFullDayWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
          assertThat(previewSaved.usersAffected()).isEqualTo(response.usersAffected());
          assertThat(previewSaved.blockedTimesAffected())
              .isEqualTo(response.blockedTimesAffected());
          assertThat(previewSaved.reservationsAffected())
              .isEqualTo(response.reservationsAffected());
          assertThat(previewSaved.conflictingBlockedTimes())
              .isEqualTo(response.conflictingBlockedTimes());
          assertThat(previewSaved.conflictingReservations())
              .isEqualTo(response.conflictingReservations());
          assertThat(previewSaved.inProgressReservations())
              .isEqualTo(response.inProgressReservations());
          assertThat(previewSaved.request()).usingRecursiveComparison().isEqualTo(requestDto);
        }

        @Test
        @DisplayName("Cria preview de bloqueio para dia todo com conflitos")
        void shouldCreatePreviewForFullDayWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              date,
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId,
              date,
              new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
              "Manutenção",
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.reservationsAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(1);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
          assertThat(previewSaved.usersAffected()).isEqualTo(response.usersAffected());
          assertThat(previewSaved.blockedTimesAffected())
              .isEqualTo(response.blockedTimesAffected());
          assertThat(previewSaved.reservationsAffected())
              .isEqualTo(response.reservationsAffected());
          assertThat(previewSaved.conflictingBlockedTimes())
              .isEqualTo(response.conflictingBlockedTimes());
          assertThat(previewSaved.conflictingReservations())
              .isEqualTo(response.conflictingReservations());
          assertThat(previewSaved.inProgressReservations())
              .isEqualTo(response.inProgressReservations());
          assertThat(previewSaved.request()).usingRecursiveComparison().isEqualTo(requestDto);
        }

        @Test
        @DisplayName("Cria preview de bloqueio para dia todo com reservas em andamento")
        void shouldCreatePreviewForFullDayWithInProgressReservations() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId1 = mockPersistCourt("Quadra 1", modality).getId();
          UUID courtId2 = mockPersistCourt("Quadra 2", modality).getId();
          LocalDate date = LocalDate.now();

          LocalTime currentTime = LocalTime.now();
          LocalTime startTime = normalizeToValidMinutes(currentTime.minusMinutes(30));
          LocalTime endTime = normalizeToValidMinutes(currentTime.plusMinutes(30));
          TimeInterval timeInterval = new TimeInterval(startTime, endTime);

          mockPersistReservationByUser(
              modality.getId(), courtId2, date, timeInterval, DEFAULT_RESERVATION_PRICE, adminId);
          mockPersistReservationByUser(
              modality.getId(), courtId1, date, timeInterval, DEFAULT_RESERVATION_PRICE, adminId);
          mockPersistBlockedTimeSpecific(courtId1, date, timeInterval, "Manutenção", adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId1), date, date, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations().size()).isEqualTo(1);

          // Verifica dados da reserva em andamento
          var inProgressReservation = response.inProgressReservations().getFirst();
          assertThat(inProgressReservation.courtName()).isEqualTo("Quadra 1");
          assertThat(inProgressReservation.modalityName()).isEqualTo("Futebol");
          assertThat(inProgressReservation.date()).isEqualTo(date);
          assertThat(inProgressReservation.status()).isEqualTo(ReservationStatus.CONFIRMED);

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
          assertThat(previewSaved.usersAffected()).isEqualTo(response.usersAffected());
          assertThat(previewSaved.blockedTimesAffected())
              .isEqualTo(response.blockedTimesAffected());
          assertThat(previewSaved.reservationsAffected())
              .isEqualTo(response.reservationsAffected());
          assertThat(previewSaved.conflictingBlockedTimes())
              .isEqualTo(response.conflictingBlockedTimes());
          assertThat(previewSaved.conflictingReservations())
              .isEqualTo(response.conflictingReservations());
          assertThat(previewSaved.inProgressReservations())
              .isEqualTo(response.inProgressReservations());
          assertThat(previewSaved.request()).usingRecursiveComparison().isEqualTo(requestDto);
        }

        // =========== Cenários com timeInterval específico ============
        @Test
        @DisplayName("Cria preview de bloqueio para horário específico sem conflitos")
        void shouldCreatePreviewForSpecificTimeWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, timeInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
          assertThat(previewSaved.usersAffected()).isEqualTo(response.usersAffected());
          assertThat(previewSaved.blockedTimesAffected())
              .isEqualTo(response.blockedTimesAffected());
          assertThat(previewSaved.reservationsAffected())
              .isEqualTo(response.reservationsAffected());
          assertThat(previewSaved.conflictingBlockedTimes())
              .isEqualTo(response.conflictingBlockedTimes());
          assertThat(previewSaved.conflictingReservations())
              .isEqualTo(response.conflictingReservations());
          assertThat(previewSaved.inProgressReservations())
              .isEqualTo(response.inProgressReservations());
          assertThat(previewSaved.request()).usingRecursiveComparison().isEqualTo(requestDto);
        }

        @Test
        @DisplayName("Cria preview de bloqueio para horário específico com conflitos")
        void shouldCreatePreviewForSpecificTimeWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval conflictInterval =
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              date,
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(courtId, date, conflictInterval, "Manutenção", adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, conflictInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.reservationsAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(1);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
          assertThat(previewSaved.usersAffected()).isEqualTo(response.usersAffected());
          assertThat(previewSaved.blockedTimesAffected())
              .isEqualTo(response.blockedTimesAffected());
          assertThat(previewSaved.reservationsAffected())
              .isEqualTo(response.reservationsAffected());
          assertThat(previewSaved.conflictingBlockedTimes())
              .isEqualTo(response.conflictingBlockedTimes());
          assertThat(previewSaved.conflictingReservations())
              .isEqualTo(response.conflictingReservations());
          assertThat(previewSaved.inProgressReservations())
              .isEqualTo(response.inProgressReservations());
          assertThat(previewSaved.request()).usingRecursiveComparison().isEqualTo(requestDto);
        }

        @Test
        @DisplayName("Cria preview de bloqueio para horário específico com reservas em andamento")
        void shouldCreatePreviewForSpecificTimeWithInProgressReservations() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now();

          LocalTime currentTime = LocalTime.now();
          LocalTime startTime = normalizeToValidMinutes(currentTime.minusMinutes(30));
          LocalTime endTime = normalizeToValidMinutes(currentTime.plusMinutes(30));
          TimeInterval timeInterval = new TimeInterval(startTime, endTime);

          mockPersistReservationByUser(
              modality.getId(), courtId, date, timeInterval, DEFAULT_RESERVATION_PRICE, adminId);
          mockPersistBlockedTimeSpecific(courtId, date, timeInterval, "Manutenção", adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, timeInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations().size()).isEqualTo(1);
        }

        @Test
        @DisplayName(
            "Cria preview de bloqueio para horário específico que atravessa a meia-noite sem conflitos")
        void shouldCreatePreviewForSpecificTimeCrossingMidnightWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDaysWithTimeInterval(
              new TimeInterval(LocalTime.of(13, 30), LocalTime.of(2, 30)));
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(23, 0), LocalTime.of(1, 0));

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, timeInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName(
            "Cria preview de bloqueio para horário específico que atravessa a meia-noite com conflitos")
        void shouldCreatePreviewForSpecificTimeCrossingMidnightWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDaysWithTimeInterval(
              new TimeInterval(LocalTime.of(13, 30), LocalTime.of(2, 30)));
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(23, 0), LocalTime.of(1, 0));

          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              date,
              new TimeInterval(LocalTime.of(0, 0), LocalTime.of(1, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), date, date, timeInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isOne();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations().size()).isEqualTo(1);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }
      }

      @Nested
      @DisplayName("Bloqueios Consecutivos")
      class BlockedTimeConsecutiveScenarios {

        @Test
        @DisplayName(
            "Cria preview de bloqueio consecutivo para 15 dias para o dia todo sem conflitos")
        void shouldCreatePreviewForFullDayWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          UUID courtId = mockPersistCourt("Quadra 1", mockPersistModality("Futebol")).getId();
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(15);

          var request =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(request)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName(
            "Cria preview de bloqueio consecutivo para 14 dias para dia todo com conflitos")
        void shouldCreatePreviewForFullDayWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(14);

          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(7),
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(9),
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(14),
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId,
              startDate.plusDays(14),
              new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
              "Manutenção",
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isEqualTo(1);
          assertThat(response.reservationsAffected()).isEqualTo(3);
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(3);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName(
            "Cria preview de bloqueio consecutivo para 21 dia com horário específico sem conflitos")
        void shouldCreatePreviewForSpecificTimeWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          UUID courtId = mockPersistCourt("Quadra 1", mockPersistModality("Futebol")).getId();
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(21);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, timeInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName(
            "Cria preview de bloqueio consecutivo para 7 dias com horário específico com conflitos")
        void shouldCreatePreviewForSpecificTimeWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(7);
          TimeInterval conflictInterval =
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(3),
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(5),
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId, startDate.plusDays(5), conflictInterval, "Manutenção", adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, conflictInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.reservationsAffected()).isEqualTo(2);
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(2);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }
      }

      @Nested
      @DisplayName("Bloqueios Recorrentes")
      class BlockedTimeRecurringScenarios {

        @Test
        @DisplayName("Cria preview de bloqueio recorrente para o dia todo sem conflitos")
        void shouldCreatePreviewForFullDayWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          UUID courtId = mockPersistCourt("Quadra 1", mockPersistModality("Futebol")).getId();

          // Define um intervalo de 6 meses que comece na próxima terça e termine na quinta-feira
          LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.TUESDAY);
          LocalDate endDate = startDate.plusMonths(6).with(java.time.DayOfWeek.THURSDAY);
          Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, null, true, selectedDaysOfWeek);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName("Cria preview de bloqueio recorrente para o dia todo com conflitos")
        void shouldCreatePreviewForFullDayWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

          // Define um intervalo de 3 meses que comece na próxima segunda e termine na sexta-feira
          LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
          LocalDate endDate = startDate.plusMonths(3).with(java.time.DayOfWeek.FRIDAY);
          Set<DayOfWeek> selectedDaysOfWeek =
              Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

          // Mock de uma reserva que não deve conflitar
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(5).with(java.time.DayOfWeek.TUESDAY), // Terça-feira
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);

          // Mock de reservas e bloqueios que conflitam com as segundas e sextas-feiras
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusWeeks(2).with(java.time.DayOfWeek.WEDNESDAY), // Quarta-feira
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusWeeks(4).with(java.time.DayOfWeek.MONDAY), // Segunda-feira
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusWeeks(2).with(java.time.DayOfWeek.FRIDAY), // Sexta-feira
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId,
              startDate.plusWeeks(6).with(java.time.DayOfWeek.FRIDAY), // Sexta-feira
              timeInterval,
              "Manutenção",
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, null, true, selectedDaysOfWeek);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isEqualTo(1);
          assertThat(response.reservationsAffected()).isEqualTo(3);
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(3);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName("Cria preview de bloqueio recorrente para horário específico sem conflitos")
        void shouldCreatePreviewForSpecificTimeWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          UUID courtId = mockPersistCourt("Quadra 1", mockPersistModality("Futebol")).getId();

          // Define um intervalo de 4 meses que comece na próxima quarta e termine na sexta-feira
          LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.WEDNESDAY);
          LocalDate endDate = startDate.plusMonths(4).with(java.time.DayOfWeek.FRIDAY);
          Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId), startDate, endDate, timeInterval, false, selectedDaysOfWeek);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isZero();
          assertThat(response.blockedTimesAffected()).isZero();
          assertThat(response.reservationsAffected()).isZero();
          assertThat(response.conflictingBlockedTimes()).isEmpty();
          assertThat(response.conflictingReservations()).isEmpty();
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName("Cria preview de bloqueio recorrente para horário específico com conflitos")
        void shouldCreatePreviewForSpecificTimeWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          TimeInterval conflictInterval =
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));
          TimeInterval blockedTimeInterval =
              new TimeInterval(LocalTime.of(9, 0), LocalTime.of(12, 0));

          // Define um intervalo de 2 meses que comece na próxima quinta e termine no sábado
          LocalDate startDate = LocalDate.now().with(java.time.DayOfWeek.THURSDAY);
          LocalDate endDate = startDate.plusMonths(2).with(java.time.DayOfWeek.SATURDAY);
          Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);

          // Mock de uma reserva que não deve conflitar
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(3).with(java.time.DayOfWeek.FRIDAY), // Sexta-feira
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);

          // Mock de reservas e bloqueios que conflitam com as quintas e sábados
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusWeeks(1).with(java.time.DayOfWeek.THURSDAY), // Quinta-feira
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusWeeks(3).with(java.time.DayOfWeek.SATURDAY), // Sábado
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId,
              startDate.plusWeeks(5).with(java.time.DayOfWeek.SATURDAY), // Sábado
              conflictInterval,
              "Manutenção",
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId),
                  startDate,
                  endDate,
                  blockedTimeInterval,
                  false,
                  selectedDaysOfWeek);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isEqualTo(1);
          assertThat(response.reservationsAffected()).isEqualTo(2);
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(2);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }
      }

      @Nested
      @DisplayName("Bloqueios Para Múltiplas Quadras")
      class BlockedTimeMultipleCourtsScenarios {

        @Test
        @DisplayName("Cria preview de bloqueio para horário específico com conflitos em múltiplas quadras")
        void shouldCreatePreviewForMultipleCourtsWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId1 = mockPersistCourt("Quadra 1", modality).getId();
          UUID courtId2 = mockPersistCourt("Quadra 2", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval conflictInterval =
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

          mockPersistReservationByUser(
              modality.getId(),
              courtId1,
              date,
              conflictInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(courtId2, date, conflictInterval, "Manutenção", adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId1, courtId2), date, date, conflictInterval, false, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.reservationsAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(1);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }

        @Test
        @DisplayName("Cria preview de bloqueio para o dia todo com conflitos em múltiplas quadras")
        void shouldCreatePreviewForMultipleCourtsFullDayWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId1 = mockPersistCourt("Quadra 1", modality).getId();
          UUID courtId2 = mockPersistCourt("Quadra 2", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          mockPersistReservationByUser(
              modality.getId(),
              courtId1,
              date,
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistBlockedTimeSpecific(
              courtId2,
              date,
              new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
              "Manutenção",
              adminId);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId1, courtId2), date, date, null, true, null);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConflictsPreviewResponseDto.class);

          // Assert
          assertThat(response.usersAffected()).isOne();
          assertThat(response.blockedTimesAffected()).isOne();
          assertThat(response.reservationsAffected()).isOne();
          assertThat(response.conflictingBlockedTimes().size()).isEqualTo(1);
          assertThat(response.conflictingReservations().size()).isEqualTo(1);
          assertThat(response.inProgressReservations()).isEmpty();

          Optional<BlockedTimeConflictsPreview> previewSavedOpt =
              getPreviewSavedFromCache(response.previewKey());
          assertThat(previewSavedOpt).isPresent();

          BlockedTimeConflictsPreview previewSaved = previewSavedOpt.get();
          assertThat(previewSaved.previewKey()).isEqualTo(response.previewKey());
        }
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class BadRequestScenarios {

      @InvalidListOfCourtIdsProvider
      @DisplayName("Tenta criar bloqueio com lista inválida de IDs de quadras")
      void shouldReturn400WhenInvalidListOfCourtIds(
          List<UUID> invalidCourtIds, String expectedErrorMessage) {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                invalidCourtIds,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                null,
                true,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorMessage);

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("courtIds")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio com startDate null")
      void shouldReturn400WhenStartDateIsNull() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()), null, LocalDate.now().plusDays(1), null, true, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode = ErrorCode.BLOCKED_TIME_START_DATE_REQUIRED;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("startDate")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio com endDate null")
      void shouldReturn400WhenEndDateIsNull() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()), LocalDate.now().plusDays(1), null, null, true, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode = ErrorCode.BLOCKED_TIME_END_DATE_REQUIRED;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("endDate")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      @InvalidTimeIntervalProvider
      @DisplayName("Tenta criar bloqueio com intervalo de tempo inválido")
      void shouldReturn400WhenInvalidTimeInterval(
          String startTime, String endTime, String expectedErrorCode) {
        // Arrange
        Map<String, Object> timeInterval = new HashMap<>();
        timeInterval.put("startTime", startTime);
        timeInterval.put("endTime", endTime);

        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("courtIds", List.of(UUID.randomUUID()));
        jsonRequest.put("startDate", LocalDate.now().plusDays(1).toString());
        jsonRequest.put("endDate", LocalDate.now().plusDays(1).toString());
        jsonRequest.put("timeInterval", timeInterval);
        jsonRequest.put("isFullDay", false);
        jsonRequest.put("selectedDaysOfWeek", null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(jsonRequest)
                .when()
                .post("/preview-conflicts")
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
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio com isFullDay null")
      void shouldReturn400WhenIsFullDayIsNull() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                null,
                null,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode = ErrorCode.BLOCKED_TIME_IS_FULL_DAY_REQUIRED;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("isFullDay")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      // ============ Regras de Negócio DTO ============
      @Test
      @DisplayName("Tenta criar bloqueio com startDate no passado")
      void shouldReturn400WhenStartDateInPast() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                null,
                true,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode = ErrorCode.BLOCKED_TIME_START_DATE_IN_PAST;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("startDate")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio com endDate antes de startDate")
      void shouldReturn400WhenEndDateBeforeStartDate() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(1),
                null,
                true,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode = ErrorCode.BLOCKED_TIME_START_DATE_AFTER_END_DATE;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("startDate")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio para o horário específico sem informar o timeInterval")
      void shouldReturn400WhenIsFullDayFalseAndTimeIntervalNull() {
        // Arrange
        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(UUID.randomUUID()),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                null,
                false,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode expectedErrorCode =
            ErrorCode.BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(expectedErrorCode.name())
                        && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio excedendo o limite máximo de ocorrências")
      void shouldReturn400WhenExceedingMaxOccurrences() {
        // Arrange
        TimeInterval timeIntervalOh = mockPersistOperatingHoursAllDays().getTimeInterval();
        Modality modality = mockPersistModality("Futebol");
        UUID courtId1 = mockPersistCourt("Quadra 1", modality).getId();
        UUID courtId2 = mockPersistCourt("Quadra 2", modality).getId();
        UUID courtId3 = mockPersistCourt("Quadra 3", modality).getId();

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(courtId1, courtId2, courtId3),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(365),
                timeIntervalOh,
                false,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_TOO_MANY_OCCURRENCES;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }

      @Test
      @DisplayName("Tenta criar bloqueio fora do horário de funcionamento")
      void shouldReturn400WhenOutsideOperatingHours() {
        // Arrange
        mockPersistOperatingHours();
        Modality modality = mockPersistModality("Futebol");
        UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

        TimeInterval invalidInterval = new TimeInterval(LocalTime.of(7, 0), LocalTime.of(8, 0));

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(courtId),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
                invalidInterval,
                false,
                null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_OUTSIDE_OPERATING_HOURS;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }

      @Test
      @DisplayName("Tenta criar bloqueio com todos os dias da semana inválidos fora do intervalo de datas informado")
      void shouldReturn400WhenSelectedDaysOfWeekOutsideDateRange() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        Modality modality = mockPersistModality("Futebol");
        UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

        LocalDate startDateMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate endDateDateWed = LocalDate.now().with(java.time.DayOfWeek.WEDNESDAY);

        // Seleciona quinta-feira (4) e sexta-feira (5), que estão fora do intervalo de segunda a quarta
        Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY);

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(courtId),
                startDateMonday,
                endDateDateWed,
                null,
                true,
                selectedDaysOfWeek);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_SELECTED_DAYS_OUTSIDE_DATE_RANGE;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }

      @Test
      @DisplayName("Tenta criar bloqueio com um dia da semana inválido fora do intervalo de datas informado")
      void shouldReturn400WhenOneSelectedDayOfWeekOutsideDateRange() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        Modality modality = mockPersistModality("Futebol");
        UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

        LocalDate startDateMonday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate endDateDateWed = LocalDate.now().with(java.time.DayOfWeek.WEDNESDAY);

        // Seleciona quinta-feira (4) e sexta-feira (5), que estão fora do intervalo de segunda a
        // quarta
        Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(courtId), startDateMonday, endDateDateWed, null, true, selectedDaysOfWeek);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_SELECTED_DAYS_OUTSIDE_DATE_RANGE;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }

      @Test
      @DisplayName("Tenta criar bloqueio para um único dia informando selectedDaysOfWeek")
      void shouldReturn400WhenSelectedDaysOfWeekInformedForSingleDate() {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(12, 0));
        LocalDate singleDate = LocalDate.now().plusDays(1);
        Set<DayOfWeek> selectedDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

        var requestDto = new BlockedTimeConflictsPreviewRequestDto(
            List.of(UUID.randomUUID()),
            singleDate,
            singleDate,
            timeInterval,
            false,
            selectedDays); // Não deveria ser informado para um único dia

        // Act
        var response = given()
            .spec(specification)
            .header("Authorization", accessToken)
            .body(requestDto)
            .when()
            .post("/preview-conflicts")
            .then()
                .log().all()
            .statusCode(400)
            .extract()
            .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_SELECTED_DAYS_NOT_ALLOWED_FOR_SINGLE_DATE;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("selectedDaysOfWeek")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta criar bloqueio para dia inteiro informando timeInterval")
      void shouldReturn400WhenTimeIntervalInformedForFullDay() {
        // Arrange
        mockPersistOperatingHoursAllDays();
        Modality modality = mockPersistModality("Futebol");
        UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

        LocalDate date = LocalDate.now().plusDays(1);
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

        var requestDto = new BlockedTimeConflictsPreviewRequestDto(
            List.of(courtId),
            date,
            date,
            timeInterval, // Não deveria ser informado quando isFullDay=true
            true,
            null);

        // Act
        var response = given()
            .spec(specification)
            .header("Authorization", accessToken)
            .body(requestDto)
            .when()
            .post("/preview-conflicts")
            .then()
            .statusCode(400)
            .extract()
            .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.BLOCKED_TIME_TIME_INTERVAL_NOT_ALLOWED_WHEN_FULL_DAY;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
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
      @DisplayName("Tenta criar bloqueio para dia sem horário de funcionamento")
      void shouldReturn404WhenNoOperatingHoursForDay() {
        // Arrange
        mockPersistOperatingHours(); // Não cria OH para domingo
        Modality modality = mockPersistModality("Futebol");
        UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

        LocalDate nextSunday = LocalDate.now().with(java.time.DayOfWeek.SUNDAY);
        TimeInterval validInterval = new TimeInterval(LocalTime.of(9, 0), LocalTime.of(10, 0));

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(courtId), nextSunday, nextSunday, validInterval, false, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }

      @Test
      @DisplayName("Tenta criar bloqueio para quadra inexistente")
      void shouldReturn404WhenCourtNotFound() {
        // Arrange
        TimeInterval timeIntervalOh = mockPersistOperatingHoursAllDays().getTimeInterval();
        LocalDate date = LocalDate.now().plusDays(1);
        Court court = mockPersistCourt("Quadra 1", mockPersistModality("Futebol"));

        var requestDto =
            new BlockedTimeConflictsPreviewRequestDto(
                List.of(court.getId(), UUID.randomUUID()), date, date, timeIntervalOh, false, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .post("/preview-conflicts")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.COURT_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
        assertThat(response.path()).isEqualTo("/api/admin/blocked-times/preview-conflicts");
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/blocked-times/confirm")
  class ConfirmBlockedTimeTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class SuccessScenarios {}
  }

  /**
   * Recupera um preview salvo no cache pelo previewKey.
   * Retorna Optional.empty() se o preview não for encontrado ou não pertencer ao admin
   * @param previewKey Chave do preview
   * @return Optional com o preview salvo ou vazio se não encontrado
   */
  private Optional<BlockedTimeConflictsPreview> getPreviewSavedFromCache(String previewKey) {
    try{
      blockedTimePreviewCachePort.validateKeyOwnership(previewKey, adminId);
      BlockedTimeConflictsPreview previewSaved = blockedTimePreviewCachePort.getPreviewOrElseThrow(previewKey);
      return Optional.of(previewSaved);
    } catch (BlockedTimeNotFoundException e) {
      return Optional.empty();
    }
  }

  /**
   * Normaliza um horário para ter minutos válidos (0 ou 30).
   * Essencial para simular reservas em andamento que sigam a regra de negócio.
   * Regras de arredondamento:
   * - 0-14 minutos → arredonda para baixo para 0
   * - 15-44 minutos → arredonda para 30
   * - 45-59 minutos → arredonda para hora seguinte com 0 minutos
   */
  private LocalTime normalizeToValidMinutes(LocalTime time) {
    int minutes = time.getMinute();

    if (minutes < 15) {
      // Arredonda para baixo: 14:05 → 14:00
      return time.withMinute(0).withSecond(0).withNano(0);
    } else if (minutes < 45) {
      // Arredonda para 30: 14:25 → 14:30
      return time.withMinute(30).withSecond(0).withNano(0);
    } else {
      // Arredonda para próxima hora: 14:50 → 15:00
      return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }
  }

}
