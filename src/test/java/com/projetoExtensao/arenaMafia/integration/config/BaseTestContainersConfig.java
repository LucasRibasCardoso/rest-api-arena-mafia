package com.projetoExtensao.arenaMafia.integration.config;

import static io.restassured.RestAssured.given;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.application.court.port.CourtRepositoryPort;
import com.projetoExtensao.arenaMafia.application.modality.port.ModalityRepositoryPort;
import com.projetoExtensao.arenaMafia.application.operatingHours.port.OperatingHoursRepositoryPort;
import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.*;
import com.projetoExtensao.arenaMafia.domain.model.enums.AccountStatus;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.model.enums.OffsetMinutes;
import com.projetoExtensao.arenaMafia.domain.model.enums.RoleEnum;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.schedule.ScheduleEntry;
import com.projetoExtensao.arenaMafia.domain.valueobjects.DateTimeSlot;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.UserJpaRepository;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.request.LoginRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.auth.dto.response.AuthResponseDto;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  @Autowired private ModalityRepositoryPort modalityRepository;
  @Autowired private CourtRepositoryPort courtRepository;
  @Autowired private OperatingHoursRepositoryPort operatingHoursRepository;
  @Autowired private PriceRuleRepositoryPort priceRuleRepository;
  @Autowired private ScheduleEntryRepositoryPort scheduleEntryRepository;

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
    JdbcTestUtils.deleteFromTables(
        jdbcTemplate,
        "tb_refresh_token",
        "tb_court_modalities",
        "tb_operating_hours",
        "tb_price_rules",
        "tb_courts",
        "tb_modalities",
        "tb_users");
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

  public OperatingHours mockPersistOperatingHoursFixedInterval(Set<DayOfWeek> daysOfWeeks, TimeInterval timeInterval) {
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
    PriceRule priceRule = PriceRule.createDefault(BigDecimal.valueOf(50));
    return priceRuleRepository.save(priceRule);
  }

  public ScheduleEntry mockPersistReservationByUser(
      UUID modalityId,
      UUID courtId,
      LocalDate date,
      TimeInterval timeInterval,
      BigDecimal price,
      UUID userId) {

    DateTimeSlot dateTimeSlot = new DateTimeSlot(date, timeInterval);
    Reservation reservation =
        Reservation.createByUser(modalityId, courtId, userId, price, dateTimeSlot);
    return scheduleEntryRepository.save(reservation);
  }
}
