package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PreviewNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConfirmRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeUpdateRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConfirmResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.response.BlockedTimeConflictsPreviewResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.schedule.dto.response.scheduleDetail.BlockedTimeDetailResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.BlockedTime.InvalidListOfCourtIdsProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.common.mapper.TypeRef;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
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
@DisplayName("Testes de integração para AdminBlockedTimeController")
public class AdminBlockedTimeControllerIntegrationTest extends WebIntegrationTestConfig {

  private static final String CONFIRM_PATH = "/api/admin/blocked-times/confirm";
  private static final String PREVIEW_CONFLICTS_PATH = "/api/admin/blocked-times/preview-conflicts";
  private static final BigDecimal DEFAULT_RESERVATION_PRICE = BigDecimal.valueOf(50);

  @Autowired private OperatingHoursRepositoryPort operatingHoursRepository;
  @Autowired private CourtRepositoryPort courtRepository;
  @Autowired private BlockedTimePreviewCachePort blockedTimePreviewCachePort;
  @Autowired private BlockedTimeRepositoryPort blockedTimeRepository;
  @Autowired private ReservationRepositoryPort reservationRepository;
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
  @DisplayName("Testes para a funcionalidade de criação de BlockedTime")
  class CreateBlockedTimeTests {
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
            assertThat(previewSaved.conflictingBlockedTimes().size())
                .isEqualTo(response.conflictingBlockedTimes().size());
            assertThat(previewSaved.conflictingReservations().size())
                .isEqualTo(response.conflictingReservations().size());
            assertThat(previewSaved.inProgressReservations().size())
                .isEqualTo(response.inProgressReservations().size());
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

            assertThat(previewSaved.conflictingBlockedTimes().size())
                .isEqualTo(response.conflictingBlockedTimes().size());
            assertThat(previewSaved.conflictingReservations().size())
                .isEqualTo(response.conflictingReservations().size());
            assertThat(previewSaved.inProgressReservations().size())
                .isEqualTo(response.inProgressReservations().size());
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

            assertThat(previewSaved.conflictingBlockedTimes().size())
                .isEqualTo(response.conflictingBlockedTimes().size());
            assertThat(previewSaved.conflictingReservations().size())
                .isEqualTo(response.conflictingReservations().size());
            assertThat(previewSaved.inProgressReservations().size())
                .isEqualTo(response.inProgressReservations().size());
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

            assertThat(previewSaved.conflictingBlockedTimes().size())
                .isEqualTo(response.conflictingBlockedTimes().size());
            assertThat(previewSaved.conflictingReservations().size())
                .isEqualTo(response.conflictingReservations().size());
            assertThat(previewSaved.inProgressReservations().size())
                .isEqualTo(response.inProgressReservations().size());

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

            LocalTime currentTime = LocalTime.of(9, 0);
            LocalTime startTime = normalizeToValidMinutes(currentTime.minusMinutes(30));
            LocalTime endTime = normalizeToValidMinutes(currentTime.plusMinutes(30));
            TimeInterval timeInterval = new TimeInterval(startTime, endTime);

            mockPersistReservationByUser(
                modality.getId(), courtId, date, timeInterval, DEFAULT_RESERVATION_PRICE, adminId);
            mockPersistBlockedTimeSpecific(courtId, date, timeInterval, "Manutenção", adminId);

            var requestDto = new BlockedTimeConflictsPreviewRequestDto(List.of(courtId), date, date, timeInterval, false, null);

            // Act
            var response =
                given()
                    .spec(specification)
                    .header("Authorization", accessToken)
                    .body(requestDto)
                    .when()
                    .post("/preview-conflicts")
                    .then()
                        .log().all()
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
            LocalDate startDate = nextDayOfWeek(java.time.DayOfWeek.TUESDAY);
            LocalDate endDate =
                startDate.plusMonths(6).with(TemporalAdjusters.next(java.time.DayOfWeek.THURSDAY));
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
            LocalDate startDate = nextDayOfWeek(java.time.DayOfWeek.MONDAY);
            LocalDate endDate =
                startDate.plusMonths(3).with(TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY));
            Set<DayOfWeek> selectedDaysOfWeek =
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

            // Mock de uma reserva que não deve conflitar
            mockPersistReservationByUser(
                modality.getId(),
                courtId,
                startDate.plusDays(1), // Terça-feira (dia seguinte a segunda)
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
            LocalDate startDate = nextDayOfWeek(java.time.DayOfWeek.WEDNESDAY);
            LocalDate endDate =
                startDate.plusMonths(4).with(TemporalAdjusters.next(java.time.DayOfWeek.FRIDAY));
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
            LocalDate startDate = nextDayOfWeek(java.time.DayOfWeek.THURSDAY);
            LocalDate endDate =
                startDate.plusMonths(2).with(TemporalAdjusters.next(java.time.DayOfWeek.SATURDAY));
            Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.THURSDAY, DayOfWeek.SATURDAY);

            // Mock de uma reserva que não deve conflitar
            mockPersistReservationByUser(
                modality.getId(),
                courtId,
                startDate.plusDays(1), // Sexta-feira (dia seguinte a quinta)
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
          @DisplayName(
              "Cria preview de bloqueio para horário específico com conflitos em múltiplas quadras")
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
          @DisplayName(
              "Cria preview de bloqueio para o dia todo com conflitos em múltiplas quadras")
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "courtIds",
              ErrorCode.valueOf(expectedErrorMessage));
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "startDate",
              ErrorCode.BLOCKED_TIME_START_DATE_REQUIRED);
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "endDate",
              ErrorCode.BLOCKED_TIME_END_DATE_REQUIRED);
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "timeInterval",
              ErrorCode.valueOf(expectedErrorCode));
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "isFullDay",
              ErrorCode.BLOCKED_TIME_IS_FULL_DAY_REQUIRED);
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "startDate",
              ErrorCode.BLOCKED_TIME_START_DATE_IN_PAST);
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "startDate",
              ErrorCode.BLOCKED_TIME_START_DATE_AFTER_END_DATE);
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "timeInterval",
              ErrorCode.BLOCKED_TIME_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY);
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

          // Assert
          assertBusinessError(
              response, 400, PREVIEW_CONFLICTS_PATH, ErrorCode.BLOCKED_TIME_TOO_MANY_OCCURRENCES);
        }

        @Test
        @DisplayName("Tenta criar bloqueio fora do horário de funcionamento")
        void shouldReturn400WhenOutsideOperatingHours() {
          // Arrange
          mockPersistOperatingHoursAllDays();
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

          // Assert
          assertBusinessError(
              response,
              400,
              PREVIEW_CONFLICTS_PATH,
              ErrorCode.BLOCKED_TIME_OUTSIDE_OPERATING_HOURS);
        }

        @Test
        @DisplayName(
            "Tenta criar bloqueio com todos os dias da semana inválidos fora do intervalo de datas informado")
        void shouldReturn400WhenSelectedDaysOfWeekOutsideDateRange() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

          LocalDate startDateMonday = nextDayOfWeek(java.time.DayOfWeek.MONDAY);
          LocalDate endDateDateWed = startDateMonday.plusDays(2); // Quarta-feira

          // Seleciona dias da semana que estão fora do intervalo de segunda a quarta
          Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

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
                  .log()
                  .all()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(
              response,
              400,
              PREVIEW_CONFLICTS_PATH,
              ErrorCode.BLOCKED_TIME_SELECTED_DAYS_OUTSIDE_DATE_RANGE);
        }

        @Test
        @DisplayName(
            "Tenta criar bloqueio com um dia da semana inválido fora do intervalo de datas informado")
        void shouldReturn400WhenOneSelectedDayOfWeekOutsideDateRange() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

          LocalDate startDateMonday = nextDayOfWeek(java.time.DayOfWeek.MONDAY);
          LocalDate endDateDateWed = startDateMonday.plusDays(2); // Quarta-feira

          // Seleciona dias da semana que estão fora do intervalo de segunda a quarta
          Set<DayOfWeek> selectedDaysOfWeek = Set.of(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

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

          // Assert
          assertBusinessError(
              response,
              400,
              PREVIEW_CONFLICTS_PATH,
              ErrorCode.BLOCKED_TIME_SELECTED_DAYS_OUTSIDE_DATE_RANGE);
        }

        @Test
        @DisplayName("Tenta criar bloqueio para um único dia informando selectedDaysOfWeek")
        void shouldReturn400WhenSelectedDaysOfWeekInformedForSingleDate() {
          // Arrange
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(12, 0));
          LocalDate singleDate = LocalDate.now().plusDays(1);
          Set<DayOfWeek> selectedDays = Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(UUID.randomUUID()),
                  singleDate,
                  singleDate,
                  timeInterval,
                  false,
                  selectedDays); // Não deveria ser informado para um único dia

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(requestDto)
                  .when()
                  .post("/preview-conflicts")
                  .then()
                  .log()
                  .all()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "selectedDaysOfWeek",
              ErrorCode.BLOCKED_TIME_SELECTED_DAYS_NOT_ALLOWED_FOR_SINGLE_DATE);
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

          var requestDto =
              new BlockedTimeConflictsPreviewRequestDto(
                  List.of(courtId),
                  date,
                  date,
                  timeInterval, // Não deveria ser informado quando isFullDay=true
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

          // Assert
          assertValidationError(
              response,
              PREVIEW_CONFLICTS_PATH,
              "timeInterval",
              ErrorCode.BLOCKED_TIME_TIME_INTERVAL_NOT_ALLOWED_WHEN_FULL_DAY);
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

          LocalDate nextSunday = nextDayOfWeek(java.time.DayOfWeek.SUNDAY);
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

          // Assert
          assertBusinessError(
              response,
              404,
              PREVIEW_CONFLICTS_PATH,
              ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND);
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
                  List.of(court.getId(), UUID.randomUUID()),
                  date,
                  date,
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
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 404, PREVIEW_CONFLICTS_PATH, ErrorCode.COURT_NOT_FOUND);
        }
      }
    }

    @Nested
    @DisplayName("Testes para o endpoint POST /api/admin/blocked-times/confirm")
    class ConfirmBlockedTimeTests {

      @Nested
      @DisplayName("Cenários de sucesso - 200 OK")
      class SuccessScenarios {

        @Test
        @DisplayName("Confirma bloqueio pontual para dia inteiro sem conflitos")
        void shouldConfirmBlockedTimeForFullDayWithoutConflicts() {
          // Arrange
          var operatingHours = mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Manutenção programada");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - Response
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(1);
          assertThat(response.blockedTimesCreatedSuccessfully()).hasSize(1);
          assertThat(response.reservationsCancelled()).isZero();
          assertThat(response.blockedTimesCancelled()).isZero();
          assertThat(response.usersAffected()).isZero();

          // Assert - Verifica dados do BlockedTime criado no banco
          UUID createdBlockedTimeId = response.blockedTimesCreatedSuccessfully().getFirst();
          var createdBlockedTime = blockedTimeRepository.findByIdOrElseThrow(createdBlockedTimeId);

          assertThat(createdBlockedTime.getCourtId()).isEqualTo(courtId);
          assertThat(createdBlockedTime.getDateTimeSlot().date()).isEqualTo(date);
          assertThat(createdBlockedTime.getDateTimeSlot().timeInterval())
              .isEqualTo(operatingHours.getTimeInterval());
          assertThat(createdBlockedTime.getDescription()).isEqualTo("Manutenção programada");
          assertThat(createdBlockedTime.isFullDay()).isTrue();
          assertThat(createdBlockedTime.getBlockedByAdminId()).isEqualTo(adminId);

          // Verifica que o preview foi removido do cache
          assertThat(getPreviewSavedFromCache(previewKey)).isEmpty();
        }

        @Test
        @DisplayName(
            "Confirma bloqueio pontual para dia inteiro com conflitos de reservas e blocked times")
        void shouldConfirmBlockedTimeForFullDayWithConflicts() {
          // Arrange
          var operatingHours = mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          var reservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  courtId,
                  date,
                  new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                  DEFAULT_RESERVATION_PRICE,
                  adminId);

          var existingBlockedTime =
              mockPersistBlockedTimeSpecific(
                  courtId,
                  date,
                  new TimeInterval(LocalTime.of(12, 0), LocalTime.of(13, 0)),
                  "Manutenção anterior",
                  adminId);

          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Bloqueio para dia todo");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - Response
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(1);
          assertThat(response.blockedTimesCreatedSuccessfully()).hasSize(1);
          assertThat(response.reservationsCancelled()).isEqualTo(1);
          assertThat(response.blockedTimesCancelled()).isEqualTo(1);
          assertThat(response.usersAffected()).isEqualTo(1);

          // Assert - Verifica que a reserva foi cancelada
          var cancelledReservation = reservationRepository.findByIdOrElseThrow(reservation.getId());
          assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

          // Assert - Verifica que o BlockedTime antigo foi removido
          assertThat(blockedTimeRepository.findById(existingBlockedTime.getId())).isEmpty();

          // Assert - Verifica dados do novo BlockedTime criado no banco
          UUID createdBlockedTimeId = response.blockedTimesCreatedSuccessfully().getFirst();
          var createdBlockedTime = blockedTimeRepository.findByIdOrElseThrow(createdBlockedTimeId);

          assertThat(createdBlockedTime.getCourtId()).isEqualTo(courtId);
          assertThat(createdBlockedTime.getDateTimeSlot().date()).isEqualTo(date);
          assertThat(createdBlockedTime.getDateTimeSlot().timeInterval())
              .isEqualTo(operatingHours.getTimeInterval());
          assertThat(createdBlockedTime.getDescription()).isEqualTo("Bloqueio para dia todo");
          assertThat(createdBlockedTime.isFullDay()).isTrue();

          // Verifica que o preview foi removido do cache
          assertThat(getPreviewSavedFromCache(previewKey)).isEmpty();
        }

        @Test
        @DisplayName("Confirma bloqueio para horário específico sem conflitos")
        void shouldConfirmBlockedTimeForSpecificTimeWithoutConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0));

          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, timeInterval, false, null);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Evento privado");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - Response
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(1);
          assertThat(response.blockedTimesCreatedSuccessfully()).hasSize(1);
          assertThat(response.reservationsCancelled()).isZero();
          assertThat(response.blockedTimesCancelled()).isZero();
          assertThat(response.usersAffected()).isZero();

          // Assert - Verifica dados do BlockedTime criado no banco
          UUID createdBlockedTimeId = response.blockedTimesCreatedSuccessfully().getFirst();
          var createdBlockedTime = blockedTimeRepository.findByIdOrElseThrow(createdBlockedTimeId);

          assertThat(createdBlockedTime.getCourtId()).isEqualTo(courtId);
          assertThat(createdBlockedTime.getDateTimeSlot().date()).isEqualTo(date);
          assertThat(createdBlockedTime.getDateTimeSlot().timeInterval()).isEqualTo(timeInterval);
          assertThat(createdBlockedTime.getDescription()).isEqualTo("Evento privado");
          assertThat(createdBlockedTime.isFullDay()).isFalse();
          assertThat(createdBlockedTime.getBlockedByAdminId()).isEqualTo(adminId);
        }

        @Test
        @DisplayName("Confirma bloqueio consecutivo para 14 dias com conflitos")
        void shouldConfirmBlockedTimeForConsecutiveDaysWithConflicts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(14);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0));

          // Cria reservas que entrarão em conflito
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(7),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              startDate.plusDays(9),
              timeInterval,
              DEFAULT_RESERVATION_PRICE,
              adminId);

          String previewKey =
              createPreviewAndGetKey(List.of(courtId), startDate, endDate, null, true, null);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Reforma da quadra");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - 15 dias (startDate até endDate inclusivos)
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(15);
          assertThat(response.blockedTimesCreatedSuccessfully()).hasSize(15);
          assertThat(response.reservationsCancelled()).isEqualTo(2);
          assertThat(response.usersAffected()).isEqualTo(1);
        }

        @Test
        @DisplayName("Confirma bloqueio recorrente semanal com dias específicos")
        void shouldConfirmBlockedTimeForRecurringWeeklyWithSpecificDays() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();

          // Período de 2 semanas
          LocalDate startDate = LocalDate.now().plusDays(1);
          LocalDate endDate = startDate.plusDays(14);

          Set<DayOfWeek> selectedDays = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
          TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(21, 0));

          String previewKey =
              createPreviewAndGetKey(
                  List.of(courtId), startDate, endDate, timeInterval, false, selectedDays);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Aulas de vôlei");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - verifica que foram criados blocked times apenas para terças e quintas
          assertThat(response.totalBlockedTimesCreated()).isGreaterThanOrEqualTo(1);
          assertThat(response.blockedTimesCreatedSuccessfully()).isNotEmpty();
          assertThat(response.reservationsCancelled()).isZero();
        }

        @Test
        @DisplayName("Confirma bloqueio em múltiplas quadras")
        void shouldConfirmBlockedTimeForMultipleCourts() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId1 = mockPersistCourt("Quadra 1", modality).getId();
          UUID courtId2 = mockPersistCourt("Quadra 2", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          String previewKey =
              createPreviewAndGetKey(List.of(courtId1, courtId2), date, date, null, true, null);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Torneio especial");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert - 2 blocked times (1 para cada quadra)
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(2);
          assertThat(response.blockedTimesCreatedSuccessfully()).hasSize(2);
        }

        @Test
        @DisplayName("Confirma bloqueio ignorando reservas em andamento")
        void shouldConfirmBlockedTimeIgnoringInProgressReservations() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Cria reserva em andamento
          LocalTime startTime = normalizeToValidMinutes(LocalTime.now().minusMinutes(30));
          LocalTime endTime = normalizeToValidMinutes(LocalTime.now().plusMinutes(30));
          TimeInterval inProgressInterval = new TimeInterval(startTime, endTime);

          var inProgressReservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  courtId,
                  LocalDate.now(),
                  inProgressInterval,
                  DEFAULT_RESERVATION_PRICE,
                  adminId);

          // Cria reserva futura que deve ser cancelada
          TimeInterval futureInterval = new TimeInterval(LocalTime.of(20, 0), LocalTime.of(21, 0));
          var futureReservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  courtId,
                  date,
                  futureInterval,
                  DEFAULT_RESERVATION_PRICE,
                  adminId);

          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Manutenção urgente");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .log()
                  .all()
                  .statusCode(200)
                  .extract()
                  .as(BlockedTimeConfirmResponseDto.class);

          // Assert
          assertThat(response.totalBlockedTimesCreated()).isEqualTo(1);
          // A reserva em andamento não deve ser cancelada
          var inProgressReservationAfter =
              reservationRepository.findByIdOrElseThrow(inProgressReservation.getId());
          assertThat(inProgressReservationAfter.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);

          // A reserva futura deve ser cancelada
          var futureReservationAfter =
              reservationRepository.findByIdOrElseThrow(futureReservation.getId());
          assertThat(futureReservationAfter.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 400 Bad Request")
      class BadRequestScenarios {

        @Test
        @DisplayName("Retorna erro quando previewKey não é informada")
        void shouldReturn400WhenPreviewKeyIsBlank() {
          // Arrange
          var confirmRequest = new BlockedTimeConfirmRequestDto("", "Descrição válida");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertValidationError(
              response, CONFIRM_PATH, "previewKey", ErrorCode.PREVIEW_KEY_REQUIRED);
        }

        @Test
        @DisplayName("Retorna erro quando previewKey é inválida")
        void shouldReturn400WhenPreviewKeyIsInvalid() {
          // Arrange
          var confirmRequest = new BlockedTimeConfirmRequestDto("invalid-key!", "Descrição válida");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 400, CONFIRM_PATH, ErrorCode.PREVIEW_KEY_INVALID);
        }

        @Test
        @DisplayName("Retorna erro quando descrição não é informada")
        void shouldReturn400WhenDescriptionIsBlank() {
          // Arrange
          var confirmRequest = new BlockedTimeConfirmRequestDto("valid-key", "");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertValidationError(
              response, CONFIRM_PATH, "description", ErrorCode.BLOCKED_TIME_DESCRIPTION_REQUIRED);
        }

        @Test
        @DisplayName("Retorna erro quando descrição é muito curta")
        void shouldReturn400WhenDescriptionIsTooShort() {
          // Arrange
          var confirmRequest = new BlockedTimeConfirmRequestDto("valid-key", "ab");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertValidationError(
              response,
              CONFIRM_PATH,
              "description",
              ErrorCode.BLOCKED_TIME_DESCRIPTION_INVALID_LENGTH);
        }

        @Test
        @DisplayName("Retorna erro quando descrição é muito longa")
        void shouldReturn400WhenDescriptionIsTooLong() {
          // Arrange
          String longDescription = "a".repeat(501);
          var confirmRequest = new BlockedTimeConfirmRequestDto("valid-key", longDescription);

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(400)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertValidationError(
              response,
              CONFIRM_PATH,
              "description",
              ErrorCode.BLOCKED_TIME_DESCRIPTION_INVALID_LENGTH);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 403 Forbidden")
      class ForbiddenScenarios {

        @Test
        @DisplayName("Retorna erro quando admin informa uma previewKey que pertence a outro admin")
        void shouldReturn403WhenPreviewBelongsToAnotherAdmin() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Cria preview com admin atual
          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          // Cria outro admin e faz login
          User otherAdmin = mockPersistOtherAdminUser();
          AuthTokensTest otherAdminTokens = mockLogin(otherAdmin.getUsername(), defaultPassword);
          String otherAdminAccessToken = "Bearer " + otherAdminTokens.accessToken();

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Tentativa de uso indevido");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", otherAdminAccessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(403)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 403, CONFIRM_PATH, ErrorCode.PREVIEW_KEY_OWNERSHIP_INVALID);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 404 Not Found")
      class NotFoundScenarios {

        @Test
        @DisplayName("Retorna erro quando previewKey não existe no cache")
        void shouldReturn404WhenPreviewKeyNotFound() {
          // Arrange
          String invalidPreviewKey = "blocked-time-preview:" + adminId + ":" + UUID.randomUUID();
          var confirmRequest =
              new BlockedTimeConfirmRequestDto(invalidPreviewKey, "Descrição válida");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 404, CONFIRM_PATH, ErrorCode.PREVIEW_NOT_FOUND);
        }

        @Test
        @DisplayName("Retorna erro quando a quadra do preview não existe mais")
        void shouldReturn404WhenCourtInPreviewNotFound() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          Court court = mockPersistCourt("Quadra 1", modality);
          LocalDate date = LocalDate.now().plusDays(1);

          // Cria preview válido
          String previewKey =
              createPreviewAndGetKey(List.of(court.getId()), date, date, null, true, null);

          // Remove a quadra do banco
          court.disable();
          courtRepository.save(court);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Descrição válida");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 404, CONFIRM_PATH, ErrorCode.COURT_NOT_FOUND);
        }

        @Test
        @DisplayName("Retorna erro quando não há horário de funcionamento para o dia do bloqueio")
        void shouldReturn404WhenNoOperatingHoursForBlockedTimeDay() {
          // Arrange
          OperatingHours operatingHours = mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Cria preview válido para domingo
          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          // Remove o horário de funcionamento do dia
          operatingHours.disable();
          operatingHoursRepository.save(operatingHours);

          var confirmRequest = new BlockedTimeConfirmRequestDto(previewKey, "Descrição válida");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(404)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(
              response, 404, CONFIRM_PATH, ErrorCode.OPERATING_HOURS_APPLICABLE_NOT_FOUND);
        }
      }

      @Nested
      @DisplayName("Cenários de erro - 409 Conflict")
      class ConflictScenarios {

        @Test
        @DisplayName(
            "Retorna erro quando preview está desatualizado por causa de nova reserva criada")
        void shouldReturn409WhenPreviewIsStaleNewReservationCreated() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Admin 1 gera preview sem conflitos
          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          // Simula race condition: uma reserva é criada após o preview ser gerado
          mockPersistReservationByUser(
              modality.getId(),
              courtId,
              date,
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              DEFAULT_RESERVATION_PRICE,
              adminId);

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Bloqueio após race condition");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 409, CONFIRM_PATH, ErrorCode.PREVIEW_DATA_STALE);
        }

        @Test
        @DisplayName(
            "Retorna erro quando preview está desatualizado por causa de outro bloqueio criado")
        void shouldReturn409WhenPreviewIsStaleNewBlockedTimeCreated() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Admin 1 gera preview sem conflitos
          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          // Simula race condition: outro BlockedTime é criado após o preview ser gerado
          mockPersistBlockedTimeSpecific(
              courtId,
              date,
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              "Bloqueio criado por outro admin",
              adminId);

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Bloqueio após race condition");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 409, CONFIRM_PATH, ErrorCode.PREVIEW_DATA_STALE);
        }

        @Test
        @DisplayName(
            "Retorna erro quando preview está desatualizado por causa de reserva cancelada")
        void shouldReturn409WhenPreviewIsStaleReservationCancelled() {
          // Arrange
          mockPersistOperatingHoursAllDays();
          Modality modality = mockPersistModality("Futebol");
          UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
          LocalDate date = LocalDate.now().plusDays(1);

          // Cria reserva que será um conflito
          var reservation =
              mockPersistReservationByUser(
                  modality.getId(),
                  courtId,
                  date,
                  new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                  DEFAULT_RESERVATION_PRICE,
                  adminId);

          // Admin 1 gera preview COM conflito (1 reserva)
          String previewKey =
              createPreviewAndGetKey(List.of(courtId), date, date, null, true, null);

          // Simula race condition: a reserva é cancelada após o preview ser gerado
          reservation.cancel();
          reservationRepository.save(reservation);

          var confirmRequest =
              new BlockedTimeConfirmRequestDto(previewKey, "Bloqueio após race condition");

          // Act
          var response =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .body(confirmRequest)
                  .when()
                  .post("/confirm")
                  .then()
                  .statusCode(409)
                  .extract()
                  .as(ErrorResponseDto.class);

          // Assert
          assertBusinessError(response, 409, CONFIRM_PATH, ErrorCode.PREVIEW_DATA_STALE);
        }
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/blocked-times")
  class GetAllBlockedTimeTests {

    @Test
    @DisplayName("Deve retornar blocked times filtrados por courtId com paginação")
    void shouldReturnBlockedTimesFilteredByCourtId() {
      // Arrange
      Modality modality = mockPersistModality("Futebol");
      UUID courtId = mockPersistCourt("Quadra 1", modality).getId();
      UUID otherCourtId = mockPersistCourt("Quadra 2", modality).getId();
      LocalDate date = LocalDate.now().plusDays(1);

      // Persiste 2 bloqueios para a Quadra 1
      var blockedTime1 =
          mockPersistBlockedTimeSpecific(
              courtId,
              date,
              new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
              "Bloqueio 1",
              adminId);
      var blockedTime2 =
          mockPersistBlockedTimeSpecific(
              courtId,
              date.plusDays(1),
              new TimeInterval(LocalTime.of(14, 0), LocalTime.of(15, 0)),
              "Bloqueio 2",
              adminId);

      // Persiste 1 bloqueio para a Quadra 2 (não deve retornar)
      mockPersistBlockedTimeSpecific(
          otherCourtId,
          date,
          new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
          "Bloqueio Outra Quadra",
          adminId);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("courtId", courtId)
              .queryParam("page", 0)
              .queryParam("size", 10)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Assert
      // Validando metadados da paginação
      assertThat(response.jsonPath().getInt("totalElements")).isEqualTo(2);
      assertThat(response.jsonPath().getInt("totalPages")).isEqualTo(1);

      // Validando o conteúdo
      List<String> returnedIds = response.jsonPath().getList("content.blockedTimeId", String.class);

      assertThat(returnedIds)
          .hasSize(2)
          .containsExactlyInAnyOrder(
              blockedTime1.getId().toString(), blockedTime2.getId().toString());
    }

    @Test
    @DisplayName("Deve respeitar os parâmetros de paginação (page e size)")
    void shouldRespectPaginationParameters() {
      // Arrange
      Modality modality = mockPersistModality("Tenis");
      UUID courtId = mockPersistCourt("Quadra Central", modality).getId();
      LocalDate date = LocalDate.now().plusDays(1);

      // Cria 5 bloqueios
      for (int i = 0; i < 5; i++) {
        mockPersistBlockedTimeSpecific(
            courtId,
            date.plusDays(i),
            new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
            "Manutenção: " + 1,
            adminId);
      }

      // Act - Requisita a página 0 com tamanho 2
      var responsePage0 =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("courtId", courtId)
              .queryParam("page", 0)
              .queryParam("size", 2)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Act - Requisita a página 1 com tamanho 2
      var responsePage1 =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("courtId", courtId)
              .queryParam("page", 1)
              .queryParam("size", 2)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .response();

      // Assert
      // Página 0
      assertThat(responsePage0.jsonPath().getInt("numberOfElements")).isEqualTo(2);
      assertThat(responsePage0.jsonPath().getInt("totalElements")).isEqualTo(5);

      // Página 1
      assertThat(responsePage1.jsonPath().getInt("numberOfElements")).isEqualTo(2);

      // Garante que os IDs da página 0 não estão na página 1
      List<UUID> idsPage0 = responsePage0.jsonPath().getList("content.blockedTimeId", UUID.class);
      List<UUID> idsPage1 = responsePage1.jsonPath().getList("content.blockedTimeId", UUID.class);

      assertThat(idsPage0).doesNotContainAnyElementsOf(idsPage1);
    }

    @Test
    @DisplayName("Deve retornar todos os blocked times quando courtId não for informado")
    void shouldReturnAllBlockedTimesWhenNoCourtIdProvided() {
      // Arrange
      Modality modality = mockPersistModality("Volei");
      UUID court1 = mockPersistCourt("Quadra 1", modality).getId();
      UUID court2 = mockPersistCourt("Quadra 2", modality).getId();

      mockPersistBlockedTimeSpecific(
          court1,
          LocalDate.now().plusDays(1),
          new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
          "Manutenção",
          adminId);

      mockPersistBlockedTimeSpecific(
          court2,
          LocalDate.now().plusDays(1),
          new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
          "Manutenção",
          adminId);

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
              .response();

      // Assert
      // Deve conter pelo menos esses 2 (pode haver sujeira de outros testes se o banco não for
      // limpo)
      assertThat(response.jsonPath().getInt("totalElements")).isGreaterThanOrEqualTo(2);

      List<String> descriptions = response.jsonPath().getList("content.description");
      assertThat(descriptions).contains("Manutenção", "Manutenção");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/blocked-times/{blockedTimeId}")
  class UpdateBlockedTimeTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class SuccessScenarios {

      @Test
      @DisplayName("Atualiza a descrição de um BlockedTime único")
      void shouldUpdateBlockedTimeDescription_whenDescriptionIsValid() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);

        ScheduleEntry blockedTime =
            mockPersistBlockedTimeSpecific(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(15, 0)),
                "Manutenção na quadra",
                adminId);

        BlockedTimeUpdateRequestDto requestDto =
            new BlockedTimeUpdateRequestDto("Quadra 1 em manutenção", false);

        // Act
        List<BlockedTimeDetailResponseDto> response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", blockedTime.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {});

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().description()).isEqualTo("Quadra 1 em manutenção");

        BlockedTime updatedBlockedTime =
            blockedTimeRepository.findByIdOrElseThrow(blockedTime.getId());
        assertThat(updatedBlockedTime.getDescription()).isEqualTo("Quadra 1 em manutenção");
      }

      @Test
      @DisplayName("Atualiza a descrição de todos os BlockedTime recorrentes")
      void shouldUpdateAllRecurringBlockedTimes_whenUpdateAllRecurringIsTrue() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);
        UUID recurringId = UUID.randomUUID();

        BlockedTime blockedTime1 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTime blockedTime2 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(2),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTimeUpdateRequestDto requestDto =
            new BlockedTimeUpdateRequestDto("Manutenção atualizada", true);

        // Act
        List<BlockedTimeDetailResponseDto> response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", blockedTime1.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {});

        // Assert
        assertThat(response).hasSize(2);
        assertThat(response)
            .extracting(BlockedTimeDetailResponseDto::description)
            .containsOnly("Manutenção atualizada");

        BlockedTime updatedBlockedTime1 =
            blockedTimeRepository.findByIdOrElseThrow(blockedTime1.getId());
        BlockedTime updatedBlockedTime2 =
            blockedTimeRepository.findByIdOrElseThrow(blockedTime2.getId());

        assertThat(updatedBlockedTime1.getDescription()).isEqualTo("Manutenção atualizada");
        assertThat(updatedBlockedTime2.getDescription()).isEqualTo("Manutenção atualizada");
      }

      @Test
      @DisplayName("Atualiza apenas um BlockedTime recorrente quando updateAllRecurring é false")
      void shouldUpdateOnlyOneRecurringBlockedTime_whenUpdateAllRecurringIsFalse() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);
        UUID recurringId = UUID.randomUUID();

        BlockedTime blockedTime1 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTime blockedTime2 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(2),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTimeUpdateRequestDto requestDto =
            new BlockedTimeUpdateRequestDto("Manutenção única", false);

        // Act
        List<BlockedTimeDetailResponseDto> response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", blockedTime1.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<>() {});

        // Assert
        assertThat(response).hasSize(1);
        assertThat(response.getFirst().description()).isEqualTo("Manutenção única");

        BlockedTime updatedBlockedTime1 = blockedTimeRepository.findByIdOrElseThrow(blockedTime1.getId());
        BlockedTime notUpdatedBlockedTime2 = blockedTimeRepository.findByIdOrElseThrow(blockedTime2.getId());

        assertThat(updatedBlockedTime1.getDescription()).isEqualTo("Manutenção única");
        assertThat(notUpdatedBlockedTime2.getDescription()).isEqualTo("Manutenção recorrente");
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class BadRequestScenarios {
      @Test
      @DisplayName("Retorna erro quando a descrição é vazia")
      void shouldReturn400_whenDescriptionIsBlank() {
        // Arrange
        BlockedTimeUpdateRequestDto requestDto = new BlockedTimeUpdateRequestDto("", false);
        UUID nonExistentId = UUID.randomUUID();

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", nonExistentId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertValidationError(
            response,
            "/api/admin/blocked-times/" + nonExistentId,
            "description",
            ErrorCode.BLOCKED_TIME_DESCRIPTION_REQUIRED);
      }

      @ParameterizedTest
      @ValueSource(ints = {2, 501})
      @DisplayName("Retorna erro quando o tamanho da descrição é inválido")
      void shouldReturn400_whenDescriptionLengthIsInvalid(int length) {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        String longDescription = "a".repeat(length);

        BlockedTimeUpdateRequestDto requestDto =
            new BlockedTimeUpdateRequestDto(longDescription, false);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", nonExistentId)
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NotFoundScenarios {
      @Test
      @DisplayName("Retorna erro quando o BlockedTime não existe")
      void shouldReturn404_whenBlockedTimeNotFound() {
        // Arrange
        BlockedTimeUpdateRequestDto requestDto =
            new BlockedTimeUpdateRequestDto("Nova descrição", false);
        UUID nonExistentId = UUID.randomUUID();

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(requestDto)
                .when()
                .patch("/{blockedTimeId}", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(
            response,
            404,
            "/api/admin/blocked-times/" + nonExistentId,
            ErrorCode.BLOCKED_TIME_NOT_FOUND);
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint DELETE /api/admin/blocked-times/{blockedTimeId}")
  class DeleteBlockedTimeTests {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class SuccessScenarios {

      @Test
      @DisplayName("Deleta um BlockedTime único")
      void shouldDeleteBlockedTime_whenItExists() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);

        ScheduleEntry blockedTime =
            mockPersistBlockedTimeSpecific(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(15, 0)),
                "Manutenção na quadra",
                adminId);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .delete("/{blockedTimeId}", blockedTime.getId())
            .then()
            .statusCode(204);

        // Assert
        assertThat(blockedTimeRepository.findById(blockedTime.getId())).isEmpty();
      }

      @Test
      @DisplayName("Deleta todos os BlockedTime recorrentes quando deleteAllRecurring é true")
      void shouldDeleteAllRecurringBlockedTimes_whenDeleteAllRecurringIsTrue() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);
        UUID recurringId = UUID.randomUUID();

        BlockedTime blockedTime1 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTime blockedTime2 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(2),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .queryParam("deleteAllRecurring", true)
            .when()
            .delete("/{blockedTimeId}", blockedTime1.getId())
            .then()
            .statusCode(204);

        // Assert
        assertThat(blockedTimeRepository.findById(blockedTime1.getId())).isEmpty();
        assertThat(blockedTimeRepository.findById(blockedTime2.getId())).isEmpty();
      }

      @Test
      @DisplayName("Deleta apenas um BlockedTime recorrente quando deleteAllRecurring é false")
      void shouldDeleteOnlyOneRecurringBlockedTime_whenDeleteAllRecurringIsFalse() {
        // Arrange
        Modality modality = mockPersistModality("Futebol");
        Court court = mockPersistCourt("Quadra 1", modality);
        UUID recurringId = UUID.randomUUID();

        BlockedTime blockedTime1 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(1),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        BlockedTime blockedTime2 =
            mockPersistBlockedTimeRecurring(
                court.getId(),
                LocalDate.now().plusDays(2),
                new TimeInterval(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                "Manutenção recorrente",
                adminId,
                recurringId);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .queryParam("deleteAllRecurring", false)
            .when()
            .delete("/{blockedTimeId}", blockedTime1.getId())
            .then()
            .statusCode(204);

        // Assert
        assertThat(blockedTimeRepository.findById(blockedTime1.getId())).isEmpty();
        assertThat(blockedTimeRepository.findById(blockedTime2.getId())).isPresent();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class NotFoundScenarios {
      @Test
      @DisplayName("Retorna erro quando o BlockedTime não existe")
      void shouldReturn404_whenBlockedTimeNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .delete("/{blockedTimeId}", nonExistentId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        assertBusinessError(
            response,
            404,
            "/api/admin/blocked-times/" + nonExistentId,
            ErrorCode.BLOCKED_TIME_NOT_FOUND);
      }
    }
  }

  /**
   * Cria um preview de conflitos e retorna a previewKey gerada.
   *
   * @return previewKey do preview criado
   */
  private String createPreviewAndGetKey(
      List<UUID> courtIds,
      LocalDate startDate,
      LocalDate endDate,
      TimeInterval timeInterval,
      boolean isFullDay,
      Set<DayOfWeek> selectedDaysOfWeek) {

    var requestDto =
        new BlockedTimeConflictsPreviewRequestDto(
            courtIds, startDate, endDate, timeInterval, isFullDay, selectedDaysOfWeek);

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

    return response.previewKey();
  }

  /**
   * Recupera um preview salvo no cache pelo previewKey. Retorna Optional.empty() se o preview não
   * for encontrado ou não pertencer ao admin
   *
   * @param previewKey Chave do preview
   * @return Optional com o preview salvo ou vazio se não encontrado
   */
  private Optional<BlockedTimeConflictsPreview> getPreviewSavedFromCache(String previewKey) {
    try {
      BlockedTimeConflictsPreview previewSaved =
          blockedTimePreviewCachePort.getPreviewOrElseThrow(previewKey, adminId);
      return Optional.of(previewSaved);
    } catch (PreviewNotFoundException e) {
      return Optional.empty();
    }
  }
}
