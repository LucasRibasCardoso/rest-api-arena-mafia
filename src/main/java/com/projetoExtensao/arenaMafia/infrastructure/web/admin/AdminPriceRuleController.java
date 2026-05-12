package com.projetoExtensao.arenaMafia.infrastructure.web.admin;

import com.projetoExtensao.arenaMafia.application.priceRule.usecase.*;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.PriceRuleMapper;
import com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit.CustomRateLimiter;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.CreatePriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.UpdateDefaultPriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.admin.dto.priceRule.request.UpdatePriceRuleRequestDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.priceRule.dto.response.PriceRuleResponseDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/price-rules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPriceRuleController {

  private final PriceRuleMapper priceRuleMapper;
  private final CreatePriceRuleUseCase createPriceRuleUseCase;
  private final EnablePriceRuleUseCase enablePriceRuleUseCase;
  private final DisablePriceRuleUseCase disablePriceRuleUseCase;
  private final UpdatePriceRuleUseCase updatePriceRuleUseCase;
  private final UpdateDefaultPriceRuleUseCase updateDefaultPriceRuleUseCase;
  private final FindPriceRuleByIdUseCase findPriceRuleByIdUseCase;
  private final FindAllPriceRuleUseCase findAllPriceRuleUseCase;

  public AdminPriceRuleController(
      PriceRuleMapper priceRuleMapper,
      CreatePriceRuleUseCase createPriceRuleUseCase,
      EnablePriceRuleUseCase enablePriceRuleUseCase,
      DisablePriceRuleUseCase disablePriceRuleUseCase,
      UpdatePriceRuleUseCase updatePriceRuleUseCase,
      UpdateDefaultPriceRuleUseCase updateDefaultPriceRuleUseCase,
      FindPriceRuleByIdUseCase findPriceRuleByIdUseCase,
      FindAllPriceRuleUseCase findAllPriceRuleUseCase) {
    this.priceRuleMapper = priceRuleMapper;
    this.createPriceRuleUseCase = createPriceRuleUseCase;
    this.enablePriceRuleUseCase = enablePriceRuleUseCase;
    this.disablePriceRuleUseCase = disablePriceRuleUseCase;
    this.updatePriceRuleUseCase = updatePriceRuleUseCase;
    this.findPriceRuleByIdUseCase = findPriceRuleByIdUseCase;
    this.findAllPriceRuleUseCase = findAllPriceRuleUseCase;
    this.updateDefaultPriceRuleUseCase = updateDefaultPriceRuleUseCase;
  }

  @PostMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<PriceRuleResponseDto> createPriceRule(
      @RequestBody @Valid CreatePriceRuleRequestDto request) {

    PriceRule priceRule = createPriceRuleUseCase.execute(request);
    PriceRuleResponseDto response = priceRuleMapper.toDto(priceRule);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(priceRule.getId())
            .toUri();

    return ResponseEntity.created(location).body(response);
  }

  @PatchMapping("/{ruleId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<PriceRuleResponseDto> updatePriceRule(
      @PathVariable UUID ruleId, @RequestBody @Valid UpdatePriceRuleRequestDto request) {
    PriceRule updatedPriceRule = updatePriceRuleUseCase.execute(ruleId, request);
    PriceRuleResponseDto response = priceRuleMapper.toDto(updatedPriceRule);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/default")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<PriceRuleResponseDto> updateDefaultPriceRule(
      @RequestBody @Valid UpdateDefaultPriceRuleRequestDto request) {
    PriceRule updatedPriceRule = updateDefaultPriceRuleUseCase.execute(request);
    PriceRuleResponseDto response = priceRuleMapper.toDto(updatedPriceRule);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{ruleId}/enable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> enablePriceRule(@PathVariable UUID ruleId) {
    enablePriceRuleUseCase.execute(ruleId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{ruleId}/disable")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<Void> disablePriceRule(@PathVariable UUID ruleId) {
    disablePriceRuleUseCase.execute(ruleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<List<PriceRuleResponseDto>> getAllPriceRule(
      @RequestParam(required = false) Boolean isActive) {
    List<PriceRule> priceRules = findAllPriceRuleUseCase.execute(isActive);
    List<PriceRuleResponseDto> response = priceRules.stream().map(priceRuleMapper::toDto).toList();
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{ruleId}")
  @CustomRateLimiter(limiterName = "globalLimiter")
  public ResponseEntity<PriceRuleResponseDto> getPriceRuleDetails(@PathVariable UUID ruleId) {
    PriceRule priceRule = findPriceRuleByIdUseCase.execute(ruleId);
    PriceRuleResponseDto response = priceRuleMapper.toDto(priceRule);
    return ResponseEntity.ok(response);
  }
}
