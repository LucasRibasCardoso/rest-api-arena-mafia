package com.projetoExtensao.arenaMafia.infrastructure.adapter.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projetoExtensao.arenaMafia.application.schedule.port.gateway.BlockedTimePreviewCachePort;
import com.projetoExtensao.arenaMafia.domain.dto.BlockedTimeConflictsPreview;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidPreviewKeyException;
import com.projetoExtensao.arenaMafia.domain.exception.forbidden.InvalidPreviewOwnershipException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
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
      String jsonValue = objectMapper.writeValueAsString(preview);
      redisTemplate.opsForValue().set(key, jsonValue, CACHE_TTL);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Erro ao serializar preview para cache", e);
    }
  }

  @Override
  public Optional<BlockedTimeConflictsPreview> find(String key) {
    String jsonValue = redisTemplate.opsForValue().get(key);
    if (jsonValue == null) {
      return Optional.empty();
    }

    try {
      BlockedTimeConflictsPreview preview =
          objectMapper.readValue(jsonValue, BlockedTimeConflictsPreview.class);
      return Optional.of(preview);
    } catch (JsonProcessingException e) {
      return Optional.empty();
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

  @Override
  public void validateKeyOwnership(String key, UUID userId) {
    if (key == null || key.isBlank() || !key.startsWith(CACHE_PREFIX)) {
      throw new InvalidPreviewKeyException();
    }

    if (!key.startsWith(CACHE_PREFIX + userId + ":")) {
      throw new InvalidPreviewOwnershipException();
    }
  }
}
