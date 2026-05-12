package com.projetoExtensao.arenaMafia.infrastructure.web.priceRule;

import com.projetoExtensao.arenaMafia.application.priceRule.usecase.FindAllPriceRuleUseCase;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.PriceRuleMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/price-rules")
public class PriceRuleController {

  private final PriceRuleMapper priceRuleMapper;
  private final FindAllPriceRuleUseCase findAllPriceRuleUseCase;

  public PriceRuleController(
      PriceRuleMapper priceRuleMapper, FindAllPriceRuleUseCase findAllPriceRuleUseCase) {
    this.priceRuleMapper = priceRuleMapper;
    this.findAllPriceRuleUseCase = findAllPriceRuleUseCase;
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<PriceRuleResponseDto>> getAllPriceRules() {
    boolean isActive = true;
    List<PriceRuleResponseDto> response =
        findAllPriceRuleUseCase.execute(isActive).stream().map(priceRuleMapper::toDto).toList();
    return ResponseEntity.ok(response);
  }
}
