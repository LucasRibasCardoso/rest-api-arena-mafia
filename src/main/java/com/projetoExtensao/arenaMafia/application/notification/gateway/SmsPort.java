package com.projetoExtensao.arenaMafia.application.notification.gateway;

public interface SmsPort {
  void sendMessage(String phone, String message);
}
