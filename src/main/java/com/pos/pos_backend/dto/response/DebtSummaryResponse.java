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
public class DebtSummaryResponse {
    private int        totalCustomersWithDebt;
    private BigDecimal totalOutstandingDebt;
    private BigDecimal highestSingleDebt;
    private String     highestDebtorName;
}