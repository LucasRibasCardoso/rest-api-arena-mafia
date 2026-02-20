package com.projetoExtensao.arenaMafia.integration.controller.priceRules;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DisplayName("Testes de Integração para PriceRuleController")
public class PriceRuleControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/public/price-rules")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista de regras de preço ativas")
  void shouldReturn200_whenPriceRulesExist() {
    // Arrange
    mockPersistListOfPriceRules();

    // Act
    var responseBodyJson =
        given().spec(specification).when().get().then().statusCode(200).extract().body().asString();

    var response = new JsonPath(responseBodyJson).getList("", PriceRuleResponseDto.class);

    // Assert
    assertThat(response).hasSize(3);
    assertThat(response).allMatch(PriceRuleResponseDto::isActive);

    // Valida a Regra de Preço Padrão
    PriceRuleResponseDto defaultRule =
        response.stream().filter(PriceRuleResponseDto::isDefault).findFirst().orElseThrow();

    assertThat(defaultRule.name()).isEqualTo("Regra de Preço Padrão");
    assertThat(defaultRule.price()).isEqualByComparingTo("50.00");
    assertThat(defaultRule.priority()).isZero();
    assertThat(defaultRule.daysOfWeek()).isNull();
    assertThat(defaultRule.timeInterval()).isNull();

    // Valida a Regra "Horário Nobre - Dias de Semana"
    PriceRuleResponseDto peakHoursRule =
        response.stream()
            .filter(r -> r.name().equals("Horário Nobre - Dias de Semana"))
            .findFirst()
            .orElseThrow();

    assertThat(peakHoursRule.price()).isEqualByComparingTo("85.00");
    assertThat(peakHoursRule.priority()).isEqualTo(1);
    assertThat(peakHoursRule.isDefault()).isFalse();
    assertThat(peakHoursRule.daysOfWeek())
        .containsAll(
            Set.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY));
    assertThat(peakHoursRule.timeInterval().startTime()).isEqualTo(LocalTime.of(19, 0));
    assertThat(peakHoursRule.timeInterval().endTime()).isEqualTo(LocalTime.of(0, 0));

    // Valida a Regra "Horário Especial - Fins de Semana"
    PriceRuleResponseDto specialHoursRule =
        response.stream()
            .filter(r -> r.name().equals("Horário Especial - Fins de Semana"))
            .findFirst()
            .orElseThrow();

    assertThat(specialHoursRule.price()).isEqualByComparingTo("85.00");
    assertThat(specialHoursRule.priority()).isEqualTo(1);
    assertThat(specialHoursRule.isDefault()).isFalse();
    assertThat(specialHoursRule.daysOfWeek())
        .containsAll(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY));
    assertThat(specialHoursRule.timeInterval().startTime()).isEqualTo(LocalTime.of(8, 0));
    assertThat(specialHoursRule.timeInterval().endTime()).isEqualTo(LocalTime.of(0, 0));
  }
}
