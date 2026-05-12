package com.projetoExtensao.arenaMafia.application.notification.gateway;

public interface NotificationPort {

  void sendSms(String phone, String content);

  void sendWhatsappMessage(String phone, String content);
}
