package com.projetoExtensao.arenaMafia.application.notification.event;

import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.Optional;

public record OnVerificationRequiredEvent(User user, String targetPhone) {

  public OnVerificationRequiredEvent(User user) {
    this(user, null);
  }

  public String getRecipientPhone() {
    return Optional.ofNullable(targetPhone).orElse(user.getPhone());
  }
}
