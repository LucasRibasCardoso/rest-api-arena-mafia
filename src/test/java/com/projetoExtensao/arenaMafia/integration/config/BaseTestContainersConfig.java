package com.projetoExtensao.arenaMafia.integration.config;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.port.repository.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.repository.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.model.*;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuração base para testes de integração com Testcontainers.
 * Utiliza o padrão Singleton para reutilizar containers entre classes de teste,
 * reduzindo significativamente o tempo de execução da suite de testes.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseTestContainersConfig {

  // =================== Singleton Containers ===================
  // Containers são iniciados uma vez e reutilizados por todas as classes de teste

  private static final PostgreSQLContainer<?> postgreSQLContainer;
  private static final GenericContainer<?> redis;
  private static final LocalStackContainer localStack;

  static {
    // PostgreSQL
    postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
        .withReuse(true);
    postgreSQLContainer.start();

    // Redis
    redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);
    redis.start();

    // LocalStack (SQS)
    localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
        .withServices(LocalStackContainer.Service.SQS)
        .withReuse(true);
    localStack.start();

    // Configurar filas SQS uma única vez
    setupSqsQueues();
  }

  private static void setupSqsQueues() {
    try {
      // 1. Criar as filas
      localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "sms-queue");
      localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "whatsapp-dlq");
      localStack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", "whatsapp-queue");

      // 2. Configurar VisibilityTimeout baixo para retries rápidos em testes
      localStack.execInContainer(
          "sh", "-c",
          "awslocal sqs set-queue-attributes " +
          "--queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/whatsapp-queue " +
          "--attributes '{\"VisibilityTimeout\":\"1\"}'");

      // 3. Configurar Redrive Policy: após 3 falhas, move para DLQ
      localStack.execInContainer(
          "sh", "-c",
          "awslocal sqs set-queue-attributes " +
          "--queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/whatsapp-queue " +
          "--attributes '{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"arn:aws:sqs:us-east-1:000000000000:whatsapp-dlq\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}'");
    } catch (Exception e) {
      throw new RuntimeException("Falha ao configurar filas SQS no LocalStack", e);
    }
  }

  // =================== Injeção de Dependências ===================

  @PersistenceContext private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private RedisTemplate<String, String> redisTemplate;
  @Autowired private PasswordEncoderPort passwordEncoder;
  @Autowired private UserRepositoryPort userRepository;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private RefreshTokenRepositoryPort refreshTokenRepository;
  @Autowired private ModalityRepositoryPort modalityRepository;
  @Autowired private CourtRepositoryPort courtRepository;
  @Autowired private OperatingHoursRepositoryPort operatingHoursRepository;
  @Autowired private PriceRuleRepositoryPort priceRuleRepository;
  @Autowired private ScheduleEntryRepositoryPort scheduleEntryRepository;
  @Autowired private BlockedTimeRepositoryPort blockedTimeRepository;

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

    // LocalStack (SQS)
    registry.add("spring.cloud.aws.region.static", localStack::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
    registry.add("spring.cloud.aws.sqs.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
  }

  @AfterEach
  void cleanupAfterEach() {
    // Limpa o cache de primeiro nível do Hibernate para evitar
    // referências a entidades que serão deletadas via JDBC
    entityManager.clear();

    // Limpeza eficiente: deleta tabelas filhas primeiro para evitar FK violations
    // Ordem correta baseada nas dependências de foreign keys
    JdbcTestUtils.deleteFromTables(
        jdbcTemplate,
        // Nível 1: Tabelas mais dependentes (subtabelas de schedule_entries)
        "tb_reservations",
        "tb_blocked_times",
        // Nível 2: Tabelas com FK para outras tabelas
        "tb_schedule_entries",
        "tb_refresh_token",
        "tb_court_modalities",
        "tb_operating_hours",
        "tb_price_rules",
        // Nível 3: Tabelas intermediárias
        "tb_courts",
        "tb_modalities",
        // Nível 4: Tabelas base
        "tb_users");

    // Limpa cache do Redis (OTP codes, rate limit, sessions, etc.)
    var connectionFactory = redisTemplate.getConnectionFactory();
    if (connectionFactory != null) {
      var connection = connectionFactory.getConnection();
      if (connection != null) {
        connection.serverCommands().flushAll();
        connection.close();
      }
    }
  }

  /**
   * Valida uma resposta de erro de validação (400 Bad Request) com fieldErrors. Útil para validar
   * erros de Bean Validation em DTOs de request.
   *
   * @param response A resposta de erro extraída do RestAssured
   * @param expectedPath O path esperado do endpoint
   * @param fieldName O nome do campo com erro
   * @param expectedErrorCode O ErrorCode esperado para o campo
   */
  public void assertValidationError(
          ErrorResponseDto response,
          String expectedPath,
          String fieldName,
          ErrorCode expectedErrorCode) {

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
    assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
    assertThat(response.path()).isEqualTo(expectedPath);
    assertThat(response.fieldErrors())
            .anyMatch(
                    fieldError ->
                            fieldError.fieldName().equals(fieldName)
                                    && fieldError.errorCode().equals(expectedErrorCode.name())
                                    && fieldError.developerMessage().equals(expectedErrorCode.getMessage()));
  }

  /**
   * Valida uma resposta de erro de negócio (404, 409, 403, etc.) sem fieldErrors.
   *
   * @param response A resposta de erro extraída do RestAssured
   * @param expectedStatus O status HTTP esperado (404, 409, etc.)
   * @param expectedPath O path esperado do endpoint
   * @param expectedErrorCode O ErrorCode esperado
   */
  public void assertBusinessError(
          ErrorResponseDto response,
          int expectedStatus,
          String expectedPath,
          ErrorCode expectedErrorCode) {

    assertThat(response.status()).isEqualTo(expectedStatus);
    assertThat(response.errorCode()).isEqualTo(expectedErrorCode.name());
    assertThat(response.developerMessage()).isEqualTo(expectedErrorCode.getMessage());
    assertThat(response.path()).isEqualTo(expectedPath);
  }

  /**
   * Normaliza um horário para ter minutos válidos (0 ou 30).
   * Essencial para simular reservas em andamento que sigam a regra de negócio.
   * Regras de arredondamento:
   * - 0-14 minutos → arredonda para baixo para 0
   * - 15-44 minutos → arredonda para 30
   * - 45-59 minutos → arredonda para hora seguinte com 0 minutos
   */
  public LocalTime normalizeToValidMinutes(LocalTime time) {
    int minutes = time.getMinute();

    if (minutes < 15) {
      // Arredonda para baixo: 14:05 → 14:00
      return time.withMinute(0).withSecond(0).withNano(0);
    } else if (minutes < 45) {
      // Arredonda para 30: 14:25 → 14:30
      return time.withMinute(30).withSecond(0).withNano(0);
    } else {
      // Arredonda para próxima hora: 14:50 → 15:00
      return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }
  }

  /**
   * Retorna a próxima data que corresponde ao dia da semana especificado.
   * Se hoje for o mesmo dia da semana, retorna a próxima semana.
   */
  public LocalDate nextDayOfWeek(java.time.DayOfWeek dayOfWeek) {
    return LocalDate.now().with(TemporalAdjusters.next(dayOfWeek));
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

  /**
   * Retorna o System User (ghost user) existente ou cria um novo caso não exista.
   * Necessário para testes que envolvam cleanup de contas desativadas (migração de reservas).
   *
   * @return O System User existente ou recém-criado
   */
  public User mockPersistSystemUser() {
    try {
      return userRepository.findSystemUserOrElseThrow();
    } catch (Exception e) {
      String passwordEncoded = passwordEncoder.encode(UUID.randomUUID().toString());
      User systemUser = User.createSystemUser(passwordEncoded);
      return userRepository.save(systemUser);
    }
  }

  public User mockPersistOtherAdminUser() {
    String passwordEncoded = passwordEncoder.encode(defaultPassword);
    Instant now = Instant.now();
    User otherAdminUser =
        User.reconstitute(
            UUID.randomUUID(),
            "outro_admin",
            "Outro Administrador",
            "+5511988887777",
            passwordEncoded,
            AccountStatus.ACTIVE,
            RoleEnum.ROLE_ADMIN,
            now,
            now);
    return userRepository.save(otherAdminUser);
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

  public void mockPersistListOfModalities() {
    modalityRepository.save(Modality.create("Beach Tennis"));
    modalityRepository.save(Modality.create("Futvolei"));
    modalityRepository.save(Modality.create("Volei de Praia"));
  }

  public Modality mockPersistModality(String name) {
    Modality modality = Modality.create(name);
    return modalityRepository.save(modality);
  }

  public Modality mockPersistDisableModality(String name) {
    Modality modality = Modality.create(name);
    modality.disable();
    return modalityRepository.save(modality);
  }

  public Court mockPersistCourt(String name, Modality modality) {
    String description = "Quadra de " + name;
    OffsetMinutes offset = OffsetMinutes.ZERO;

    Court court = Court.create(name, description, offset, Set.of(modality.getId()));
    courtRepository.save(court);
    return court;
  }

  public Court mockPersistCourt(
      String name, String description, OffsetMinutes offset, Set<Modality> modalities) {

    Set<UUID> modalitiesUUID = modalities.stream().map(Modality::getId).collect(Collectors.toSet());
    Court court = Court.create(name, description, offset, modalitiesUUID);
    return courtRepository.save(court);
  }

  public Court mockPersistCourt(
      String name,
      String description,
      OffsetMinutes offset,
      Set<Modality> modalities,
      boolean active) {

    Set<UUID> modalitiesUUID = modalities.stream().map(Modality::getId).collect(Collectors.toSet());
    Court court =
        Court.reconstitute(
            UUID.randomUUID(), name, description, offset, active, modalitiesUUID, Instant.now());
    return courtRepository.save(court);
  }

  public void mockPersistListOfOperatingHours() {
    // Horário de funcionamento para dias de semana
    TimeInterval timeIntervalMorning = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(12, 0));
    TimeInterval timeIntervalAfternoon = new TimeInterval(LocalTime.of(13, 30), LocalTime.of(0, 0));
    Set<DayOfWeek> daysOfWeeks =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
    OperatingHours operatingHours1 = OperatingHours.create(daysOfWeeks, timeIntervalMorning);
    OperatingHours operatingHours2 = OperatingHours.create(daysOfWeeks, timeIntervalAfternoon);

    operatingHoursRepository.save(operatingHours1);
    operatingHoursRepository.save(operatingHours2);

    // Horário de funcionamento para fins de semana
    TimeInterval timeIntervalAllDay = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> weekendDays = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    OperatingHours operatingHours3 = OperatingHours.create(weekendDays, timeIntervalAllDay);

    operatingHoursRepository.save(operatingHours3);

    // Horário de funcionamento inativo
    TimeInterval timeIntervalInactive = new TimeInterval(LocalTime.of(10, 0), LocalTime.of(14, 0));
    Set<DayOfWeek> inactiveDays = Set.of(DayOfWeek.WEDNESDAY);
    OperatingHours operatingHours4 = OperatingHours.create(inactiveDays, timeIntervalInactive);
    operatingHours4.disable();
    operatingHoursRepository.save(operatingHours4);
  }

  public OperatingHours mockPersistOperatingHoursFixedInterval(
      Set<DayOfWeek> daysOfWeeks, TimeInterval timeInterval) {
    OperatingHours operatingHours = OperatingHours.create(daysOfWeeks, timeInterval);
    return operatingHoursRepository.save(operatingHours);
  }

  public OperatingHours mockPersistOperatingHours() {
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> daysOfWeek =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
    OperatingHours operatingHours = OperatingHours.create(daysOfWeek, timeInterval);
    return operatingHoursRepository.save(operatingHours);
  }

  public OperatingHours mockPersistOperatingHoursAllDays() {
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> daysOfWeek =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY);
    OperatingHours operatingHours = OperatingHours.create(daysOfWeek, timeInterval);
    return operatingHoursRepository.save(operatingHours);
  }

  public OperatingHours mockPersistOperatingHoursAllDaysWithTimeInterval(TimeInterval timeInterval) {
    Set<DayOfWeek> daysOfWeek =
            Set.of(
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY,
                    DayOfWeek.SUNDAY);
    OperatingHours operatingHours = OperatingHours.create(daysOfWeek, timeInterval);
    return operatingHoursRepository.save(operatingHours);
  }

  public OperatingHours mockPersistDisabledOperatingHours() {
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> daysOfWeek =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
    OperatingHours operatingHours = OperatingHours.create(daysOfWeek, timeInterval);
    operatingHours.disable();
    return operatingHoursRepository.save(operatingHours);
  }

  public void mockPersistListOfPriceRules() {
    // A regra padrão já é criada automaticamente ao iniciar a aplicação

    // Horário Nobre - Dias de Semana
    String rule1Name = "Horário Nobre - Dias de Semana";
    BigDecimal rule1Price = BigDecimal.valueOf(85.00);
    int rule1Priority = 1;
    TimeInterval peakHours = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> weekdayDays =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
    var rule1 = PriceRule.create(rule1Name, weekdayDays, peakHours, rule1Price, rule1Priority);
    priceRuleRepository.save(rule1);

    // Horário Especial - Fins de Semana
    String rule2Name = "Horário Especial - Fins de Semana";
    BigDecimal rule2Price = BigDecimal.valueOf(85.00);
    int rule2Priority = 1;
    TimeInterval specialHours = new TimeInterval(LocalTime.of(8, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> weekendDays = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    var rule2 = PriceRule.create(rule2Name, weekendDays, specialHours, rule2Price, rule2Priority);
    priceRuleRepository.save(rule2);

    // Regra Inativa
    String rule3Name = "Regra Inativa";
    BigDecimal rule3Price = BigDecimal.valueOf(70.00);
    int rule3Priority = 2;
    TimeInterval inactiveHours = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(16, 0));
    Set<DayOfWeek> inactiveDays = Set.of(DayOfWeek.TUESDAY);
    var rule3 = PriceRule.create(rule3Name, inactiveDays, inactiveHours, rule3Price, rule3Priority);
    rule3.disable();
    priceRuleRepository.save(rule3);
  }

  public PriceRule mockPersistPriceRule() {
    String ruleName = "Regra de Preço Teste";
    BigDecimal rulePrice = new BigDecimal("85.00");
    int rulePriority = 1;
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(19, 0), LocalTime.of(0, 0));
    Set<DayOfWeek> daysOfWeek =
        Set.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY);
    PriceRule priceRule =
        PriceRule.create(ruleName, daysOfWeek, timeInterval, rulePrice, rulePriority);
    return priceRuleRepository.save(priceRule);
  }

  public PriceRule mockPersistDisabledPriceRule() {
    String ruleName = "Regra de Preço Inativa Teste";
    BigDecimal rulePrice = new BigDecimal("40.00");
    int rulePriority = 2;
    TimeInterval timeInterval = new TimeInterval(LocalTime.of(14, 0), LocalTime.of(16, 0));
    Set<DayOfWeek> daysOfWeek = Set.of(DayOfWeek.SATURDAY);
    PriceRule priceRule =
        PriceRule.create(ruleName, daysOfWeek, timeInterval, rulePrice, rulePriority);
    priceRule.disable();
    return priceRuleRepository.save(priceRule);
  }

  public PriceRule mockPersistDefaultPriceRule() {
    PriceRule priceRule = PriceRule.createDefault();
    return priceRuleRepository.save(priceRule);
  }

  public Reservation mockPersistReservationByUser(
      UUID modalityId,
      UUID courtId,
      LocalDate date,
      TimeInterval timeInterval,
      BigDecimal price,
      UUID userId) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    Reservation reservation =
        Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
    return (Reservation) scheduleEntryRepository.save(reservation);
  }

  public Reservation mockPersistReservationByUserWithStatus(
      UUID modalityId,
      UUID courtId,
      LocalDate date,
      TimeInterval timeInterval,
      BigDecimal price,
      UUID userId,
      ReservationStatus status) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    Reservation reservation = Reservation.reconstitute(
        UUID.randomUUID(),
        courtId,
        modalityId,
        userId,
        null,
        null,
        price,
        dateTimeSlot,
        status,
        null,
        Instant.now());
    return (Reservation) scheduleEntryRepository.save(reservation);
  }

  public Reservation mockPersistRecurringReservation(
      UUID modalityId,
      UUID courtId,
      LocalDate date,
      TimeInterval timeInterval,
      BigDecimal price,
      UUID userId,
      UUID adminId,
      UUID recurringReservationId) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    Reservation reservation =
        Reservation.createRecurring(modalityId, courtId, userId, adminId, price, dateTimeSlot, recurringReservationId);
    return (Reservation) scheduleEntryRepository.save(reservation);
  }

  // =================== Blocked Time Helpers ===================
  public BlockedTime mockPersistBlockedTimeSpecific(
      UUID courtId, LocalDate date, TimeInterval timeInterval, String reason, UUID adminId) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    BlockedTime blockedTime = BlockedTime.createSpecificTime(courtId, dateTimeSlot, reason, adminId);
    return blockedTimeRepository.save(blockedTime);
  }

  public BlockedTime mockPersistBlockedTimeRecurring(
          UUID courtId, LocalDate date, TimeInterval timeInterval, String reason, UUID adminId, UUID recurringId
  ) {
    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    BlockedTime blockedTime = BlockedTime.createRecurring(courtId, dateTimeSlot, reason, adminId, true, recurringId);
    return blockedTimeRepository.save(blockedTime);
  }
}
