package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway.notification.whatsapp;

import com.projetoExtensao.arenaMafia.application.notification.gateway.WhatsAppPort;

public interface WhatsAppStrategy extends WhatsAppPort {

    WhatsAppProvider getProvider();
}
