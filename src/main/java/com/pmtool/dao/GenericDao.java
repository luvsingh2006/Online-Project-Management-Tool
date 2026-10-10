package com.pmtool.dao;

import com.pmtool.model.Identifiable;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface GenericDao<T extends Identifiable<ID>, ID extends Serializable> {
    Optional<T> findById(ID id);
    List<T> findAll();
    T save(T entity);
    boolean update(T entity);
    boolean deleteById(ID id);
    long count();
}
