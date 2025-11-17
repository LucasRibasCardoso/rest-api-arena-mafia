package com.projetoExtensao.arenaMafia.integration.controller.operatingHours;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
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

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para OperatingHoursController")
public class OperatingHoursControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/operating-hours")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista de contendo os horários de funcionamento")
  void shouldReturn200_whenOperatingHoursExist() {
    // Arrange
    mockPersistListOfOperatingHours();

    // Act
    var responseBodyJson =
        given().spec(specification).when().get().then().statusCode(200).extract().body().asString();

    var response = new JsonPath(responseBodyJson).getList("", OperatingHoursResponseDto.class);

    // Assert
    assertThat(response).hasSize(3);
    assertThat(response).allMatch(OperatingHoursResponseDto::isActive);

    Set<DayOfWeek> weekDays =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);

    OperatingHoursResponseDto weekdayMorning =
        response.stream()
            .filter(h -> h.daysOfWeek().equals(weekDays))
            .filter(h -> h.timeInterval().startTime().equals(LocalTime.of(8, 0)))
            .findFirst()
            .orElseThrow();

    assertThat(weekdayMorning.timeInterval().endTime()).isEqualTo(LocalTime.of(12, 0));

    OperatingHoursResponseDto weekdayAfternoon =
        response.stream()
            .filter(h -> h.daysOfWeek().equals(weekDays))
            .filter(h -> h.timeInterval().startTime().equals(LocalTime.of(13, 30)))
            .findFirst()
            .orElseThrow();

    assertThat(weekdayAfternoon.timeInterval().endTime()).isEqualTo(LocalTime.of(0, 0));

    OperatingHoursResponseDto weekend =
        response.stream()
            .filter(h -> h.daysOfWeek().equals(Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
            .findFirst()
            .orElseThrow();

    assertThat(weekend.timeInterval().startTime()).isEqualTo(LocalTime.of(8, 0));
    assertThat(weekend.timeInterval().endTime()).isEqualTo(LocalTime.of(0, 0));
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista vazia quando não houver horários de funcionamento")
  void shouldReturn200AndEmptyList_whenNoOperatingHoursExist() {
    // Act & Assert
    var responseBodyJson =
        given().spec(specification).when().get().then().statusCode(200).extract().body().asString();

    var response = new JsonPath(responseBodyJson).getList("", OperatingHoursResponseDto.class);

    assertThat(response).isEmpty();
  }
}
