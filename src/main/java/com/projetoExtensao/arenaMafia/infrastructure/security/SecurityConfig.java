package com.projetoExtensao.arenaMafia.infrastructure.security;

import com.projetoExtensao.arenaMafia.infrastructure.security.jwt.JwtTokenFilter;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.customHandlers.CustomAccessDeniedHandler;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.customHandlers.CustomUnauthorizedHandler;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final CustomAccessDeniedHandler accessDeniedHandler;
  private final CustomUnauthorizedHandler unauthorizedHandler;
  private final JwtTokenFilter tokenFilter;

  public SecurityConfig(
      CustomAccessDeniedHandler accessDeniedHandler,
      CustomUnauthorizedHandler unauthorizedHandler,
      JwtTokenFilter tokenFilter) {
    this.accessDeniedHandler = accessDeniedHandler;
    this.unauthorizedHandler = unauthorizedHandler;
    this.tokenFilter = tokenFilter;
  }

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("ROLE_MODERATOR > ROLE_ADMIN > ROLE_USER");
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
        // Desabilita o CSRF
        .csrf(CsrfConfigurer::disable)

        // Habilita o H2 Console para ser exibido em um iframe
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

        // Define a política de sessão como stateless
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Configura o ponto de entrada para erros de autenticação
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(unauthorizedHandler)
                    .accessDeniedHandler(accessDeniedHandler))

        // Configura as requisições HTTP
        .authorizeHttpRequests(
            auth ->
                auth
                    // Endpoints privados
                    .requestMatchers("/api/auth/logout", "/api/users/**", "/api/schedules/**")
                    .authenticated()

                    // Endpoints públicos
                    .requestMatchers("/api/auth/**", "/api/public/**")
                    .permitAll()

                    // Endpoints públicos para desenvolvimento
                    .requestMatchers(
                        "/h2-console/**",
                        "/docs/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**",
                        "/openapi.yml")
                    .permitAll()

                    // Endpoints de administração de usuários restritos a administradores
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")

                    // Restringe o acesso a endpoints de monitoramento
                    .requestMatchers(EndpointRequest.toAnyEndpoint())
                    .hasRole("DEVELOPER")

                    // Exige autenticação para todas as outras requisições
                    .anyRequest()
                    .authenticated())

        // Adiciona o filtro JWT antes do filtro de autenticação de username e senha
        .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }
}
