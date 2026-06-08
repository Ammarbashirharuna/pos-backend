package com.pos.pos_backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashierPerformanceResponse {
    private Long       cashierId;
    private String     cashierName;
    private int        transactionCount;
    private BigDecimal totalRevenue;
}