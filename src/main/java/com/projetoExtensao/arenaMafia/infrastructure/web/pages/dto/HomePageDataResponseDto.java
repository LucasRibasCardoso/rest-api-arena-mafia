package com.projetoExtensao.arenaMafia.infrastructure.web.pages.dto;

import com.projetoExtensao.arenaMafia.infrastructure.web.modality.dto.response.ModalityResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.operatingHours.dto.response.OperatingHoursResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import java.util.List;

public record HomePageDataResponseDto(
        List<OperatingHoursResponseDto> operatingHours,
        List<ModalityResponseDto> modalities,
        List<PriceRuleResponseDto> priceRules
) {}
