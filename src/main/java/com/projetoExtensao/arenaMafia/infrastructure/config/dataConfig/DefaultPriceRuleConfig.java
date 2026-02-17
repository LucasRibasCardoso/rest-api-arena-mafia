package com.projetoExtensao.arenaMafia.infrastructure.config.dataConfig;

import com.projetoExtensao.arenaMafia.application.priceRule.port.PriceRuleRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.DefaultPriceRuleNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.PriceRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultPriceRuleConfig implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DefaultPriceRuleConfig.class);

  private final PriceRuleRepositoryPort priceRuleRepositoryPort;

  public DefaultPriceRuleConfig(PriceRuleRepositoryPort priceRuleRepositoryPort) {
    this.priceRuleRepositoryPort = priceRuleRepositoryPort;
  }

  @Override
  public void run(String... args) {
    try {
      PriceRule priceRule = priceRuleRepositoryPort.findDefaultRuleOrElseThrow();
      log.info("Regra de preço padrão encontrada: [{}] - [R$ {}]", priceRule.getName(), priceRule.getPrice());
    }
    catch (DefaultPriceRuleNotFoundException e) {
      PriceRule priceRule = PriceRule.createDefault();
      priceRuleRepositoryPort.save(priceRule);
      log.info("Regra de preço padrão criada com sucesso: [{}] - [R$ {}]", priceRule.getName(), priceRule.getPrice());
    }

    catch (Exception e) {
      log.error("Erro ao verificar/criar regra de preço padrão", e);
      throw new IllegalStateException("Falha ao inicializar regra de preço padrão do sistema", e);
    }
  }
}
