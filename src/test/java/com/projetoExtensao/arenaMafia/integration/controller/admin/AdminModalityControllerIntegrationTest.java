package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.ModalityEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.ModalityJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.modality.request.CreateModalityRequestDto;
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

@DisplayName("Testes de integração para AdminModalityController")
public class AdminModalityControllerIntegrationTest extends WebIntegrationTestConfig {

  private RequestSpecification specification;
  private String accessToken;
  @Autowired private ModalityJpaRepository modalityJpaRepository;

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
  @DisplayName("Testes para endpoint GET /api/admin/modalities")
  class GetAllModalitiesTests {

    @Test
    @DisplayName("Deve buscar todas as modalidades sem filtro")
    void getAll_shouldReturn200_whenNoFilterIsApplied() {
      // Arrange
      mockPersistModality("Modalidade 1");
      mockPersistModality("Modalidade 2");
      mockPersistDisableModality("Modalidade 3");

      // Act
      ModalityResponseDto[] response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(ModalityResponseDto[].class);

      // Assert
      assertThat(response.length).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Deve buscar apenas as modalidades ativas")
    void getAll_shouldReturn200_whenFilterByActiveModalities() {
      // Arrange
      mockPersistModality("Modalidade Ativa 1");
      mockPersistModality("Modalidade Ativa 2");
      mockPersistDisableModality("Modalidade Inativa 1");

      // Act
      ModalityResponseDto[] response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", true)
              .when()
              .get()
              .then()
              .statusCode(200)
              .log()
              .all()
              .extract()
              .as(ModalityResponseDto[].class);

      // Assert
      assertThat(response).allMatch(ModalityResponseDto::isActive);
    }

    @Test
    @DisplayName("Deve buscar apenas as modalidades inativas")
    void getAll_shouldReturn200_whenFilterByInactiveModalities() {
      // Arrange
      mockPersistModality("Modalidade Ativa 1");
      mockPersistDisableModality("Modalidade Inativa 1");
      mockPersistDisableModality("Modalidade Inativa 2");

      // Act
      ModalityResponseDto[] response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("isActive", false)
              .when()
              .get()
              .then()
              .statusCode(200)
              .extract()
              .as(ModalityResponseDto[].class);

      // Assert
      assertThat(response).allMatch(modality -> !modality.isActive());
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
  @DisplayName("Testes para o endpoint PATCH /api/admin/modalities/{modalityId}")
  class DisableModalityTests {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class DisableModalitySuccessScenarios {
      @Test
      @DisplayName("Deve desativar uma modalidade com sucesso (soft delete)")
      void disable_shouldReturn204_whenDisableModalitySuccessfully() {
        // Arrange
        Modality modality = mockPersistModality("Modalidade Para Desativar");

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("modalityId", modality.getId())
            .when()
            .patch("/{modalityId}/disable")
            .then()
            .statusCode(204);

        ModalityEntity disabledModality =
            modalityJpaRepository.findById(modality.getId()).orElseThrow();
        assertThat(disabledModality.isActive()).isFalse();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class DisableModalityError400Scenarios {
      @Test
      @DisplayName("Tenta desativar uma modalidade com ID inválido")
      void disable_shouldReturn400_whenModalityIdIsInvalid() {
        // Arrange
        String invalidModalityId = "invalid-uuid";

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", invalidModalityId)
                .when()
                .patch("/{modalityId}/disable")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + invalidModalityId + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class DisableModalityError404Scenarios {
      @Test
      @DisplayName("Tenta desativar uma modalidade inexistente")
      void disable_shouldReturn404_whenModalityDoesNotExist() {
        // Arrange
        String nonExistentModalityId = UUID.randomUUID().toString();

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", nonExistentModalityId)
                .when()
                .patch("/{modalityId}/disable")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + nonExistentModalityId + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class DisableModalityError409Scenarios {
      @Test
      @DisplayName("Tenta desativar uma modalidade que está em uso")
      void disable_shouldReturn409_whenModalityIsInUse() {
        // Arrange
        Modality modalityInUse = mockPersistModality("Modalidade Em Uso");
        mockPersistCourt("Quadra 1", modalityInUse);

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", modalityInUse.getId())
                .when()
                .patch("/{modalityId}/disable")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_IN_USE;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + modalityInUse.getId() + "/disable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint PATCH /api/admin/modalities/{modalityId}/enable")
  class EnableModalityTests {

    @Nested
    @DisplayName("Cenários de sucesso - 204 No Content")
    class EnableModalitySuccessScenarios {
      @Test
      @DisplayName("Deve ativar uma modalidade com sucesso")
      void enable_shouldReturn204_whenEnableModalitySuccessfully() {
        // Arrange
        Modality modality = mockPersistDisableModality("Modalidade Para Ativar");

        // Act
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("modalityId", modality.getId())
            .when()
            .patch("/{modalityId}/enable")
            .then()
            .statusCode(204);

        ModalityEntity enabledModality =
            modalityJpaRepository.findById(modality.getId()).orElseThrow();
        assertThat(enabledModality.isActive()).isTrue();
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 400 Bad Request")
    class EnableModalityError400Scenarios {

      @Test
      @DisplayName("Tenta ativar uma modalidade com ID inválido")
      void enable_shouldReturn400_whenModalityIdIsInvalid() {
        // Arrange
        String invalidModalityId = "invalid-uuid";

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", invalidModalityId)
                .when()
                .patch("/{modalityId}/enable")
                .then()
                .statusCode(400)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.INVALID_REQUEST_PARAMETER;

        // Assert
        assertThat(response.status()).isEqualTo(400);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + invalidModalityId + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 404 Not Found")
    class EnableModalityError404Scenarios {

      @Test
      @DisplayName("Tenta ativar uma modalidade inexistente")
      void enable_shouldReturn404_whenModalityDoesNotExist() {
        // Arrange
        String nonExistentModalityId = UUID.randomUUID().toString();

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", nonExistentModalityId)
                .when()
                .patch("/{modalityId}/enable")
                .then()
                .statusCode(404)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_NOT_FOUND;

        // Assert
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + nonExistentModalityId + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }

    @Nested
    @DisplayName("Cenários de erro - 409 Conflict")
    class EnableModalityError409Scenarios {

      @Test
      @DisplayName("Tenta ativar uma modalidade que já está ativa")
      void enable_shouldReturn409_whenModalityIsAlreadyActive() {
        // Arrange
        Modality activeModality = mockPersistModality("Modalidade Já Ativa");

        ErrorResponseDto response =
            given()
                .spec(specification)
                .header("Authorization", accessToken)
                .pathParam("modalityId", activeModality.getId())
                .when()
                .patch("/{modalityId}/enable")
                .then()
                .statusCode(409)
                .extract()
                .as(ErrorResponseDto.class);

        ErrorCode errorCode = ErrorCode.MODALITY_ALREADY_ENABLE;

        // Assert
        assertThat(response.status()).isEqualTo(409);
        assertThat(response.path())
            .isEqualTo("/api/admin/modalities/" + activeModality.getId() + "/enable");
        assertThat(response.errorCode()).isEqualTo(errorCode.name());
        assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      }
    }
  }
}
