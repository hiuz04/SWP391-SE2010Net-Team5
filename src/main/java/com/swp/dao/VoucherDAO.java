package com.swp.dao;

import com.swp.model.Voucher;
import com.swp.model.dto.VoucherValidationResult;
import com.swp.util.DBContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoucherDAO {

    private static final String TYPE_PERCENT = "PERCENT";
    private static final String TYPE_FIXED = "FIXED";
    private static final String STATUS_ACTIVE = "ACTIVE";

    public List<Voucher> getAllVouchers() throws SQLException {
        String sql = """
                SELECT id,
                       code,
                       name,
                       discount_type,
                       discount_value,
                       min_order,
                       quantity,
                       used,
                       start_date,
                       end_date,
                       status,
                       created_at,
                       updated_at
                FROM vouchers
                ORDER BY created_at DESC,
                         id DESC
                """;

        List<Voucher> vouchers = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                vouchers.add(mapVoucher(rs));
            }
        }
        return vouchers;
    }

    public Voucher findById(int id) throws SQLException {
        String sql = baseSelectSql() + " WHERE id = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapVoucher(rs) : null;
            }
        }
    }

    public Voucher findByCode(String code) throws SQLException {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null || normalizedCode.isEmpty()) {
            return null;
        }

        String sql = baseSelectSql() + " WHERE code = ?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapVoucher(rs) : null;
            }
        }
    }

    public boolean createVoucher(Voucher voucher) throws SQLException {
        String sql = """
                INSERT INTO vouchers (
                    code,
                    name,
                    discount_type,
                    discount_value,
                    min_order,
                    quantity,
                    used,
                    start_date,
                    end_date,
                    status,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, GETDATE())
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setVoucherStatement(ps, voucher);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateVoucher(Voucher voucher) throws SQLException {
        String sql = """
                UPDATE vouchers
                SET code = ?,
                    name = ?,
                    discount_type = ?,
                    discount_value = ?,
                    min_order = ?,
                    quantity = ?,
                    start_date = ?,
                    end_date = ?,
                    status = ?,
                    updated_at = GETDATE()
                WHERE id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setVoucherStatement(ps, voucher);
            ps.setInt(10, voucher.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        String sql = """
                UPDATE vouchers
                SET status = ?,
                    updated_at = GETDATE()
                WHERE id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeStatus(status));
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean incrementUsed(int voucherId, Connection conn) throws SQLException {
        String sql = """
                UPDATE vouchers
                SET used = used + 1,
                    updated_at = GETDATE()
                WHERE id = ?
                  AND used < quantity
                """;
        return updateUsedCounter(voucherId, conn, sql);
    }

    public boolean decrementUsed(int voucherId, Connection conn) throws SQLException {
        String sql = """
                UPDATE vouchers
                SET used = used - 1,
                    updated_at = GETDATE()
                WHERE id = ?
                  AND used > 0
                """;
        return updateUsedCounter(voucherId, conn, sql);
    }

    public VoucherValidationResult validateVoucher(String code, BigDecimal orderAmount) throws SQLException {
        return validateVoucher(code, orderAmount, null);
    }

    public VoucherValidationResult validateVoucher(String code, BigDecimal orderAmount, Long customerId) throws SQLException {
        BigDecimal safeOrderAmount = money(orderAmount);
        Voucher voucher = findByCode(code);
        if (voucher == null) {
            return VoucherValidationResult.invalid("Mã giảm giá không tồn tại.");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(voucher.getStatus())) {
            return VoucherValidationResult.invalid("Mã giảm giá không còn hoạt động.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() == null || voucher.getStartDate().isAfter(now)) {
            return VoucherValidationResult.invalid("Mã giảm giá chưa đến thời gian sử dụng.");
        }
        if (voucher.getEndDate() == null || voucher.getEndDate().isBefore(now)) {
            return VoucherValidationResult.invalid("Mã giảm giá đã hết hạn.");
        }
        if (customerId != null
                && customerId > 0
                && hasCustomerUsedVoucher(voucher.getId(), customerId)) {
            return VoucherValidationResult.invalid("Bạn đã sử dụng mã giảm giá này.");
        }
        if (voucher.getUsed() >= voucher.getQuantity()) {
            return VoucherValidationResult.invalid("Mã giảm giá đã hết lượt sử dụng.");
        }

        BigDecimal minOrder = money(voucher.getMinOrder());
        if (safeOrderAmount.compareTo(minOrder) < 0) {
            return VoucherValidationResult.invalid("Đơn hàng chưa đạt giá trị tối thiểu để dùng mã giảm giá.");
        }

        BigDecimal discountValue = money(voucher.getDiscountValue());
        if (discountValue.signum() <= 0) {
            return VoucherValidationResult.invalid("Giá trị mã giảm giá không hợp lệ.");
        }

        String type = normalizeType(voucher.getDiscountType());
        BigDecimal discountAmount;
        if (TYPE_PERCENT.equals(type)) {
            if (discountValue.compareTo(BigDecimal.ONE) < 0
                    || discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                return VoucherValidationResult.invalid("Giá trị mã giảm theo phần trăm không hợp lệ.");
            }
            discountAmount = safeOrderAmount
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (TYPE_FIXED.equals(type)) {
            discountAmount = discountValue.setScale(2, RoundingMode.HALF_UP);
        } else {
            return VoucherValidationResult.invalid("Loại mã giảm giá không hợp lệ.");
        }

        if (discountAmount.compareTo(safeOrderAmount) > 0) {
            discountAmount = safeOrderAmount;
        }

        BigDecimal finalAmount = safeOrderAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        return VoucherValidationResult.valid(voucher, discountAmount, finalAmount);
    }

    public boolean hasCustomerUsedVoucher(int voucherId, long customerId) throws SQLException {
        return hasCustomerUsedVoucher(voucherId, customerId, null);
    }

    public boolean hasCustomerUsedVoucher(int voucherId, long customerId, Connection conn) throws SQLException {
        if (voucherId <= 0 || customerId <= 0) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM voucher_usages
                WHERE voucher_id = ?
                  AND customer_id = ?
                """;

        if (conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, voucherId);
                ps.setLong(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        try (Connection ownConn = DBContext.getConnection();
             PreparedStatement ps = ownConn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean recordUsage(
            int voucherId,
            long customerId,
            long bookingId,
            long paymentId,
            Connection conn
    ) throws SQLException {
        if (voucherId <= 0 || customerId <= 0 || bookingId <= 0 || paymentId <= 0) {
            return false;
        }

        String sql = """
                INSERT INTO voucher_usages (
                    voucher_id,
                    customer_id,
                    booking_id,
                    payment_id,
                    used_at
                )
                SELECT ?, ?, ?, ?, GETDATE()
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM voucher_usages WITH (UPDLOCK, HOLDLOCK)
                    WHERE voucher_id = ?
                      AND customer_id = ?
                )
                """;

        if (conn != null) {
            return insertUsage(conn, sql, voucherId, customerId, bookingId, paymentId);
        }

        try (Connection ownConn = DBContext.getConnection()) {
            return insertUsage(ownConn, sql, voucherId, customerId, bookingId, paymentId);
        }
    }

    private boolean insertUsage(
            Connection conn,
            String sql,
            int voucherId,
            long customerId,
            long bookingId,
            long paymentId
    ) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setLong(2, customerId);
            ps.setLong(3, bookingId);
            ps.setLong(4, paymentId);
            ps.setInt(5, voucherId);
            ps.setLong(6, customerId);
            return ps.executeUpdate() == 1;
        }
    }

    private String baseSelectSql() {
        return """
                SELECT id,
                       code,
                       name,
                       discount_type,
                       discount_value,
                       min_order,
                       quantity,
                       used,
                       start_date,
                       end_date,
                       status,
                       created_at,
                       updated_at
                FROM vouchers
                """;
    }

    private void setVoucherStatement(PreparedStatement ps, Voucher voucher)
            throws SQLException {
        ps.setString(1, normalizeCode(voucher.getCode()));
        ps.setString(2, voucher.getName());
        ps.setString(3, normalizeType(voucher.getDiscountType()));
        ps.setBigDecimal(4, money(voucher.getDiscountValue()));
        ps.setBigDecimal(5, money(voucher.getMinOrder()));
        ps.setInt(6, voucher.getQuantity());
        ps.setTimestamp(7, Timestamp.valueOf(voucher.getStartDate()));
        ps.setTimestamp(8, Timestamp.valueOf(voucher.getEndDate()));
        ps.setString(9, normalizeStatus(voucher.getStatus()));
    }

    private boolean updateUsedCounter(int voucherId, Connection conn, String sql) throws SQLException {
        if (voucherId <= 0) {
            return false;
        }

        if (conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, voucherId);
                return ps.executeUpdate() == 1;
            }
        }

        try (Connection ownConn = DBContext.getConnection();
             PreparedStatement ps = ownConn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            return ps.executeUpdate() == 1;
        }
    }

    private Voucher mapVoucher(ResultSet rs) throws SQLException {
        Voucher voucher = new Voucher();
        voucher.setId(rs.getInt("id"));
        voucher.setCode(rs.getString("code"));
        voucher.setName(rs.getString("name"));
        voucher.setDiscountType(rs.getString("discount_type"));
        voucher.setDiscountValue(rs.getBigDecimal("discount_value"));
        voucher.setMinOrder(rs.getBigDecimal("min_order"));
        voucher.setQuantity(rs.getInt("quantity"));
        voucher.setUsed(rs.getInt("used"));
        voucher.setStartDate(toLocalDateTime(rs.getTimestamp("start_date")));
        voucher.setEndDate(toLocalDateTime(rs.getTimestamp("end_date")));
        voucher.setStatus(rs.getString("status"));
        voucher.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        voucher.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return voucher;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeType(String type) {
        return type == null ? null : type.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }
}
