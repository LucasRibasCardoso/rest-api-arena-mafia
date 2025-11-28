package com.projetoExtensao.arenaMafia.application.priceRule.service;

import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import com.projetoExtensao.arenaMafia.domain.model.enums.DayOfWeek;
import com.projetoExtensao.arenaMafia.domain.valueobjects.TimeInterval;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PriceCalculatorService {

  /**
   * Calcula o preço de um slot de tempo específico baseado nas regras de preço ativas.
   *
   * <p>A lógica de cálculo: 1. Filtra as regras de preço aplicáveis ao dia da semana e horário 2.
   * Seleciona a regra com maior prioridade 3. Retorna o preço da regra selecionada 4. Retorna
   * BigDecimal.ZERO se nenhuma regra for aplicável
   *
   * @param slot intervalo de tempo do slot
   * @param priceRules lista de regras de preço ativas
   * @param dayOfWeek dia da semana
   * @return preço calculado
   */
  public BigDecimal calculateSlotPrice(
      TimeInterval slot, List<PriceRule> priceRules, DayOfWeek dayOfWeek) {

    return priceRules.stream()
        .filter(rule -> rule.isApplicable(dayOfWeek, slot.startTime()))
        .max(Comparator.comparingInt(PriceRule::getPriority))
        .map(PriceRule::getPrice)
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Calcula o preço para um TimeInterval e data específicos.
   *
   * @param timeInterval intervalo de tempo
   * @param date data da reserva
   * @param priceRules lista de regras de preço ativas
   * @return preço calculado
   */
  public BigDecimal calculatePrice(
      TimeInterval timeInterval, LocalDate date, List<PriceRule> priceRules) {

    DayOfWeek dayOfWeek = DayOfWeek.convertToDayOfWeek(date);
    return calculateSlotPrice(timeInterval, priceRules, dayOfWeek);
  }
}
