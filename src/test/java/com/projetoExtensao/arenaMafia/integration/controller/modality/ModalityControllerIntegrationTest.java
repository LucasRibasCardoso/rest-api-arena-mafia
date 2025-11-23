package com.projetoExtensao.arenaMafia.integration.controller.modality;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para ModalityController")
class ModalityControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/public/modalities")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista de modalidades quando existirem modalidades")
  void shouldReturn200_whenModalitiesExist() {
    // Arrange
    mockPersistListOfModalities();

    // Act & Assert
    given()
        .spec(specification)
        .when()
        .get()
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("$", hasSize(3))
        .body("[0].name", equalTo("Beach Tennis"))
        .body("[1].name", equalTo("Futvolei"))
        .body("[2].name", equalTo("Volei de Praia"));
  }

  @Test
  @DisplayName("Deve retornar 200 OK e uma lista vazia quando não existirem modalidades")
  void shouldReturn200_whenNoModalitiesExist() {
    // Act & Assert
    given()
        .spec(specification)
        .when()
        .get()
        .then()
        .statusCode(HttpStatus.OK.value())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .body("$", hasSize(0));
  }

  @Test
  @DisplayName("Deve permitir acesso sem autenticação")
  void shouldAllowAccessWithoutAuthentication() {
    // Arrange
    mockPersistModality("Basquete");

    // Act & Assert
    given()
        .spec(specification)
        .when()
        .get()
        .then()
        .statusCode(HttpStatus.OK.value())
        .body("$", hasSize(1))
        .body("[0].name", equalTo("Basquete"));
  }
}
