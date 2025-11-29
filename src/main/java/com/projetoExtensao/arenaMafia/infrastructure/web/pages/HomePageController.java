package com.projetoExtensao.arenaMafia.infrastructure.web.pages;

import com.projetoExtensao.arenaMafia.application.modality.usecase.FindAllModalitiesUseCase;
import com.projetoExtensao.arenaMafia.application.operatingHours.usecase.FindAllOperatingHoursUseCase;
import com.projetoExtensao.arenaMafia.application.priceRule.usecase.FindAllPriceRuleUseCase;
import com.projetoExtensao.arenaMafia.domain.model.Modality;
import com.projetoExtensao.arenaMafia.domain.model.OperatingHours;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.ModalityMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.PriceRuleMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.pages.dto.HomePageDataResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/homepage")
public class HomePageController {

  private final PriceRuleMapper priceRuleMapper;
  private final OperatingHoursMapper operatingHoursMapper;
  private final ModalityMapper modalityMapper;
  private final FindAllPriceRuleUseCase findAllPriceRuleUseCase;
  private final FindAllOperatingHoursUseCase findAllOperatingHoursUseCase;
  private final FindAllModalitiesUseCase findAllModalitiesUseCase;

  public HomePageController(
      PriceRuleMapper priceRuleMapper,
      OperatingHoursMapper operatingHoursMapper,
      ModalityMapper modalityMapper,
      FindAllPriceRuleUseCase findAllPriceRuleUseCase,
      FindAllOperatingHoursUseCase findAllOperatingHoursUseCase,
      FindAllModalitiesUseCase findAllModalitiesUseCase) {
    this.priceRuleMapper = priceRuleMapper;
    this.operatingHoursMapper = operatingHoursMapper;
    this.modalityMapper = modalityMapper;
    this.findAllPriceRuleUseCase = findAllPriceRuleUseCase;
    this.findAllOperatingHoursUseCase = findAllOperatingHoursUseCase;
    this.findAllModalitiesUseCase = findAllModalitiesUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<HomePageDataResponseDto> getAllData() {
    boolean filterByActive = true;
    List<Modality> modalities = findAllModalitiesUseCase.execute(filterByActive);
    List<OperatingHours> operatingHours = findAllOperatingHoursUseCase.execute(filterByActive);
    List<PriceRule> priceRules = findAllPriceRuleUseCase.execute(filterByActive);

    var response =
        new HomePageDataResponseDto(
            operatingHours.stream().map(operatingHoursMapper::toDto).toList(),
            modalities.stream().map(modalityMapper::toDto).toList(),
            priceRules.stream().map(priceRuleMapper::toDto).toList());

    return ResponseEntity.ok(response);
  }
}
