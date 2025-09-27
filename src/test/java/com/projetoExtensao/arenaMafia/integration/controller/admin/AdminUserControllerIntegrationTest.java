package com.projetoExtensao.arenaMafia.integration.controller.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.response.UserAdminResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.integration.config.WebIntegrationTestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para AdminUserController")
public class AdminUserControllerIntegrationTest extends WebIntegrationTestConfig {

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
  @DisplayName("Testes para o endpoints GET /api/admin/users")
  class ListUsers {

    @Test
    @DisplayName("Deve retornar 200 e a lista de usuários paginada com valores padrão")
    void shouldReturn200AndPaginatedListOfUsersWithDefaultValues() {
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
    @DisplayName("Testes de Sucesso para Paginação, Filtros e Ordenação")
    class ListUsersSuccessTests {

      @Nested
      @DisplayName("Testes de paginação")
      class PaginationTests {

        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários paginada")
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
        @DisplayName("Deve retornar 200 e a lista de usuários paginada na última página")
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
        @DisplayName("Deve retornar 200 e a lista de usuários encontrados pelo nome")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users).hasSize(1);
          assertThat(users.getFirst().username()).isEqualTo("gabriela_alves");
        }

        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários encontrados pelo nome completo")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users).hasSize(1);
          assertThat(users.getFirst().fullName()).isEqualTo("João da Silva");
        }

        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários encontrados pelo telefone")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

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
        @DisplayName("Deve retornar 200 e a lista de usuários filtrada por status")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.stream().allMatch(user -> user.status().equals(status.name()))).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
            value = RoleEnum.class,
            names = {"ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER"})
        @DisplayName("Deve retornar 200 e a lista de usuários filtrada por role")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.stream().allMatch(user -> user.role().equals(role.name()))).isTrue();
        }

        @Test
        @DisplayName(
            "Deve retornar 200 e a lista de usuários filtrada por intervalo de data de criação")
        void shouldReturn200AndListOfUsersFilteredByCreationDateRange() {
          // Arrange
          Date startDate = Date.from(Instant.now().minusSeconds(3600)); // 1 hour in the past
          Date endDate = Date.from(Instant.now().plusSeconds(3600)); // 1 hour in the future

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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users).isNotEmpty();
          assertThat(users.size()).isEqualTo(11);
        }
      }

      @Nested
      @DisplayName("Testes de ordenação")
      class SortingTests {
        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários ordenada por username asc")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("ana_oliveira");
          assertThat(users.getLast().username()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários ordenada por username desc")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("testuser");
          assertThat(users.getLast().username()).isEqualTo("ana_oliveira");
        }

        @Test
        @DisplayName("Deve retornar 200 e a lista de usuários ordenada por createdAt desc")
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
          List<UserAdminResponseDto> users =
              jsonPath.getList("content", UserAdminResponseDto.class);

          assertThat(users.getFirst().username()).isEqualTo("gabriela_alves");
          assertThat(users.getLast().username()).isEqualTo("testuser");
        }
      }
    }

    @Test
    @DisplayName("Deve retornar 400 quando os parâmetros de paginação forem inválidos")
    void shouldReturn400WhenPaginationParametersAreInvalid() {
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
    @DisplayName("Deve retonar 400 quando o parâmetro 'term' for inválido")
    void shouldReturn400WhenTermParameterIsInvalid() {
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

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("term");
      assertThat(response.fieldErrors().getFirst().errorCode())
          .isEqualTo(ErrorCode.TERM_TOO_LONG.name());
    }

    @Test
    @DisplayName("Deve retornar 400 quando o parâmetro 'status' for inválido")
    void shouldReturn400WhenStatusParameterIsInvalid() {
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

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("status");
      assertThat(response.fieldErrors().getFirst().errorCode())
          .isEqualTo(ErrorCode.INVALID_ACCOUNT_STATUS.name());
    }

    @Test
    @DisplayName("Deve retornar 400 quando o parâmetro 'role' for inválido")
    void shouldReturn400WhenRoleParameterIsInvalid() {
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

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
      assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");

      assertThat(response.fieldErrors().getFirst().fieldName()).isEqualTo("role");
      assertThat(response.fieldErrors().getFirst().errorCode())
          .isEqualTo(ErrorCode.INVALID_ROLE.name());
    }

    @Test
    @DisplayName("Deve retornar 400 quando o a data de início for posterior a data de fim")
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

      // Assert
      assertThat(response.status()).isEqualTo(400);
      assertThat(response.errorCode()).isEqualTo(ErrorCode.START_DATE_AFTER_END_DATE.name());
      assertThat(response.developerMessage())
          .isEqualTo(ErrorCode.START_DATE_AFTER_END_DATE.getMessage());
      assertThat(response.path()).isEqualTo("/api/admin/users");
    }

    @Test
    @DisplayName("Deve retornar 403 quando o usuário não tiver permissão de ADMIN ou DEVELOPER")
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
}
