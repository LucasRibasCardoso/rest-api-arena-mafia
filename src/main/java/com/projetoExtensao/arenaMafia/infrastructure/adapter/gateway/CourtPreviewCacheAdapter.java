package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.court.port.gateway.CourtPreviewCachePort;
import com.projetoExtensao.arenaMafia.application.court.preview.CourtDisablePreview;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPreviewKeyException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.InvalidPreviewOwnershipException;
import com.projetoExtensao.arenaMafia.domain.exception.notFound.PreviewNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class CourtPreviewCacheAdapter implements CourtPreviewCachePort {

  private static final String CACHE_PREFIX = "court-disable-preview:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  public CourtPreviewCacheAdapter(
      RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(String key, CourtDisablePreview preview) {
    try {
      deleteExistingPreviewsForUser(extractUserIdFromKey(key));
      String jsonValue = objectMapper.writeValueAsString(preview);
      redisTemplate.opsForValue().set(key, jsonValue, CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Erro ao serializar preview para cache", e);
    }
  }

  @Override
  public CourtDisablePreview getPreviewOrElseThrow(String key, UUID userId) {
    validateKeyOwnership(key, userId);

    String jsonValue = redisTemplate.opsForValue().get(key);
    if (jsonValue == null) {
      throw new PreviewNotFoundException();
    }

    try {
      return objectMapper.readValue(jsonValue, CourtDisablePreview.class);
    } catch (JsonProcessingException e) {
      throw new PreviewNotFoundException();
    }
  }

  @Override
  public void delete(String key) {
    redisTemplate.delete(key);
  }

  @Override
  public String generateKey(UUID userId) {
    return CACHE_PREFIX + userId + ":" + UUID.randomUUID();
  }

  private void validateKeyOwnership(String key, UUID userId) {
    if (key == null || key.isBlank() || !key.startsWith(CACHE_PREFIX)) {
      throw new InvalidPreviewKeyException();
    }

    if (!key.startsWith(CACHE_PREFIX + userId + ":")) {
      throw new InvalidPreviewOwnershipException();
    }
  }

  private UUID extractUserIdFromKey(String key) {
    String userIdPart = key.substring(CACHE_PREFIX.length());
    String userIdString = userIdPart.substring(0, userIdPart.indexOf(':'));
    return UUID.fromString(userIdString);
  }

  private void deleteExistingPreviewsForUser(UUID userId) {
    String pattern = CACHE_PREFIX + userId + ":*";
    redisTemplate.delete(redisTemplate.keys(pattern));
  }
}
