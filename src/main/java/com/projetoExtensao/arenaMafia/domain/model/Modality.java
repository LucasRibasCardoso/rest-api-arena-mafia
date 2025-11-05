package com.projetoExtensao.arenaMafia.domain.model;

import com.projetoExtensao.arenaMafia.domain.exception.ErrorCode;
import com.projetoExtensao.arenaMafia.domain.exception.badRequest.InvalidModalityNameFormatException;
import com.projetoExtensao.arenaMafia.domain.exception.conflict.ModalityStatusConflictException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Modality {

    private final UUID id;
    private String name;
    private boolean isActive;
    private final Instant createdAt;

    /**
     * Factory Method para criar uma nova modalidade.
     *
     * @param name nome da modalidade
     * @return uma nova instância de Modality
     */
    public static Modality create(String name) {
        UUID newId = UUID.randomUUID();
        Instant now = Instant.now();
        boolean isActive = true;
        return new Modality(newId, name, isActive, now);
    }

    /**
     * Factory Method para RECONSTRUIR uma modalidade a partir de dados existentes do banco. Esse
     * metodo é usado pelo MapStruct para mapear uma entidade para Modality.
     *
     * @param id        id da modalidade
     * @param name      nome da modalidade
     * @param isActive  status da modalidade
     * @param createdAt data de criação da modalidade
     * @return uma instância de Modality reconstruída
     */
    public static Modality reconstitute(UUID id, String name, boolean isActive, Instant createdAt) {
        return new Modality(id, name, isActive, createdAt);
    }

    private Modality(UUID id, String name, boolean isActive, Instant createdAt) {
        validateName(name);
        this.id = id;
        this.name = name;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    // Validações
    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidModalityNameFormatException(ErrorCode.MODALITY_NAME_REQUIRED);
        }

        if (name.length() < 3 || name.length() > 100) {
            throw new InvalidModalityNameFormatException(ErrorCode.MODALITY_NAME_INVALID_LENGTH);
        }
    }

    // Atualizar atributos
    public void updateName(String newName) {
        validateName(newName);
        this.name = newName;
    }

    public void disable() {
        if (!this.isActive) {
            throw new ModalityStatusConflictException(ErrorCode.MODALITY_ALREADY_DISABLE);
        }
        this.isActive = false;
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Modality modality)) return false;
        return Objects.equals(id, modality.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
