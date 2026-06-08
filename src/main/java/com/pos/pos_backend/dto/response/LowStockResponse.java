package com.pos.pos_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * LowStockResponse — one row in GET /api/dashboard/low-stock
 */
@Data
@Builder
public class LowStockResponse {

    private Long   id;
    private String name;
    private int    stock;       // current stock level
    private int    minStock;    // threshold that triggered low-stock
    private String categoryName;
}