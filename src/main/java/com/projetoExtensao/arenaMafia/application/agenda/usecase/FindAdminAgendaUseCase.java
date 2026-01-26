package com.projetoExtensao.arenaMafia.application.agenda.usecase;

import com.projetoExtensao.arenaMafia.domain.model.agenda.admin.AdminAgendaItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FindAdminAgendaUseCase {
  List<AdminAgendaItem> execute(LocalDate date, Optional<UUID> courtId);
}
