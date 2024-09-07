package com.b2bshop.project.repository;

import com.b2bshop.project.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByShopTenantId(Long tenantId);
}
