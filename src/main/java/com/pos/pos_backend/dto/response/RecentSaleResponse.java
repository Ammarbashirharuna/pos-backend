package com.pos.pos_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RecentSaleResponse — one row in GET /api/dashboard/recent-sales
 */
@Data
@Builder
public class RecentSaleResponse {

    private Long          id;
    private String        invoiceNo;
    private String        customerName;   // null if walk-in (no customer linked)
    private BigDecimal    totalAmount;
    private String        paymentMethod;
    private String        cashierName;    // username of the cashier who made the sale
    private LocalDateTime createdAt;
}