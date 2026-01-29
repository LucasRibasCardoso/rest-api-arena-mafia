package com.projetoExtensao.arenaMafia.infrastructure.adapter.repository;

import com.projetoExtensao.arenaMafia.application.user.port.repository.AdminUserRepositoryPort;
import com.projetoExtensao.arenaMafia.domain.model.User;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.entity.UserEntity;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.mapper.UserMapper;
import com.projetoExtensao.arenaMafia.infrastructure.persistence.repository.AdminUserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserRepositoryAdapter implements AdminUserRepositoryPort {

  private final AdminUserJpaRepository adminUserJpaRepository;
  private final UserMapper userMapper;

  public AdminUserRepositoryAdapter(
      AdminUserJpaRepository adminUserJpaRepository, UserMapper userMapper) {
    this.adminUserJpaRepository = adminUserJpaRepository;
    this.userMapper = userMapper;
  }

  @Override
  public Page<User> search(Specification<UserEntity> spec, Pageable pageable) {
    return adminUserJpaRepository.findAll(spec, pageable).map(userMapper::toDomain);
  }
}
