package com.b2bshop.project.model;

import com.b2bshop.project.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    @Column(unique = true)
    String orderNumber;
    private String orderNote;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;
    private Date orderDate;
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;
    @ManyToOne
    private Address invoiceAddress;
    @ManyToOne
    private Address receiverAddress;
    private Double totalPrice;
    private Double withoutTaxPrice;
    private Double totalTax;

    public void generateAndSetOrderNumber(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmmss");
        String timestamp = now.format(formatter);

        String orderNumber = tenantId.toString() + timestamp.substring(4, 11);
        this.orderNumber = orderNumber;
    }

    public void applyBasketItemsToOrder(List<BasketItem> basketItems) {
        Double totalPrice = 0.0;
        Double withoutTaxPrice = 0.0;
        Double totalTax = 0.0;

        for (BasketItem basketItem : basketItems) {
            Product refProduct = basketItem.getProduct();
            boolean isStockAvailable = refProduct.isStockAvailable(basketItem.getQuantity());

            if (isStockAvailable) {
                List<Image> imagesCopy = new ArrayList<>();
                for (Image image : refProduct.getImages()) {
                    Image imageCopy = new Image();
                    imageCopy.setUrl(image.getUrl());
                    imageCopy.setIsThumbnail(image.getIsThumbnail());
                    imagesCopy.add(imageCopy);
                }

                OrderItem orderItem = new OrderItem();
                orderItem.setRefProductId(refProduct.getId());
                orderItem.setName(refProduct.getName());
                orderItem.setSalesPrice(refProduct.getSalesPrice());
                orderItem.setGrossPrice(refProduct.getGrossPrice());
                orderItem.setQuantity(basketItem.getQuantity());
                orderItem.setImages(imagesCopy);

                totalPrice += refProduct.getGrossPrice() * basketItem.getQuantity();
                withoutTaxPrice += refProduct.getSalesPrice() * basketItem.getQuantity();
                totalTax = totalPrice - withoutTaxPrice;

                refProduct.setStock(refProduct.getStock() - basketItem.getQuantity());

                this.getOrderItems().add(orderItem);
            } else {
                throw new ResourceNotFoundException("Stock is not enough for material: " + refProduct.getName());
            }
        }

        this.setTotalPrice(totalPrice);
        this.setWithoutTaxPrice(withoutTaxPrice);
        this.setTotalTax(totalTax);
    }
}
