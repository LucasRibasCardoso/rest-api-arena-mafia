package com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RateLimitKeyProvider {

  private static final String OPERATION_SEPARATOR = ":";

  /**
   * Gera a chave para rate limiting baseada no usuário/IP e tipo de operação.
   *
   * Para usuários autenticados: {username}:{operationType}
   * Para usuários anônimos: {clientIp}:{operationType}
   *
   * Exemplos:
   * - user123:sms
   * - 192.168.1.1:sms
   * - user123:auth
   *
   * @param request a requisição HTTP
   * @param operationType tipo de operação (ex: "sms", "auth", "login"). Se vazio, ignora a separação.
   * @return chave única para rate limiting
   */
  public String resolveKey(HttpServletRequest request, String operationType) {
    String baseKey = getUsernameFromPrincipal().orElseGet(() -> getIpFromRequest(request));

    // Se operationType está vazio, retorna apenas a chave base (username/IP)
    if (operationType == null || operationType.isEmpty()) {
      return baseKey;
    }

    return baseKey + OPERATION_SEPARATOR + operationType;
  }

  /**
   * Versão simplificada que mantém compatibilidade com código existente.
   * Resolve a chave sem diferenciação por operação.
   */
  public String resolveKey(HttpServletRequest request) {
    return resolveKey(request, "");
  }

  private Optional<String> getUsernameFromPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !"anonymousUser".equals(authentication.getPrincipal())) {
      return Optional.ofNullable(authentication.getName());
    }
    return Optional.empty();
  }

  private String getIpFromRequest(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }
}
