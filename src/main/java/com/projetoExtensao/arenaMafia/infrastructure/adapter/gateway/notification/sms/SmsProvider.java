package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms;

/**
 * Enum que define os provedores de SMS disponíveis na aplicação. Utilizado para alternar entre
 * diferentes APIs de envio de SMS via configuração.
 */
public enum SmsProvider {
  MOCK,
  COMTELE
}
