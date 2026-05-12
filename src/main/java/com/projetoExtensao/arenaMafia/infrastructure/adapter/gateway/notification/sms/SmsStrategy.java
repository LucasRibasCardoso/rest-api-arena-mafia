package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms;

public interface SmsStrategy extends SmsClient {

  SmsProvider getProvider();
}
