# Plan: Refatoração de Desativação de Quadra com Preview e Confirmação

A abordagem de preview + confirmação para desativar quadras é **excelente e consistente** com o padrão já estabelecido para BlockedTime. Quanto à migração de reservas entre quadras, você está correto: seria demasiadamente complexo e não escalável, especialmente considerando horários de funcionamento diferentes, modalidades suportadas e disponibilidade.

## Steps

### 1. Criar DTOs de request/response para preview e confirmação de desativação de quadra

Criar os seguintes DTOs na camada de infraestrutura:

**`CourtDisablePreviewRequestDto.java`** em `infrastructure/web/admin/dto/court/request/`:
- Contém apenas `courtId` (UUID)
- Validações: `@NotNull` para courtId

**`CourtDisablePreviewResponseDto.java`** em `infrastructure/web/admin/dto/court/response/`:
- Reutilizar estrutura semelhante a `BlockedTimeConflictsPreviewResponseDto`
- Campos: `previewKey`, `courtId`, `courtName`, `usersAffected`, `reservationsAffected`, `blockedTimesAffected`
- Listas: `conflictingReservations` (List<ReservationDetail>), `conflictingBlockedTimes` (List<BlockedTimeDetail>), `inProgressReservations` (List<ReservationDetail>)

**`CourtDisableConfirmRequestDto.java`** em `infrastructure/web/admin/dto/court/request/`:
- Campos: `previewKey` (String) e `cancellationReason` (String)
- Validações: ambos `@NotBlank`

### 2. Criar preview domain model e cache port para desativação de quadra

**`CourtDisablePreview.java`** em `application/court/preview/`:
```java
public record CourtDisablePreview(
    String previewKey,
    UUID courtId,
    int usersAffected,
    int reservationsAffected,
    int blockedTimesAffected,
    List<ReservationDetail> conflictingReservations,
    List<BlockedTimeDetail> conflictingBlockedTimes,
    List<ReservationDetail> inProgressReservations
) {}
```

**`CourtDisablePreviewCachePort.java`** em `application/court/port/gateway/`:
- Seguir o padrão de `BlockedTimePreviewCachePort`
- Métodos: `generateKey(UUID adminId)`, `save(String key, CourtDisablePreview preview)`, `getPreviewOrElseThrow(String key, UUID adminId)`, `delete(String key)`, `validateKeyOwnership(String key, UUID adminId)`

### 3. Implementar casos de uso PreviewCourtDisable e ConfirmCourtDisable

**`PreviewCourtDisableUseCase.java`** e **`PreviewCourtDisableUseCaseImp.java`** em `application/court/usecase/`:

Lógica do preview:
1. Validar que a quadra existe e está ativa (`courtRepository.findByIdOrElseThrow()`)
2. Buscar todas as reservas confirmadas futuras para o `courtId` (a partir de `LocalDate.now()`)
3. Buscar todos os BlockedTimes futuros para o `courtId`
4. Enriquecer os dados com `ScheduleEntryEnrichmentService.enrichScheduleEntries()`
5. Separar reservas em andamento das demais
6. Calcular estatísticas (usuários afetados, quantidade de reservas e bloqueios)
7. Criar `CourtDisablePreview` e salvar no cache
8. Retornar o preview

**`ConfirmCourtDisableUseCase.java`** e **`ConfirmCourtDisableUseCaseImp.java`** em `application/court/usecase/`:

Lógica da confirmação:
1. Recuperar preview do cache via `previewKey`
2. Validar ownership (adminId)
3. Validar que a quadra ainda existe e está ativa
4. Validar staleness: buscar conflitos atuais e comparar quantidade com o preview
5. Cancelar reservas conflitantes (exceto in-progress) usando `ReservationBatchCancellationService`
6. Deletar BlockedTimes conflitantes em batch
7. Desativar a quadra (`court.disable()` e `courtRepository.save()`)
8. Limpar preview do cache
9. Retornar resultado com estatísticas

### 4. Criar adapter de cache para preview de desativação de quadra

**`CourtDisablePreviewCacheAdapter.java`** em `infrastructure/adapter/gateway/`:

Implementação seguindo exatamente o padrão de `BlockedTimePreviewCacheAdapter`:
- Chave Redis: `court-disable-preview:{adminId}:{UUID.randomUUID()}`
- TTL: 15 minutos (mesmo padrão)
- Validação de ownership do adminId
- Serialização/deserialização JSON usando ObjectMapper
- Tratamento de exceções: `PreviewNotFoundException`, `PreviewKeyOwnershipException`
- Garantir que apenas um preview por admin existe por vez (deletar preview anterior ao criar novo)

### 5. Criar endpoints REST no AdminCourtController

Adicionar dois novos endpoints:

**`POST /api/admin/courts/{courtId}/disable/preview`**:
- Path variable: `courtId`
- Retorna: `CourtDisablePreviewResponseDto` (200 OK)
- Chama: `PreviewCourtDisableUseCase.execute(courtId, adminId)`
- Erros possíveis:
  - 404: Quadra não encontrada
  - 409: Quadra já está desativada

**`POST /api/admin/courts/disable/confirm`**:
- Request body: `CourtDisableConfirmRequestDto`
- Retorna: 204 No Content
- Chama: `ConfirmCourtDisableUseCase.execute(request, adminId)`
- Erros possíveis:
  - 400: Validação falhou (previewKey ou cancellationReason vazios)
  - 404: Preview não encontrado ou quadra não existe
  - 403: Preview pertence a outro admin
  - 409: Preview está obsoleto (staleness) ou quadra já desativada
  - 500: Falha no cancelamento em lote

### 6. Implementar testes de integração completos

**`AdminCourtDisableIntegrationTest.java`** em `test/java/.../integration/controller/admin/`:

Estrutura organizada com `@Nested`:

**Preview Scenarios:**
- Success:
  - Preview sem conflitos (quadra sem reservas futuras)
  - Preview com múltiplas reservas conflitantes
  - Preview com BlockedTimes conflitantes
  - Preview com reservas em andamento (devem ser separadas)
  - Preview com mix de reservas e BlockedTimes
  
- Error 404:
  - Quadra não existe
  
- Error 409:
  - Quadra já está desativada

**Confirm Scenarios:**
- Success:
  - Confirmação bem-sucedida cancela todas as reservas e BlockedTimes
  - Confirmação com reservas in-progress não as cancela
  - Verificar que notificações são enviadas aos usuários
  - Verificar que quadra está desativada após confirmação
  
- Error 400:
  - PreviewKey vazio
  - CancellationReason vazio
  
- Error 404:
  - Preview não existe (chave inválida)
  - Quadra foi deletada entre preview e confirmação
  
- Error 403:
  - Preview pertence a outro admin
  
- Error 409:
  - Preview obsoleto (nova reserva criada entre preview e confirmação)
  - Quadra já foi desativada entre preview e confirmação

**Verificações nos testes:**
- Preview salvo corretamente no cache (verificar com `getPreviewSavedFromCache()`)
- Reservas canceladas com status `CANCELLED`
- BlockedTimes deletados do banco
- Quadra com `active = false`
- Eventos de notificação publicados
- Cache limpo após confirmação

## Further Considerations

### 1. Mensagem de cancelamento para usuários

A mensagem "Sua reserva foi cancelada pois a quadra X foi desativada" é adequada e clara. Usuários entendem que infraestrutura pode ser desativada (manutenção, reforma, remoção permanente). 

**Alternativa sugerida**: Incluir o campo `cancellationReason` do `CourtDisableConfirmRequestDto` na mensagem de notificação, permitindo que o admin personalize:
- "Quadra desativada para reforma"
- "Quadra removida permanentemente"
- "Quadra em manutenção por tempo indeterminado"

Isso melhora a experiência do usuário ao fornecer contexto específico.

### 2. Validação de staleness mais rigorosa

Considere implementar validação que além de contar conflitos, também verifica:
- Se há novos conflitos que não existiam no preview
- Se algum conflito do preview foi removido
- Se a quadra teve suas modalidades alteradas

Isso garante que o admin sempre veja o estado mais recente e completo antes de desativar a quadra.

**Implementação sugerida**:
```java
private void validatePreviewIsNotStale(CourtDisablePreview preview, UUID courtId) {
    List<ScheduleEntry> currentConflicts = findAllFutureConflicts(courtId);
    
    int previewTotal = preview.conflictingReservations().size() 
                     + preview.conflictingBlockedTimes().size()
                     + preview.inProgressReservations().size();
    
    if (currentConflicts.size() != previewTotal) {
        throw new StalePreviewException();
    }
    
    // Validação adicional: verificar IDs específicos
    Set<UUID> previewIds = extractAllIds(preview);
    Set<UUID> currentIds = currentConflicts.stream()
        .map(ScheduleEntry::getId)
        .collect(Collectors.toSet());
    
    if (!previewIds.equals(currentIds)) {
        throw new StalePreviewException();
    }
}
```

### 3. Documentação OpenAPI

Após a implementação, documentar os dois novos endpoints em `src/main/resources/static/docs/`:

**Estrutura de arquivos**:
- `paths/admin/courts/DisableCourtPreview.yml`
- `paths/admin/courts/DisableCourtConfirm.yml`
- `components/schemas/court/CourtDisablePreviewRequestDto.yml`
- `components/schemas/court/CourtDisablePreviewResponseDto.yml`
- `components/schemas/court/CourtDisableConfirmRequestDto.yml`

**Incluir**:
- Descrições detalhadas de cada endpoint
- Exemplos de request e response
- Todos os possíveis códigos de erro (400, 403, 404, 409, 500)
- Exemplos de cada cenário de erro
- Anotação de segurança (BearerAuth)
- Tags apropriadas ("Administração - Quadras")

### 4. Considerações de performance

Para quadras com muitas reservas futuras (cenário raro mas possível):
- Implementar paginação no preview se necessário
- Considerar processamento assíncrono para o cancelamento
- Monitorar tempo de execução do `ConfirmCourtDisableUseCase`

### 5. Logs e auditoria

Adicionar logs estratégicos:
- Preview gerado: courtId, adminId, quantidade de conflitos
- Confirmação iniciada: previewKey, adminId
- Reservas canceladas: quantidade, IDs
- BlockedTimes deletados: quantidade, IDs
- Quadra desativada: courtId, courtName, adminId, timestamp

Considerar criar uma tabela de auditoria para registrar desativações de quadra.

## Cenários de Uso

### Cenário 1: Quadra sem conflitos
Admin desativa quadra que não possui reservas futuras nem BlockedTimes.
- Preview retorna listas vazias
- Confirmação apenas desativa a quadra

### Cenário 2: Quadra com reservas futuras
Admin desativa quadra com 10 reservas nos próximos 30 dias.
- Preview mostra todas as 10 reservas com detalhes dos usuários
- Confirmação cancela as 10 reservas e notifica todos os usuários

### Cenário 3: Quadra com reserva em andamento
Admin tenta desativar quadra às 14:30, há uma reserva das 14:00-15:00.
- Preview separa essa reserva em `inProgressReservations`
- Confirmação NÃO cancela essa reserva, mas desativa a quadra

### Cenário 4: Preview obsoleto
Admin gera preview, outro admin cria nova reserva, primeiro admin confirma.
- Validação de staleness detecta conflito novo
- Sistema lança `StalePreviewException` (409)
- Admin precisa gerar novo preview

### Cenário 5: Múltiplos admins tentando desativar mesma quadra
Admin A gera preview, Admin B gera preview (sobrescreve o de A no cache).
- Quando Admin A tenta confirmar com sua chave antiga
- Sistema lança `PreviewNotFoundException` (404)
- Admin A precisa gerar novo preview

## Estrutura de Pastas Resultante

```
application/court/
├── port/
│   ├── gateway/
│   │   └── CourtDisablePreviewCachePort.java (NOVO)
│   └── CourtRepositoryPort.java
├── preview/
│   └── CourtDisablePreview.java (NOVO)
├── usecase/
│   ├── PreviewCourtDisableUseCase.java (NOVO)
│   ├── ConfirmCourtDisableUseCase.java (NOVO)
│   ├── DisableCourtUseCase.java (DEPRECATED - manter para compatibilidade)
│   └── imp/
│       ├── PreviewCourtDisableUseCaseImp.java (NOVO)
│       ├── ConfirmCourtDisableUseCaseImp.java (NOVO)
│       └── DisableCourtUseCaseImp.java (marcar como deprecated)

infrastructure/adapter/gateway/
└── CourtDisablePreviewCacheAdapter.java (NOVO)

infrastructure/web/admin/dto/court/
├── request/
│   ├── CourtDisablePreviewRequestDto.java (NOVO)
│   └── CourtDisableConfirmRequestDto.java (NOVO)
└── response/
    └── CourtDisablePreviewResponseDto.java (NOVO)

test/java/.../integration/controller/admin/
└── AdminCourtDisableIntegrationTest.java (NOVO)
```

## Próximos Passos

1. Implementar os DTOs de request/response
2. Criar o domain model `CourtDisablePreview`
3. Criar a porta `CourtDisablePreviewCachePort` e adapter
4. Implementar os casos de uso (preview e confirm)
5. Adicionar endpoints no controller
6. Escrever testes de integração
7. Documentar no OpenAPI
8. Deprecar o `DisableCourtUseCase` antigo (manter para compatibilidade temporária)
9. Atualizar documentação do projeto

## ErrorCodes Necessários

Adicionar ao enum `ErrorCode`:

```java
// Court Disable Preview/Confirm
COURT_DISABLE_PREVIEW_NOT_FOUND("Preview de desativação de quadra não encontrado."),
COURT_DISABLE_PREVIEW_OWNERSHIP_INVALID("Este preview pertence a outro administrador."),
COURT_DISABLE_PREVIEW_STALE("O preview está desatualizado. Novas reservas foram criadas. Gere um novo preview."),
COURT_ALREADY_DISABLED("A quadra já está desativada."),
```

## Validações Importantes

1. **Quadra existe e está ativa** - tanto no preview quanto na confirmação
2. **Preview pertence ao admin** - validação de ownership
3. **Preview não está obsoleto** - validação de staleness
4. **Reservas in-progress não são canceladas** - filtro específico
5. **Operação é atômica** - se falhar cancelamento de uma reserva, rollback de tudo

