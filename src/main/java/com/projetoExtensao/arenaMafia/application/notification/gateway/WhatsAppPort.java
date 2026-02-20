package com.projetoExtensao.arenaMafia.application.notification.gateway;

public interface WhatsAppPort {
  void sendMessage(String phone, String message);
}
