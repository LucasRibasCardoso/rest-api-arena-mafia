package com.projetoExtensao.arenaMafia.application.modality.usecase;

import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.UUID;

public interface UpdateModalityUseCase {

  Modality execute(UUID id, String newName);
}
