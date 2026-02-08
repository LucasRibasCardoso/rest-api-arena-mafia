package com.projetoExtensao.arenaMafia.infrastructure.config.dataConfig;

import com.projetoExtensao.arenaMafia.application.security.port.gateway.PasswordEncoderPort;
import com.projetoExtensao.arenaMafia.application.user.port.repository.UserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.SystemUserNotFoundException;
import com.projetoExtensao.arenaMafia.domain.model.User;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SystemUserConfig implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(SystemUserConfig.class);

  private final UserRepositoryPort userRepositoryPort;
  private final PasswordEncoderPort passwordEncoderPort;

  public SystemUserConfig(
      UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
    this.userRepositoryPort = userRepositoryPort;
    this.passwordEncoderPort = passwordEncoderPort;
  }

  @Override
  public void run(String... args) {

    try {
      User systemUser = userRepositoryPort.findSystemUserOrElseThrow();
      logger.info("System User encontrado: ID [{}]", systemUser.getId());
    }
    catch (SystemUserNotFoundException e) {
      String hash = passwordEncoderPort.encode(UUID.randomUUID().toString());
      User newGhostUser = User.createSystemUser(hash);
      userRepositoryPort.save(newGhostUser);

      logger.info("System User criado com sucesso: ID [{}]", newGhostUser.getId());
    }
    catch (Exception e) {
      logger.error("Erro ao verificar/criar usuário", e);
      throw new IllegalStateException("Falha ao inicializar usuário", e);
    }
  }
}
