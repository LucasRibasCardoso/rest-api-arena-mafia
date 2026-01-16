package com.projetoExtensao.arenaMafia.infrastructure.web.agenda.dto.response.enums;

public enum AgendaSlotType {

  /** Horário disponível para reserva */
  AVAILABLE,

  /** Horário já reservado (não mostra detalhes do usuário) */
  RESERVED,

  /** Horário bloqueado pela administração */
  BLOCKED_TIME
}
