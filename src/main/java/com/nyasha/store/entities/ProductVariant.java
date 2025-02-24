package com.nyasha.store.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size;
    private String color;
    private String material;
    private Double priceAdjustment;
    private String sku;
}
