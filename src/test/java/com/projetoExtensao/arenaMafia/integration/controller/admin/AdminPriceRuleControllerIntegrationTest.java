package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.PriceRuleJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreatePriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdateDefaultPriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.UpdatePriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.dayOfWeek.InvalidDaysOfWeekProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.priceRule.InvalidPriceProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.priceRule.InvalidPriceRuleNameProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.priceRule.InvalidPriorityProvider;
import com.projetoExtensao.arenaMafia.integration.config.util.timeInterval.InvalidTimeIntervalProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
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
@DisplayName("Testes de integração para AdminPriceRuleController")
public class AdminPriceRuleControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private PriceRuleJpaRepository priceRuleJpaRepository;
  @Autowired private PriceRuleRepositoryPort priceRuleRepositoryPort;
  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/price-rules")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistAdminUser();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para o endpoint POST /api/admin/price-rules")
  class CreatePriceRuleTest {
    @Nested
    @DisplayName("Cenários de sucesso - 201 Created")
    class CreatePriceRuleSuccessScenarios {

      @Test
      @DisplayName("Deve criar uma nova regra de preço com sucesso")
      void shouldReturn201_whenCreatingNewPriceRule() {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        String ruleName = "Horário Nobre Semana";
        BigDecimal price = BigDecimal.valueOf(85);
        var request = new CreatePriceRuleRequestDto(ruleName, daysOfWeek, timeInterval, price, 1);

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
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.name()).isEqualTo(ruleName);
        assertThat(response.price()).isEqualTo(price);
        assertThat(response.daysOfWeek()).isEqualTo(daysOfWeek);
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve criar uma regra de preço para todos os dias da semana")
      void shouldReturn201_whenCreatingPriceRuleForAllDaysOfWeek() {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        String ruleName = "Horário Nobre Semana";
        BigDecimal price = BigDecimal.valueOf(85);
        var request = new CreatePriceRuleRequestDto(ruleName, null, timeInterval, price, 1);

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
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.name()).isEqualTo(ruleName);
        assertThat(response.price()).isEqualTo(price);
        assertThat(response.daysOfWeek()).isNull();
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve criar uma regra de preço com intervalo de tempo atravessando meia-noite")
      void shouldReturn201_whenCreatingPriceRuleWithIntervalCrossingMidnight() {
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(1, 0));
        String ruleName = "Horário Nobre Semana";
        BigDecimal price = BigDecimal.valueOf(85);
        var request = new CreatePriceRuleRequestDto(ruleName, null, timeInterval, price, 1);

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
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id().toString()).hasSize(36);
        assertThat(response.name()).isEqualTo(ruleName);
        assertThat(response.price()).isEqualTo(price);
        assertThat(response.daysOfWeek()).isNull();
        assertThat(response.timeInterval().startTime()).isEqualTo(timeInterval.startTime());
        assertThat(response.timeInterval().endTime()).isEqualTo(timeInterval.endTime());
        assertThat(response.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class CreatePriceRuleBadRequestScenarios {

      @InvalidPriceRuleNameProvider
      @DisplayName("Tenta criar uma regra de preço com nome inválido")
      void shouldReturn400_whenCreatingPriceRuleWithInvalidName(
          String invalidName, String expectedErrorCode) {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        BigDecimal price = BigDecimal.valueOf(85);
        var request =
            new CreatePriceRuleRequestDto(invalidName, daysOfWeek, timeInterval, price, 1);

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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidDaysOfWeekProvider
      @DisplayName("Tenta criar uma regra de preço com dias da semana inválidos")
      void shouldReturn400_whenCreatingPriceRuleWithInvalidDaysOfWeek(
          String[] invalidDay, String expectedErrorCode) {
        // Arrange
        Map<String, Object> jsonRequest = new HashMap<>();

        jsonRequest.put("name", "Regra com Dias Inválidos");
        jsonRequest.put("daysOfWeek", invalidDay);
        jsonRequest.put("timeInterval", Map.of("startTime", "08:00", "endTime", "00:00"));
        jsonRequest.put("price", 100);
        jsonRequest.put("priority", 1);
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
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("daysOfWeek")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidTimeIntervalProvider
      @DisplayName("Tenta criar uma regra de preço com intervalo de tempo inválido")
      void shouldReturn400_whenCreatingPriceRuleWithInvalidTimeInterval(
          String startTime, String endTime, String expectedErrorCode) {
        // Arrange
        Map<String, Object> timeIntervalMap = new HashMap<>();
        timeIntervalMap.put("startTime", startTime);
        timeIntervalMap.put("endTime", endTime);

        Map<String, Object> jsonRequest = new HashMap<>();

        jsonRequest.put("name", "Regra com Intervalo Inválido");
        jsonRequest.put("daysOfWeek", List.of("MONDAY", "TUESDAY"));
        jsonRequest.put("timeInterval", timeIntervalMap);
        jsonRequest.put("price", 100);
        jsonRequest.put("priority", 1);
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
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("timeInterval")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPriceProvider
      @DisplayName("Tenta criar uma regra de preço com preço inválido")
      void shouldReturn400_whenCreatingPriceRuleWithInvalidPrice(
          BigDecimal invalidPrice, String expectedErrorCode) {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        String ruleName = "Horário Nobre Semana";
        var request =
            new CreatePriceRuleRequestDto(ruleName, daysOfWeek, timeInterval, invalidPrice, 1);

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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("price")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @InvalidPriorityProvider
      @DisplayName("Tenta criar uma regra de preço com prioridade inválida")
      void shouldReturn400_whenCreatingPriceRuleWithInvalidPriority(
          Integer invalidPriority, String expectedErrorCode) {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        String ruleName = "Horário Nobre Semana";
        BigDecimal price = BigDecimal.valueOf(85);
        var request =
            new CreatePriceRuleRequestDto(
                ruleName, daysOfWeek, timeInterval, price, invalidPriority);

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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("priority")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class CreatePriceRuleConflictScenarios {

      @Test
      @DisplayName("Tenta criar uma regra de preço com nome já existente")
      void shouldReturn409_whenCreatingPriceRuleWithExistingName() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY);
        String ruleName = existingRule.getName();
        BigDecimal price = BigDecimal.valueOf(85);
        var request = new CreatePriceRuleRequestDto(ruleName, daysOfWeek, timeInterval, price, 1);

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

        ErrorCode errorCode = ErrorCode.PRICE_RULE_ALREADY_EXISTS;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta criar uma regra de preço que conflita com uma regra ativa existente")
      void shouldReturn409_whenCreatingPriceRuleThatConflictsWithExistingActiveRule() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        TimeInterval timeInterval = existingRule.getTimeInterval();
        Set<DayOfWeek> daysOfWeek = existingRule.getDaysOfWeek();
        String ruleName = "Regra Conflitante";
        BigDecimal price = BigDecimal.valueOf(90);
        var request = new CreatePriceRuleRequestDto(ruleName, daysOfWeek, timeInterval, price, 1);

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

        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRIORITY_OVERLAP;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/price-rules/{ruleId}")
  class UpdatePriceRuleTest {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class UpdatePriceRuleSuccessScenarios {

      @Test
      @DisplayName("Deve atualizar o nome e o preço de uma regra de preço com sucesso")
      void shouldReturn200_whenUpdatingPriceRuleNameAndPrice() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        String updatedName = "Nome Atualizado";
        BigDecimal updatedPrice = BigDecimal.valueOf(120);
        var request = new UpdatePriceRuleRequestDto(updatedName, updatedPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(existingRule.getId());
        assertThat(response.name()).isEqualTo(updatedName);
        assertThat(response.price()).isEqualTo(updatedPrice);
      }

      @Test
      @DisplayName("Deve atualizar apenas o nome de uma regra de preço")
      void shouldReturn200_whenUpdatingOnlyPriceRuleName() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        String updatedName = "Nome Atualizado Somente";
        var request = new UpdatePriceRuleRequestDto(updatedName, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(existingRule.getId());
        assertThat(response.name()).isEqualTo(updatedName);
        assertThat(response.price()).isEqualTo(existingRule.getPrice());
      }

      @Test
      @DisplayName("Deve atualizar apenas o preço de uma regra de preço")
      void shouldReturn200_whenUpdatingOnlyPriceRulePrice() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        BigDecimal updatedPrice = BigDecimal.valueOf(150);
        var request = new UpdatePriceRuleRequestDto(null, updatedPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule.getId())
                .then()
                .statusCode(200)
                .extract()
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(existingRule.getId());
        assertThat(response.name()).isEqualTo(existingRule.getName());
        assertThat(response.price()).isEqualTo(updatedPrice);
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class UpdatePriceRuleBadRequestScenarios {

      @Test
      @DisplayName("Tenta atualizar uma regra de preço com nome inválido")
      void shouldReturn400_whenUpdatingPriceRuleWithInvalidName() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        BigDecimal existingPrice = existingRule.getPrice();
        var request = new UpdatePriceRuleRequestDto("a".repeat(101), existingPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule.getId())
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.PRICE_RULE_NAME_INVALID_LENGTH;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/" + existingRule.getId());
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta atualizar uma regra de preço com preço inválido")
      void shouldReturn400_wheUnUpdatingPriceRuleWithInvalidPrice() {
        // Arrange
        PriceRule existingRule = mockPersistPriceRule();

        String existingName = existingRule.getName();
        var request = new UpdatePriceRuleRequestDto(existingName, BigDecimal.valueOf(-150));

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule.getId())
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRICE_INVALID;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/" + existingRule.getId());
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("price")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class UpdatePriceRuleNotFoundScenarios {

      @Test
      @DisplayName("Tenta atualizar uma regra de preço inexistente")
      void shouldReturn404_whenUpdatingNonExistentPriceRule() {
        // Arrange
        UUID nonExistentRuleId = UUID.randomUUID();

        String updatedName = "Nome Atualizado";
        BigDecimal updatedPrice = BigDecimal.valueOf(120);
        var request = new UpdatePriceRuleRequestDto(updatedName, updatedPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", nonExistentRuleId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_NOT_FOUND;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/" + nonExistentRuleId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class UpdatePriceRuleConflictScenarios {

      @Test
      @DisplayName("Tenta atualizar uma regra de preço com nome já existente")
      void shouldReturn409_whenUpdatingPriceRuleWithExistingName() {
        // Arrange
        PriceRule existingRule1 = mockPersistPriceRule();
        PriceRule existingRule2 =
            PriceRule.create(
                "Outra Regra",
                null,
                new TimeInterval(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                BigDecimal.valueOf(90),
                2);
        priceRuleRepositoryPort.save(existingRule2);

        String updatedName = existingRule1.getName();
        var request = new UpdatePriceRuleRequestDto(updatedName, null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/{ruleId}", existingRule2.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_ALREADY_EXISTS;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/" + existingRule2.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/price-rules/default")
  class UpdatePriceRuleDefaultTest {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class UpdatePriceRuleDefaultSuccessScenarios {

      @Test
      @DisplayName("Deve atualizar a regra de preço padrão com sucesso")
      void shouldReturn200_whenUpdatingDefaultPriceRule() {
        // Arrange
        BigDecimal newDefaultPrice = BigDecimal.valueOf(110);
        var request = new UpdateDefaultPriceRuleRequestDto(newDefaultPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/default")
                .then()
                .statusCode(200)
                .extract()
                .as(PriceRuleResponseDto.class);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Regra de Preço Padrão");
        assertThat(response.price()).isEqualTo(newDefaultPrice);
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class UpdatePriceRuleDefaultBadRequestScenarios {

      @Test
      @DisplayName("Tenta atualizar a regra de preço padrão com preço inválido")
      void shouldReturn400_whenUpdatingDefaultPriceRuleWithInvalidPrice() {
        // Arrange
        var request = new UpdateDefaultPriceRuleRequestDto(BigDecimal.valueOf(-50));

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/default")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRICE_INVALID;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/default");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("price")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }

      @Test
      @DisplayName("Tenta atualizar a regra de preço padrão sem fornecer preço")
      void shouldReturn400_whenUpdatingDefaultPriceRuleWithoutPrice() {
        // Arrange
        var request = new UpdateDefaultPriceRuleRequestDto(null);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/default")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRICE_REQUIRED;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/default");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("price")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class UpdatePriceRuleDefaultNotFoundScenarios {

      @Test
      @DisplayName("Tenta atualizar a regra de preço padrão quando ela não existe")
      void shouldReturn404_whenUpdatingDefaultPriceRuleThatDoesNotExist() {
        // Arrange
        priceRuleJpaRepository.deleteAll();

        BigDecimal newDefaultPrice = BigDecimal.valueOf(110);
        var request = new UpdateDefaultPriceRuleRequestDto(newDefaultPrice);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(request)
                .when()
                .patch("/default")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_DEFAULT_NOT_FOUND;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/default");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/price-rules")
  class GetAllPriceRulesTest {

    @Test
    @DisplayName("Deve retornar 200 OK com a lista de todas as regras de preço")
    void shouldReturn200_whenGettingAllPriceRules() {
      // Arrange
      mockPersistListOfPriceRules();

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
              .as(PriceRuleResponseDto[].class);

      // Assert
      assertThat(response).hasSize(3);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com a lista vazia quando não houver regras de preço")
    void shouldReturn200_whenNoPriceRulesExist() {
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
              .as(PriceRuleResponseDto[].class);

      // Assert
      assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar 200 OK com a lista de regras de preço filtradas isActive=true")
    void shouldReturn200_whenFilteringByActiveStatus() {
      // Arrange
      mockPersistListOfPriceRules();

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
              .as(PriceRuleResponseDto[].class);

      // Assert
      assertThat(response).hasSize(3);
      assertThat(response).allMatch(PriceRuleResponseDto::isActive);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com a lista de regras de preço filtradas isActive=false")
    void shouldReturn200_whenFilteringByInactiveStatus() {
      // Arrange
      mockPersistListOfPriceRules();

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
              .as(PriceRuleResponseDto[].class);

      // Assert
      assertThat(response).hasSize(1);
      assertThat(response).noneMatch(PriceRuleResponseDto::isActive);
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/price-rules/{ruleId}")
  class GetPriceRuleByIdTest {

    @Test
    @DisplayName("Deve retornar 200 OK quando a regra de preço for encontrada pelo ID")
    void shouldReturn200_whenPriceRuleFoundById() {
      // Arrange
      PriceRule priceRule = mockPersistPriceRule();

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get("/{ruleId}", priceRule.getId())
              .then()
              .statusCode(200)
              .extract()
              .as(PriceRuleResponseDto.class);

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(priceRule.getId());
      assertThat(response.name()).isEqualTo(priceRule.getName());
      assertThat(response.price()).isEqualTo(priceRule.getPrice());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando a regra de preço não for encontrada pelo ID")
    void shouldReturn404_whenPriceRuleNotFoundById() {
      // Arrange
      UUID nonExistentRuleId = UUID.randomUUID();

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get("/{ruleId}", nonExistentRuleId)
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.PRICE_RULE_NOT_FOUND;

      // Assert
      assertThat(response).isNotNull();
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.path()).isEqualTo("/api/admin/price-rules/" + nonExistentRuleId);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/price-rules/{ruleId}/enable")
  class EnablePriceRuleTest {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class EnablePriceRuleSuccessScenarios {

      @Test
      @DisplayName("Deve habilitar uma regra sem sobreposição quando prioridades são diferentes")
      void shouldReturn204_whenEnablingRuleWithDifferentPriorities() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.MONDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.MONDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(16, 0));

        BigDecimal price = BigDecimal.valueOf(70);

        PriceRule priceRule1 =
            PriceRule.create("Regra Prioridade Alta", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Prioridade Baixa", daysOfWeek2, timeInterval2, price, 2);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{ruleId}/enable", priceRule1.getId())
            .then()
            .statusCode(204);

        // Assert
        PriceRule updatedPriceRule =
            priceRuleRepositoryPort.findByIdOrElseThrow(priceRule1.getId());
        assertThat(updatedPriceRule.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve habilitar uma regra sem sobreposição quando não há dias em comum")
      void shouldReturn204_whenEnablingRuleWithNoDaysOverlap() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.MONDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.TUESDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));

        BigDecimal price = BigDecimal.valueOf(65);

        PriceRule priceRule1 =
            PriceRule.create("Regra Segunda", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Terça", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{ruleId}/enable", priceRule1.getId())
            .then()
            .statusCode(204);

        // Assert
        PriceRule updatedPriceRule =
            priceRuleRepositoryPort.findByIdOrElseThrow(priceRule1.getId());
        assertThat(updatedPriceRule.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve habilitar uma regra com horários adjacentes sem sobreposição")
      void shouldReturn204_whenEnablingRuleWithAdjacentTimeIntervals() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.FRIDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.FRIDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(16, 0));

        BigDecimal price = BigDecimal.valueOf(60);

        PriceRule priceRule1 =
            PriceRule.create("Regra Manhã", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Tarde", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{ruleId}/enable", priceRule1.getId())
            .then()
            .statusCode(204);

        // Assert
        PriceRule updatedPriceRule =
            priceRuleRepositoryPort.findByIdOrElseThrow(priceRule1.getId());
        assertThat(updatedPriceRule.isActive()).isTrue();
      }

      @Test
      @DisplayName("Deve habilitar uma regra de preço inativa")
      void shouldReturn204_whenEnablingInactivePriceRule() {
        // Arrange
        PriceRule priceRule = mockPersistDisabledPriceRule();

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{ruleId}/enable", priceRule.getId())
            .then()
            .statusCode(204);

        // Assert
        PriceRule updatedPriceRule = priceRuleRepositoryPort.findByIdOrElseThrow(priceRule.getId());

        assertThat(updatedPriceRule.isActive()).isTrue();
        assertThat(updatedPriceRule.getName()).isEqualTo(priceRule.getName());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class EnablePriceRuleNotFoundScenarios {
      @Test
      @DisplayName("Tenta habilitar uma regra de preço inexistente")
      void shouldReturn404_whenEnablingNonExistentPriceRule() {
        // Arrange
        UUID nonExistentRuleId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", nonExistentRuleId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_NOT_FOUND;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + nonExistentRuleId + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class EnablePriceRuleConflictScenarios {
      @Test
      @DisplayName("Tenta habilitar uma regra de preço já ativa")
      void shouldReturn409_whenEnablingAlreadyActivePriceRule() {
        // Arrange
        PriceRule priceRule = mockPersistPriceRule();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_ALREADY_ENABLED;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta habilitar uma regra com um nome que já existe em outra regra ativa")
      void shouldReturn409_whenEnablingRuleWithDuplicateName() {
        // Arrange
        TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
        Set<DayOfWeek> daysOfWeekSet = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        String commonName = "Weekend Special";
        BigDecimal price = BigDecimal.valueOf(85);

        PriceRule priceRule1 = PriceRule.create(commonName, daysOfWeekSet, timeInterval, price, 2);
        PriceRule priceRule2 = PriceRule.create(commonName, daysOfWeekSet, timeInterval, price, 2);
        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule1.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_ALREADY_EXISTS;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule1.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName(
          "Tenta habilitar uma regra com sobreposição de dias e horários - intervalo atravessa meia-noite")
      void shouldReturn409_whenEnablingOverlappingRuleWithIntervalCrossingMidnight() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        Set<DayOfWeek> daysOfWeek2 =
            Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(13, 0), LocalTime.of(18, 0));

        BigDecimal price = BigDecimal.valueOf(90);

        PriceRule priceRule1 = PriceRule.create("Regra 2", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 = PriceRule.create("Regra 1", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule1.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRIORITY_OVERLAP;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule1.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName(
          "Tenta habilitar uma regra com sobreposição de horários sem atravessar meia-noite")
      void shouldReturn409_whenEnablingOverlappingRuleWithoutCrossingMidnight() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(12, 0), LocalTime.of(16, 0));

        BigDecimal price = BigDecimal.valueOf(75);

        PriceRule priceRule1 =
            PriceRule.create("Regra Manhã/Tarde", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Tarde", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule1.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRIORITY_OVERLAP;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule1.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta habilitar uma regra quando ambos intervalos atravessam meia-noite")
      void shouldReturn409_whenEnablingOverlappingRuleWithBothIntervalsCrossingMidnight() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.SATURDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(20, 0), LocalTime.of(2, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(22, 0), LocalTime.of(4, 0));

        BigDecimal price = BigDecimal.valueOf(100);

        PriceRule priceRule1 =
            PriceRule.create("Regra Noturna 1", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Noturna 2", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule1.getId())
                .then()
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRIORITY_OVERLAP;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule1.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta habilitar uma regra com um intervalo contido completamente em outro")
      void shouldReturn409_whenEnablingRuleWithIntervalFullyContainedInAnother() {
        // Arrange
        Set<DayOfWeek> daysOfWeek1 = Set.of(DayOfWeek.WEDNESDAY);
        Set<DayOfWeek> daysOfWeek2 = Set.of(DayOfWeek.WEDNESDAY);

        TimeInterval timeInterval1 = new TimeInterval(LocalTime.of(11, 0), LocalTime.of(13, 0));
        TimeInterval timeInterval2 = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));

        BigDecimal price = BigDecimal.valueOf(80);

        PriceRule priceRule1 =
            PriceRule.create("Regra Restrita", daysOfWeek1, timeInterval1, price, 1);
        PriceRule priceRule2 =
            PriceRule.create("Regra Ampla", daysOfWeek2, timeInterval2, price, 1);

        priceRule1.disable();
        priceRuleRepositoryPort.save(priceRule1);
        priceRuleRepositoryPort.save(priceRule2);

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/enable", priceRule1.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_PRIORITY_OVERLAP;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule1.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/price-rules/{ruleId}/disable")
  class DisableOperatingHoursTest {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class DisablePriceRuleSuccessScenarios {

      @Test
      @DisplayName("Deve desabilitar uma regra de preço ativa com sucesso")
      void shouldReturn204_whenDisablingActivePriceRule() {
        // Arrange
        PriceRule priceRule = mockPersistPriceRule();

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .when()
            .patch("/{ruleId}/disable", priceRule.getId())
            .then()
            .statusCode(204);

        // Assert
        PriceRule updatedPriceRule = priceRuleRepositoryPort.findByIdOrElseThrow(priceRule.getId());
        assertThat(updatedPriceRule.isActive()).isFalse();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class DisablePriceRuleBadRequestScenarios {

      @Test
      @DisplayName("Tenta desabilitar uma regra com ID inválido")
      void shouldReturn400_whenDisablingRuleWithInvalidId() {
        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/invalid-uuid/disable")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/price-rules/invalid-uuid/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class DisablePriceRuleNotFoundScenarios {

      @Test
      @DisplayName("Tenta desabilitar uma regra de preço inexistente")
      void shouldReturn404_whenDisablingNonExistentPriceRule() {
        // Arrange
        UUID nonExistentRuleId = UUID.randomUUID();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/disable", nonExistentRuleId)
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_NOT_FOUND;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + nonExistentRuleId + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class DisablePriceRuleConflictScenarios {

      @Test
      @DisplayName("Tenta desabilitar a regra de preço padrão")
      void shouldReturn409_whenDisablingDefaultPriceRule() {
        // Arrange
        PriceRule defaultPriceRule = mockPersistDefaultPriceRule();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/disable", defaultPriceRule.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_CANNOT_DISABLE_DEFAULT;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + defaultPriceRule.getId() + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @Test
      @DisplayName("Tenta desabilitar uma regra de preço já inativa")
      void shouldReturn409ConflictWhenDisablingAlreadyInactivePriceRule() {
        // Arrange
        PriceRule priceRule = mockPersistDisabledPriceRule();

        // Act
        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .when()
                .patch("/{ruleId}/disable", priceRule.getId())
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.PRICE_RULE_ALREADY_DISABLED;

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/price-rules/" + priceRule.getId() + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
