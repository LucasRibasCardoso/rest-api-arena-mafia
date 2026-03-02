package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp;

public interface WhatsAppClient {
  void sendMessage(String phone, String message);
}
