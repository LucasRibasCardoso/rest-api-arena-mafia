# Plano de Implementação: Criar BlockedTime com Cache de Preview

## Contexto

Implementar o endpoint de criação de `BlockedTime` que utiliza cache para garantir segurança e consistência entre o preview de conflitos e a confirmação da criação.

## Problema a Resolver

Após o usuário visualizar o preview dos conflitos (reservas e blocked times que serão afetados), ele precisa confirmar a criação do novo `BlockedTime`. Para evitar manipulação de dados pelo frontend e garantir consistência, os dados do preview devem ser armazenados em cache.

## Objetivos

1. ✅ Salvar resultado do preview em cache com TTL curto
2. ✅ Criar endpoint de confirmação que usa a chave do cache
3. ✅ Revalidar conflitos antes de confirmar
4. ✅ Cancelar reservas conflitantes de forma atômica
5. ✅ Criar o novo BlockedTime
6. ✅ Garantir segurança (usuário não pode usar preview de outro usuário)

## Vantagens da Abordagem

### Segurança
- Evita que o frontend envie IDs manipulados de reservas
- Garante que apenas o usuário que gerou o preview pode confirmar
- Previne ataques de modificação de dados

### Consistência
- Garante que os dados do preview sejam os mesmos da confirmação
- Revalida antes de confirmar para detectar mudanças concorrentes
- Operação atômica (transação) para cancelar + criar

### UX (User Experience)
- Frontend só precisa enviar a chave do cache na confirmação
- Dados do preview já estão armazenados
- Processo simplificado de confirmação

### Performance
- Evita reprocessamento completo dos conflitos
- Cache Redis é rápido
- Queries otimizadas

## Desvantagens e Mitigações

### Expiração do Cache
**Problema**: E se o usuário demorar para confirmar?
**Mitigação**: TTL de 5 minutos + mensagem clara para gerar novo preview

### Concorrência
**Problema**: E se outra reserva for criada entre preview e confirmação?
**Mitigação**: Revalidar conflitos antes de confirmar + retornar erro se divergir

### Complexidade
**Problema**: Adiciona dependência do Redis
**Mitigação**: Redis já está sendo usado no projeto (refresh tokens)

## Arquitetura

### Camadas Envolvidas

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
│  ┌────────────────┐  ┌─────────────────────────────────┐   │
│  │   Controller   │  │    Redis Cache Adapter           │   │
│  │  (REST API)    │  │  (BlockedTimePreviewCachePort)   │   │
│  └────────────────┘  └─────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  PreviewBlockedTimeConflictsUseCase (modificado)     │  │
│  │  - Gera preview                                       │  │
│  │  - Salva no cache com chave única                    │  │
│  │  - Retorna chave para o frontend                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  CreateBlockedTimeUseCase (novo)                      │  │
│  │  - Busca preview do cache                            │  │
│  │  - Revalida conflitos                                │  │
│  │  - Cancela reservas conflitantes                     │  │
│  │  - Cria BlockedTime (com recorrência)                │  │
│  │  - Limpa cache                                       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  CancelReservationUseCase (reutilizar existente)     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│  ┌──────────────────┐  ┌─────────────────────────────┐     │
│  │   BlockedTime    │  │      Reservation            │     │
│  │   (Entity)       │  │      (Entity)               │     │
│  └──────────────────┘  └─────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Fluxo Completo

### 1. Preview de Conflitos

```
Frontend                Backend                     Cache
   │                       │                          │
   │  POST /preview        │                          │
   ├──────────────────────>│                          │
   │                       │                          │
   │                       │ 1. Calcula conflitos     │
   │                       │                          │
   │                       │ 2. Gera chave única      │
   │                       │  "preview:{userId}:{uuid}"│
   │                       │                          │
   │                       │  3. Salva no cache       │
   │                       ├─────────────────────────>│
   │                       │     SET key, data, TTL=5m│
   │                       │                          │
   │  200 OK               │                          │
   │  { previewKey,        │                          │
   │    conflicts }        │                          │
   │<──────────────────────┤                          │
   │                       │                          │
```

### 2. Confirmação da Criação

```
Frontend                Backend                     Cache         Database
   │                       │                          │               │
   │  POST /blocked-times  │                          │               │
   │  { previewKey, ... }  │                          │               │
   ├──────────────────────>│                          │               │
   │                       │                          │               │
   │                       │ 1. Busca preview         │               │
   │                       ├─────────────────────────>│               │
   │                       │     GET key              │               │
   │                       │<─────────────────────────┤               │
   │                       │     data                 │               │
   │                       │                          │               │
   │                       │ 2. Valida userId         │               │
   │                       │    na chave              │               │
   │                       │                          │               │
   │                       │ 3. Revalida conflitos    │               │
   │                       │                          │               │
   │                       │ 4. Compara com cache     │               │
   │                       │                          │               │
   │                       │ 5. BEGIN TRANSACTION     │               │
   │                       │                          │               │
   │                       │ 6. Cancela reservas      │               │
   │                       ├─────────────────────────────────────────>│
   │                       │    UPDATE reservations   │               │
   │                       │    SET status='CANCELLED'│               │
   │                       │                          │               │
   │                       │ 7. Cria BlockedTime      │               │
   │                       ├─────────────────────────────────────────>│
   │                       │    INSERT blocked_times  │               │
   │                       │                          │               │
   │                       │ 8. COMMIT                │               │
   │                       │                          │               │
   │                       │ 9. Limpa cache           │               │
   │                       ├─────────────────────────>│               │
   │                       │     DEL key              │               │
   │                       │                          │               │
   │  201 Created          │                          │               │
   │  { blockedTime }      │                          │               │
   │<──────────────────────┤                          │               │
```

## Estrutura de Arquivos

### 1. Domain Layer

#### DTOs (já existem)
- `domain/dto/BlockedTimeDetail.java` ✅
- `domain/dto/ReservationDetail.java` ✅
- `domain/dto/ScheduleDetail.java` ✅

#### Entidades
- `domain/model/schedule/BlockedTime.java` (ajustar se necessário)
- `domain/model/schedule/Reservation.java` ✅

#### Value Objects
- `domain/valueobjects/RecurrencePattern.java` (criar se não existir)
- `domain/valueobjects/TimeInterval.java` ✅

#### Exceções
- `domain/exception/badRequest/PreviewExpiredException.java` (novo)
- `domain/exception/conflict/ConflictsChangedException.java` (novo)

#### ErrorCodes
```java
// domain/exception/ErrorCode.java (adicionar)
BLOCKED_TIME_PREVIEW_EXPIRED("O preview de bloqueio expirou. Gere um novo preview."),
BLOCKED_TIME_CONFLICTS_CHANGED("Os conflitos mudaram desde o preview. Revise as alterações."),
BLOCKED_TIME_PREVIEW_KEY_REQUIRED("A chave do preview é obrigatória."),
BLOCKED_TIME_PREVIEW_KEY_INVALID("A chave do preview é inválida."),
BLOCKED_TIME_PREVIEW_NOT_FOUND("Preview não encontrado. Pode ter expirado."),
```

### 2. Application Layer

#### DTOs
- `application/schedule/dto/BlockedTimeConflictsPreview.java` ✅
- `application/schedule/dto/CreateBlockedTimeCommand.java` (novo)

#### Ports
```java
// application/schedule/port/cache/BlockedTimePreviewCachePort.java (novo)
public interface BlockedTimePreviewCachePort {
    void save(String key, BlockedTimeConflictsPreview preview, Duration ttl);
    Optional<BlockedTimeConflictsPreview> find(String key);
    void delete(String key);
    String generateKey(UUID userId);
}
```

```java
// application/schedule/port/repository/BlockedTimeRepositoryPort.java (novo)
public interface BlockedTimeRepositoryPort {
    BlockedTime save(BlockedTime blockedTime);
    Optional<BlockedTime> findById(UUID id);
    List<BlockedTime> findByRecurringBlockedTimeId(UUID recurringId);
    void delete(UUID id);
}
```

#### Use Cases

**PreviewBlockedTimeConflictsUseCase** (modificar)
```java
// application/schedule/usecase/blockedtime/PreviewBlockedTimeConflictsUseCase.java
public interface PreviewBlockedTimeConflictsUseCase {
    // Retorno modificado para incluir a chave do cache
    BlockedTimePreviewResult execute(BlockedTimeConflictsPreviewRequestDto request);
}
```

**CreateBlockedTimeUseCase** (novo)
```java
// application/schedule/usecase/blockedtime/CreateBlockedTimeUseCase.java
public interface CreateBlockedTimeUseCase {
    BlockedTime execute(CreateBlockedTimeRequestDto request);
}
```

**CancelReservationUseCase** (reutilizar existente)
```java
// application/schedule/usecase/reservation/CancelReservationUseCase.java
// Já deve existir, apenas reutilizar
```

#### Services
- `application/schedule/service/ScheduleEntryEnrichmentService.java` ✅
- `application/schedule/service/BlockedTimeRecurrenceService.java` (novo - para criar recorrências)

### 3. Infrastructure Layer

#### Adapters - Cache
```java
// infrastructure/adapter/cache/BlockedTimePreviewCacheAdapter.java (novo)
@Component
public class BlockedTimePreviewCacheAdapter implements BlockedTimePreviewCachePort {
    private final RedisTemplate<String, BlockedTimeConflictsPreview> redisTemplate;
    
    @Override
    public void save(String key, BlockedTimeConflictsPreview preview, Duration ttl) {
        redisTemplate.opsForValue().set(key, preview, ttl);
    }
    
    @Override
    public Optional<BlockedTimeConflictsPreview> find(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
    
    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    
    @Override
    public String generateKey(UUID userId) {
        return String.format("blocked-time-preview:%s:%s", userId, UUID.randomUUID());
    }
}
```

#### Adapters - Persistence
```java
// infrastructure/persistence/adapter/BlockedTimeRepositoryAdapter.java (novo)
@Component
public class BlockedTimeRepositoryAdapter implements BlockedTimeRepositoryPort {
    private final BlockedTimeJpaRepository jpaRepository;
    private final BlockedTimeMapper mapper;
    
    @Override
    public BlockedTime save(BlockedTime blockedTime) {
        BlockedTimeJpaEntity entity = mapper.toEntity(blockedTime);
        BlockedTimeJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    // ... outros métodos
}
```

#### Controllers
```java
// infrastructure/web/admin/AdminBlockedTimesController.java (modificar)
@RestController
@RequestMapping("/api/admin/blocked-times")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlockedTimesController {
    
    private final PreviewBlockedTimeConflictsUseCase previewUseCase;
    private final CreateBlockedTimeUseCase createUseCase;
    
    @PostMapping("/preview-conflicts")
    public ResponseEntity<BlockedTimeConflictsPreviewResponseDto> previewConflicts(
        @RequestBody @Valid BlockedTimeConflictsPreviewRequestDto request) {
        
        BlockedTimePreviewResult result = previewUseCase.execute(request);
        BlockedTimeConflictsPreviewResponseDto response = toResponseDto(result);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<BlockedTimeResponseDto> createBlockedTime(
        @RequestBody @Valid CreateBlockedTimeRequestDto request,
        @AuthenticationPrincipal UserDetails userDetails) {
        
        BlockedTime blockedTime = createUseCase.execute(request);
        BlockedTimeResponseDto response = mapper.toDto(blockedTime);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

#### DTOs - Request
```java
// infrastructure/web/admin/dto/blockedtime/request/CreateBlockedTimeRequestDto.java (novo)
public record CreateBlockedTimeRequestDto(
    @NotBlank(message = "BLOCKED_TIME_PREVIEW_KEY_REQUIRED")
    String previewKey,
    
    @NotNull(message = "BLOCKED_TIME_COURT_IDS_REQUIRED")
    @Size(min = 1, max = 20, message = "BLOCKED_TIME_COURT_IDS_SIZE_INVALID")
    List<UUID> courtIds,
    
    @NotNull(message = "BLOCKED_TIME_START_DATE_REQUIRED")
    @FutureOrPresent(message = "BLOCKED_TIME_START_DATE_MUST_BE_FUTURE_OR_PRESENT")
    LocalDate startDate,
    
    @NotNull(message = "BLOCKED_TIME_END_DATE_REQUIRED")
    @FutureOrPresent(message = "BLOCKED_TIME_END_DATE_MUST_BE_FUTURE_OR_PRESENT")
    LocalDate endDate,
    
    @Valid
    TimeIntervalDto timeInterval, // Pode ser null se isFullDay = true
    
    @NotNull(message = "BLOCKED_TIME_IS_FULL_DAY_REQUIRED")
    Boolean isFullDay,
    
    @Size(max = 500, message = "BLOCKED_TIME_DESCRIPTION_TOO_LONG")
    String description,
    
    @Valid
    RecurrenceDto recurrence // Pode ser null se não houver recorrência
) {
    public CreateBlockedTimeRequestDto {
        // Validação customizada
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidBlockDateException(ErrorCode.BLOCKED_TIME_START_DATE_AFTER_END_DATE);
        }
        
        if (!isFullDay && timeInterval == null) {
            throw new InvalidBlockDateException(ErrorCode.BLOCKED_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY);
        }
    }
}
```

```java
// infrastructure/web/admin/dto/blockedtime/request/RecurrenceDto.java (novo)
public record RecurrenceDto(
    @NotNull(message = "RECURRENCE_TYPE_REQUIRED")
    RecurrenceType type, // DAILY, WEEKLY, MONTHLY
    
    Integer interval, // Ex: a cada 2 semanas (interval = 2)
    
    List<DayOfWeek> daysOfWeek, // Para recorrência semanal
    
    LocalDate endDate // Até quando a recorrência deve se repetir
) {}
```

#### DTOs - Response
```java
// infrastructure/web/admin/dto/blockedtime/response/BlockedTimeConflictsPreviewResponseDto.java (modificar)
public record BlockedTimeConflictsPreviewResponseDto(
    @NotBlank
    String previewKey, // NOVO: chave para usar na confirmação
    
    int usersAffected,
    int blockedTimesAffected,
    int reservationsAffected,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> conflictingReservations
) {}
```

```java
// infrastructure/web/admin/dto/blockedtime/response/BlockedTimeResponseDto.java (novo)
public record BlockedTimeResponseDto(
    UUID id,
    UUID courtId,
    String courtName,
    LocalDate date,
    TimeIntervalDto timeInterval,
    String description,
    boolean isFullDay,
    UUID recurringBlockedTimeId,
    LocalDateTime createdAt
) {}
```

#### Configuration
```java
// infrastructure/config/RedisConfig.java (modificar - adicionar bean para cache de preview)
@Configuration
public class RedisConfig {
    
    // ...existing beans...
    
    @Bean
    public RedisTemplate<String, BlockedTimeConflictsPreview> blockedTimePreviewRedisTemplate(
        RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, BlockedTimeConflictsPreview> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

## Implementação Detalhada

### Passo 1: Criar Exceções de Domínio

```java
// domain/exception/badRequest/PreviewExpiredException.java
package com.projetoExtensao.arenaMafia.domain.exception.badRequest;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class PreviewExpiredException extends BadRequestException {
    public PreviewExpiredException() {
        super(ErrorCode.BLOCKED_TIME_PREVIEW_EXPIRED);
    }
}
```

```java
// domain/exception/conflict/ConflictsChangedException.java
package com.projetoExtensao.arenaMafia.domain.exception.conflict;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;

public class ConflictsChangedException extends ConflictException {
    public ConflictsChangedException() {
        super(ErrorCode.BLOCKED_TIME_CONFLICTS_CHANGED);
    }
}
```

### Passo 2: Adicionar ErrorCodes

```java
// domain/exception/ErrorCode.java (adicionar)
// Preview de BlockedTime
BLOCKED_TIME_PREVIEW_EXPIRED("O preview de bloqueio expirou. Gere um novo preview.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_CONFLICTS_CHANGED("Os conflitos mudaram desde o preview. Revise as alterações antes de confirmar.", HttpStatus.CONFLICT),
BLOCKED_TIME_PREVIEW_KEY_REQUIRED("A chave do preview é obrigatória.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_PREVIEW_KEY_INVALID("A chave do preview é inválida ou foi manipulada.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_PREVIEW_NOT_FOUND("Preview não encontrado. Pode ter expirado após 5 minutos.", HttpStatus.NOT_FOUND),

// Validações de BlockedTime
BLOCKED_TIME_COURT_IDS_REQUIRED("Pelo menos uma quadra deve ser selecionada.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_COURT_IDS_SIZE_INVALID("Você pode selecionar entre 1 e 20 quadras.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_START_DATE_REQUIRED("A data de início é obrigatória.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_END_DATE_REQUIRED("A data de fim é obrigatória.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_START_DATE_AFTER_END_DATE("A data de início não pode ser posterior à data de fim.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_START_DATE_MUST_BE_FUTURE_OR_PRESENT("A data de início deve ser hoje ou no futuro.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_END_DATE_MUST_BE_FUTURE_OR_PRESENT("A data de fim deve ser hoje ou no futuro.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_INTERVAL_REQUIRED_WHEN_NOT_FULL_DAY("O intervalo de tempo é obrigatório quando não for dia inteiro.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_IS_FULL_DAY_REQUIRED("Informe se o bloqueio é para o dia inteiro.", HttpStatus.BAD_REQUEST),
BLOCKED_TIME_DESCRIPTION_TOO_LONG("A descrição não pode ter mais de 500 caracteres.", HttpStatus.BAD_REQUEST),

// Recorrência
RECURRENCE_TYPE_REQUIRED("O tipo de recorrência é obrigatório.", HttpStatus.BAD_REQUEST),
```

### Passo 3: Criar Port do Cache

```java
// application/schedule/port/cache/BlockedTimePreviewCachePort.java
package com.projetoExtensao.arenaMafia.application.schedule.port.cache;

import com.projetoExtensao.arenaMafia.application.schedule.dto.BlockedTimeConflictsPreview;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface BlockedTimePreviewCachePort {
    
    /**
     * Salva o preview de conflitos no cache.
     * 
     * @param key chave única do preview
     * @param preview dados do preview
     * @param ttl tempo de vida no cache
     */
    void save(String key, BlockedTimeConflictsPreview preview, Duration ttl);
    
    /**
     * Busca um preview no cache.
     * 
     * @param key chave do preview
     * @return preview se encontrado
     */
    Optional<BlockedTimeConflictsPreview> find(String key);
    
    /**
     * Remove um preview do cache.
     * 
     * @param key chave do preview
     */
    void delete(String key);
    
    /**
     * Gera uma chave única para o preview.
     * 
     * @param userId ID do usuário (para segurança)
     * @return chave única
     */
    String generateKey(UUID userId);
    
    /**
     * Valida se a chave pertence ao usuário.
     * 
     * @param key chave do preview
     * @param userId ID do usuário
     * @return true se a chave pertence ao usuário
     */
    boolean validateKeyOwnership(String key, UUID userId);
}
```

### Passo 4: Modificar PreviewBlockedTimeConflictsUseCase

```java
// application/schedule/dto/BlockedTimePreviewResult.java (novo)
package com.projetoExtensao.arenaMafia.application.schedule.dto;

public record BlockedTimePreviewResult(
    String previewKey,
    BlockedTimeConflictsPreview conflicts
) {}
```

```java
// application/schedule/usecase/blockedtime/PreviewBlockedTimeConflictsUseCase.java (modificar)
package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.application.schedule.dto.BlockedTimePreviewResult;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.BlockedTimeConflictsPreviewRequestDto;
import java.util.UUID;

public interface PreviewBlockedTimeConflictsUseCase {
    /**
     * Gera um preview dos conflitos e salva no cache.
     * 
     * @param request dados para gerar o preview
     * @param userId ID do usuário admin que está gerando o preview
     * @return resultado contendo a chave do cache e os conflitos
     */
    BlockedTimePreviewResult execute(BlockedTimeConflictsPreviewRequestDto request, UUID userId);
}
```

```java
// application/schedule/usecase/blockedtime/imp/PreviewBlockedTimeConflictsUseCaseImp.java (modificar)
@Service
@Transactional(readOnly = true)
public class PreviewBlockedTimeConflictsUseCaseImp implements PreviewBlockedTimeConflictsUseCase {

  private final ScheduleEntryRepositoryPort scheduleEntryRepository;
  private final ScheduleEntryEnrichmentService enrichmentService;
  private final OperatingHoursRepositoryPort operatingHourRepository;
  private final BlockedTimePreviewCachePort cachePort; // NOVO

  public PreviewBlockedTimeConflictsUseCaseImp(
      ScheduleEntryRepositoryPort scheduleEntryRepository,
      ScheduleEntryEnrichmentService enrichmentService,
      OperatingHoursRepositoryPort operatingHourRepository,
      BlockedTimePreviewCachePort cachePort) { // NOVO
    this.scheduleEntryRepository = scheduleEntryRepository;
    this.operatingHourRepository = operatingHourRepository;
    this.enrichmentService = enrichmentService;
    this.cachePort = cachePort; // NOVO
  }

  @Override
  public BlockedTimePreviewResult execute(
      BlockedTimeConflictsPreviewRequestDto request, 
      UUID userId) { // MODIFICADO

    TimeInterval searchInterval = calculateOperatingInterval(request);

    List<ScheduleEntry> conflicts = scheduleEntryRepository.findConflicts(
        request.courtIds(),
        request.startDate(),
        request.endDate(),
        searchInterval);

    BlockedTimeConflictsPreview preview = conflicts.isEmpty() 
        ? new BlockedTimeConflictsPreview(0, 0, 0, List.of(), List.of())
        : enrichmentConflicts(conflicts);
    
    // NOVO: Salvar no cache
    String previewKey = cachePort.generateKey(userId);
    cachePort.save(previewKey, preview, Duration.ofMinutes(5));
    
    return new BlockedTimePreviewResult(previewKey, preview);
  }
  
  // ...existing methods...
}
```

### Passo 5: Criar CreateBlockedTimeUseCase

```java
// application/schedule/usecase/blockedtime/CreateBlockedTimeUseCase.java
package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime;

import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.CreateBlockedTimeRequestDto;
import java.util.UUID;

public interface CreateBlockedTimeUseCase {
    /**
     * Cria um novo bloqueio de tempo, cancelando reservas conflitantes.
     * Valida o preview antes de confirmar a operação.
     * 
     * @param request dados para criar o bloqueio
     * @param userId ID do usuário admin que está criando
     * @return bloqueio criado
     */
    BlockedTime execute(CreateBlockedTimeRequestDto request, UUID userId);
}
```

```java
// application/schedule/usecase/blockedtime/imp/CreateBlockedTimeUseCaseImp.java
package com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.imp;

import com.projetoExtensao.arenaMafia.application.schedule.dto.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.port.cache.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.BlockedTimeRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ReservationRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.port.repository.ScheduleEntryRepositoryPort;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.CreateBlockedTimeUseCase;
import com.projetoExtensao.arenaMafia.application.schedule.usecase.blockedtime.PreviewBlockedTimeConflictsUseCase;
import com.projetoExtensao.arenaMafia.domain.dto.ReservationDetail;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.PreviewExpiredException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ConflictsChangedException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.InvalidPreviewKeyException;
import com.projetoExtensao.arenaMafia.domain.model.schedule.BlockedTime;
import com.projetoExtensao.arenaMafia.domain.model.schedule.Reservation;
import com.projetoExtensao.arenaMafia.domain.model.enums.ReservationStatus;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.blockedtime.request.CreateBlockedTimeRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreateBlockedTimeUseCaseImp implements CreateBlockedTimeUseCase {

    private final BlockedTimePreviewCachePort cachePort;
    private final BlockedTimeRepositoryPort blockedTimeRepository;
    private final ReservationRepositoryPort reservationRepository;
    private final ScheduleEntryRepositoryPort scheduleEntryRepository;

    public CreateBlockedTimeUseCaseImp(
            BlockedTimePreviewCachePort cachePort,
            BlockedTimeRepositoryPort blockedTimeRepository,
            ReservationRepositoryPort reservationRepository,
            ScheduleEntryRepositoryPort scheduleEntryRepository) {
        this.cachePort = cachePort;
        this.blockedTimeRepository = blockedTimeRepository;
        this.reservationRepository = reservationRepository;
        this.scheduleEntryRepository = scheduleEntryRepository;
    }

    @Override
    @Transactional
    public BlockedTime execute(CreateBlockedTimeRequestDto request, UUID userId) {
        
        // 1. Validar ownership da chave do preview
        if (!cachePort.validateKeyOwnership(request.previewKey(), userId)) {
            throw new InvalidPreviewKeyException();
        }
        
        // 2. Buscar preview do cache
        BlockedTimeConflictsPreview cachedPreview = cachePort
            .find(request.previewKey())
            .orElseThrow(PreviewExpiredException::new);
        
        // 3. Revalidar conflitos (verificar se algo mudou)
        List<ScheduleEntry> currentConflicts = scheduleEntryRepository.findConflicts(
            request.courtIds(),
            request.startDate(),
            request.endDate(),
            request.isFullDay() ? calculateFullDayInterval(request) : request.timeInterval()
        );
        
        // 4. Comparar conflitos
        if (!conflictsMatch(cachedPreview, currentConflicts)) {
            throw new ConflictsChangedException();
        }
        
        // 5. Cancelar todas as reservas conflitantes
        List<UUID> reservationIds = cachedPreview.conflictingReservations()
            .stream()
            .map(ReservationDetail::reservationId)
            .toList();
        
        cancelReservations(reservationIds);
        
        // 6. Criar o BlockedTime
        BlockedTime blockedTime = buildBlockedTime(request);
        BlockedTime savedBlockedTime = blockedTimeRepository.save(blockedTime);
        
        // 7. Se houver recorrência, criar os bloqueios recorrentes
        if (request.recurrence() != null) {
            createRecurringBlockedTimes(request, savedBlockedTime.getId());
        }
        
        // 8. Limpar cache
        cachePort.delete(request.previewKey());
        
        return savedBlockedTime;
    }
    
    /**
     * Verifica se os conflitos atuais são os mesmos do preview.
     */
    private boolean conflictsMatch(
            BlockedTimeConflictsPreview cached, 
            List<ScheduleEntry> current) {
        
        // Extrair IDs das reservas do cache
        List<UUID> cachedReservationIds = cached.conflictingReservations()
            .stream()
            .map(ReservationDetail::reservationId)
            .sorted()
            .toList();
        
        // Extrair IDs das reservas atuais
        List<UUID> currentReservationIds = current.stream()
            .filter(entry -> entry instanceof Reservation)
            .map(ScheduleEntry::getId)
            .sorted()
            .toList();
        
        return cachedReservationIds.equals(currentReservationIds);
    }
    
    /**
     * Cancela todas as reservas conflitantes.
     */
    private void cancelReservations(List<UUID> reservationIds) {
        reservationIds.forEach(id -> {
            Reservation reservation = reservationRepository.findByIdOrElseThrow(id);
            reservation.cancel(); // Método de domínio que muda status para CANCELLED
            reservationRepository.save(reservation);
        });
    }
    
    /**
     * Constrói a entidade BlockedTime a partir do request.
     */
    private BlockedTime buildBlockedTime(CreateBlockedTimeRequestDto request) {
        // Implementar construção do BlockedTime
        // Usar factory method da entidade de domínio
        return BlockedTime.create(
            request.courtIds().get(0), // Por enquanto apenas 1 quadra
            request.startDate(),
            request.timeInterval(),
            request.description(),
            request.isFullDay(),
            null // recurringBlockedTimeId será definido depois
        );
    }
    
    /**
     * Cria os bloqueios recorrentes se houver recorrência.
     */
    private void createRecurringBlockedTimes(
            CreateBlockedTimeRequestDto request, 
            UUID parentId) {
        // Implementar lógica de recorrência
        // Por enquanto, deixar como TODO
    }
    
    /**
     * Calcula o intervalo de tempo para bloqueio de dia inteiro.
     */
    private TimeInterval calculateFullDayInterval(CreateBlockedTimeRequestDto request) {
        // Buscar horário de funcionamento e retornar
        // Por enquanto, retornar um intervalo padrão
        return new TimeInterval(LocalTime.of(0, 0), LocalTime.of(23, 59));
    }
}
```

### Passo 6: Implementar Adapter do Cache

```java
// infrastructure/adapter/cache/BlockedTimePreviewCacheAdapter.java
package com.projetoExtensao.arenaMafia.infrastructure.adapter.cache;

import com.projetoExtensao.arenaMafia.application.schedule.dto.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.application.schedule.port.cache.BlockedTimePreviewCachePort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class BlockedTimePreviewCacheAdapter implements BlockedTimePreviewCachePort {

    private static final String KEY_PREFIX = "blocked-time-preview";
    private static final String KEY_SEPARATOR = ":";
    
    private final RedisTemplate<String, BlockedTimeConflictsPreview> redisTemplate;

    public BlockedTimePreviewCacheAdapter(
            RedisTemplate<String, BlockedTimeConflictsPreview> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String key, BlockedTimeConflictsPreview preview, Duration ttl) {
        redisTemplate.opsForValue().set(key, preview, ttl);
    }

    @Override
    public Optional<BlockedTimeConflictsPreview> find(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public String generateKey(UUID userId) {
        return String.format(
            "%s%s%s%s%s",
            KEY_PREFIX,
            KEY_SEPARATOR,
            userId,
            KEY_SEPARATOR,
            UUID.randomUUID()
        );
    }

    @Override
    public boolean validateKeyOwnership(String key, UUID userId) {
        if (key == null || !key.startsWith(KEY_PREFIX)) {
            return false;
        }
        
        String expectedPrefix = String.format(
            "%s%s%s%s",
            KEY_PREFIX,
            KEY_SEPARATOR,
            userId,
            KEY_SEPARATOR
        );
        
        return key.startsWith(expectedPrefix);
    }
}
```

### Passo 7: Atualizar Controller

```java
// infrastructure/web/admin/AdminBlockedTimesController.java (modificar)
@RestController
@RequestMapping("/api/admin/blocked-times")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminBlockedTimesController {

  private final PreviewBlockedTimeConflictsUseCase previewUseCase;
  private final CreateBlockedTimeUseCase createUseCase;

  public AdminBlockedTimesController(
      PreviewBlockedTimeConflictsUseCase previewUseCase,
      CreateBlockedTimeUseCase createUseCase) {
    this.previewUseCase = previewUseCase;
    this.createUseCase = createUseCase;
  }

  @PostMapping("/preview-conflicts")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeConflictsPreviewResponseDto> previewConflicts(
      @RequestBody @Valid BlockedTimeConflictsPreviewRequestDto requestDto,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    
    BlockedTimePreviewResult result = previewUseCase.execute(
        requestDto, 
        userDetails.getUserId()
    );
    
    BlockedTimeConflictsPreviewResponseDto response = toResponseDto(result);
    return ResponseEntity.ok(response);
  }
  
  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<BlockedTimeResponseDto> createBlockedTime(
      @RequestBody @Valid CreateBlockedTimeRequestDto requestDto,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    
    BlockedTime blockedTime = createUseCase.execute(
        requestDto,
        userDetails.getUserId()
    );
    
    BlockedTimeResponseDto response = toBlockedTimeResponseDto(blockedTime);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
  
  private BlockedTimeConflictsPreviewResponseDto toResponseDto(BlockedTimePreviewResult result) {
    return new BlockedTimeConflictsPreviewResponseDto(
        result.previewKey(), // NOVO
        result.conflicts().usersAffected(),
        result.conflicts().blockedTimesAffected(),
        result.conflicts().reservationsAffected(),
        result.conflicts().conflictingBlockedTimes(),
        result.conflicts().conflictingReservations()
    );
  }
  
  private BlockedTimeResponseDto toBlockedTimeResponseDto(BlockedTime blockedTime) {
    // Implementar mapeamento
    return new BlockedTimeResponseDto(
        blockedTime.getId(),
        blockedTime.getCourtId(),
        null, // courtName - buscar depois
        blockedTime.getDateTimeSlot().date(),
        new TimeIntervalDto(
            blockedTime.getDateTimeSlot().timeInterval().startTime(),
            blockedTime.getDateTimeSlot().timeInterval().endTime()
        ),
        blockedTime.getDescription(),
        blockedTime.isFullDay(),
        blockedTime.getRecurringBlockedTimeId(),
        blockedTime.getCreatedAt()
    );
  }
}
```

## Validações e Regras de Negócio

### 1. Validação de Datas
- `startDate` deve ser hoje ou no futuro
- `endDate` deve ser >= `startDate`
- Se `startDate` == `endDate`, é um bloqueio de um único dia
- TTL do cache: 5 minutos

### 2. Validação de TimeInterval
- Se `isFullDay` == true, `timeInterval` pode ser null
- Se `isFullDay` == false, `timeInterval` é obrigatório
- `timeInterval` deve estar dentro do horário de funcionamento

### 3. Validação de Preview
- Preview deve existir no cache
- Preview não pode ter expirado
- Conflitos devem ser os mesmos do momento do preview
- Chave do preview deve pertencer ao usuário autenticado

### 4. Operação Atômica (Transação)
```java
@Transactional
public BlockedTime execute(...) {
    // 1. Validar preview
    // 2. Cancelar reservas
    // 3. Criar BlockedTime
    // 4. Criar recorrências (se houver)
    // Se qualquer etapa falhar, rollback completo
}
```

## Cenários de Erro

### 1. Preview Expirado (400)
```json
{
  "timestamp": "2025-12-30T14:30:00Z",
  "status": 400,
  "errorCode": "BLOCKED_TIME_PREVIEW_EXPIRED",
  "developerMessage": "O preview de bloqueio expirou. Gere um novo preview.",
  "path": "/api/admin/blocked-times"
}
```

### 2. Conflitos Mudaram (409)
```json
{
  "timestamp": "2025-12-30T14:30:00Z",
  "status": 409,
  "errorCode": "BLOCKED_TIME_CONFLICTS_CHANGED",
  "developerMessage": "Os conflitos mudaram desde o preview. Revise as alterações antes de confirmar.",
  "path": "/api/admin/blocked-times"
}
```

### 3. Preview Key Inválida (403)
```json
{
  "timestamp": "2025-12-30T14:30:00Z",
  "status": 403,
  "errorCode": "BLOCKED_TIME_PREVIEW_KEY_INVALID",
  "developerMessage": "A chave do preview é inválida ou foi manipulada.",
  "path": "/api/admin/blocked-times"
}
```

## Testes

### Testes Unitários

#### PreviewBlockedTimeConflictsUseCaseImpTest
- ✅ Deve gerar preview e salvar no cache
- ✅ Deve retornar chave única do cache
- ✅ Deve calcular conflitos corretamente
- ✅ Deve usar TTL de 5 minutos

#### CreateBlockedTimeUseCaseImpTest
- ✅ Deve criar BlockedTime quando preview é válido
- ✅ Deve lançar exceção se preview expirou
- ✅ Deve lançar exceção se conflitos mudaram
- ✅ Deve lançar exceção se chave não pertence ao usuário
- ✅ Deve cancelar todas as reservas conflitantes
- ✅ Deve limpar cache após criação

#### BlockedTimePreviewCacheAdapterTest
- ✅ Deve salvar e recuperar preview do Redis
- ✅ Deve respeitar TTL configurado
- ✅ Deve gerar chave única
- ✅ Deve validar ownership da chave

### Testes de Integração

#### AdminBlockedTimesControllerIntegrationTest
- ✅ Deve gerar preview com sucesso (200)
- ✅ Deve retornar preview key no response
- ✅ Deve criar BlockedTime com preview válido (201)
- ✅ Deve retornar 400 se preview expirou
- ✅ Deve retornar 409 se conflitos mudaram
- ✅ Deve retornar 403 se preview key for de outro usuário
- ✅ Deve cancelar reservas em transação atômica

## Documentação OpenAPI

### Preview Endpoint

```yaml
paths:
  /api/admin/blocked-times/preview-conflicts:
    post:
      operationId: previewBlockedTimeConflicts
      tags: ["Administração - Horários Bloqueados"]
      summary: Visualizar conflitos antes de criar bloqueio
      description: |
        Gera um preview dos conflitos (reservas e bloqueios) que serão afetados
        ao criar um novo horário bloqueado. 
        
        O resultado é salvo em cache por 5 minutos e retorna uma chave única
        que deve ser usada para confirmar a criação.
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/BlockedTimeConflictsPreviewRequestDto'
      responses:
        200:
          description: Preview gerado com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BlockedTimeConflictsPreviewResponseDto'
        400:
          $ref: '#/components/responses/BadRequest'
        401:
          $ref: '#/components/responses/Unauthorized'
        403:
          $ref: '#/components/responses/Forbidden'
```

### Create Endpoint

```yaml
paths:
  /api/admin/blocked-times:
    post:
      operationId: createBlockedTime
      tags: ["Administração - Horários Bloqueados"]
      summary: Criar novo horário bloqueado
      description: |
        Cria um novo horário bloqueado, cancelando automaticamente as reservas
        conflitantes identificadas no preview.
        
        Requer a chave do preview (válida por 5 minutos) para garantir que
        o administrador revisou os conflitos antes de confirmar.
        
        A operação é atômica: se falhar, nenhuma reserva é cancelada.
      security:
        - BearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateBlockedTimeRequestDto'
      responses:
        201:
          description: Horário bloqueado criado com sucesso
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/BlockedTimeResponseDto'
        400:
          description: Preview expirado ou dados inválidos
          content:
            application/json:
              examples:
                PreviewExpired:
                  summary: Preview expirou
                  value:
                    timestamp: "2025-12-30T14:30:00Z"
                    status: 400
                    errorCode: "BLOCKED_TIME_PREVIEW_EXPIRED"
                    developerMessage: "O preview de bloqueio expirou. Gere um novo preview."
        403:
          description: Preview key inválida ou de outro usuário
        409:
          description: Conflitos mudaram desde o preview
          content:
            application/json:
              example:
                timestamp: "2025-12-30T14:30:00Z"
                status: 409
                errorCode: "BLOCKED_TIME_CONFLICTS_CHANGED"
                developerMessage: "Os conflitos mudaram desde o preview. Revise as alterações antes de confirmar."
```

## Migration (Banco de Dados)

```sql
-- Se necessário ajustar a tabela blocked_times
ALTER TABLE blocked_times ADD COLUMN IF NOT EXISTS is_full_day BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE blocked_times ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE blocked_times ADD COLUMN IF NOT EXISTS recurring_blocked_time_id UUID REFERENCES blocked_times(id);

-- Índices para performance
CREATE INDEX IF NOT EXISTS idx_blocked_times_recurring ON blocked_times(recurring_blocked_time_id);
CREATE INDEX IF NOT EXISTS idx_blocked_times_court_date ON blocked_times(court_id, date);
```

## Checklist de Implementação

### Domain Layer
- [ ] Criar `PreviewExpiredException`
- [ ] Criar `ConflictsChangedException`
- [ ] Criar `InvalidPreviewKeyException`
- [ ] Adicionar ErrorCodes
- [ ] Ajustar entidade `BlockedTime` (se necessário)

### Application Layer
- [ ] Criar `BlockedTimePreviewCachePort`
- [ ] Criar `BlockedTimeRepositoryPort`
- [ ] Criar `BlockedTimePreviewResult`
- [ ] Modificar `PreviewBlockedTimeConflictsUseCase`
- [ ] Modificar `PreviewBlockedTimeConflictsUseCaseImp`
- [ ] Criar `CreateBlockedTimeUseCase`
- [ ] Criar `CreateBlockedTimeUseCaseImp`

### Infrastructure Layer
- [ ] Criar `BlockedTimePreviewCacheAdapter`
- [ ] Criar `BlockedTimeRepositoryAdapter`
- [ ] Configurar `RedisTemplate` para preview
- [ ] Criar `CreateBlockedTimeRequestDto`
- [ ] Criar `RecurrenceDto`
- [ ] Modificar `BlockedTimeConflictsPreviewResponseDto` (adicionar previewKey)
- [ ] Criar `BlockedTimeResponseDto`
- [ ] Atualizar `AdminBlockedTimesController`

### Testes
- [ ] Testes unitários do `CreateBlockedTimeUseCaseImp`
- [ ] Testes unitários do `BlockedTimePreviewCacheAdapter`
- [ ] Testes de integração do controller
- [ ] Testes de cenários de erro

### Documentação
- [ ] Atualizar OpenAPI para endpoint de preview
- [ ] Documentar endpoint de criação
- [ ] Documentar schemas
- [ ] Adicionar exemplos de erro

## Próximos Passos

1. **Implementar Recorrência**: Sistema para criar bloqueios recorrentes (diário, semanal, mensal)
2. **Notificações**: Enviar SMS/Email para usuários cujas reservas foram canceladas
3. **Auditoria**: Registrar quem criou o bloqueio e quais reservas foram canceladas
4. **Dashboard**: Relatório de bloqueios criados e reservas afetadas

## Referências

- Redis Cache: https://docs.spring.io/spring-data/redis/docs/current/reference/html/
- Transactions: https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction
- OpenAPI: https://spec.openapis.org/oas/v3.1.0

