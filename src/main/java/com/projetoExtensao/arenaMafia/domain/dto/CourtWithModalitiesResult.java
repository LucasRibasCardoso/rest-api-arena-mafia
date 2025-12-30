package com.projetoExtensao.arenaMafia.domain.dto;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;

public record CourtWithModalitiesResult(Court court, List<Modality> modalities) {}
