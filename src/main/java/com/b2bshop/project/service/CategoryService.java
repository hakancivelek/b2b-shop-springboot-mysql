package com.b2bshop.project.service;

import com.b2bshop.project.exception.ResourceNotFoundException;
import com.b2bshop.project.model.Category;
import com.b2bshop.project.model.Role;
import com.b2bshop.project.model.Shop;
import com.b2bshop.project.model.User;
import com.b2bshop.project.repository.CategoryRepository;
import com.b2bshop.project.repository.ShopRepository;
import com.b2bshop.project.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final SecurityService securityService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public CategoryService(CategoryRepository categoryRepository, ShopRepository shopRepository, SecurityService securityService, JwtService jwtService, UserRepository userRepository, EntityManager entityManager) {
        this.categoryRepository = categoryRepository;
        this.shopRepository = shopRepository;
        this.securityService = securityService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    public List<Map<String, Object>> getAllCategories(HttpServletRequest request) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        Long tenantId = securityService.returnTenantIdByUsernameOrToken("token", token);

        String userName = jwtService.extractUser(token);
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Set<Role> userRoles = user.getAuthorities();

        List<Category> categories;

        if (userRoles.contains(Role.ROLE_CUSTOMER_USER)) {
            tenantId = user.getCustomer().getShop().getTenantId();
            categories = categoryRepository.findByShopTenantIdAndIsActiveTrue(tenantId);
        } else if (tenantId != null) {
            categories = categoryRepository.findByShopTenantId(tenantId);
        } else {
            categories = categoryRepository.findAll();
        }

        return buildCategoryHierarchy(categories);
    }

    private List<Map<String, Object>> buildCategoryHierarchy(List<Category> categories) {
        Map<Long, Map<String, Object>> categoryMap = new HashMap<>();

        for (Category category : categories) {
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("id", category.getId());
            categoryData.put("name", category.getName());
            categoryData.put("isActive", category.getIsActive());
            categoryData.put("parentCategory", category.getParentCategory() != null ? category.getParentCategory().getId() : null);
            categoryData.put("subCategories", new ArrayList<Map<String, Object>>());
            categoryMap.put(category.getId(), categoryData);
        }

        List<Map<String, Object>> rootCategories = new ArrayList<>();
        for (Category category : categories) {
            if (category.getParentCategory() == null) {
                rootCategories.add(categoryMap.get(category.getId()));
            } else {
                Map<String, Object> parentCategory = categoryMap.get(category.getParentCategory().getId());
                List<Map<String, Object>> subCategories = (List<Map<String, Object>>) parentCategory.get("subCategories");
                subCategories.add(categoryMap.get(category.getId()));
            }
        }

        return rootCategories;
    }

    public List<Category> getCategoriesByShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));
        return categoryRepository.findByShop(shop);
    }

    public Map<String, Object> getCategoryById(HttpServletRequest request, Long id) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        Long tenantId = securityService.returnTenantIdByUsernameOrToken("token", token);
        String userName = jwtService.extractUser(token);
        User user = userRepository.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found"));
        Set<Role> userRoles = user.getAuthorities();

        Category category;
        if (userRoles.contains(Role.ROLE_CUSTOMER_USER)) {
            tenantId = user.getCustomer().getShop().getTenantId();
            category = categoryRepository.findByIdAndShopTenantIdAndIsActiveTrue(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Active category not found for id: " + id));
        } else {
            category = categoryRepository.findByIdAndShopTenantId(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found for id: " + id));
        }

        List<Category> allCategories = categoryRepository.findAll();
        List<Map<String, Object>> categoryHierarchy = buildCategoryHierarchy(allCategories);

        Map<String, Object> categoryData = findCategoryInHierarchy(categoryHierarchy, id);
        if (categoryData == null) {
            throw new ResourceNotFoundException("Category not found in hierarchy with id: " + id);
        }

        return categoryData;
    }

    private Map<String, Object> findCategoryInHierarchy(List<Map<String, Object>> hierarchy, Long id) {
        for (Map<String, Object> category : hierarchy) {
            if (category.get("id").equals(id)) {
                return category;
            }
            List<Map<String, Object>> subCategories = (List<Map<String, Object>>) category.get("subCategories");
            Map<String, Object> result = findCategoryInHierarchy(subCategories, id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Transactional
    public Category createCategory(HttpServletRequest request, JsonNode json) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        String userName = jwtService.extractUser(token);
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Shop shop = user.getShop();

        Category parentCategory = null;

        if (json.has("parentCategory") && !json.get("parentCategory").isNull()) {
            JsonNode parentCategoryJson = json.get("parentCategory");
            Long parentCategoryId = parentCategoryJson.get("id").asLong();

            parentCategory = categoryRepository.findById(parentCategoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + parentCategoryId));
        }

        Category newCategory = Category.builder()
                .name(json.get("name").asText())
                .isActive(json.get("active").asBoolean())
                .parentCategory(parentCategory)
                .shop(shop)
                .build();

        newCategory = categoryRepository.save(newCategory);

        if (parentCategory != null && !parentCategory.getChildCategoryIds().contains(newCategory)) {
            parentCategory.getChildCategoryIds().add(newCategory.getId());
            if (parentCategory.getParentCategory() != null) {
                parentCategory.getParentCategory().getChildCategoryIds().add(newCategory.getId());
            }

            categoryRepository.save(parentCategory);
        }

        return newCategory;
    }

    public Category updateById(Long id, Category updatedCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        category.setName(updatedCategory.getName());
        category.setIsActive(updatedCategory.getIsActive());
        category.setParentCategory(updatedCategory.getParentCategory());
        category.setShop(updatedCategory.getShop());

        return categoryRepository.saveAndFlush(category);
    }

    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        categoryRepository.delete(category);
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElseThrow(()
                -> new RuntimeException("category not found by id: " + id));
    }
}

