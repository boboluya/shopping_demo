package com.tom.shop.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillProduct {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createdAt;

    private Integer version;
}
