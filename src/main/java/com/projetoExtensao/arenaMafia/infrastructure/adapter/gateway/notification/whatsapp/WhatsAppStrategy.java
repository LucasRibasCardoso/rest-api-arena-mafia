package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp;

public interface WhatsAppStrategy extends WhatsAppClient {

  WhatsAppProvider getProvider();
}
