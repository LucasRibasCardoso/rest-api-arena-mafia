package com.projetoExtensao.arenaMafia.infrastructure.security.jwt;

import com.projetoExtensao.arenaMafia.infrastructure.web.exception.customHandlers.CustomUnauthorizedHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final CustomUnauthorizedHandler customUnauthorizedHandler;

  public JwtTokenFilter(
      JwtTokenProvider tokenProvider, CustomUnauthorizedHandler customUnauthorizedHandler) {
    this.tokenProvider = tokenProvider;
    this.customUnauthorizedHandler = customUnauthorizedHandler;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String token = tokenProvider.resolveToken(request);

    if (token != null) {
      try {
        Authentication authentication = tokenProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (AuthenticationException exception) {
        SecurityContextHolder.clearContext();
        customUnauthorizedHandler.commence(request, response, exception);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
