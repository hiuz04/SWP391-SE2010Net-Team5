package com.swp.dao;

import com.swp.model.Voucher;
import com.swp.model.dto.UserVoucherDTO;
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

    public List<Voucher> getAllExchangeVouchers(String targetUser) {
        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM vouchers
            WHERE status = 'ACTIVE'
              AND quantity > used
              AND start_date <= GETDATE()
              AND end_date >= GETDATE()
            """);

        boolean hasFilter = targetUser != null && !targetUser.isBlank() && !"ALL_TYPE".equalsIgnoreCase(targetUser);
        if (hasFilter) {
            sql.append(" AND target_user = ? ");
        }
        sql.append(" ORDER BY exchange_points ASC");

        List<Voucher> list = new ArrayList<>();

        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            if (hasFilter) {
                ps.setString(1, targetUser.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapVoucher(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách voucher đổi", e);
        }

        return list;
    }

    public boolean redeemVoucher(long userId, long voucherId) {
        String getVoucherSql = """
                SELECT * FROM vouchers WITH (UPDLOCK, ROWLOCK)
                WHERE id = ?
            """;

        String getUserSql = """
                SELECT available_reward_points
                FROM users WITH (UPDLOCK, ROWLOCK)
                WHERE user_id = ?
            """;

        String updatePointSql = """
                UPDATE users
                SET available_reward_points = available_reward_points - ?
                WHERE user_id = ? AND available_reward_points >= ?
            """;

        String updateVoucherSql = """
                UPDATE vouchers
                SET used = used + 1
                WHERE id = ? AND used < quantity AND status = 'ACTIVE'
            """;

        String insertUserVoucherSql = """
                INSERT INTO user_vouchers
                (user_id, voucher_id, status, received_at, expired_at)
                VALUES (?, ?, 'AVAILABLE', GETDATE(), ?)
            """;

        Connection conn = null;

        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            int exchangePoint;
            Timestamp endDate;
            String status;

            // Lấy thông tin voucher (khóa dòng để tránh race condition)
            try (PreparedStatement ps = conn.prepareStatement(getVoucherSql)) {
                ps.setLong(1, voucherId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    exchangePoint = rs.getInt("exchange_points");
                    endDate = rs.getTimestamp("end_date");
                    status = rs.getString("status");
                }
            }

            if (!"ACTIVE".equalsIgnoreCase(status)) {
                conn.rollback();
                return false;
            }

            int userPoint;

            // Lấy điểm user (khóa dòng)
            try (PreparedStatement ps = conn.prepareStatement(getUserSql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    userPoint = rs.getInt("available_reward_points");
                }
            }

            if (userPoint < exchangePoint) {
                conn.rollback();
                return false;
            }

            // Trừ điểm - điều kiện chặn double-check ngay trong SQL
            try (PreparedStatement ps = conn.prepareStatement(updatePointSql)) {
                ps.setInt(1, exchangePoint);
                ps.setLong(2, userId);
                ps.setInt(3, exchangePoint);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // Tăng used - điều kiện chặn double-check ngay trong SQL
            try (PreparedStatement ps = conn.prepareStatement(updateVoucherSql)) {
                ps.setLong(1, voucherId);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // Thêm vào kho voucher của user
            try (PreparedStatement ps = conn.prepareStatement(insertUserVoucherSql)) {
                ps.setLong(1, userId);
                ps.setLong(2, voucherId);
                ps.setTimestamp(3, endDate);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public List<UserVoucherDTO> getUserVouchers(long userId, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM (
                SELECT
                    uv.user_voucher_id,
                    uv.voucher_id,
                    v.code AS voucher_code,
                    v.name AS voucher_name,
                    v.discount_type,
                    v.discount_value,
                    v.min_order,
                    v.exchange_points,
                    uv.received_at,
                    uv.expired_at,
                    uv.used_at,
                    CASE
                        WHEN uv.status = 'USED' THEN 'USED'
                        WHEN uv.status = 'AVAILABLE' AND uv.expired_at < SYSDATETIME() THEN 'EXPIRED'
                        ELSE uv.status
                    END AS effective_status
                FROM user_vouchers uv
                JOIN vouchers v ON uv.voucher_id = v.id
                WHERE uv.user_id = ?
            ) t
            """);

        boolean hasFilter = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        if (hasFilter) {
            sql.append(" WHERE effective_status = ? ");
        }
        sql.append(" ORDER BY expired_at ASC");

        List<UserVoucherDTO> result = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setLong(1, userId);
            if (hasFilter) {
                ps.setString(2, status.toUpperCase());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UserVoucherDTO dto = new UserVoucherDTO();
                    dto.setUserVoucherId(rs.getLong("user_voucher_id"));
                    dto.setVoucherId(rs.getLong("voucher_id"));
                    dto.setVoucherCode(rs.getString("voucher_code"));
                    dto.setVoucherName(rs.getString("voucher_name"));
                    dto.setDiscountType(rs.getString("discount_type"));
                    dto.setDiscountValue(rs.getBigDecimal("discount_value"));
                    dto.setMinOrder(rs.getBigDecimal("min_order"));
                    dto.setExchangePoints(rs.getInt("exchange_points"));
                    dto.setReceivedAt(rs.getObject("received_at", LocalDateTime.class));
                    dto.setExpiredAt(rs.getObject("expired_at", LocalDateTime.class));
                    dto.setUsedAt(rs.getObject("used_at", LocalDateTime.class));
                    dto.setEffectiveStatus(rs.getString("effective_status"));
                    result.add(dto);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khi lấy danh sách voucher của user", e);
        }

        return result;
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
        voucher.setExchangePoint(rs.getInt("exchange_points"));
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
