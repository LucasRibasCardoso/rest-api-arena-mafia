package com.projetoExtensao.arenaMafia.integration.config;

import static io.restassured.RestAssured.given;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseTestContainersConfig {

  @Container
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  private static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;

  public final String defaultUsername = "testuser";
  public final String defaultPassword = "123456";
  public final String defaultFullName = "Usuário de Teste";
  public final String defaultPhone = "+558320548186";

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    // PostgreSQL
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

    // Redis
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @AfterEach
  void cleanupAfterEach() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, "tb_refresh_token", "tb_users");
    redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
  }

  public record AuthTokensTest(
      String accessToken, RefreshTokenVO refreshToken, Cookie refreshTokenCookie) {}

  public AuthTokensTest mockLogin(String username, String password) {
    LoginRequestDto loginRequest = new LoginRequestDto(username, password);
    Response loginResponse =
        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(loginRequest)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .extract()
            .response();

    String accessToken = loginResponse.as(AuthResponseDto.class).accessToken();
    Cookie refreshTokenCookie = loginResponse.getDetailedCookie("refreshToken");
    RefreshTokenVO refreshToken = RefreshTokenVO.fromString(refreshTokenCookie.getValue());
    return new AuthTokensTest(accessToken, refreshToken, refreshTokenCookie);
  }

  public User mockPersistUser() {
    return mockPersistUser(AccountStatus.ACTIVE);
  }

  public User mockPersistUser(AccountStatus status) {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    Instant now = Instant.now();
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            defaultUsername,
            defaultFullName,
            defaultPhone,
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            now,
            now);
    return userRepository.save(user);
  }

  public User mockPersistUser(String username, String fullName, String phone, String password) {
    String passwordEncoded = passwordEncoder.encode(password);
    Instant now = Instant.now();
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            username,
            fullName,
            phone,
            passwordEncoded,
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_USER,
            now,
            now);
    return userRepository.save(user);
  }

  public User mockPersistUser(
      String username, String phone, AccountStatus status, Instant createdAt, Instant updatedAt) {
    String passwordEncoded = passwordEncoder.encode("123456");
    User user =
        User.reconstitute(
            UUID.randomUUID(),
            username,
            "Test User",
            phone,
            passwordEncoded,
            status,
            RoleEnum.ROLE_USER,
            createdAt,
            updatedAt);
    return userRepository.save(user);
  }

  public User mockPersistUser(
      String username,
      String fullName,
      String phone,
      String password,
      AccountStatus status,
      RoleEnum role) {
    String passwordEncoded = passwordEncoder.encode(password);
    Instant now = Instant.now();
    User user =
        User.reconstitute(
            UUID.randomUUID(), username, fullName, phone, passwordEncoded, status, role, now, now);
    return userRepository.save(user);
  }

  public RefreshToken mockPersistRefreshToken(Long expirationTime, User user) {
    RefreshToken refreshToken = RefreshToken.create(expirationTime, user);
    return refreshTokenRepository.save(refreshToken);
  }

  public void deleteMockUser(UUID uuid) {
    userRepository.findById(uuid).ifPresent(user -> userJpaRepository.deleteById(user.getId()));
  }

  public void alterAccountStatus(UUID userId, AccountStatus status) {
    User user = userRepository.findById(userId).orElseThrow();

    User lockedUser =
        User.reconstitute(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getPhone(),
            user.getPasswordHash(),
            status,
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt());
    userRepository.save(lockedUser);
  }

  public User mockPersistAdminUser() {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    Instant now = Instant.now();
    User adminUser =
        User.reconstitute(
            UUID.randomUUID(),
            defaultUsername,
            defaultFullName,
            defaultPhone,
            passwordEncoded,
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_ADMIN,
            now,
            now);
    return userRepository.save(adminUser);
  }

  public void mockPersistListOfUsers() {
    // Usuários adicionais para testes de filtro e paginação
    mockPersistUser(
        "joao_silva",
        "João da Silva",
        "+5511999000001",
        defaultPassword,
        AccountStatus.ACTIVE,
        RoleEnum.ROLE_USER);
    mockPersistUser(
        "maria_souza",
        "Maria Souza",
        "+5511999000002",
        defaultPassword,
        AccountStatus.PENDING_VERIFICATION,
        RoleEnum.ROLE_USER);
    mockPersistUser(
        "carlos_pereira",
        "Carlos Pereira",
        "+5511999000003",
        defaultPassword,
        AccountStatus.LOCKED,
        RoleEnum.ROLE_ADMIN);
    mockPersistUser(
        "ana_oliveira",
        "Ana Oliveira",
        "+5511999000004",
        defaultPassword,
        AccountStatus.DISABLED,
        RoleEnum.ROLE_ADMIN);
    mockPersistUser(
        "bruno_ferreira",
        "Bruno Ferreira",
        "+5511999000005",
        defaultPassword,
        AccountStatus.ACTIVE,
        RoleEnum.ROLE_MODERATOR);
    mockPersistUser(
        "carla_gomes",
        "Carla Gomes",
        "+5511999000006",
        defaultPassword,
        AccountStatus.ACTIVE,
        RoleEnum.ROLE_USER);
    mockPersistUser(
        "diego_rodrigues",
        "Diego Rodrigues",
        "+5511999000007",
        defaultPassword,
        AccountStatus.PENDING_VERIFICATION,
        RoleEnum.ROLE_MODERATOR);
    mockPersistUser(
        "elaine_martins",
        "Elaine Martins",
        "+5511999000008",
        defaultPassword,
        AccountStatus.LOCKED,
        RoleEnum.ROLE_USER);
    mockPersistUser(
        "felipe_ribeiro",
        "Felipe Ribeiro",
        "+5511999000009",
        defaultPassword,
        AccountStatus.DISABLED,
        RoleEnum.ROLE_ADMIN);
    mockPersistUser(
        "gabriela_alves",
        "Gabriela Alves",
        "+5511999000010",
        defaultPassword,
        AccountStatus.ACTIVE,
        RoleEnum.ROLE_USER);
  }
}
