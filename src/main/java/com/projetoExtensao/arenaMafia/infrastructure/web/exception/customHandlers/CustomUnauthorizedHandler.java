package com.projetoExtensao.arenaMafia.infrastructure.web.exception.customHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.InvalidJwtTokenException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.UnauthorizedException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomUnauthorizedHandler implements AuthenticationEntryPoint {

  private static final int UNAUTHORIZED_STATUS = HttpServletResponse.SC_UNAUTHORIZED;

  private final ObjectMapper objectMapper;

  public CustomUnauthorizedHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    String originalPath = (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    if (originalPath == null) {
      originalPath = request.getRequestURI();
    }

    ErrorCode errorCode;
    if (authException instanceof InvalidJwtTokenException) {
      errorCode = ErrorCode.SESSION_EXPIRED;
    } else if (authException instanceof UnauthorizedException unAuthorizedException) {
      errorCode = unAuthorizedException.getErrorCode();
    } else {
      errorCode = ErrorCode.SESSION_EXPIRED;
    }

    var dto = ErrorResponseDto.forGeneralError(UNAUTHORIZED_STATUS, errorCode, originalPath);

    response.setStatus(UNAUTHORIZED_STATUS);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(dto));
  }
}
