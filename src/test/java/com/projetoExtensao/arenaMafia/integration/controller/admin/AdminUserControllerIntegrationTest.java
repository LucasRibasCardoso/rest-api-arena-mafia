package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.UpdateUserRoleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.request.UpdateUserStatusRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.user.response.AdminUserResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
@DisplayName("Testes de Integração para AdminUserController")
public class AdminUserControllerIntegrationTest extends WebIntegrationTestConfig {

  @Autowired private UserRepositoryPort userRepository;
  private RequestSpecification specification;
  private String accessToken;

  @BeforeEach
  void setup() {
    super.setupRestAssured();

    specification =
        new RequestSpecBuilder()
            .setBasePath("/api/admin/users")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();

    mockPersistAdminUser();
    AuthTokensTest tokensTest = mockLogin(defaultUsername, defaultPassword);
    accessToken = "Bearer " + tokensTest.accessToken();

    mockPersistListOfUsers();
  }

  @Nested
  @DisplayName("Testes para o endpoint GET /api/admin/users")
  class ListUsersTests {

    @Test
    @DisplayName("Deve retornar 200 OK e a lista de usuários paginada com valores padrão")
    void listUsers_shouldReturn200_withDefaultPaginationValues() {
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
      assertThat(jsonPath.getInt("totalElements")).isGreaterThan(10);
      assertThat(jsonPath.getList("content").size()).isEqualTo(11);
    }

    @Nested
    @DisplayName("Deve retornar 200 OK para Paginação, Filtros e Ordenação")
    class ListUsersSuccessTests {

      @Nested
      @DisplayName("Testes de paginação")
      class PaginationTests {

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários paginada")
        void shouldReturn200AndPaginatedListOfUsers() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("size", 5)
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

          assertThat(jsonPath.getInt("size")).isEqualTo(5);
          assertThat(jsonPath.getInt("number")).isEqualTo(1);
          assertThat(jsonPath.getInt("totalElements")).isGreaterThan(10);
          assertThat(jsonPath.getList("content").size()).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários paginada na última página")
        void shouldReturn200AndPaginatedListOfUsersOnLastPage() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("size", 5)
                  .queryParam("page", 2)
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);

          assertThat(jsonPath.getInt("size")).isEqualTo(5);
          assertThat(jsonPath.getInt("number")).isEqualTo(2);
          assertThat(jsonPath.getInt("totalElements")).isGreaterThan(10);
          assertThat(jsonPath.getList("content").size()).isEqualTo(1);
        }
      }

      @Nested
      @DisplayName("Testes de buscas por username, fullName e phone")
      class SearchTests {
        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários encontrados pelo nome")
        void shouldReturn200AndListOfUsersFoundedByName() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("term", "gabriela_alves")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).hasSize(1);
          assertThat(users.getFirst().username()).isEqualTo("gabriela_alves");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários encontrados pelo nome completo")
        void shouldReturn200AndListOfUsersFoundedByFullName() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("term", "João da Silva")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).hasSize(1);
          assertThat(users.getFirst().fullName()).isEqualTo("João da Silva");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários encontrados pelo telefone")
        void shouldReturn200AndListOfUsersFoundedByPhone() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("term", "000001")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).hasSize(1);
          assertThat(users.getFirst().phone()).isEqualTo("+5511999000001");
        }
      }

      @Nested
      @DisplayName("Testes de filtros")
      class FilterTests {

        @ParameterizedTest
        @EnumSource(
            value = AccountStatus.class,
            names = {"ACTIVE", "LOCKED", "DISABLED", "PENDING_VERIFICATION"})
        @DisplayName("Deve retornar 200 OK e a lista de usuários filtrada por status")
        void shouldReturn200AndListOfUsersFilteredByStatus(AccountStatus status) {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("status", status)
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.stream().allMatch(user -> user.status().equals(status.name()))).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
            value = RoleEnum.class,
            names = {"ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR"})
        @DisplayName("Deve retornar 200 OK e a lista de usuários filtrada por role")
        void shouldReturn200AndListOfUsersFilteredByRole(RoleEnum role) {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("role", role)
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.stream().allMatch(user -> user.role().equals(role.name()))).isTrue();
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários filtrada por intervalo de data")
        void shouldReturn200AndListOfUsersFilteredByCreationDateRange() {
          // Arrange
          Date startDate = Date.from(Instant.now().minusSeconds(3600)); // 1 hora no passado
          Date endDate = Date.from(Instant.now().plusSeconds(3600)); // 1 hora no futuro

          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("startDate", startDate.getTime())
                  .queryParam("endDate", endDate.getTime())
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.size()).isEqualTo(11);
        }
      }

      @Nested
      @DisplayName("Testes de ordenação")
      class SortingTests {
        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários ordenada por username asc")
        void shouldReturn200AndListOfUsersOrderedByUsernameAsc() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("sort", "username")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("ana_oliveira");
          assertThat(users.getLast().username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários ordenada por username desc")
        void shouldReturn200AndListOfUsersOrderedByUsernameDesc() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("sort", "username,desc")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("testuser");
          assertThat(users.getLast().username()).isEqualTo("ana_oliveira");
        }

        @Test
        @DisplayName("Deve retornar 200 OK e a lista de usuários ordenada por createdAt desc")
        void shouldReturn200AndListOfUsersOrderedByCreatedDesc() {
          // Act
          String responseBody =
              given()
                  .spec(specification)
                  .header("Authorization", accessToken)
                  .queryParam("sort", "createdAt,desc")
                  .when()
                  .get()
                  .then()
                  .statusCode(200)
                  .extract()
                  .body()
                  .asString();

          // Assert
          JsonPath jsonPath = new JsonPath(responseBody);
          List<AdminUserResponseDto> users =
              jsonPath.getList("content", AdminUserResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("gabriela_alves");
          assertThat(users.getLast().username()).isEqualTo("testuser");
        }
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando os parâmetros de paginação forem inválidos")
    void listUsers_shouldReturn400_whenPaginationParametersAreInvalid() {
      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("sort", "invalidField,ask")
              .when()
              .get()
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.INVALID_SORT_PARAMETER;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o parâmetro 'term' for inválido")
    void listUsers_shouldReturn400_whenTermParameterIsInvalid() {
      // Arrange
      String invalidTerm = "a".repeat(101);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("term", invalidTerm)
              .when()
              .get()
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.TERM_TOO_LONG;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("term");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o parâmetro 'status' for inválido")
    void listUsers_shouldReturn400_whenStatusParameterIsInvalid() {
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
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.INVALID_ACCOUNT_STATUS;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("status");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o parâmetro 'role' for inválido")
    void listUsers_shouldReturn400_whenRoleParameterIsInvalid() {
      // Arrange
      String invalidRole = "INVALID_ROLE";

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("role", invalidRole)
              .when()
              .get()
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.INVALID_ROLE;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("role");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName(
        "Deve retornar 400 Bad Request quando o a data de início for posterior a data de fim")
    void shouldReturn400WhenStartDateIsAfterEndDate() {
      // Arrange
      LocalDate startDate = LocalDate.now().plusDays(1);
      LocalDate endDate = LocalDate.now();

      DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
      String startDateAsString = startDate.format(formatter);
      String endDateAsString = endDate.format(formatter);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .queryParam("createdAtStart", startDateAsString)
              .queryParam("createdAtEnd", endDateAsString)
              .when()
              .get()
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.START_DATE_AFTER_END_DATE;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o usuário não tiver permissão para acessar")
    void shouldReturn403WhenUserIsNotAdminOrDeveloper() {
      // Arrange
      AuthTokensTest tokensTest = mockLogin("joao_silva", defaultPassword);
      accessToken = tokensTest.accessToken();

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + accessToken)
              .when()
              .get()
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/admin/users/{userId}/status")
  class UpdateUserStatusTests {

    @Nested
    @DisplayName("Testes de Sucesso")
    class UpdateUserStatusSuccessTests {

      @Test
      @DisplayName("Deve retornar 204 OK ao atualizar o status para ACTIVE com sucesso")
      void shouldReturn204_whenUpdateStatusToActive() {
        // Arrange
        User user =
            mockPersistUser(
                "user_active",
                "User Active",
                "+5511999000020",
                "pass",
                AccountStatus.LOCKED,
                RoleEnum.ROLE_USER);
        var request = new UpdateUserStatusRequestDto(AccountStatus.ACTIVE);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("userId", user.getId())
            .body(request)
            .when()
            .patch("/{userId}/status")
            .then()
            .statusCode(204);

        User updated = userRepository.findByIdOrElseThrow(user.getId());
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.ACTIVE);
      }

      @Test
      @DisplayName("Deve retornar 204 OK ao atualizar o status para LOCKED com sucesso")
      void shouldReturn204_whenUpdateStatusToLocked() {
        User user =
            mockPersistUser(
                "user_locked",
                "User Locked",
                "+5511999000021",
                "pass",
                AccountStatus.ACTIVE,
                RoleEnum.ROLE_USER);
        var request = new UpdateUserStatusRequestDto(AccountStatus.LOCKED);

        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("userId", user.getId())
            .body(request)
            .when()
            .patch("/{userId}/status")
            .then()
            .statusCode(204);

        User updated = userRepository.findByIdOrElseThrow(user.getId());
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.LOCKED);
      }

      @Test
      @DisplayName("Deve retornar 204 OK ao atualizar o status para DISABLED com sucesso")
      void shouldReturn204_whenUpdateStatusToDisabled() {
        User user =
            mockPersistUser(
                "user_disabled",
                "User Disabled",
                "+5511999000022",
                "pass",
                AccountStatus.ACTIVE,
                RoleEnum.ROLE_USER);
        var request = new UpdateUserStatusRequestDto(AccountStatus.DISABLED);

        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("userId", user.getId())
            .body(request)
            .when()
            .patch("/{userId}/status")
            .then()
            .statusCode(204);

        User updated = userRepository.findByIdOrElseThrow(user.getId());
        assertThat(updated.getStatus()).isEqualTo(AccountStatus.DISABLED);
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o status for inválido")
    void shouldReturn400_whenStatusIsInvalid() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      String invalidStatus = "INVALID_STATUS";
      String requestBody = String.format("{\"status\":\"%s\"}", invalidStatus);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", userId)
              .body(requestBody)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.INVALID_ACCOUNT_STATUS;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/status");

      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("status");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o status não for informado")
    void shouldReturn400_whenStatusIsNotInformed() {
      // Arrange
      UUID userId = UUID.randomUUID();
      String requestBody = "{}";

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", userId)
              .body(requestBody)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.ACCOUNT_STATUS_REQUIRED;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/status");

      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("status");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o usuário não tiver permissão para acessar")
    void shouldReturn403_whenUserIsNotPermission() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      AuthTokensTest tokensTest = mockLogin("joao_silva", defaultPassword);
      accessToken = tokensTest.accessToken();
      var request = new UpdateUserStatusRequestDto(AccountStatus.LOCKED);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + accessToken)
              .pathParam("userId", userId)
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/status");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o admin tentar atualizar o próprio status")
    void shouldReturn403_whenAdminTryToUpdateOwnStatus() {
      // Arrange
      User adminUser = userRepository.findByUsername(defaultUsername).orElseThrow();
      var request = new UpdateUserStatusRequestDto(AccountStatus.LOCKED);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", adminUser.getId())
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ADMIN_CANNOT_UPDATE_OWN_STATUS;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + adminUser.getId() + "/status");
    }

    @Test
    @DisplayName(
        "Deve retornar 403 Forbidden quando o admin tentar atualizar o status de um usuário com"
            + " verificação pendente")
    void shouldReturn403_whenAdminTryToUpdateStatusOfUserPending() {
      // Arrange
      User pendingUser =
          mockPersistUser(
              "pending_user",
              "Pending User",
              "+5511999000024",
              "pass",
              AccountStatus.PENDING_VERIFICATION,
              RoleEnum.ROLE_USER);
      var request = new UpdateUserStatusRequestDto(AccountStatus.LOCKED);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", pendingUser.getId())
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ADMIN_CANNOT_UPDATE_STATUS_OF_UNVERIFIED_USER;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + pendingUser.getId() + "/status");
    }

    @Test
    @DisplayName(
        "Deve retornar 403 Forbidden quando o admin tentar atualizar o status para"
            + " PENDING_VERIFICATION")
    void shouldReturn403_whenTryToUpdateStatusToPendingVerification() {
      // Arrange
      User user =
          mockPersistUser(
              "user_pending",
              "User Pending",
              "+5511999000025",
              "pass",
              AccountStatus.LOCKED,
              RoleEnum.ROLE_USER);
      var request = new UpdateUserStatusRequestDto(AccountStatus.PENDING_VERIFICATION);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", user.getId())
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.INVALID_ACCOUNT_STATUS;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + user.getId() + "/status");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void shouldReturn404_whenUserIsNotFound() {
      // Arrange
      UUID nonExistentUserId = UUID.randomUUID();
      var request = new UpdateUserStatusRequestDto(AccountStatus.LOCKED);

      // Act
      ErrorResponseDto response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", nonExistentUserId)
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      // Assert
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + nonExistentUserId + "/status");
    }

    @ParameterizedTest
    @EnumSource(
        value = AccountStatus.class,
        names = {"ACTIVE", "LOCKED", "DISABLED"})
    @DisplayName("Deve retornar 409 Conflict quando tentar atualizar o status para o mesmo")
    void shouldReturn409_whenTryToUpdateToSameStatus(AccountStatus status) {
      // Arrange
      User user =
          mockPersistUser(
              "conflict_user",
              "Conflict User",
              "+5511999000023",
              "pass",
              status,
              RoleEnum.ROLE_USER);
      var request = new UpdateUserStatusRequestDto(status);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", user.getId())
              .body(request)
              .when()
              .patch("/{userId}/status")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode expectedErrorCode;
      switch (status) {
        case ACTIVE -> expectedErrorCode = ErrorCode.ACCOUNT_ALREADY_ACTIVE;
        case LOCKED -> expectedErrorCode = ErrorCode.ACCOUNT_ALREADY_LOCKED;
        case DISABLED -> expectedErrorCode = ErrorCode.ACCOUNT_ALREADY_DISABLED;
        default -> expectedErrorCode = ErrorCode.ACCOUNT_STATE_CONFLICT;
      }

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
      assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + user.getId() + "/status");
    }
  }

  @Nested
  @DisplayName("Testes para o endpoint /api/admin/users/{userId}/role")
  class UpdateUserRoleTests {

    @Nested
    @DisplayName("Testes de Sucesso")
    class UpdateUserRoleSuccessTests {

      @Test
      @DisplayName("Deve retornar 204 OK ao atualizar a role para ROLE_ADMIN")
      void shouldReturn204_whenUpdateRoleToAdmin() {
        // Arrange
        User user =
            mockPersistUser(
                "user_to_admin",
                "User to Admin",
                "+5511999000030",
                "pass",
                AccountStatus.ACTIVE,
                RoleEnum.ROLE_USER);
        var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_ADMIN);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("userId", user.getId())
            .body(request)
            .when()
            .patch("/{userId}/role")
            .then()
            .statusCode(204);

        User updated = userRepository.findByIdOrElseThrow(user.getId());
        assertThat(updated.getRole()).isEqualTo(RoleEnum.ROLE_ADMIN);
      }

      @Test
      @DisplayName("Deve retornar 204 OK ao atualizar a role para ROLE_USER")
      void shouldReturn204_whenUpdateRoleToUser() {
        // Arrange
        User user =
            mockPersistUser(
                "admin_to_user",
                "Admin to User",
                "+5511999000031",
                "pass",
                AccountStatus.ACTIVE,
                RoleEnum.ROLE_ADMIN);
        var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_USER);

        // Act & Assert
        given()
            .spec(specification)
            .header("Authorization", accessToken)
            .pathParam("userId", user.getId())
            .body(request)
            .when()
            .patch("/{userId}/role")
            .then()
            .statusCode(204);

        User updated = userRepository.findByIdOrElseThrow(user.getId());
        assertThat(updated.getRole()).isEqualTo(RoleEnum.ROLE_USER);
      }
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a role for inválida")
    void shouldReturn400WhenRoleIsInvalid() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      String invalidRole = "INVALID_ROLE";
      String requestBody = String.format("{\"role\":\"%s\"}", invalidRole);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", userId)
              .body(requestBody)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode erroCode = ErrorCode.INVALID_ROLE;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/role");
      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("role");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(erroCode.name());
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando a role não for informada")
    void shouldReturn400_whenRoleIsNotInformed() {
      // Arrange
      String userId = UUID.randomUUID().toString();
      String requestBody = "{}";

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", userId)
              .body(requestBody)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(400)
              .extract()
              .as(ErrorResponseDto.class);

      List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
      ErrorCode errorCode = ErrorCode.ROLE_REQUIRED;

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/role");
      assertThat(fieldErrors.getFirst().fieldName()).isEqualTo("role");
      assertThat(fieldErrors.getFirst().errorCode()).isEqualTo(errorCode.name());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o usuário não tiver permissão para acessar")
    void shouldReturn403_whenUserIsNotPermission() {
      // Arrange
      AuthTokensTest tokensTest = mockLogin("joao_silva", defaultPassword);
      accessToken = tokensTest.accessToken();
      var userId = UUID.randomUUID();
      var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_USER);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", "Bearer " + accessToken)
              .pathParam("userId", userId)
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ACCESS_DENIED;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + userId + "/role");
    }

    @Test
    @DisplayName(
        "Deve retornar 403 Forbidden quando o admin tentar alterar a role para um nível superior ao"
            + " seu")
    void shouldReturn403_whenAdminTryToChangeRoleToHigherLevel() {
      // Arrange
      User user =
          mockPersistUser(
              "user_to_moderator",
              "User to Moderator",
              "+5511999000041",
              "pass",
              AccountStatus.ACTIVE,
              RoleEnum.ROLE_USER);
      var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_MODERATOR);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", user.getId())
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.INVALID_ROLE;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + user.getId() + "/role");
    }

    @Test
    @DisplayName(
        "Deve retornar 403 Forbidden quando o admin tentar atualizar a permissão de um usuário com"
            + " verificação pendente")
    void shouldReturn403_whenAdminTryToChangeRoleFromUserNotVerified() {
      // Arrange
      User pendingUser =
          mockPersistUser(
              "pending_role_user",
              "Pending Role User",
              "+5511999000043",
              "pass",
              AccountStatus.PENDING_VERIFICATION,
              RoleEnum.ROLE_USER);
      var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_ADMIN);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", pendingUser.getId())
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ADMIN_CANNOT_UPDATE_ROLE_OF_UNVERIFIED_USER;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + pendingUser.getId() + "/role");
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden quando o admin tentar atualizar a própria role")
    void shouldReturn403_whenAdminTryToUpdateOwnRole() {
      // Arrange
      User adminUser = userRepository.findByUsername(defaultUsername).orElseThrow();
      var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_USER);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", adminUser.getId())
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(403)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.ADMIN_CANNOT_UPDATE_OWN_ROLE;

      // Assert
      assertThat(response.status()).isEqualTo(403);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + adminUser.getId() + "/role");
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found quando o usuário não for encontrado")
    void shouldReturn404_whenUserIsNotFound() {
      // Arrange
      var nonExistentUserId = UUID.randomUUID();
      var request = new UpdateUserRoleRequestDto(RoleEnum.ROLE_MODERATOR);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", nonExistentUserId)
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(404)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode errorCode = ErrorCode.USER_NOT_FOUND;

      // Assert
      assertThat(response.status()).isEqualTo(404);
      assertThat(response.errorCode()).isEqualTo(errorCode.name());
      assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + nonExistentUserId + "/role");
    }

    @ParameterizedTest
    @EnumSource(
        value = RoleEnum.class,
        names = {"ROLE_ADMIN", "ROLE_USER"})
    @DisplayName("Deve retornar 409 Conflict quando tentar atualizar para a mesma role")
    void shouldReturn409_whenTryToUpdateToSameRole(RoleEnum role) {
      // Arrange
      User user =
          mockPersistUser(
              "conflict_role_user",
              "Conflict Role User",
              "+5511999000042",
              "pass",
              AccountStatus.ACTIVE,
              role);
      var request = new UpdateUserRoleRequestDto(role);

      // Act
      var response =
          given()
              .spec(specification)
              .header("Authorization", accessToken)
              .pathParam("userId", user.getId())
              .body(request)
              .when()
              .patch("/{userId}/role")
              .then()
              .statusCode(409)
              .extract()
              .as(ErrorResponseDto.class);

      ErrorCode expectedErrorCode;
      switch (role) {
        case ROLE_ADMIN -> expectedErrorCode = ErrorCode.USER_ALREADY_ADMIN;
        case ROLE_USER -> expectedErrorCode = ErrorCode.USER_ALREADY_USER;
        default -> expectedErrorCode = ErrorCode.ACCOUNT_STATE_CONFLICT;
      }

      // Assert
      assertThat(response.status()).isEqualTo(409);
      assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
      assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users/" + user.getId() + "/role");
    }
  }
}
