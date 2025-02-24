package com.nyasha.store.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_info")
@Data
public class ShippingInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shippingId;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String trackingNumber;
    private String carrier;
    private String status;
    private LocalDateTime estimatedDelivery;
}
