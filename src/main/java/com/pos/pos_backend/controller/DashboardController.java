package com.pos.pos_backend.controller;

import com.pos.pos_backend.dto.ApiResponse;
import com.pos.pos_backend.dto.response.*;
import com.pos.pos_backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Dashboard", description = "Dashboard KPIs, recent sales, low stock, top products")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "KPI stats — today revenue, transactions, low stock count, customers")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.ok("Dashboard stats", dashboardService.getStats()));
    }

    @Operation(summary = "Last 10 sales with customer and cashier info")
    @GetMapping("/recent-sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<List<RecentSaleResponse>>> getRecentSales() {
        return ResponseEntity.ok(
                ApiResponse.ok("Recent sales", dashboardService.getRecentSales()));
    }

    @Operation(summary = "Products at or below min_stock — most critical first")
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<List<LowStockResponse>>> getLowStock() {
        return ResponseEntity.ok(
                ApiResponse.ok("Low stock products", dashboardService.getLowStock()));
    }

    @Operation(summary = "Top 5 products by quantity sold this month — ADMIN only")
    @GetMapping("/top-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts() {
        return ResponseEntity.ok(
                ApiResponse.ok("Top products", dashboardService.getTopProducts()));
    }
    @Operation(summary = "Total outstanding debt across all customers — ADMIN only")
    @GetMapping("/debt-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DebtSummaryResponse>> getDebtSummary() {
        return ResponseEntity.ok(
                ApiResponse.ok("Debt summary", dashboardService.getDebtSummary()));
    }

    @Operation(summary = "Sales totals and transaction counts per cashier today — ADMIN only")
    @GetMapping("/cashier-performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CashierPerformanceResponse>>> getCashierPerformance() {
        return ResponseEntity.ok(
                ApiResponse.ok("Cashier performance", dashboardService.getCashierPerformance()));
    }
}