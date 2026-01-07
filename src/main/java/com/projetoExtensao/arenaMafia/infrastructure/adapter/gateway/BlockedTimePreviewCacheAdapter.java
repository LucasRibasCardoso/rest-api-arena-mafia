package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.application.schedule.preview.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPreviewKeyException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.InvalidPreviewOwnershipException;
import java.time.Duration;
import java.util.UUID;

import com.projetoExtensao.arenaMafia.domain.exception.notFound.BlockedTimeNotFoundException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BlockedTimePreviewCacheAdapter implements BlockedTimePreviewCachePort {

  private static final String CACHE_PREFIX = "blocked-time-preview:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(5);

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  public BlockedTimePreviewCacheAdapter(
      RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  @Override
  public void save(String key, BlockedTimeConflictsPreview preview) {
    try {
      deleteExistingPreviewsForUser(extractUserIdFromKey(key));
      String jsonValue = objectMapper.writeValueAsString(preview);
      redisTemplate.opsForValue().set(key, jsonValue, CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Erro ao serializar preview para cache", e);
    }
  }

  @Override
  public BlockedTimeConflictsPreview getPreviewOrElseThrow(String key, UUID userId) {
    validateKeyOwnership(key, userId);

    String jsonValue = redisTemplate.opsForValue().get(key);
    if (jsonValue == null) {
      throw new BlockedTimeNotFoundException(ErrorCode.BLOCKED_TIME_PREVIEW_NOT_FOUND);
    }

    try {
      return objectMapper.readValue(jsonValue, BlockedTimeConflictsPreview.class);
    } catch (JsonProcessingException e) {
      throw new BlockedTimeNotFoundException(ErrorCode.BLOCKED_TIME_PREVIEW_NOT_FOUND);
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
