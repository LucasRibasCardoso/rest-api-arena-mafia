package com.projetoExtensao.arenaMafia.infrastructure.security.rateLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomRateLimiter {
  /**
   * O nome da instância do RateLimiter definida no application.yml.
   * Exemplos: "loginRateLimiter", "sensitiveOperationLimiter", "smsRateLimiter", "globalLimiter"
   */
  String limiterName();

  /**
   * O tipo de operação para diferenciação do rate limit.
   * Exemplos: "sms", "auth", "login"
   *
   * Isso permite que múltiplos endpoints usando o mesmo limiter tenham limites separados.
   * Por exemplo: signup (sms), resend-otp (sms), login (auth) podem ter limites individuais.
   *
   * Padrão: "" (vazio) = usa apenas username/IP sem diferenciação por operação
   */
  String operationType() default "";
}
