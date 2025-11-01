package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.request.CreateModalityRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import com.projetoExtensao.arenaMafia.integration.config.util.modality.InvalidModalityNameProvider;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de integração para AdminModalityController")
public class AdminModalityControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private ModalityRepositoryPort modalityRepository;
  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/modalities")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistAdminUser();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();
  }

  @Nested
  @DisplayName("Testes para endpoint POST /api/admin/modalities")
  class CreateModalityTests {

    @Nested
    @DisplayName("Cenários de sucesso - 201 Created")
    class CreateModalitySuccessScenarios {
      @Test
      @DisplayName("Deve criar uma modalidade com sucesso")
      void create_shouldReturn201_whenCreateModalitySuccessfully() {
        // Arrange
        CreateModalityRequestDto newModalityName =
            new CreateModalityRequestDto(("Nova Modalidade"));

        var response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(newModalityName)
                .when()
                .post()
                .then()
                .statusCode(201)
                .extract()
                .as(ModalityResponseDto.class);

        // Assert
        assertThat(response.name()).isEqualTo(newModalityName.name());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class CreateModalityErrorScenarios {

      @InvalidModalityNameProvider
      @DisplayName("Tenta criar uma modalidade com nome inválido")
      void create_shouldReturn400_whenModalityNameIsInvalid(String name, String expectedErrorCode) {
        // Arrange
        CreateModalityRequestDto request = new CreateModalityRequestDto(name);

        // Act
        ErrorResponseDto response =
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
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/modalities");
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class CreateModalityConflictScenarios {
      @Test
      @DisplayName("Tenta criar uma modalidade que já existe")
      void create_shouldReturn409_whenModalityAlreadyExists() {
        // Arrange
        CreateModalityRequestDto existingModality =
            new CreateModalityRequestDto("Modalidade Existente");
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .body(existingModality)
            .when()
            .post();

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .body(existingModality)
                .when()
                .post()
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_ALREADY_EXISTS;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/modalities");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para endpoint GET /api/admin/modalities/{modalityId}")
  class GetModalityByIdTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class GetModalityByIdSuccessScenarios {
      @Test
      @DisplayName("Deve buscar uma modalidade existente por ID")
      void getById_shouldReturn200_whenModalityExists() {
        // Arrange
        Modality modality = mockPersistModality("Modalidade Teste");

        // Act
        ModalityResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", modality.getId())
                .when()
                .get("/{modalityId}")
                .then()
                .statusCode(200)
                .extract()
                .as(ModalityResponseDto.class);

        // Assert
        assertThat(response.id()).isEqualTo(modality.getId());
        assertThat(response.name()).isEqualTo(modality.getName());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class GetModalityByIdError400Scenarios {
      @Test
      @DisplayName("Tenta buscar uma modalidade por um ID inválido")
      void getById_shouldReturn400_whenModalityIdIsInvalid() {
        // Arrange
        String invalidModalityId = "invalid-uuid";

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", invalidModalityId)
                .when()
                .get("/{modalityId}")
                .then()
                .log()
                .all()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + invalidModalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class GetModalityByIdError404Scenarios {
      @Test
      @DisplayName("Tenta buscar uma modalidade inexistente por um ID")
      void getById_shouldReturn404_whenModalityDoesNotExist() {
        // Arrange
        String modalityId = UUID.randomUUID().toString();

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", modalityId)
                .when()
                .get("/{modalityId}")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + modalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para endpoint PUT /api/admin/modalities/{modalityId}")
  class UpdateModalityTests {

    @Nested
    @DisplayName("Cenários de sucesso - 200 OK")
    class UpdateModalitySuccessScenarios {
      @Test
      @DisplayName("Deve atualizar uma modalidade com sucesso")
      void update_shouldReturn200_whenUpdateModalitySuccessfully() {
        // Arrange
        Modality existingModality = mockPersistModality("Modalidade Antiga");
        String updatedName = "Modalidade Atualizada";
        var updateRequest = new CreateModalityRequestDto(updatedName);

        // Act
        ModalityResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", existingModality.getId())
                .body(updateRequest)
                .when()
                .put("/{modalityId}")
                .then()
                .statusCode(200)
                .extract()
                .as(ModalityResponseDto.class);

        // Assert
        assertThat(response.id()).isEqualTo(existingModality.getId());
        assertThat(response.name()).isEqualTo(updatedName);
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class UpdateModalityError400Scenarios {

      @Test
      @DisplayName("Tenta atualizar uma modalidade com ID inválido")
      void update_shouldReturn400_whenModalityIdIsInvalid() {
        // Arrange
        String invalidModalityId = "invalid-uuid";
        var updateRequest = new CreateModalityRequestDto("Nome Válido");

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", invalidModalityId)
                .body(updateRequest)
                .when()
                .put("/{modalityId}")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + invalidModalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }

      @InvalidModalityNameProvider
      @DisplayName("Tenta atualizar uma modalidade com nome inválido")
      void update_shouldReturn400_whenModalityNameIsInvalid(String name, String expectedErrorCode) {
        // Arrange
        String uniqueModalityName = "Modalidade " + new Random().nextInt(10000);
        Modality existingModality = mockPersistModality(uniqueModalityName);
        var updateRequest = new CreateModalityRequestDto(name);

        // Act
        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", existingModality.getId())
                .body(updateRequest)
                .when()
                .put("/{modalityId}")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        // Assert
        ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
        List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + existingModality.getId());
        assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
        assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());

        assertThat(fieldErrors).isNotEmpty();
        assertThat(fieldErrors)
            .anyMatch(
                fieldError ->
                    fieldError.fieldName().equals("name")
                        && fieldError.errorCode().equals(errorCode.name())
                        && fieldError.developerMessage().equals(errorCode.getMessage()));
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class UpdateModalityError404Scenarios {
      @Test
      @DisplayName("Tenta atualizar uma modalidade inexistente")
      void update_shouldReturn404_whenModalityDoesNotExist() {
        // Arrange
        String nonExistentModalityId = UUID.randomUUID().toString();
        var updateRequest = new CreateModalityRequestDto("Nome Válido");

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", nonExistentModalityId)
                .body(updateRequest)
                .when()
                .put("/{modalityId}")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + nonExistentModalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class UpdateModalityError409Scenarios {
      @Test
      @DisplayName("Tenta atualizar uma modalidade com um nome que já existe")
      void update_shouldReturn409_whenModalityNameAlreadyExists() {
        // Arrange
        String existingModalityName = "Modalidade Existente";
        mockPersistModality(existingModalityName);

        Modality modalityToUpdate = mockPersistModality("Modalidade Para Atualizar");
        var updateRequest = new CreateModalityRequestDto(existingModalityName);

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", modalityToUpdate.getId())
                .body(updateRequest)
                .when()
                .put("/{modalityId}")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_ALREADY_EXISTS;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + modalityToUpdate.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint DELETE /api/admin/modalities/{modalityId}")
  class DeleteModalityTests {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class DeleteModalitySuccessScenarios {
      @Test
      @DisplayName("Deve deletar uma modalidade com sucesso")
      void delete_shouldReturn204_whenDeleteModalitySuccessfully() {
        // Arrange
        Modality modality = mockPersistModality("Modalidade Para Deletar");

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("modalityId", modality.getId())
            .when()
            .delete("/{modalityId}")
            .then()
            .statusCode(204);

        // Assert
        assertThat(modalityRepository.findById(modality.getId())).isEmpty();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class DeleteModalityError400Scenarios {
      @Test
      @DisplayName("Tenta deletar uma modalidade com ID inválido")
      void delete_shouldReturn400_whenModalityIdIsInvalid() {
        // Arrange
        String invalidModalityId = "invalid-uuid";

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", invalidModalityId)
                .when()
                .delete("/{modalityId}")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + invalidModalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class DeleteModalityError404Scenarios {
      @Test
      @DisplayName("Tenta deletar uma modalidade inexistente")
      void delete_shouldReturn404_whenModalityDoesNotExist() {
        // Arrange
        String nonExistentModalityId = UUID.randomUUID().toString();

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", nonExistentModalityId)
                .when()
                .delete("/{modalityId}")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + nonExistentModalityId);
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class DeleteModalityError409Scenarios {
      @Test
      @DisplayName("Tenta deletar uma modalidade que está em uso")
      void delete_shouldReturn409_whenModalityIsInUse() {
        // Arrange
        Modality modalityInUse = mockPersistModality("Modalidade Em Uso");
        mockPersistCourt("Quadra 1", modalityInUse);

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", modalityInUse.getId())
                .when()
                .delete("/{modalityId}")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_IN_USE;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path()).isEqualTo("/api/admin/modalities/" + modalityInUse.getId());
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
