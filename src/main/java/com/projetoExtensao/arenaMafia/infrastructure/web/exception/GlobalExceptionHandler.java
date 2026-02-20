package com.projetoExtensao.arenaMafia.infrastructure.web.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.ApplicationException;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ConflictException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.ForbiddenException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.NotFoundException;
import com.projetoExtensao.arenaMafia.domain.exception.unauthorized.UnauthorizedException;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.ErrorResponseDto;
import com.projetoExtensao.arenaMafia.infrastructure.web.exception.dto.FieldErrorResponseDto;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponseDto> handleApplicationException(
      ApplicationException e, HttpServletRequest request) {
    return buildGeneralErrorResponse(mapExceptionToStatus(e), e.getErrorCode(), request);
  }

  @ExceptionHandler(RequestNotPermitted.class)
  public ResponseEntity<ErrorResponseDto> handleRateLimitException(HttpServletRequest request) {

    final HttpStatus httpStatus = HttpStatus.TOO_MANY_REQUESTS;

    if (request.getRequestURI().equals("/api/auth/login")) {
      return buildGeneralErrorResponse(httpStatus, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, request);
    }
    return buildGeneralErrorResponse(httpStatus, ErrorCode.TOO_MANY_REQUESTS, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e, HttpServletRequest request) {

    Throwable rootCause = NestedExceptionUtils.getRootCause(e);
    Throwable immediateCause = e.getCause();

    List<FieldErrorResponseDto> fieldErrors;
    String fieldName = "unknown";

    if (rootCause instanceof InvalidFormatException exception) {
      fieldName = extractFieldNameFromPath(exception);
      fieldErrors = List.of(buildMismatchErrorForField(fieldName, exception.getTargetType()));

    } else if (rootCause instanceof ApplicationException appException) {
      String errorCodeString = appException.getErrorCode().name();
      String devMessage = appException.getErrorCode().getMessage();

      if (immediateCause instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
        fieldName = extractFieldNameFromJsonMappingException(jme);
      }
      fieldErrors = List.of(new FieldErrorResponseDto(fieldName, errorCodeString, devMessage));
    } else {
      String errorCodeString = ErrorCode.MALFORMED_JSON_REQUEST.name();
      String devMessage = ErrorCode.MALFORMED_JSON_REQUEST.getMessage();
      fieldErrors = List.of(new FieldErrorResponseDto(fieldName, errorCodeString, devMessage));
    }

    return buildValidationErrorResponse(fieldErrors, request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationException(
      MethodArgumentNotValidException e, HttpServletRequest request) {

    List<FieldErrorResponseDto> fieldErrors =
        e.getBindingResult().getFieldErrors().stream()
            .map(
                fieldError ->
                    fieldError.isBindingFailure()
                        ? buildFieldErrorToMismatchException(fieldError, e)
                        : buildFieldErrorToValidationException(fieldError))
            .toList();
    return buildValidationErrorResponse(fieldErrors, request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e, HttpServletRequest request) {

    return buildGeneralErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST_PARAMETER, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDto> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e, HttpServletRequest request) {

    Throwable rootCause = NestedExceptionUtils.getRootCause(e);
    if (rootCause instanceof ApplicationException appException) {
      return handleApplicationException(appException, request);
    }
    return buildGeneralErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST_PARAMETER, request);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ErrorResponseDto> handlePropertyReferenceException(
      HttpServletRequest request) {
    return buildGeneralErrorResponse(
        HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SORT_PARAMETER, request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
      AuthenticationException e, HttpServletRequest request) {

    ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;

    if (e instanceof UnauthorizedException customException) {
      errorCode = customException.getErrorCode();
    }
    return buildGeneralErrorResponse(HttpStatus.UNAUTHORIZED, errorCode, request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
      HttpServletRequest request) {
    return buildGeneralErrorResponse(
        HttpStatus.CONFLICT, ErrorCode.DATA_INTEGRITY_VIOLATION, request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleGenericException(
      Exception e, HttpServletRequest request) {

    logger.error("ERRO CRÍTICO NÃO TRATADO: ", e);
    return buildGeneralErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNEXPECTED_ERROR, request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(
      NoResourceFoundException e, HttpServletRequest request) {

    logger.warn(
        "Tentativa de acesso a recurso inexistente: {} {}",
        request.getMethod(),
        request.getRequestURI());

    // Retorna 404 corretamente
    return buildGeneralErrorResponse(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, request);
  }

  /**
   * Mapeia exceções de aplicação para status HTTP apropriados.
   *
   * @param e A exceção de aplicação a ser mapeada
   * @return O status HTTP correspondente
   */
  private HttpStatus mapExceptionToStatus(ApplicationException e) {
    if (e instanceof NotFoundException) return HttpStatus.NOT_FOUND;
    if (e instanceof ConflictException) return HttpStatus.CONFLICT;
    if (e instanceof ForbiddenException) return HttpStatus.FORBIDDEN;
    return HttpStatus.BAD_REQUEST;
  }

  /**
   * Extrai o nome do campo a partir do path de uma InvalidFormatException. Percorre o path da
   * exceção do início para o fim para encontrar o primeiro elemento que contenha um fieldName
   * não-nulo.
   *
   * @param exception A InvalidFormatException que contém o path
   * @return O nome do campo onde ocorreu o erro, ou "unknown" se não for possível determinar
   */
  private String extractFieldNameFromPath(InvalidFormatException exception) {
    if (exception.getPath() == null || exception.getPath().isEmpty()) {
      return "unknown";
    }

    // Percorre o path do início para o fim para encontrar o primeiro fieldName não-nulo
    // No caso de Set<DayOfWeek>, o path[0] contém o fieldName correto, path[1] é null
    for (JsonMappingException.Reference reference : exception.getPath()) {
      if (reference.getFieldName() != null) {
        return reference.getFieldName();
      }
    }

    return "unknown";
  }

  /**
   * Extrai o nome do campo a partir do path de uma JsonMappingException. Percorre o path da exceção
   * do início para o fim para encontrar o primeiro elemento que contenha um fieldName não-nulo.
   *
   * @param exception A JsonMappingException que contém o path
   * @return O nome do campo onde ocorreu o erro, ou "unknown" se não for possível determinar
   */
  private String extractFieldNameFromJsonMappingException(JsonMappingException exception) {
    if (exception.getPath() == null || exception.getPath().isEmpty()) {
      return "unknown";
    }

    // Percorre o path do início para o fim para encontrar o primeiro fieldName não-nulo
    for (JsonMappingException.Reference reference : exception.getPath()) {
      if (reference.getFieldName() != null) {
        return reference.getFieldName();
      }
    }

    return "unknown";
  }

  /**
   * Constrói uma ResponseEntity com um corpo de resposta de erro genérico.
   *
   * @param status Status HTTP a ser retornado
   * @param errorCode Código de erro específico
   * @param request Objeto HttpServletRequest para obter a URI da requisição
   * @return ResponseEntity contendo o ErrorResponseDto
   */
  private ResponseEntity<ErrorResponseDto> buildGeneralErrorResponse(
      HttpStatus status, ErrorCode errorCode, HttpServletRequest request) {
    ErrorResponseDto responseBody =
        ErrorResponseDto.forGeneralError(status.value(), errorCode, request.getRequestURI());
    return ResponseEntity.status(status).body(responseBody);
  }

  /**
   * Constrói uma ResponseEntity com um corpo de resposta de erro de validação de DTOs de erro de
   * campo.
   *
   * @param fieldErrors Lista de FieldErrorResponseDto representando os erros de validação
   * @param request Objeto HttpServletRequest para obter a URI da requisição
   * @return ResponseEntity contendo o ErrorResponseDto
   */
  private ResponseEntity<ErrorResponseDto> buildValidationErrorResponse(
      List<FieldErrorResponseDto> fieldErrors, HttpServletRequest request) {
    ErrorResponseDto responseBody =
        ErrorResponseDto.forValidationErrors(request.getRequestURI(), fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody);
  }

  /**
   * Cria um FieldErrorResponseDto com o nome do campo, código de erro e mensagem.
   *
   * @param fieldName Nome do campo que gerou o erro
   * @param errorCode Código de erro associado ao problema
   * @return FieldErrorResponseDto contendo os detalhes do erro do campo
   */
  private FieldErrorResponseDto buildFieldError(String fieldName, ErrorCode errorCode) {
    return new FieldErrorResponseDto(fieldName, errorCode.name(), errorCode.getMessage());
  }

  /**
   * Converte um FieldError de falha de binding em um FieldErrorResponseDto apropriado. Tenta mapear
   * o tipo do campo para um ErrorCode; se não for possível, cria um FieldErrorResponseDto genérico.
   *
   * @param fieldError O FieldError que representa o erro de binding
   * @param exception A exceção MethodArgumentNotValidException que contém o FieldError
   * @return FieldErrorResponseDto com detalhes do erro do campo
   */
  private FieldErrorResponseDto buildFieldErrorToMismatchException(
      FieldError fieldError, MethodArgumentNotValidException exception) {

    String fieldName = fieldError.getField();
    Class<?> requestDtoClass = exception.getParameter().getParameterType();

    try {
      Field field = requestDtoClass.getDeclaredField(fieldName);
      return buildMismatchErrorForField(fieldName, field.getType());

    } catch (NoSuchFieldException e) {
      logger.warn("Campo '{}' não encontrado no objeto de destino.", fieldName);
    }
    return buildFieldError(fieldName, ErrorCode.INVALID_REQUEST_PARAMETER);
  }

  /**
   * Mapeia o tipo da enum para seu respectivo ErrorCode de conversão. Se o tipo não for uma enum,
   * retorna um ErrorCode genérico.
   *
   * @param fieldName nome do campo que gerou o erro
   * @param fieldType tipo do campo que gerou o erro
   * @return FieldErrorResponseDto com detalhes do erro do campo
   */
  private FieldErrorResponseDto buildMismatchErrorForField(String fieldName, Class<?> fieldType) {
    ErrorCode errorCode =
        ErrorCode.getForEnumType(fieldType).orElse(ErrorCode.INVALID_REQUEST_PARAMETER);
    return buildFieldError(fieldName, errorCode);
  }

  /**
   * Converte um FieldError de validação em um FieldErrorResponseDto apropriado. Tenta mapear o
   * código de erro da mensagem padrão; se não for possível, cria um FieldErrorResponseDto genérico.
   * Exemplo: se o campo for anotado com @NotNull(message = "REQUIRED_FIELD"), tenta mapear
   * "REQUIRED_FIELD" para um ErrorCode.
   *
   * @param fieldError O FieldError que representa o erro de validação
   * @return FieldErrorResponseDto com detalhes do erro do campo
   */
  private FieldErrorResponseDto buildFieldErrorToValidationException(FieldError fieldError) {
    String errorCodeString = fieldError.getDefaultMessage();
    try {
      ErrorCode errorCode = ErrorCode.valueOf(errorCodeString);
      return buildFieldError(fieldError.getField(), errorCode);
    } catch (IllegalArgumentException ex) {
      String devMessage = "Código de erro de validação não mapeado: " + errorCodeString;
      return new FieldErrorResponseDto(fieldError.getField(), errorCodeString, devMessage);
    }
  }
}
