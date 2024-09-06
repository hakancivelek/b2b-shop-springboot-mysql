package com.b2bshop.project.model;

import com.b2bshop.project.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Data
@Entity
@Table(name = "basket")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Basket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BasketItem> basketItems;
    private int basketItemCount = 0;
    private double totalCost = 0;
    private double subTotal = 0;

    public void addItem(Product product, int quantity, boolean updateQuantity) {
        if (basketItems == null) {
            basketItems = new ArrayList<>();
        }

        boolean itemExists = false;

        for (BasketItem item : basketItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                if (updateQuantity) {
                    item.setQuantity(quantity);
                } else {
                    item.setQuantity(item.getQuantity() + quantity);
                }
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            BasketItem newItem = BasketItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .build();
            basketItems.add(newItem);
        }

        basketItemCount = basketItems.size();
        totalCost += product.getGrossPrice() * quantity;
    }

    public boolean removeItem(Long productId) {
        if (basketItems == null || basketItems.isEmpty()) {
            throw new IllegalStateException("Basket is empty or not initialized.");
        }

        boolean itemFound = false;

        Iterator<BasketItem> iterator = basketItems.iterator();
        while (iterator.hasNext()) {
            BasketItem item = iterator.next();
            if (item.getProduct().getId().equals(productId)) {
                totalCost -= item.getProduct().getGrossPrice() * item.getQuantity();
                iterator.remove();
                itemFound = true;
                break;
            }
        }

        if (!itemFound) {
            throw new ResourceNotFoundException("Product with ID " + productId + " not found in the basket.");
        }

        basketItemCount = basketItems.size();

        return basketItems.isEmpty();
    }
}
