package com.b2bshop.project.controller;

import com.b2bshop.project.dto.CreateShopRequest;
import com.b2bshop.project.model.Shop;
import com.b2bshop.project.repository.ShopRepository;
import com.b2bshop.project.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
@Slf4j
public class ShopController {
    private final ShopService shopService;
    private final ShopRepository shopRepository;

    public ShopController(ShopService shopService, ShopRepository shopRepository) {
        this.shopService = shopService;
        this.shopRepository = shopRepository;
    }

    @GetMapping()
    public List<Shop> getAllCompanies() {
        return shopRepository.findAll();
    }

    @PostMapping()
    public Shop addShop(@RequestBody CreateShopRequest request) {
        return shopService.createShop(request);
    }

    @GetMapping("/{shopId}")
    public Shop getShopById(@PathVariable Long shopId) {
        return shopRepository.findById(shopId).orElse(null);
    }

    @PutMapping("/{shopId}")
    public Shop updateShopById(@PathVariable Long shopId, @RequestBody Shop newShop) {
        return shopService.updateShopById(shopId, newShop);
    }

    @DeleteMapping("/{shopId}")
    public void deleteShopById(@PathVariable Long shopId) {
        shopRepository.deleteById(shopId);
    }
}
