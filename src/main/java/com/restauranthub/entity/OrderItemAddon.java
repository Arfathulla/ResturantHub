package com.restauranthub.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_addons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonIgnore
    private OrderItem orderItem;

    @Column(name = "addon_name_snapshot", nullable = false, length = 100)
    private String addonNameSnapshot;

    @Column(name = "addon_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal addonPriceSnapshot;
}
