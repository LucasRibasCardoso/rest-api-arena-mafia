package com.projetoExtensao.arenaMafia.application.court.aggregate;

import com.projetoExtensao.arenaMafia.domain.model.Court;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import java.util.List;

public record CourtWithModalities(Court court, List<Modality> modalities) {}
