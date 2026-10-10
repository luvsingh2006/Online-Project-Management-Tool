package com.pmtool.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public abstract class BaseEntity implements Identifiable<Long>, Auditable, Serializable {

    private static final long serialVersionUID = 1L;

    protected Long id;
    protected Timestamp createdAt;
    protected Timestamp updatedAt;

    public BaseEntity() {
        Timestamp now = Timestamp.from(Instant.now());
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BaseEntity(Long id) {
        this();
        this.id = id;
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    @Override public Timestamp getCreatedAt() { return createdAt; }
    @Override public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    @Override public Timestamp getUpdatedAt() { return updatedAt; }
    @Override public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public void touch() {
        this.updatedAt = Timestamp.from(Instant.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : super.hashCode());
    }
}
