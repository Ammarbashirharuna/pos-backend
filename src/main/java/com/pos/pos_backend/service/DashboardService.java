package com.pos.pos_backend.service;

import com.pos.pos_backend.dto.response.CashierPerformanceResponse;
import com.pos.pos_backend.dto.response.DashboardStatsResponse;
import com.pos.pos_backend.dto.response.DebtSummaryResponse;
import com.pos.pos_backend.dto.response.LowStockResponse;
import com.pos.pos_backend.dto.response.RecentSaleResponse;
import com.pos.pos_backend.dto.response.TopProductResponse;
import com.pos.pos_backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        String schema = getCurrentSchema();

        LocalDateTime startOfDay   = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay     = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();

        BigDecimal todayRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) " +
                        "FROM \"" + schema + "\".sales " +
                        "WHERE created_at BETWEEN ? AND ?",
                BigDecimal.class, startOfDay, endOfDay);

        Long todayTransactions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM \"" + schema + "\".sales " +
                        "WHERE created_at BETWEEN ? AND ?",
                Long.class, startOfDay, endOfDay);

        Long lowStockCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM \"" + schema + "\".products " +
                        "WHERE stock <= min_stock AND is_active = TRUE",
                Long.class);

        Long totalCustomers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"" + schema + "\".customers",
                Long.class);

        BigDecimal totalDebt = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(outstanding_balance), 0) " +
                        "FROM \"" + schema + "\".customers",
                BigDecimal.class);

        BigDecimal monthlyRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) " +
                        "FROM \"" + schema + "\".sales " +
                        "WHERE created_at >= ?",
                BigDecimal.class, startOfMonth);

        return DashboardStatsResponse.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .todayTransactions(todayTransactions != null ? todayTransactions : 0L)
                .lowStockCount(lowStockCount != null ? lowStockCount : 0L)
                .totalCustomers(totalCustomers != null ? totalCustomers : 0L)
                .totalOutstandingDebt(totalDebt != null ? totalDebt : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .build();
    }

    // ── Recent Sales ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RecentSaleResponse> getRecentSales() {
        String schema = getCurrentSchema();

        String sql =
                "SELECT s.id, s.invoice_no, s.total_amount, s.payment_method, s.created_at, " +
                        "       c.name     AS customer_name, " +
                        "       u.username AS cashier_name " +
                        "FROM \"" + schema + "\".sales s " +
                        "LEFT JOIN \"" + schema + "\".customers c ON c.id = s.customer_id " +
                        "LEFT JOIN \"" + schema + "\".users     u ON u.id = s.cashier_id " +
                        "ORDER BY s.created_at DESC " +
                        "LIMIT 10";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                RecentSaleResponse.builder()
                        .id(rs.getLong("id"))
                        .invoiceNo(rs.getString("invoice_no"))
                        .customerName(rs.getString("customer_name"))
                        .totalAmount(rs.getBigDecimal("total_amount"))
                        .paymentMethod(rs.getString("payment_method"))
                        .cashierName(rs.getString("cashier_name"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build());
    }

    // ── Low Stock ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LowStockResponse> getLowStock() {
        String schema = getCurrentSchema();

        String sql =
                "SELECT p.id, p.name, p.stock, p.min_stock, " +
                        "       cat.name AS category_name " +
                        "FROM \"" + schema + "\".products p " +
                        "LEFT JOIN \"" + schema + "\".categories cat ON cat.id = p.category_id " +
                        "WHERE p.stock <= p.min_stock AND p.is_active = TRUE " +
                        "ORDER BY p.stock ASC " +
                        "LIMIT 20";

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                LowStockResponse.builder()
                        .id(rs.getLong("id"))
                        .name(rs.getString("name"))
                        .stock(rs.getInt("stock"))
                        .minStock(rs.getInt("min_stock"))
                        .categoryName(rs.getString("category_name"))
                        .build());
    }

    // ── Top Products ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts() {
        String schema = getCurrentSchema();

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();

        String sql =
                "SELECT si.product_id, si.product_name, " +
                        "       SUM(si.quantity) AS total_quantity, " +
                        "       SUM(si.subtotal) AS total_revenue " +
                        "FROM \"" + schema + "\".sale_items si " +
                        "JOIN \"" + schema + "\".sales s ON s.id = si.sale_id " +
                        "WHERE s.created_at >= ? " +
                        "GROUP BY si.product_id, si.product_name " +
                        "ORDER BY total_quantity DESC " +
                        "LIMIT 5";

        return jdbcTemplate.query(sql,
                new Object[]{ startOfMonth },
                (rs, rowNum) -> TopProductResponse.builder()
                        .productId(rs.getLong("product_id"))
                        .productName(rs.getString("product_name"))
                        .totalQuantity(rs.getLong("total_quantity"))
                        .totalRevenue(rs.getBigDecimal("total_revenue"))
                        .build());
    }

    // ── Debt Summary ──────────────────────────────────────────────────────────

    /**
     * Returns total outstanding debt across all customers.
     * Also returns the single highest debtor so the admin can see
     * who owes the most at a glance.
     * ADMIN only — cashiers do not see debt data.
     */
    @Transactional(readOnly = true)
    public DebtSummaryResponse getDebtSummary() {
        String schema = getCurrentSchema();

        // Count customers who actually have outstanding debt
        Long totalCustomersWithDebt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " +
                        "FROM \"" + schema + "\".customers " +
                        "WHERE outstanding_balance > 0",
                Long.class);

        // Sum of all outstanding balances
        BigDecimal totalOutstandingDebt = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(outstanding_balance), 0) " +
                        "FROM \"" + schema + "\".customers " +
                        "WHERE outstanding_balance > 0",
                BigDecimal.class);

        // The single customer with the highest debt
        // Returns null row if no customers have debt — handled safely below
        List<java.util.Map<String, Object>> topDebtor = jdbcTemplate.queryForList(
                "SELECT name, outstanding_balance " +
                        "FROM \"" + schema + "\".customers " +
                        "WHERE outstanding_balance > 0 " +
                        "ORDER BY outstanding_balance DESC " +
                        "LIMIT 1");

        String     highestDebtorName = null;
        BigDecimal highestSingleDebt = BigDecimal.ZERO;

        if (!topDebtor.isEmpty()) {
            java.util.Map<String, Object> row = topDebtor.get(0);
            highestDebtorName = (String) row.get("name");
            highestSingleDebt = (BigDecimal) row.get("outstanding_balance");
        }

        return DebtSummaryResponse.builder()
                .totalCustomersWithDebt(
                        totalCustomersWithDebt != null
                                ? totalCustomersWithDebt.intValue() : 0)
                .totalOutstandingDebt(
                        totalOutstandingDebt != null
                                ? totalOutstandingDebt : BigDecimal.ZERO)
                .highestSingleDebt(highestSingleDebt)
                .highestDebtorName(highestDebtorName)
                .build();
    }

    // ── Cashier Performance ───────────────────────────────────────────────────

    /**
     * Returns sales totals and transaction counts per cashier for today.
     * Helps admin see which cashier is performing best on the current shift.
     * ADMIN only.
     */
    @Transactional(readOnly = true)
    public List<CashierPerformanceResponse> getCashierPerformance() {
        String schema = getCurrentSchema();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(23, 59, 59);

        String sql =
                "SELECT u.id        AS cashier_id, " +
                        "       u.username  AS cashier_name, " +
                        "       COUNT(s.id) AS transaction_count, " +
                        "       COALESCE(SUM(s.total_amount), 0) AS total_revenue " +
                        "FROM \"" + schema + "\".users u " +
                        "JOIN \"" + schema + "\".sales s ON s.cashier_id = u.id " +
                        "WHERE s.created_at BETWEEN ? AND ? " +
                        "GROUP BY u.id, u.username " +
                        "ORDER BY total_revenue DESC";

        return jdbcTemplate.query(sql,
                new Object[]{ startOfDay, endOfDay },
                (rs, rowNum) -> CashierPerformanceResponse.builder()
                        .cashierId(rs.getLong("cashier_id"))
                        .cashierName(rs.getString("cashier_name"))
                        .transactionCount(rs.getInt("transaction_count"))
                        .totalRevenue(rs.getBigDecimal("total_revenue"))
                        .build());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Reads the current tenant schema name from the authenticated user's
     * JWT claims via CustomUserDetails in the SecurityContext.
     * Never trust client-sent schema names — always read from JWT.
     */
    private String getCurrentSchema() {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return userDetails.getSchemaName();
    }
}