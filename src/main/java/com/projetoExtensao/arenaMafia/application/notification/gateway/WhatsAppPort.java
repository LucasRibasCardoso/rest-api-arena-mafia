package com.projetoExtensao.arenaMafia.application.notification.gateway;

public interface WhatsAppPort {
  void send(String phone, String message);
}
