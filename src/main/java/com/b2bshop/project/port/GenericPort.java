package com.b2bshop.project.port;

import java.util.List;
import java.util.Optional;

public interface GenericPort<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    Optional<List<T>> findByTenantId(ID tenantId);
    List<T> findAll();
    void deleteById(ID id);
}
