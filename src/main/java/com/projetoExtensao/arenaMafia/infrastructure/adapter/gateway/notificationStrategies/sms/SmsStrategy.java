package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notificationStrategies.sms;

import com.projetoExtensao.arenaMafia.application.notification.gateway.SmsPort;

public interface SmsStrategy extends SmsPort {

  SmsProvider getProvider();
}
