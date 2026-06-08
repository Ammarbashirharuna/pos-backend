package com.pos.pos_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DashboardStatsResponse — returned by GET /api/dashboard/stats
 * Contains all KPI values needed for the 4 stat cards on the dashboard.
 */
@Data
@Builder
public class DashboardStatsResponse {

    // Today's total revenue from all completed sales
    private BigDecimal todayRevenue;

    // Number of transactions completed today
    private long todayTransactions;

    // Number of products at or below their min_stock threshold
    private long lowStockCount;

    // Total number of active customers
    private long totalCustomers;

    // Total outstanding debt across all customers
    private BigDecimal totalOutstandingDebt;

    // This month's revenue (for the trend indicator)
    private BigDecimal monthlyRevenue;
}