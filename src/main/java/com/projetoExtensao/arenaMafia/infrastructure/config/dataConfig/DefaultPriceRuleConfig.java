package com.projetoExtensao.arenaMafia.infrastructure.config.dataConfig;

import com.projetoExtensao.arenaMafia.application.priceRule.ports.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração responsável por garantir que existe uma regra de preço padrão no sistema. A regra
 * padrão é aplicável a todos os dias e horários quando nenhuma outra regra específica se aplica.
 */
@Configuration
public class DefaultPriceRuleConfig implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DefaultPriceRuleConfig.class);
  private static final BigDecimal DEFAULT_PRICE = new BigDecimal("50.00");

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public DefaultPriceRuleConfig(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  /**
   * Verifica se existe uma regra de preço padrão no banco de dados. Se não existir, cria uma nova
   * regra de preço padrão com um valor predefinido. Dessa forma, garante que sempre haverá uma
   * regra de preço padrão disponível para uso na aplicação.
   *
   * @param args incoming main method arguments
   */
  @Override
  public void run(String... args) {
    try {
      priceRuleRepositoryPort
          .findDefaultRule()
          .ifPresentOrElse(
              existingRule ->
                  log.info(
                      "Regra de preço padrão já existe: {} - R$ {}",
                      existingRule.getName(),
                      existingRule.getPrice()),
              this::createDefaultPriceRule);
    } catch (Exception e) {
      log.error("Erro ao verificar/criar regra de preço padrão", e);
      throw new IllegalStateException("Falha ao inicializar regra de preço padrão do sistema", e);
    }
  }

  private void createDefaultPriceRule() {
    PriceRule defaultPriceRule = PriceRule.createDefault(DEFAULT_PRICE);
    priceRuleRepositoryPort.save(defaultPriceRule);
    log.info(
        "Regra de preço padrão criada com sucesso: {} (ID: {})",
        defaultPriceRule.getName(),
        defaultPriceRule.getId());
  }
}
