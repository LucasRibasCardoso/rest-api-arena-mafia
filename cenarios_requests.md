### 1. Bloqueio Pontual (Dia Inteiro)

**Cenário:** Manutenção de emergência ou feriado específico em uma quadra.

* `startDate` é igual a `endDate`.
* `isFullDay` é `true` (o `timeInterval` pode ser omitido ou null).
* `selectedDaysOfWeek` é omitido (não faz diferença em dia único).

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2025-11-15",
  "endDate": "2025-11-15",
  "isFullDay": true,
  "timeInterval": null,
  "selectedDaysOfWeek": null
}

```

### 2. Bloqueio Pontual (Horário Parcial)

**Cenário:** Um evento privado que ocupa apenas parte do dia.

* `startDate` é igual a `endDate`.
* `isFullDay` é `false` (obriga o envio de `timeInterval`).

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2025-11-20",
  "endDate": "2025-11-20",
  "isFullDay": false,
  "timeInterval": {
    "start": "18:00:00",
    "end": "22:00:00"
  },
  "selectedDaysOfWeek": null
}

```

### 3. Bloqueio Consecutivo (Período Longo / "Férias")

**Cenário:** Reforma da quadra que vai durar 10 dias seguidos (incluindo fins de semana).

* `startDate` diferente de `endDate`.
* `selectedDaysOfWeek` é `null` ou vazio (implica bloquear **todos** os dias dentro do intervalo).
* `isFullDay` é `true`.

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2025-12-01",
  "endDate": "2025-12-10",
  "isFullDay": true,
  "timeInterval": null,
  "selectedDaysOfWeek": null
}

```

### 4. Bloqueio Consecutivo Parcial (Manutenção Diária)

**Cenário:** Durante uma semana, a quadra ficará fechada todas as manhãs para limpeza, mas abre à tarde.

* Intervalo de datas.
* `isFullDay` é `false`.
* `selectedDaysOfWeek` é `null` (aplica-se a todos os dias do período).

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2025-11-01",
  "endDate": "2025-11-07",
  "isFullDay": false,
  "timeInterval": {
    "start": "08:00:00",
    "end": "12:00:00"
  },
  "selectedDaysOfWeek": null
}

```

### 5. Bloqueio Recorrente Semanal (Aulas / Treinos)

**Cenário:** Aulas de Vôlei todas as Terças e Quintas, das 19h às 21h, durante o semestre.

* Intervalo de datas longo.
* `isFullDay` é `false`.
* `selectedDaysOfWeek` preenchido (bloqueia apenas nos dias especificados).

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2026-02-01",
  "endDate": "2026-06-30",
  "isFullDay": false,
  "timeInterval": {
    "start": "19:00:00",
    "end": "21:00:00"
  },
  "selectedDaysOfWeek": [
    "TUESDAY",
    "THURSDAY"
  ]
}

```

### 6. Bloqueio Recorrente de Dia Inteiro

**Cenário:** A quadra não abre aos Domingos durante o verão.

* Intervalo de datas.
* `isFullDay` é `true`.
* `selectedDaysOfWeek` contém apenas DOMINGO.

```json
{
  "courtIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"],
  "startDate": "2026-01-01",
  "endDate": "2026-03-31",
  "isFullDay": true,
  "timeInterval": null,
  "selectedDaysOfWeek": [
    "SUNDAY"
  ]
}

```

### 7. Bloqueio em Múltiplas Quadras (Lote)

**Cenário:** Um torneio que vai ocupar 3 quadras simultaneamente no fim de semana.

* Lista `courtIds` com múltiplos UUIDs.

```json
{
  "courtIds": [
    "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
    "b1ffcd88-8d0a-3de7-aa5c-5cc8bd270b22",
    "c2eedf77-7e0f-2cd6-bb4b-4bb7bd160c33"
  ],
  "startDate": "2025-11-22",
  "endDate": "2025-11-23",
  "isFullDay": true,
  "timeInterval": null,
  "selectedDaysOfWeek": [
    "SATURDAY",
    "SUNDAY"
  ]
}

```