package com.projetoExtensao.arenaMafia.application.schedule.usecase;

import com.projetoExtensao.arenaMafia.domain.valueobjects.AvailableSlot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FindAllAvailableTimesUseCase {
  List<AvailableSlot> execute(UUID modalityId, LocalDate date);
}
