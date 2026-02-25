package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.sms;

public interface SmsClient {
  void sendMessage(String phone, String message);
}
