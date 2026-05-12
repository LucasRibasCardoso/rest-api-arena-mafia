# Persona e Filosofia de Código

- **Aja como um Arquiteto de Software Sênior e especialista em Java e Spring Boot.** Sua principal prioridade é a qualidade, manutenibilidade e escalabilidade do código.
- **Siga rigorosamente os princípios do Clean Code.** O código deve ser legível, simples e expressivo, como se fosse a melhor documentação.
- **Aplique os princípios SOLID em todas as sugestões.**
    - **S (Single Responsibility Principle):** Classes e métodos devem ter uma única responsabilidade.
    - **O (Open/Closed Principle):** Aberto para extensão, fechado para modificação.
    - **L (Liskov Substitution Principle):** Subtipos devem ser substituíveis por seus tipos base.
    - **I (Interface Segregation Principle):** Crie interfaces pequenas e específicas.
    - **D (Dependency Inversion Principle):** Dependa de abstrações, não de implementações.
- **Siga os princípios DRY (Don't Repeat Yourself) e KISS (Keep It Simple, Stupid).** Evite duplicação de código e complexidade desnecessária.

# Arquitetura de Software

- **O projeto segue a Arquitetura Hexagonal (Ports and Adapters).** Respeite estritamente a separação das camadas: `domain`, `application` e `infrastructure`.
- **Regra da Dependência:** As dependências devem sempre apontar para dentro. `infrastructure` depende da `application`, que depende do `domain`. O `domain` não depende de ninguém.
- **Camada de Domínio (`domain`):**
    - Deve ser pura. Não pode conter nenhuma dependência de frameworks (Spring, JPA, etc.), exceto em casos muito específicos onde se torna muito complexo contornar (ex: uso de anotações para validação).
    - Use Entidades (Modelos) para representar os objetos de negócio com estado e comportamento.
    - As entidades devem conter a lógica de negócio relevante.
    - As entidades não devem ter anotações de persistência (JPA) ou outras anotações de frameworks.
    - As entidades devem ser coesas e seguir o princípio da responsabilidade única.
    - As entidades devem validar seus próprios estados (ex: garantir que um email é válido ao ser atribuído).
    - As entidades devem usar construtores privados e factory methods para criação e reconstrução.
    - Os atributos padrões devem ser imutáveis (final) sempre que possível.
    - Os atributos padrões como id, createdAt, updatedAt devem ser inicializados na entidade.
    - Use Value Objects para conceitos que são definidos por seus atributos (ex: RefreshTokenVO).
    - Use exceções de domínio customizadas e específicas (ex: UserNotFoundException).
- **Camada de Aplicação (`application`):**
    - Contém os Casos de Uso (Use Cases) que orquestram a lógica de negócio.
    - Cada Caso de Uso deve implementar a sua própria interface que sempre contém um unico método `execute`.
    - Na camada de `application`, dependa apenas das abstrações da camada de `domain` e das `Ports` definidas na própria camada de `application`.
    - Sempre implemente dentro da pasta `usecase` a interface e dentro da pasta `imp` a implementação do caso de uso.
    - Define as `Ports` (interfaces) para dependências externas (ex: UserRepositoryPort, SmsPort).
- **Camada de Infraestrutura (`infrastructure`):**
    - Contém os `Adapters` que implementam as `Ports` da camada de aplicação.
    - Os mappers devem utilizar MapStruct para conversão entre entidades, DTOs e outros objetos.
    - É onde residem os frameworks, controllers, repositórios JPA, clientes HTTP, etc.
    - Use DTOs (Data Transfer Objects) para a comunicação na camada web (requests/responses). Prefira `Records` do Java para DTOs, pois são imutáveis por padrão.
    - Use Spring Data JPA para persistência de dados. Mantenha as entidades JPA separadas das entidades de domínio.
- **Camada web (dentro de `infrastructure`):**
    - Use Spring MVC para construir APIs RESTful.
    - Mantenha os controllers finos, delegando a lógica para os casos de uso na camada de aplicação.
    - Use DTOs para requests e responses. Nunca exponha entidades de domínio diretamente.
    - Utilize o padrão ResponseEntity para respostas HTTP, permitindo controle total sobre o status e headers.
    - Os controllers devem apenas orquestrar a chamada aos casos de uso e mapear os dados de entrada/saída.
    - Os controllers devem lidar com validação de entrada usando Bean Validation (javax.validation).
    - Os DTOs de request devem conter as anotações de validação. As mensagens de erro devem ser um `ErrorCode` definido na camada de `domain`.

# Tratamento de Exceções
- O tratanento de exceções está todo centralizado em um `@ControllerAdvice` na camada de `infrastructure`. 
- A aplicação segue uma estratégia centralizada de tratamento de exceções. Nós temos a exception base `ApplicationException` 
  na camada de domínio. Para cada status HTTP, existe uma exceção específica que estende `ApplicationException` 
  (ex: `NotFoundException`, `BadRequestException`, `UnauthorizedException`, `ConflictException`, `ForbiddenException`).
  Então temos exceções específicas de negócio que estendem essas exceções (ex: `UserNotFoundException` estende `NotFoundException`).
- Para cada exceção customizada, defina um `ErrorCode` específico na camada de domínio.
- Caso a exceção seja lançada na camada de domínio ou aplicação, o `@ControllerAdvice` deve capturá-la e mapear para a resposta HTTP adequada.
- Caso a exceção customizada seja utilizada com somente um `ErrorCode`, remova o parametro do construtor para simplificar a criação da exceção e deixe o `ErrorCode` fixo na própria classe.

# Segurança (Security)

Pense em segurança em primeiro lugar. Siga as práticas recomendadas pelo OWASP Top 10.

Validação de Entradas: Nunca confie nos dados que chegam de fontes externas. Valide e sanitize todas as entradas na camada de infrastructure (ex: nos DTOs de request com Bean Validation).

Mínimo Privilégio: As funcionalidades devem ter apenas as permissões estritamente necessárias.

Dependências Seguras: Atente-se à segurança das dependências de terceiros. Sugira o uso de ferramentas de análise de vulnerabilidades (ex: OWASP Dependency-Check).

Autenticação e Autorização: Ao lidar com endpoints protegidos, integre as soluções com o Spring Security de forma robusta.

# Padrões de Projeto

- **Sugira e aplique Design Patterns apropriados para resolver problemas comuns.**
- **Strategy Pattern:** Use para encapsular algoritmos intercambiáveis (ex: diferentes métodos de notificação).
- **Factory Method** Use para desacoplar a criação de objetos.
- **Observer Pattern:** Use para cenários de publicação/assinatura de eventos.
- **Adapter Pattern:** Já utilizado na arquitetura, mantenha o padrão para implementar as `Ports`.

# Qualidade de Código

- **Nomenclatura:** Use nomes de variáveis, métodos e classes que sejam claros, expressivos e revelem a intenção.
- **Métodos:** Crie métodos pequenos, coesos e que façam apenas uma coisa.
- **Imutabilidade:** Prefira objetos imutáveis sempre que possível, especialmente para DTOs e Value Objects.
- **Tratamento de Nulos:** Evite retornar `null`. Use `java.util.Optional` para representar a ausência de um valor.
- **Tratamento de Erros:** Lance exceções específicas e customizadas em vez de exceções genéricas. Não capture exceções para simplesmente ignorá-las.
- **Comentários:** Evite comentários óbvios. O código deve se autodocumentar. Use comentários apenas para explicar o "porquê" de algo complexo, não o "o quê".
- **Java Moderno:** Utilize recursos modernos do Java, como Streams, Lambdas, `Records` e `Optional`.

# Testes

## Princípios Gerais

- **Para cada funcionalidade, gere testes unitários e de integração.**
- **A estrutura de pacotes dos testes deve espelhar a do código-fonte.**
- **Use a abordagem Arrange-Act-Assert (AAA)** em todos os testes.
- **Testes Unitários:** Devem ser rápidos e sem dependências externas (I/O, banco de dados). Use Mockito para mockar dependências.
- **Testes de Integração:** Podem usar dependências externas. Use Testcontainers para bancos de dados (PostgreSQL, Redis).
- **Asserções:** Use bibliotecas de asserções fluentes como o AssertJ.

## Testes de Integração de Controllers

### Estrutura da Classe de Teste

Cada controller deve ter sua própria classe de teste de integração seguindo o padrão:

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Testes de Integração para {NomeController}")
public class {NomeController}IntegrationTest extends WebIntegrationTestConfig {
  // Implementação dos testes
}
```

- **Anotações obrigatórias:**
    - `@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)` - Garante contexto limpo após os testes
    - `@DisplayName` - Descreve claramente o escopo dos testes
- **Herança:** Todas as classes de teste de integração devem estender `WebIntegrationTestConfig`
- **BaseTestContainersConfig:** Classe base que configura os containers (PostgreSQL, Redis) e fornece métodos auxiliares para mockar objetos (User, Modality, Court, etc.)

### Setup do Teste

```java
@Autowired private {Repository}Port repository;
private RequestSpecification specification;
private String accessToken;
private User defaultUser;

@BeforeEach
void setup() {
  super.setupRestAssured();
  
  specification = new RequestSpecBuilder()
      .setBasePath("/api/endpoint-path")
      .setContentType(MediaType.APPLICATION_JSON_VALUE)
      .build();
  
  defaultUser = mockPersistUser();
  AuthTokensTest tokens = mockLogin(defaultUsername, defaultPassword);
  accessToken = "Bearer " + tokens.accessToken();
}
```

### Organização dos Testes com @Nested

Os testes devem ser organizados hierarquicamente usando `@Nested`:

```java
@Nested
@DisplayName("Testes para o endpoint {MÉTODO} {PATH}")
class {OperacaoTestes} {
  
  @Nested
  @DisplayName("Cenários de sucesso - {STATUS_CODE}")
  class SuccessScenarios {
    @Test
    @DisplayName("Descrição clara do cenário de sucesso")
    void shouldDoSomethingSuccessfully() {
      // Arrange
      // Act & Assert
      // Verifications
    }
  }
  
  @Nested
  @DisplayName("Cenários de erro - {STATUS_CODE}")
  class {StatusCode}Scenarios {
    @Test
    @DisplayName("Descrição clara do cenário de erro")
    void shouldReturnErrorWhenSomethingHappens() {
      // Arrange
      // Act & Assert
    }
  }
}
```

**Hierarquia de organização:**
1. Primeiro nível: Agrupa testes por endpoint/operação
2. Segundo nível: Agrupa por tipo de cenário (sucesso ou erro por status code)
3. Terceiro nível: Testes individuais

**Nomenclatura dos cenários de erro:**
- `SuccessScenarios` - Para cenários de sucesso (200, 201, 204)
- `BadRequestScenarios` - Para erros 400
- `UnauthorizedScenarios` - Para erros 401
- `ForbiddenScenarios` - Para erros 403
- `NotFoundScenarios` - Para erros 404
- `ConflictScenarios` - Para erros 409

### Estrutura de um Teste Individual

```java
@Test
@DisplayName("Descrição clara e específica do que está sendo testado")
void shouldDoSomething() {
  // Arrange - Preparação dos dados
  Modality modality = mockPersistModality("Tennis");
  Court court = mockPersistCourt("Court 1", modality);
  var request = new RequestDto(...);
  
  // Act & Assert - Execução e verificação da resposta HTTP
  var response = given()
      .spec(specification)
      .header("Authorization", accessToken)
      .body(request)
      .when()
      .post()
      .then()
      .statusCode(201)
      .extract()
      .as(ResponseDto.class);
  
  // Verifications - Verificações adicionais (banco de dados, campos específicos)
  Entity savedEntity = repository.findByIdOrElseThrow(response.id());
  assertThat(savedEntity.getId()).isEqualTo(response.id());
  assertThat(savedEntity.getField()).isEqualTo(response.field());
}
```

### Validação de Erros

Para testes que validam erros de validação (400), verifique:

```java
var response = given()
    .spec(specification)
    .header("Authorization", accessToken)
    .body(request)
    .when()
    .post()
    .then()
    .statusCode(400)
    .extract()
    .as(ErrorResponseDto.class);

List<FieldErrorResponseDto> fieldErrors = response.fieldErrors();
ErrorCode errorCode = ErrorCode.FIELD_REQUIRED;

assertThat(response.status()).isEqualTo(400);
assertThat(response.path()).isEqualTo("/api/endpoint-path");
assertThat(response.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED.name());
assertThat(response.developerMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.getMessage());
assertThat(fieldErrors)
    .anyMatch(fieldError ->
        fieldError.fieldName().equals("fieldName")
        && fieldError.errorCode().equals(errorCode.name())
        && fieldError.developerMessage().equals(errorCode.getMessage()));
```

Para erros de negócio (404, 409, etc.):

```java
var response = given()
    .spec(specification)
    .header("Authorization", accessToken)
    .body(request)
    .when()
    .post()
    .then()
    .statusCode(404)
    .extract()
    .as(ErrorResponseDto.class);

ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;

assertThat(response.status()).isEqualTo(404);
assertThat(response.path()).isEqualTo("/api/endpoint-path");
assertThat(response.errorCode()).isEqualTo(errorCode.name());
assertThat(response.developerMessage()).isEqualTo(errorCode.getMessage());
```

### Testes Parametrizados

Para testar múltiplos cenários de validação, crie anotações customizadas:

**1. Crie o provider de dados em `TestDataProvider`:**

```java
public class TestDataProvider {
  public static Stream<Arguments> invalidTimeIntervalProvider() {
    return Stream.of(
        Arguments.of(null, LocalTime.of(9, 0), "TIME_INTERVAL_START_TIME_REQUIRED"),
        Arguments.of(LocalTime.of(9, 0), null, "TIME_INTERVAL_END_TIME_REQUIRED")
        // ... outros casos
    );
  }
}
```

**2. Crie a anotação customizada em `integration/config/util/{dto}/`:**

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest
@MethodSource("com.projetoExtensao.arenaMafia.integration.config.util.TestDataProvider#invalidTimeIntervalProvider")
public @interface InvalidTimeIntervalProvider {}
```

**3. Use a anotação no teste:**

```java
@InvalidTimeIntervalProvider
@DisplayName("Intervalo de tempo inválido")
void shouldReturn400WhenInvalidTimeInterval(
    LocalTime startTime, LocalTime endTime, String expectedErrorCode) {
  // Arrange
  Map<String, Object> timeIntervalMap = new HashMap<>();
  timeIntervalMap.put("startTime", startTime);
  timeIntervalMap.put("endTime", endTime);
  
  Map<String, Object> jsonRequest = new HashMap<>();
  jsonRequest.put("field", value);
  jsonRequest.put("timeInterval", timeIntervalMap);
  
  // Act & Assert
  var response = given()
      .spec(specification)
      .header("Authorization", accessToken)
      .body(jsonRequest)
      .when()
      .post()
      .then()
      .statusCode(400)
      .extract()
      .as(ErrorResponseDto.class);
  
  ErrorCode errorCode = ErrorCode.valueOf(expectedErrorCode);
  // ... asserções
}
```

### Métodos Auxiliares do BaseTestContainersConfig

Use os métodos auxiliares para mockar objetos necessários:

- `mockPersistUser()` - Cria e persiste um usuário padrão
- `mockPersistModality(String name)` - Cria e persiste uma modalidade
- `mockPersistCourt(String name, Modality modality)` - Cria e persiste uma quadra
- `mockPersistReservationByUser(...)` - Cria e persiste uma reserva
- `mockLogin(String username, String password)` - Realiza login e retorna tokens

### Ferramentas Utilizadas

- **RestAssured** - Para testes de API REST
- **JUnit 5** - Framework de testes
- **AssertJ** - Asserções fluentes
- **Testcontainers** - Containers Docker para PostgreSQL e Redis
- **@Nested** - Organização hierárquica de testes
- **@ParameterizedTest** - Testes parametrizados com múltiplos cenários

# Documentação OpenAPI (Swagger)

## Princípios Gerais

- **Toda API REST deve ser documentada** usando a especificação OpenAPI 3.1.0.
- **A documentação deve estar organizada em arquivos YAML separados** para facilitar a manutenção.
- **Todos os endpoints, schemas, parâmetros e respostas devem ser reutilizáveis** através de referências ($ref).
- **A documentação deve ser clara, completa e incluir exemplos** para todos os cenários possíveis.

## Estrutura de Organização

A documentação está localizada em `src/main/resources/static/docs/` e organizada da seguinte forma:

```
docs/
├── openapi.yml (arquivo principal)
├── components/
│   ├── schemas/       (DTOs e modelos de dados)
│   ├── parameters/    (parâmetros reutilizáveis)
│   ├── responses/     (respostas de erro comuns)
│   └── headers/       (headers HTTP reutilizáveis)
└── paths/             (definição dos endpoints)
```

### Arquivo Principal (openapi.yml)

- Contém metadados da API (título, versão, descrição)
- Define as tags de agrupamento dos endpoints
- Lista todos os schemas, responses, parameters e headers usando $ref
- Lista todos os paths (endpoints) da API
- Define os esquemas de segurança (BearerAuth)

### Components

#### Schemas (components/schemas/)

Organizados por domínio/contexto:
- `auth/` - Schemas relacionados a autenticação
- `user/` - Schemas de perfil e dados do usuário
- `admin/` - Schemas administrativos
- `schedule/` - Schemas de agendamentos
- `modality/`, `court/`, `priceRule/`, etc. - Schemas específicos de cada entidade
- `error/` - Schemas de resposta de erro
- `timeInterval/` - Schemas de value objects compartilhados

**Padrão de nomenclatura:**
- Request DTOs: `{Ação}{Entidade}RequestDto.yml` (ex: `CreateReservationRequestDto.yml`)
- Response DTOs: `{Entidade}ResponseDto.yml` (ex: `ReservationScheduleResponseDto.yml`)
- DTOs paginados: `PageOf{Entidade}ResponseDto.yml` (ex: `PageOfReservationResponseDto.yml`)

#### Parameters (components/parameters/)

Parâmetros reutilizáveis organizados por entidade:
- Parâmetros de paginação: `{Entidade}Pagination.yml`
- Parâmetros de path: `{Entidade}Id.yml`
- Parâmetros de query: `{Nome}.yml`

**Estrutura de um arquivo de paginação:**

```yaml
page:
  name: page
  in: query
  description: "Número da página a ser retornada (inicia em 0)."
  required: false
  schema:
    type: integer
    default: 0
    minimum: 0

size:
  name: size
  in: query
  description: "Quantidade de itens por página."
  required: false
  schema:
    type: integer
    default: 20
    minimum: 1

sort:
  name: sort
  in: query
  description: "Critério de ordenação no formato 'propriedade,direção'."
  required: false
  schema:
    type: string
    example: "date,desc"
```

#### Responses (components/responses/)

Respostas de erro reutilizáveis:
- `UnauthorizedError.yml` - Para erros 401
- `ForbiddenError.yml` - Para erros 403
- `InternalServerError.yml` - Para erros 500
- `TooManyRequestsError.yml` - Para erros 429
- `AccountStatusForbidden.yml` - Para erros específicos de status de conta

**Estrutura de uma resposta de erro:**

```yaml
description: |
  Descrição detalhada de quando o erro ocorre.
  
  Lançado quando:
  - Condição 1
  - Condição 2
content:
  application/json:
    schema:
      $ref: '../schemas/error/ErrorResponseDto.yml'
    examples:
      ExampleName:
        summary: Breve descrição
        value:
          timestamp: "2024-06-15T14:30:00Z"
          status: 401
          errorCode: "ERROR_CODE"
          developerMessage: "Mensagem descritiva do erro." Essa mensagem deve ser a mesma do `ErrorCode`.
          path: "/api/endpoint"
```

### Paths (paths/)

Organizados por contexto/módulo seguindo a estrutura de controllers:
- `auth/` - Endpoints de autenticação
- `user/` - Endpoints do usuário autenticado
- `admin/` - Endpoints administrativos
- `schedule/` - Endpoints de agendamento
- `modality/`, `priceRule/`, etc. - Endpoints públicos

**Nomenclatura dos arquivos:**
- Para endpoints que retornam coleção e criam recurso: `{Entidades}.yml` (ex: `Modalities.yml`)
- Para endpoints de recurso específico: `{Entidade}ById.yml`
- Para ações específicas: `{Acao}{Entidade}.yml` (ex: `DisableModality.yml`, `CancelReservation.yml`)

## Estrutura de um Endpoint

### Estrutura Básica

```yaml
{metodo}:
  operationId: {nomeOperacao}
  tags: [ "Nome da Tag" ]
  summary: Breve resumo da operação
  description: |
    Descrição detalhada do endpoint.
    
    Pode incluir múltiplas linhas explicando:
    - O que o endpoint faz
    - Quais são os pré-requisitos
    - Regras de negócio importantes
  security:
    - BearerAuth: [ ]  # Apenas para endpoints protegidos
  
  # Se houver parâmetros
  parameters:
    - $ref: "../../components/parameters/ParameterFile.yml#/parameterName"
  
  # Se houver corpo da requisição
  requestBody:
    description: Descrição do corpo da requisição
    required: true
    content:
      application/json:
        schema:
          $ref: "../../components/schemas/path/RequestDto.yml"
  
  responses:
    {statusCode}:
      description: Descrição da resposta
      content:
        application/json:
          schema:
            $ref: "../../components/schemas/path/ResponseDto.yml"
          examples:
            ExampleName:
              summary: Descrição do exemplo
              value:
                # Exemplo completo de resposta
```

### Documentação de Respostas de Sucesso

Para endpoints que retornam dados (GET, POST):

```yaml
200:
  description: |
    Operação bem-sucedida. Descrição detalhada do que é retornado.
  content:
    application/json:
      schema:
        $ref: "../../components/schemas/path/ResponseDto.yml"
      examples:
        SuccessExample:
          summary: Exemplo de sucesso
          value:
            # Dados de exemplo realistas
```

Para endpoints de criação (POST):

```yaml
201:
  description: Recurso criado com sucesso.
  headers:
    Location:
      description: URL do recurso criado
      schema:
        type: string
  content:
    application/json:
      schema:
        $ref: "../../components/schemas/path/ResponseDto.yml"
```

Para endpoints sem retorno (DELETE, PATCH sem corpo):

```yaml
204:
  description: Operação concluída com sucesso. Sem conteúdo no corpo da resposta.
```

### Documentação de Respostas de Erro

**Estrutura padrão para erros de validação (400):**

```yaml
400:
  description: Requisição inválida ou dados de validação incorretos.
  content:
    application/json:
      schema:
        $ref: "../../components/schemas/error/ErrorResponseDto.yml"
      examples:
        ValidationError1:
          summary: Campo obrigatório não informado
          value:
            timestamp: "2024-06-15T14:30:00Z"
            status: 400
            errorCode: "VALIDATION_FAILED"
            developerMessage: "A validação falhou. Verifique os detalhes dos campos para mais informações."
            path: "/api/endpoint"
            fieldErrors:
              - fieldName: "fieldName"
                errorCode: "FIELD_REQUIRED"
                developerMessage: "O campo X é obrigatório."
        ValidationError2:
          summary: Campo com formato inválido
          value:
            timestamp: "2024-06-15T14:30:00Z"
            status: 400
            errorCode: "VALIDATION_FAILED"
            developerMessage: "A validação falhou. Verifique os detalhes dos campos para mais informações."
            path: "/api/endpoint"
            fieldErrors:
              - fieldName: "fieldName"
                errorCode: "FIELD_INVALID_FORMAT"
                developerMessage: "O campo X possui formato inválido."
```

**Para erros de negócio (404, 409):**

```yaml
404:
  description: Recurso não encontrado.
  content:
    application/json:
      schema:
        $ref: "../../components/schemas/error/ErrorResponseDto.yml"
      examples:
        ResourceNotFound:
          summary: Recurso não encontrado
          value:
            timestamp: "2024-06-15T14:30:00Z"
            status: 404
            errorCode: "RESOURCE_NOT_FOUND"
            developerMessage: "O recurso solicitado não foi encontrado."
            path: "/api/endpoint/{id}"
```

**Para erros comuns, usar referências:**

```yaml
401:
  $ref: "../../components/responses/UnauthorizedError.yml"
403:
  $ref: "../../components/responses/ForbiddenError.yml"
429:
  $ref: "../../components/responses/TooManyRequestsError.yml"
500:
  $ref: "../../components/responses/InternalServerError.yml"
```

## Estrutura de um Schema (DTO)

### Request DTO

```yaml
type: object
description: Breve descrição do que representa este DTO.
required:
  - campo1
  - campo2
properties:
  campo1:
    type: string
    description: |
      Descrição detalhada do campo.
      Pode incluir regras de validação e restrições.
    example: "valor_exemplo"
  campo2:
    type: string
    format: uuid
    description: Identificador único do recurso relacionado.
    example: "550e8400-e29b-41d4-a716-446655440002"
  campo3:
    type: string
    format: date
    description: Data no formato ISO 8601 (yyyy-MM-dd).
    example: "2025-11-25"
  campoAninhado:
    $ref: "../path/OutroDto.yml"
```

### Response DTO

```yaml
type: object
description: Representa os dados retornados pela API.
properties:
  id:
    type: string
    format: uuid
    description: Identificador único do recurso.
    example: "cc7a4a42-1666-4fd7-b463-0d911d595d92"
  campo1:
    type: string
    description: Descrição do campo.
    example: "valor"
  createdAt:
    type: string
    format: date-time
    description: Data e hora de criação do recurso (ISO 8601).
    example: "2025-11-28T14:30:00Z"
  campoAninhado:
    $ref: "../path/OutroDto.yml"
```

### DTO Paginado

```yaml
type: object
description: Resposta paginada contendo uma lista de recursos.
properties:
  content:
    type: array
    description: Lista de recursos da página atual.
    items:
      $ref: "./ResourceDto.yml"
  page:
    type: object
    description: Informações sobre a paginação.
    properties:
      size:
        type: integer
        description: Quantidade de itens por página.
        example: 20
      number:
        type: integer
        description: Número da página atual (inicia em 0).
        example: 0
      totalElements:
        type: integer
        description: Total de elementos encontrados.
        example: 100
      totalPages:
        type: integer
        description: Total de páginas disponíveis.
        example: 5
```

## Boas Práticas

### 1. Organização de Arquivos

- **Um arquivo por recurso/operação** nos paths
- **Schemas agrupados por contexto** (auth, user, admin, etc.)
- **Reutilize componentes** sempre que possível usando $ref
- **Mantenha a hierarquia de pastas** consistente com a estrutura do código

### 2. Descrições e Exemplos

- **Descrições claras e objetivas** para cada campo e endpoint
- **Exemplos realistas** que representem dados reais do sistema
- **Documente todos os cenários de erro** com exemplos específicos
- **Use markdown** nas descrições para formatação quando necessário

### 3. Tags e Agrupamento

- **Agrupe endpoints relacionados** usando tags consistentes
- **Padrão de nomenclatura das tags:**
  - "Autenticação - {Subtópico}" para autenticação
  - "Usuário - {Funcionalidade}" para endpoints do usuário
  - "Administração - {Recurso}" para endpoints administrativos
  - "Endpoints Públicos" para endpoints sem autenticação

### 4. Segurança

- **Sempre especifique security** para endpoints protegidos:
  ```yaml
  security:
    - BearerAuth: [ ]
  ```
- **Documente todos os erros de autenticação/autorização** (401, 403)
- **Inclua rate limiting** (429) quando aplicável

### 5. Versionamento e Manutenção

- **Mantenha a documentação sincronizada** com o código
- **Atualize exemplos** quando houver mudanças nos DTOs
- **Documente breaking changes** na descrição da API
- **Use o campo `deprecated: true`** para endpoints obsoletos

## Checklist para Novos Endpoints

Ao documentar um novo endpoint, verifique se:

- [ ] O arquivo path está criado na pasta correta
- [ ] O endpoint está registrado no `openapi.yml`
- [ ] Todos os schemas (Request/Response DTOs) estão criados
- [ ] Os schemas estão registrados no `openapi.yml`
- [ ] Parâmetros reutilizáveis estão em `components/parameters/`
- [ ] As tags estão definidas corretamente
- [ ] Todos os status codes possíveis estão documentados
- [ ] Cada status code tem pelo menos um exemplo
- [ ] Erros de validação incluem `fieldErrors` quando aplicável
- [ ] A segurança (BearerAuth) está configurada se necessário
- [ ] Descrições estão claras e completas
- [ ] Exemplos são realistas e úteis
- [ ] Referências ($ref) estão corretas e funcionando

