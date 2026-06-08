package com.pos.pos_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * TopProductResponse — one bar in GET /api/dashboard/top-products
 * Top 5 products by total quantity sold this month.
 */
@Data
@Builder
public class TopProductResponse {

    private Long       productId;
    private String     productName;
    private long       totalQuantity;   // units sold this month
    private BigDecimal totalRevenue;    // revenue generated this month
}