package com.nyasha.store.entities;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Data
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnId;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    private String reason;
    private String status;
    private Double refundAmount;
    private LocalDateTime processedAt;
}
