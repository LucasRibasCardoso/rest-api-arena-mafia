package com.projetoExtensao.arenaMafia.unit.infrastructure.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.projetoExtensao.arenaMafia.infrastructure.config.web.CorsConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

class CorsConfigTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:5173";

  private CorsConfigurationSource configurationSource;
  private DefaultCorsProcessor corsProcessor;

  @BeforeEach
  void setup() {
    configurationSource =
        new CorsConfig().corsConfigurationSource(List.of(ALLOWED_ORIGIN));
    corsProcessor = new DefaultCorsProcessor();
  }

  @Test
  void preflightRequest_shouldAllowConfiguredOrigin() throws Exception {
    MockHttpServletRequest request = preflightRequest(ALLOWED_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean processed =
        corsProcessor.processRequest(
            configurationSource.getCorsConfiguration(request), request, response);

    assertThat(processed).isTrue();
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
        .isEqualTo(ALLOWED_ORIGIN);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
    String allowedHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
    assertThat(allowedHeaders).containsIgnoringCase("Authorization");
    assertThat(allowedHeaders).containsIgnoringCase("Content-Type");
  }

  @Test
  void preflightRequest_shouldRejectUnconfiguredOrigin() throws Exception {
    MockHttpServletRequest request = preflightRequest("http://localhost:3000");
    MockHttpServletResponse response = new MockHttpServletResponse();

    boolean processed =
        corsProcessor.processRequest(
            configurationSource.getCorsConfiguration(request), request, response);

    assertThat(processed).isFalse();
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
  }

  private MockHttpServletRequest preflightRequest(String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
    request.addHeader(HttpHeaders.ORIGIN, origin);
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type");
    return request;
  }
}
