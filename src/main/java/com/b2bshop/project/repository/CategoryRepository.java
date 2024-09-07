package com.b2bshop.project.repository;

import com.b2bshop.project.model.Category;
import com.b2bshop.project.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByShop(Shop shop);
    List<Category> findByShopTenantId(Long tenantId);
    List<Category> findByShopTenantIdAndIsActiveTrue(Long shopId);
    Optional<Category> findByIdAndShopTenantIdAndIsActiveTrue(Long id, Long tenantId);
    Optional<Category> findByIdAndShopTenantId(Long id, Long tenantId);
}
