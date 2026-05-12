package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.auth.port.repository.RefreshTokenRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.RefreshToken;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.domain.valueobjects.RefreshTokenVO;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.RefreshTokenEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

  private final RefreshTokenJpaRepository tokenJpaRepository;
  private final RefreshTokenMapper tokenMapper;
  private final UserMapper userMapper;

  public RefreshTokenRepositoryAdapter(
      RefreshTokenJpaRepository tokenJpaRepository,
      RefreshTokenMapper tokenMapper,
      UserMapper userMapper) {
    this.tokenJpaRepository = tokenJpaRepository;
    this.tokenMapper = tokenMapper;
    this.userMapper = userMapper;
  }

  @Override
  public Optional<RefreshToken> findByToken(RefreshTokenVO token) {
    return tokenJpaRepository.findByToken(token.toString()).map(tokenMapper::toDomain);
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    RefreshTokenEntity refreshTokenEntity = tokenMapper.toEntity(refreshToken);
    return tokenMapper.toDomain(tokenJpaRepository.save(refreshTokenEntity));
  }

  @Override
  public void deleteByUser(User user) {
    UserEntity userEntity = userMapper.toEntity(user);
    tokenJpaRepository.deleteByUser(userEntity);
    tokenJpaRepository.flush();
  }

  @Override
  public void delete(RefreshToken refreshToken) {
    RefreshTokenEntity refreshTokenEntity = tokenMapper.toEntity(refreshToken);
    tokenJpaRepository.delete(refreshTokenEntity);
  }

  @Override
  public void deleteAllByUser(List<User> users) {
    tokenJpaRepository.deleteAllByUserIn(users.stream().map(userMapper::toEntity).toList());
    tokenJpaRepository.flush();
  }
}
