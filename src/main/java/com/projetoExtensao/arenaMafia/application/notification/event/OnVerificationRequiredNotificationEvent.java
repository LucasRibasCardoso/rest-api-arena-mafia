package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.Optional;

public record OnVerificationRequiredNotificationEvent(User user, String targetPhone) {

  public OnVerificationRequiredNotificationEvent(User user) {
    this(user, null);
  }

  public String getRecipientPhone() {
    return Optional.ofNullable(targetPhone).orElse(user.getPhone());
  }
}
