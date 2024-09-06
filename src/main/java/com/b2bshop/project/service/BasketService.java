package com.b2bshop.project.service;

import com.b2bshop.project.exception.ResourceNotFoundException;
import com.b2bshop.project.model.*;
import com.b2bshop.project.repository.BasketItemRepository;
import com.b2bshop.project.repository.BasketRepository;
import com.b2bshop.project.repository.ProductRepository;
import com.b2bshop.project.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BasketService {
    private final EntityManager entityManager;
    private final BasketRepository basketRepository;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final UserService userService;

    private final BasketItemRepository basketItemRepository;
    private final ProductRepository productRepository;

    public BasketService(BasketRepository basketRepository, JwtService jwtService,
                         UserRepository userRepository, EntityManager entityManager,
                         ProductService productService, UserService userService, BasketItemRepository basketItemRepository,
                         ProductRepository productRepository) {
        this.basketRepository = basketRepository;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.productService = productService;
        this.entityManager = entityManager;
        this.userService = userService;
        this.basketItemRepository = basketItemRepository;
        this.productRepository = productRepository;
    }

    public Map<String, Object> getBasket(HttpServletRequest request) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        String userName = jwtService.extractUser(token);
        User user = userRepository.findByUsername(userName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found by name: " + userName));

        Long userId = user.getId();
        Basket basket = basketRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket not found for userId: " + userId));

        Map<String, Object> basketMap = new HashMap<>();
        basketMap.put("id", basket.getId());

        int basketItemCount = 0;
        double totalCost = 0;
        double subTotal = 0;

        Map<String, Map<String, Object>> basketItemsMap = new HashMap<>();
        for (BasketItem basketItem : basket.getBasketItems()) {
            Product product = basketItem.getProduct();
            String key = product.getId() + "-" + basketItem.getId();
            Map<String, Object> basketItemMap = basketItemsMap.get(key);

            if (basketItemMap == null) {
                basketItemMap = new HashMap<>();
                basketItemMap.put("basketItemId", basketItem.getId());
                basketItemMap.put("productId", product.getId());
                basketItemMap.put("productStock", product.getStock());
                basketItemMap.put("productName", product.getName());
                basketItemMap.put("quantity", basketItem.getQuantity());
                basketItemMap.put("grossPrice", product.getGrossPrice());
                basketItemMap.put("salesPrice", product.getSalesPrice());

                List<Map<String, Object>> images = new ArrayList<>();
                for (Image image : product.getImages()) {
                    Map<String, Object> imageMap = new HashMap<>();
                    imageMap.put("id", image.getId());
                    imageMap.put("url", image.getUrl());
                    imageMap.put("isThumbnail", image.getIsThumbnail());
                    images.add(imageMap);
                }
                basketItemMap.put("images", images);

                basketItemsMap.put(key, basketItemMap);

                totalCost += product.getGrossPrice() * basketItem.getQuantity();
                subTotal += product.getSalesPrice() * basketItem.getQuantity();
                basketItemCount += 1;
            }
        }

        basketMap.put("basketItems", new ArrayList<>(basketItemsMap.values()));
        basketMap.put("basketItemCount", basketItemCount);
        basketMap.put("subTotal", subTotal);
        basketMap.put("totalCost", totalCost);
        basketMap.put("totalTax", totalCost - subTotal);

        return basketMap;
    }

    @Transactional
    public Map<String, String> addItemOnBasket(HttpServletRequest request, JsonNode json) {
        Map<String, String> response = new HashMap<>();
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        String userName = jwtService.extractUser(token);
        User user = userService.findUserByName(userName);
        Long userId = user.getId();
        Basket basket;
        Optional<Basket> optionalBasket = basketRepository.findByUserId(userId);
        if (optionalBasket.isPresent()) {
            basket = optionalBasket.get();
        } else {
            basket = new Basket();
            basket.setUser(user);
        }

        long productId = json.get("productId").asLong();
        int quantity = json.get("quantity").asInt();
        boolean updateQuantity = json.get("updateQuantity").asBoolean();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product could not find by id: " + productId));

        basket.addItem(product, quantity, updateQuantity);

        if (!product.isStockAvailable(quantity)) {
            throw new ResourceNotFoundException("Stock is not enough for product: " + product.getName());
        }

        basketRepository.save(basket);
        response.put("success", "true");
        return response;
    }

    public Basket findById(Long id) {
        return basketRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Basket could not find by id: " + id));
    }

    public Map<String, String> removeItem(HttpServletRequest request, JsonNode json) {
        Map<String, String> response = new HashMap<>();

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid or missing Authorization header");
        }

        String token = authorizationHeader.split("Bearer ")[1];
        String userName = jwtService.extractUser(token);

        User user = userService.findUserByName(userName);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        Long userId = user.getId();

        Basket basket = basketRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Basket could not be found for user ID: " + userId));

        long productId = json.get("productId").asLong();

        boolean isBasketEmpty = basket.removeItem(productId);

        if (isBasketEmpty) {
            basketRepository.delete(basket);
        } else {
            basketRepository.save(basket);
        }

        response.put("success", "true");
        return response;
    }

    @Transactional
    public Map<String, String> cleanBasket(HttpServletRequest request) {
        String token = request.getHeader("Authorization").split("Bearer ")[1];
        String userName = jwtService.extractUser(token);
        User user = userService.findUserByName(userName);
        Long userId = user.getId();
        Basket basket;
        Optional<Basket> optionalBasket = basketRepository.findByUserId(userId);
        if (optionalBasket.isPresent()) {
            basket = optionalBasket.get();
            basketRepository.delete(basket);
        } else {
            throw new ResourceNotFoundException("Basket could not find for this user! UserId:" + user.getId());
        }

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");

        basketRepository.save(basket);

        return response;
    }
}
