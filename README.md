# Arena Mafia - Sistema de Agendamento de Quadras Esportivas

> Uma aplicação moderna de agendamento de quadras esportivas desenvolvida como projeto de extensão universitária no IFSC.  
> Construída com **Arquitetura Hexagonal**, **Spring Boot 3.5**, **Amazon Web Services (SQS, EventBridge Scheduler)** e observabilidade em tempo real com **OpenTelemetry**.

## 🎯 Visão Geral

**Arena Mafia** é uma solução completa e cloud-native para gerenciar agendamentos de quadras esportivas (tênis, vôlei, badminton, etc.). A aplicação oferece:

✅ **Autenticação e Autorização** — Sistema JWT com refresh tokens  
✅ **Gestão de Usuários** — Perfis de administrador e cliente  
✅ **Agendamento Inteligente** — Suporte a modalidades, quadras, horários e preços dinâmicos  
✅ **Preços Variáveis** — Regras de preço por período, horário e modalidade  
✅ **Notificações Cloud-Native** — Pipeline assíncrono com AWS SQS, WhatsApp, SMS e fallback automático  
✅ **Task Scheduling Distribuído** — Agendamento de tarefas com AWS EventBridge Scheduler  
✅ **Observabilidade Completa** — Traces distribuído, logs centralizados e métricas em tempo real  
✅ **API RESTful Documentada** — OpenAPI 3.1 com Swagger UI integrado

---

## Executar a arquitetura local completa

O `docker-compose.yml` inicia PostgreSQL, Redis, LocalStack, backend, Nginx e a
stack de observabilidade completa.

### Pré-requisitos

- Docker Desktop instalado e em execução
- GitHub Personal Access Token com permissão `read:packages`

### 1. Autenticar no GitHub Container Registry

A imagem do backend está publicada no GHCR. Faça login antes da primeira
execução:

```bash
echo "SEU_TOKEN_GITHUB" | docker login ghcr.io -u SEU_USUARIO_GITHUB --password-stdin
```

Não salve o token do GitHub no `.env` ou no repositório.

### 2. Configurar o ambiente local

O Compose possui valores padrão funcionais. Para sobrescrever portas,
credenciais locais ou resolver conflitos com outros projetos:

```bash
cp .env.example .env
```

O Nginx exige um arquivo local de autenticação básica para proteger a
documentação:

```bash
printf "admin:$(openssl passwd -apr1 admin)\n" > gateway/.htpasswd
```

### 3. Iniciar os serviços

```bash
docker compose up -d
docker compose ps
```

O `backend-app`, PostgreSQL, Redis e LocalStack devem aparecer como `healthy`.
O `localstack-init` deve aparecer como `Exited (0)`, pois é executado apenas
para criar filas, DLQs, role e grupo do Scheduler.

### Acessos locais

| Serviço | Endereço | Credenciais |
|---|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html | Não exige login |
| OpenAPI YAML | http://localhost:8080/docs/openapi.yml | Não exige login |
| API pelo gateway | http://localhost/api/ | JWT nos endpoints protegidos |
| Swagger pelo gateway | http://localhost/swagger-ui.html | `admin` / `admin` |
| Grafana | http://localhost:3000/grafana/ | `admin` / valor de `GF_SECURITY_ADMIN_PASSWORD` |
| Prometheus | http://localhost:9090 | Não exige login |
| LocalStack | http://localhost:4567 | Credenciais locais `test` / `test` |
| PostgreSQL | `localhost:5433` | Valores de `POSTGRES_USER` e `POSTGRES_PASSWORD` |
| Redis | `localhost:6380` | Sem senha no ambiente local |

As portas podem ser alteradas no `.env`. Consulte `.env.example` para ver
todas as variáveis disponíveis.

### Comandos úteis

```bash
# Acompanhar logs do backend
docker compose logs -f backend-app

# Baixar a versão mais recente da imagem e recriar o backend
docker compose pull backend-app
docker compose up -d backend-app

# Parar os serviços preservando os volumes
docker compose down

# Parar os serviços e remover os dados locais
docker compose down -v
```

---

## 🏗️ Arquitetura

### Diagrama da Arquitetura

![Arquitetura Arena Mafia](assets/diagrama-infraestrutura-app.png)

### Arquitetura Hexagonal (Ports & Adapters)

A aplicação segue rigorosamente o padrão de **Arquitetura Hexagonal**, garantindo:
- **Independência de Frameworks**: O domínio é totalmente desacoplado de Spring, JPA e outras dependências externas
- **Testabilidade**: Cada camada pode ser testada isoladamente
- **Manutenibilidade**: Código organizado e previsível
- **Escalabilidade**: Fácil adicionar novos adapters e ports

#### Estrutura de Camadas

```
src/main/java/com/projetoExtensao/arenaMafia/
├── domain/                          # Domínio (Regras de Negócio)
│   ├── model/                       # Entidades de negócio
│   ├── valueobjects/                # Value Objects (conceitos imutáveis)
│   ├── exception/                   # Exceções customizadas de domínio
│   └── /* Policies e Validators */
│
├── application/                     # Aplicação (Use Cases)
│   ├── agenda/                      # Use cases de agenda
│   ├── auth/                        # Use cases de autenticação
│   ├── court/                       # Use cases de quadras
│   ├── modality/                    # Use cases de modalidades
│   ├── priceRule/                   # Use cases de regras de preço
│   ├── schedule/                    # Use cases de agendamentos
│   ├── user/                        # Use cases de usuários
│   ├── notification/                # Eventos e listeners de notificação (WhatsApp, SMS, OTP)
│   ├── scheduleTask/                # Eventos e listeners de agendamento de tarefas
│   ├── {feature}/
│   │   ├── usecase/                 # Interfaces dos casos de uso
│   │   └── imp/                     # Implementações dos casos de uso
│   └── security/                    # Portas de segurança
│
└── infrastructure/                  # Infraestrutura (Adapters)
    ├── adapter/
    │   └── gateway/
    │       ├── notification/         # AWS SQS Producers & Consumers (SMS, WhatsApp)
    │       └── scheduling/          # AWS EventBridge Scheduler & SQS Consumers
    ├── config/                      # Configurações Spring & AWS
    ├── persistence/                 # JPA Repositories (adaptam a porta do repositório)
    ├── security/                    # Spring Security (autenticação/autorização)
    └── web/                         # REST Controllers (adaptam as portas de entrada)
```

### Fluxo de Dados

```
HTTP Request
     ↓
[Controller] (Infrastructure)
     ↓
[Use Case] (Application)
     ↓
[Entity] (Domain) - Lógica de Negócio
     ↓
[Port] (Application) - Abstração
     ↓
[Adapter] (Infrastructure) - Implementação Concreta
     ↓
[Banco de Dados / Serviço Externo]
```

---

## ☁️ AWS Cloud Architecture — Notificações & Task Scheduling

A aplicação integra-se com **Amazon Web Services (AWS)** para garantir **processamento assíncrono, resiliência e escalabilidade** nos fluxos de notificação e agendamento de tarefas. Toda a comunicação com serviços AWS segue o padrão **Ports & Adapters**, mantendo o domínio completamente desacoplado da infraestrutura cloud.

### Visão Geral da Integração AWS

```
┌─────────────────────────── Aplicação Spring Boot ───────────────────────────────────┐
│                                                                                     │
│  Use Case executa ação                                                              │
│       ↓                                                                             │
│  Event Publisher (Spring Events)                                                    │
│       ↓                         ↓                                                   │
│  ScheduleTaskEventListener     NotificationEventListener                            │
│       ↓                         ↓                                                   │
│  ScheduledTaskPort (Port)      NotificationPort (Port)                              │
│       ↓                         ↓                                                   │
│  ScheduledTaskProducer ───→ AWS EventBridge ───→ NotificationProducer ───→ AWS SQS  │
│       (Adapter)            Scheduler               (Adapter)                        │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 📨 Pipeline de Notificações (AWS SQS)

O sistema de notificações é totalmente **event-driven** e baseado em **filas AWS SQS**, garantindo desacoplamento, entrega confiável e fallback automático entre canais de comunicação.

#### Fluxo Arquitetural

```
  Use Case (ex: criar reserva)
       ↓
  ApplicationEventPublisher.publishEvent()
       ↓
  NotificationEventListener (Application Layer)
       │
       ├── OTP (verificação)  → NotificationPort.sendSms()
       ├── Lembrete           → NotificationPort.sendWhatsappMessage()
       └── Aviso admin        → NotificationPort.sendWhatsappMessage()
                                       ↓
                              NotificationProducer (Infrastructure)
                                       ↓ SqsTemplate.send()
                  ┌────────────────────────────────────────┐
                  │          Amazon SQS Queues             │
                  │                                        │
                  │  ┌─────────────────────────────────┐   │
                  │  │ whatsapp-transactional-queue    │   │
                  │  │  → WhatsAppConsumer             │   │
                  │  │  → API WhatsApp                 │   │
                  │  │                                 │   │
                  │  │  Retry: 3x → DLQ ↓              │   │
                  │  └─────────────────────────────────┘   │
                  │                                        │
                  │  ┌─────────────────────────────────┐   │
                  │  │ whatsapp-transactional-dlq      │   │
                  │  │  → WhatsAppFallbackConsumer     │   │
                  │  │  → Fallback: envia via SMS      │   │
                  │  └─────────────────────────────────┘   │
                  │                                        │
                  │  ┌─────────────────────────────────┐   │
                  │  │ sms-queue                       │   │
                  │  │  → SmsConsumer                  │   │
                  │  │  → API SMS                      │   │
                  │  │  Retry: 1x → general-trash-dlq  │   │
                  │  └─────────────────────────────────┘   │
                  └────────────────────────────────────────┘
```

#### Tipos de Notificação

| Evento | Canal | Fila SQS | Descrição |
|--------|-------|----------|-----------|
| Verificação de telefone (OTP) | SMS | `sms-queue` | Código de verificação de 6 dígitos |
| Reserva criada pelo admin | WhatsApp | `whatsapp-transactional-queue` | Confirmação com dados da reserva |
| Reserva cancelada pelo admin | WhatsApp | `whatsapp-transactional-queue` | Notificação com motivo do cancelamento |
| Reservas recorrentes criadas | WhatsApp | `whatsapp-transactional-queue` | Detalhes do período e dias da semana |
| Lembrete de reserva | WhatsApp | `whatsapp-transactional-queue` | Lembrete 2h antes da reserva |

#### Estratégia de Resiliência (DLQ & Fallback)

```
              WhatsApp falhou?
                    ↓
    SQS Retry automático (até 3 tentativas)
                    ↓
            Ainda falhou?
                    ↓
    Mensagem movida para DLQ (whatsapp-transactional-dlq)
                    ↓
    WhatsAppFallbackConsumer consome da DLQ
                    ↓
    Envia notificação via SMS como backup
```

> **Decisão de Arquitetura**: O fallback WhatsApp → SMS garante que o usuário **sempre** receba a notificação, mesmo em caso de indisponibilidade total da API WhatsApp. O SQS gerencia automaticamente os retries e o redrive para a DLQ.

---

### ⏰ Task Scheduling Distribuído (AWS EventBridge Scheduler)

O agendamento de tarefas utiliza o **AWS EventBridge Scheduler** para criar schedules one-time que, no momento programado, enviam mensagens para filas **AWS SQS** consumidas pela aplicação.

#### Fluxo Arquitetural

```
  Use Case (ex: criar reserva)
       ↓
  ApplicationEventPublisher.publishEvent()
       ↓
  ScheduledTaskEventListener (Application Layer)
       ↓
  ScheduledTaskPort (Port Interface)
       ↓
  ScheduledTaskProducer (Infrastructure — AWS SDK)
       ↓
  AWS EventBridge Scheduler
       │
       ├── Cria Schedule one-time: at(2026-03-15T18:00:00)
       │   Target: SQS schedule-task-queue
       │   ActionAfterCompletion: DELETE
       │
       └── Cria Schedule one-time: at(2026-03-15T16:00:00)
           Target: SQS whatsapp-reminder-queue
           ActionAfterCompletion: DELETE

  ──── Quando o horário chega ────

  AWS EventBridge Scheduler dispara
       ↓
  Envia mensagem para a fila SQS
       ↓
  ┌────────────────────────────────────────────┐
  │  schedule-task-queue                       │
  │   → ScheduledTaskConsumer                  │
  │   → Completa reserva / Deleta blocked time │
  │   Retry: 3x → schedule-task-dlq            │
  └────────────────────────────────────────────┘
  ┌────────────────────────────────────────────┐
  │  whatsapp-reminder-queue                   │
  │   → ScheduledReminderConsumer              │
  │   → Publica evento de lembrete via WhatsApp│
  │   Retry: 3x → schedule-task-dlq            │
  └────────────────────────────────────────────┘
```

#### Eventos Gerenciados pelo Scheduler

| Evento de Domínio | Ação no EventBridge | Fila SQS Target | Consumer |
|--------------------|---------------------|-----------------|----------|
| Reserva criada | Agenda task de conclusão + lembrete | `schedule-task-queue` + `whatsapp-reminder-queue` | `ScheduledTaskConsumer` + `ScheduledReminderConsumer` |
| Reserva cancelada | Cancela task de conclusão + lembrete | — | — |
| Blocked time criado | Agenda task de deleção automática | `schedule-task-queue` | `ScheduledTaskConsumer` |
| Blocked time deletado | Cancela task de deleção | — | — |

#### Topologia Completa de Filas AWS SQS

```
                   Amazon SQS
  ┌─────────────────────────────────────────────────────┐
  │                                                     │
  │  ┌────────────────────────┐   ┌──────────────────┐  │
  │  │ schedule-task-queue    │──→│ schedule-task-dlq│  │
  │  │ (Retry: 3x)            │   │ (Dead Letter)    │  │
  │  └────────────────────────┘   └──────────────────┘  │
  │                                                     │
  │  ┌──────────────────────────┐ ┌──────────────────┐  │
  │  │ whatsapp-reminder-queue  │→│ schedule-task-dlq│  │
  │  │ (Retry: 3x)              │ │ (Dead Letter)    │  │
  │  └──────────────────────────┘ └──────────────────┘  │
  │                                                     │
  │  ┌──────────────────────────────┐ ┌───────────────┐ │
  │  │ whatsapp-transactional-queue │→│ whatsapp-     │ │
  │  │ (Retry: 3x)                  │ │ transactional │ │
  │  └──────────────────────────────┘ │ -dlq          │ │
  │                                   │ (Fallback SMS)│ │
  │                                   └───────────────┘ │
  │                                                     │
  │  ┌────────────────────┐   ┌──────────────────────┐  │
  │  │ sms-queue          │──→│ general-trash-dlq    │  │
  │  │ (Retry: 1x)        │   │ (Descartado)         │  │
  │  └────────────────────┘   └──────────────────────┘  │
  │                                                     │
  └─────────────────────────────────────────────────────┘
```

#### Ports & Adapters — Desacoplamento AWS

A integração com AWS segue rigorosamente a **Arquitetura Hexagonal**. A camada de domínio e aplicação não possuem nenhuma dependência direta com serviços AWS:

```
┌─────────────────────────────────────────────────┐
│  APPLICATION LAYER (Ports)                      │
│                                                 │
│  ScheduledTaskPort (interface)                  │
│    ├── scheduleTask(id, type, executionTime)    │
│    ├── cancelTask(id, type)                     │
│    ├── scheduleReservationReminderTask(id, time)│
│    └── cancelReservationReminderTask(id)        │
│                                                 │
│  NotificationPort (interface)                   │
│    ├── sendSms(phone, content)                  │
│    └── sendWhatsappMessage(phone, content)      │
└────────────────────┬────────────────────────────┘
                     │ implementa
┌────────────────────▼────────────────────────────┐
│  INFRASTRUCTURE LAYER (Adapters)                │
│                                                 │
│  ScheduledTaskProducer                          │
│    → AWS SDK SchedulerClient                    │
│    → Cria/deleta schedules no EventBridge       │
│                                                 │
│  NotificationProducer                           │
│    → Spring Cloud AWS SqsTemplate               │
│    → Publica mensagens nas filas SQS            │
└─────────────────────────────────────────────────┘
```

> **Princípio**: Trocar o AWS SQS por RabbitMQ ou o EventBridge por Quartz requer apenas a implementação de novos adapters — **zero alterações no domínio ou na camada de aplicação**.

---

## 💻 Tecnologias

### Backend
- **Java 21** - Linguagem principal
- **Spring Boot 3.5.4** - Framework web
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - Persistência de dados
- **Spring Cloud AWS (SQS)** - Integração nativa com AWS SQS
- **Flyway 11.10.2** - Versionamento de banco de dados
- **MapStruct 1.5.5** - Mapeamento entre DTOs e entidades
- **PostgreSQL** - Banco de dados principal
- **Redis** - Cache e sessions distribuídas
- **OpenTelemetry** - Observabilidade (traces, logs, métricas)

### Amazon Web Services (AWS)
- **AWS SQS** - Filas de mensagens assíncronas (notificações e tarefas)
- **AWS SQS DLQ** - Dead Letter Queues para fallback e reprocessamento
- **AWS EventBridge Scheduler** - Agendamento de tarefas one-time distribuídas
- **AWS IAM** - Controle de acesso e roles para os serviços

### Infraestrutura & DevOps
- **Docker** - Containerização
- **Docker Compose** - Orquestração local
- **Nginx** - Proxy reverso
- **OpenTelemetry Collector** - Centralização de observabilidade

### Observabilidade
- **Prometheus** - Coleta de métricas
- **Grafana** - Visualização de métricas
- **Tempo** - Armazenamento de traces distribuídos
- **Loki** - Agregação de logs
- **OpenTelemetry Java Agent** - Instrumentação automática

### Testes
- **JUnit 5** - Framework de testes
- **Mockito** - Mocking de dependências
- **AssertJ** - Asserções fluentes
- **TestContainers** - Containers Docker para testes de integração
- **REST Assured** - Testes de API REST

---

## 📁 Estrutura do Projeto

```
.
├── src/
│   ├── main/
│   │   ├── java/com/projetoExtensao/arenaMafia/
│   │   │   ├── domain/                 # Camada de Domínio
│   │   │   ├── application/            # Camada de Aplicação
│   │   │   │   ├── notification/       #   ├── Eventos e listeners de notificação
│   │   │   │   │   ├── event/          #   │     Domain events (OTP, Lembrete, Aviso)
│   │   │   │   │   ├── gateway/        #   │     Ports (NotificationPort, OtpPort)
│   │   │   │   │   └── listener/       #   │     NotificationEventListener
│   │   │   │   ├── scheduleTask/       #   ├── Eventos e listeners de task scheduling
│   │   │   │   │   ├── event/          #   │     Domain events (Reserva, BlockedTime)
│   │   │   │   │   ├── gateway/        #   │     Port (ScheduledTaskPort)
│   │   │   │   │   └── listener/       #   │     ScheduledTaskEventListener
│   │   │   │   └── {feature}/          #   └── Demais use cases
│   │   │   └── infrastructure/         # Camada de Infraestrutura
│   │   │       └── adapter/gateway/
│   │   │           ├── notification/   #     AWS SQS Notification Adapters
│   │   │           │   ├── consumer/   #       SmsConsumer, WhatsAppConsumer, WhatsAppFallbackConsumer
│   │   │           │   ├── producer/   #       NotificationProducer (SqsTemplate)
│   │   │           │   ├── sms/        #       SmsClient, SmsProvider (Strategy Pattern)
│   │   │           │   └── whatsapp/   #       WhatsAppClient, WhatsAppProvider (Strategy Pattern)
│   │   │           └── scheduling/     #     AWS EventBridge Scheduler Adapters
│   │   │               ├── consumer/   #       ScheduledTaskConsumer, ScheduledReminderConsumer
│   │   │               ├── producer/   #       ScheduledTaskProducer (SchedulerClient SDK)
│   │   │               └── dto/        #       ScheduledTaskDto, ScheduledReminderTaskDto
│   │   └── resources/
│   │       ├── application.yml         # Configurações padrão
│   │       ├── application-dev.yml     # Configurações dev (inclui AWS)
│   │       ├── application-prod.yml    # Configurações prod (inclui AWS)
│   │       ├── db/migration/           # Scripts Flyway
│   │       └── static/docs/            # Documentação OpenAPI
│   └── test/
│       ├── java/                       # Testes (Unit + Integration)
│       └── resources/
│           └── application-test.yml    # Configurações para testes
│
├── config/                              # Configurações de infraestrutura
│   ├── otel-collector-config.yml        # OpenTelemetry Collector
│   ├── prometheus.yml                   # Prometheus
│   ├── loki.yml                         # Loki
│   ├── tempo.yml                        # Tempo
│   └── grafana/                         # Grafana dashboards
│
├── gateway/
│   └── nginx.conf                       # Nginx reverse proxy
│
├── docker-compose.yml                   # Arquitetura local completa
├── .env.example                         # Variáveis locais opcionais
├── Dockerfile                           # Build da imagem
├── pom.xml                              # Maven config
├── LICENSE                              # Licença
└── README.md                            # Este arquivo
```

---

## 📊 Observabilidade

### OpenTelemetry (Unified Observability)

A aplicação implementa observabilidade completa com **OpenTelemetry**:

#### Componentes
- **Java Agent**: Instrumentação automática via `opentelemetry-javaagent.jar`
- **OTel Collector**: Centraliza e processa traces, logs e métricas
- **Prometheus**: Coleta de métricas (Pull Model)
- **Tempo**: Armazenamento de traces distribuídos
- **Loki**: Agregação centralizada de logs
- **Grafana**: Visualização unificada

#### Fluxo de Dados

```
Java Application (Instrumented by Agent)
        ↓ (gRPC - OTLP)
OTel Collector
    ↙     ↓     ↘
Prometheus  Tempo  Loki
    ↓       ↓     ↓
  Grafana (Unified Dashboard)
```

### Acessar Dashboards

**Grafana** (Visualização unificada)
- Dashboards pré-configurados:
  - **Spring Boot Observability** - Métricas da aplicação
  - **JVM Micrometer** - Métricas da JVM

## 🧪 Testes

### Estrutura

```
src/test/java/com/projetoExtensao/arenaMafia/
├── integration/                     # Testes de Integração
│   └── config/
│       ├── WebIntegrationTestConfig.java
│       └── BaseTestContainersConfig.java
└── unit/                            # Testes Unitários
```

### Padrão AAA (Arrange-Act-Assert)

```java
@Test
@DisplayName("Deve criar um novo usuário com sucesso")
void shouldCreateUserSuccessfully() {
    // Arrange - Preparação
    CreateUserRequest request = new CreateUserRequest("user@example.com", "password123");
    
    // Act - Execução
    UserResponse response = createUserUseCase.execute(request);
    
    // Assert - Verificação
    assertThat(response.id()).isNotNull();
    assertThat(response.email()).isEqualTo("user@example.com");
}
```

### Testes de Integração com REST Assured

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para UserController")
public class UserControllerIntegrationTest extends WebIntegrationTestConfig {
    
    @BeforeEach
    void setup() {
        super.setupRestAssured();
        specification = new RequestSpecBuilder()
            .setBasePath("/api/users")
            .setContentType(MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    @Test
    @DisplayName("POST /api/users - Deve criar usuário com sucesso (201)")
    void shouldCreateUserSuccessfully() {
        var response = given()
            .spec(specification)
            .body(new CreateUserRequest("user@example.com", "password123"))
            .when()
            .post()
            .then()
            .statusCode(201)
            .extract()
            .as(UserResponse.class);
        
        assertThat(response.id()).isNotNull();
    }
}
```

---

## 📚 Documentação da API

### Swagger UI

![Documentação Arena Mafia](assets/Documentação.png)

### Especificação OpenAPI

```
/static/docs/openapi.yml
```

Estrutura:
```
docs/
├── openapi.yml              # Arquivo principal
├── components/
│   ├── schemas/             # DTOs (Request/Response)
│   ├── parameters/          # Parâmetros reutilizáveis
│   ├── responses/           # Respostas de erro comuns
│   └── headers/             # Headers HTTP
└── paths/                   # Definição dos endpoints
```

---

## 🔐 Segurança

### Autenticação JWT

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@example.com",
    "password": "password123"
  }'

# Resposta:
# {
#   "accessToken": "eyJhbGc...",
#   "refreshToken": "eyJhbGc..."
# }

# 2. Usar Access Token
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGc..."
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGc..."
  }'
```

---

### Guideline de Commits

```
<tipo>(<escopo>): <subject>

<body>

<footer>
```

**Tipos:**
- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação, semântica (sem mudança de código)
- `refactor`: Refatoração de código
- `test`: Adição ou atualização de testes
- `chore`: Build, dependências, tooling

**Exemplo:**
```
feat(user): adicionar endpoint de perfil de usuário

- Implementa GET /api/users/me
- Adiciona testes de integração
- Documenta no OpenAPI

Closes #123
```

---

## 📝 Licença

Este projeto é licenciado sob a [MIT License](LICENSE).

---

## 👥 Autores

Projeto desenvolvido como iniciativa de extensão universitária no **Instituto Federal de Santa Catarina (IFSC)**.
