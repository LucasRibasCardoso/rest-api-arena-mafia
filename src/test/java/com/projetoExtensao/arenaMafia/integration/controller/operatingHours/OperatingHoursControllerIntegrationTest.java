package com.projetoExtensao.arenaMafia.integration.controller.operatingHours;

import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
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
            .setBasePath("/api/modalities")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista de contendo os horários de funcionamento")
  void shouldReturn200_whenOperatingHoursExist() {
    // Arrange
    mockPersistOperatingHours();

    // Act & Assert

  }
}
