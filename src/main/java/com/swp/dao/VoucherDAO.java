package com.swp.dao;

import com.swp.model.User;
import com.swp.model.Voucher;
import com.swp.model.dto.UserVoucherDTO;
import com.swp.model.dto.VoucherRedeemResult;
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

/**
 * Quản lý dữ liệu voucher cho Owner, Voucher Center, booking và payment.
 * DAO là nguồn chính cho validation/tính discount để frontend không thể gửi số tiền giảm giả.
 */
public class VoucherDAO {

    public enum UsageInsertResult {
        INSERTED,
        ALREADY_EXISTS,
        CONFLICT
    }

    private static final String TYPE_PERCENT = "PERCENT";
    private static final String TYPE_FIXED = "FIXED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String USER_VOUCHER_AVAILABLE = "AVAILABLE";
    private static final String USER_VOUCHER_RESERVED = "RESERVED";
    private static final String USER_VOUCHER_USED = "USED";
    private static final String DISTRIBUTION_PUBLIC_CODE = Voucher.DISTRIBUTION_PUBLIC_CODE;
    private static final String DISTRIBUTION_REWARD_VOUCHER = Voucher.DISTRIBUTION_REWARD_VOUCHER;
    private static final String TARGET_ALL = Voucher.TARGET_ALL;
    private static final String TARGET_MEMBER = Voucher.TARGET_MEMBER;

    /**
     * Lấy toàn bộ voucher cho màn Owner, gồm cả voucher đang tắt/hết hạn để Owner theo dõi lịch sử.
     */
    public List<Voucher> getAllVouchers() throws SQLException {
        // SQL: Lấy toàn bộ voucher cho Owner, sắp xếp voucher mới nhất trước.
        String sql = baseSelectSql() + """
                ORDER BY created_at DESC,
                         id DESC
                """;

        List<Voucher> vouchers = new ArrayList<>();
        // Duyệt toàn bộ voucher để Owner xem được cả lịch sử mã đang tắt/hết hạn.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            // Mỗi row được map sang entity Voucher cho JSP list.
            while (rs.next()) {
                vouchers.add(mapVoucher(rs));
            }
        }
        return vouchers;
    }

    /**
     * Tìm voucher theo id để Owner edit hoặc các transaction booking/payment kiểm tra lại dữ liệu gốc.
     */
    public Voucher findById(int id) throws SQLException {
        // SQL: Lấy voucher theo id bằng base select dùng chung.
        String sql = baseSelectSql() + " WHERE id = ?";
        // Lookup theo id dùng cả cho form edit và transaction validate.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                // Không tìm thấy thì trả null để caller quyết định 404/invalid.
                return rs.next() ? mapVoucher(rs) : null;
            }
        }
    }

    /**
     * Tìm voucher theo code đã trim/uppercase, dùng cho kiểm tra trùng code và PUBLIC_CODE.
     */
    public Voucher findByCode(String code) throws SQLException {
        String normalizedCode = normalizeCode(code);
        // Code rỗng/null không cần query DB.
        if (normalizedCode == null || normalizedCode.isEmpty()) {
            return null;
        }

        // SQL: Lấy voucher theo code đã normalize để validate trùng/apply PUBLIC_CODE.
        String sql = baseSelectSql() + " WHERE code = ?";
        // Code đã normalize uppercase để khớp cách Owner lưu voucher.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedCode);
            try (ResultSet rs = ps.executeQuery()) {
                // Code unique nên tối đa một voucher được trả về.
                return rs.next() ? mapVoucher(rs) : null;
            }
        }
    }

    /**
     * Tạo voucher mới. used luôn bắt đầu bằng 0 và distribution_type quyết định ý nghĩa của used về sau.
     */
    public boolean createVoucher(Voucher voucher) throws SQLException {
        // SQL: Insert voucher mới, cố định used = 0 để lượt dùng chỉ tăng qua nghiệp vụ.
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
                    distribution_type,
                    target_user,
                    exchange_points,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, GETDATE())
                """;

        // Insert voucher mới, used được cố định 0 trong SQL.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setVoucherStatement(ps, voucher);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Cập nhật voucher từ Owner nhưng không nhận used từ request.
     * used chỉ thay đổi qua redeem/payment để dữ liệu không bị sửa tay.
     */
    public boolean updateVoucher(Voucher voucher) throws SQLException {
        // SQL: Cập nhật cấu hình voucher nhưng không cập nhật used.
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
                    distribution_type = ?,
                    target_user = ?,
                    exchange_points = ?,
                    updated_at = GETDATE()
                WHERE id = ?
                """;

        // Update cấu hình voucher nhưng không update used.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setVoucherStatement(ps, voucher);
            ps.setInt(13, voucher.getId());
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Bật/tắt voucher. DISABLED chặn đổi mới nhưng không thu hồi user_vouchers đã phát hành.
     */
    public boolean updateStatus(int id, String status) throws SQLException {
        // SQL: Đổi trạng thái voucher ACTIVE/DISABLED mà không xóa dữ liệu lịch sử.
        String sql = """
                UPDATE vouchers
                SET status = ?,
                    updated_at = GETDATE()
                WHERE id = ?
                """;

        // Chỉ đổi status ACTIVE/DISABLED, không xóa voucher khỏi DB.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeStatus(status));
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Kiểm tra voucher đã được phát hành/sử dụng chưa để Owner không sửa các trường làm giảm quyền lợi.
     */
    public boolean hasIssuedOrUsed(int voucherId) throws SQLException {
        // SQL: Kiểm tra voucher đã có user_vouchers hoặc voucher_usages hay chưa.
        String sql = """
                SELECT 1
                WHERE EXISTS (
                    SELECT 1 FROM user_vouchers WHERE voucher_id = ?
                )
                   OR EXISTS (
                    SELECT 1 FROM voucher_usages WHERE voucher_id = ?
                )
                """;
        // Nếu có user_vouchers hoặc voucher_usages thì voucher đã phát hành/sử dụng.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setInt(2, voucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Tăng used cho PUBLIC_CODE sau payment success hoặc cho REWARD_VOUCHER sau redeem.
     * Điều kiện used < quantity chặn vượt số lượng khi nhiều transaction chạy song song.
     */
    public boolean incrementUsed(int voucherId, Connection conn) throws SQLException {
        // SQL: Tăng used có điều kiện used < quantity để tránh vượt số lượng voucher.
        String sql = """
                UPDATE vouchers
                SET used = used + 1,
                    updated_at = GETDATE()
                WHERE id = ?
                  AND used < quantity
                """;
        return updateUsedCounter(voucherId, conn, sql);
    }

    /**
     * Giảm used khi cần rollback nghiệp vụ phát hành; không dùng cho payment thất bại thông thường.
     */
    public boolean decrementUsed(int voucherId, Connection conn) throws SQLException {
        // SQL: Giảm used có điều kiện used > 0 để rollback lượt phát hành an toàn.
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
        return validatePublicVoucher(code, orderAmount, null);
    }

    public VoucherValidationResult validateVoucher(String code, BigDecimal orderAmount, Long customerId) throws SQLException {
        User customer = null;
        // Có customerId thì dựng User tối thiểu để kiểm tra lịch sử dùng voucher.
        if (customerId != null && customerId > 0) {
            customer = new User();
            customer.setUserId(customerId);
        }
        return validatePublicVoucher(code, orderAmount, customer);
    }

    /**
     * Validate mã công khai khi Customer nhập ở booking confirmation.
     * PUBLIC_CODE không cần user_vouchers, nhưng vẫn kiểm tra target_user và lịch sử sử dụng.
     */
    public VoucherValidationResult validatePublicVoucher(String code, BigDecimal orderAmount, User customer) throws SQLException {
        BigDecimal safeOrderAmount = money(orderAmount);
        Voucher voucher = findByCode(code);

        // Không có code tương ứng trong DB thì báo invalid ngay.
        if (voucher == null) {
            return VoucherValidationResult.invalid("Mã voucher không tồn tại.");
        }
        // PUBLIC_CODE là mã nhập trực tiếp, khác với voucher đổi điểm trong user_vouchers.
        if (!DISTRIBUTION_PUBLIC_CODE.equalsIgnoreCase(nullToDefault(voucher.getDistributionType(), DISTRIBUTION_PUBLIC_CODE))) {
            return VoucherValidationResult.invalid("Mã voucher này không phải mã công khai.");
        }
        // Voucher bị Owner tắt thì không được áp mới ở booking.
        if (!STATUS_ACTIVE.equalsIgnoreCase(voucher.getStatus())) {
            return VoucherValidationResult.invalid("Voucher không còn hoạt động.");
        }
        VoucherValidationResult commonResult = validateCommonVoucherRules(voucher, safeOrderAmount, false, isVipCurrentlyValid(customer));
        // Các rule chung gồm hạn dùng, số lượng, target VIP và min order.
        if (!commonResult.isValid()) {
            return commonResult;
        }
        // PUBLIC_CODE chỉ cho mỗi Customer dùng một lần sau payment success.
        if (customer != null
                && customer.getUserId() != null
                && hasCustomerUsedVoucher(voucher.getId(), customer.getUserId())) {
            return VoucherValidationResult.invalid("Bạn đã sử dụng mã giảm giá này.");
        }
        return buildDiscountResult(voucher, null, safeOrderAmount);
    }

    /**
     * Validate voucher đổi điểm đã thuộc sở hữu Customer trước khi preview/tạo booking.
     */
    public VoucherValidationResult validateOwnedRewardVoucher(long userVoucherId, BigDecimal orderAmount, long customerId)
            throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return validateOwnedRewardVoucher(conn, userVoucherId, orderAmount, customerId, false);
        }
    }

    /**
     * Validate và khóa user_vouchers khi booking đang chuẩn bị reserve voucher.
     * Khi forUpdate=true, UPDLOCK/HOLDLOCK chặn cùng voucher bị dùng cho hai booking HOLD song song.
     */
    public VoucherValidationResult validateOwnedRewardVoucher(
            Connection conn,
            long userVoucherId,
            BigDecimal orderAmount,
            long customerId,
            boolean forUpdate
    ) throws SQLException {
        // Id không dương thì không thể là user_voucher hợp lệ.
        if (userVoucherId <= 0 || customerId <= 0) {
            return VoucherValidationResult.invalid("Voucher không hợp lệ.");
        }

        String lockHint = forUpdate ? " WITH (UPDLOCK, HOLDLOCK) " : "";
        // SQL: Lấy user_voucher kèm voucher gốc để validate quyền sở hữu và điều kiện áp dụng.
        String sql = """
                SELECT uv.user_voucher_id,
                       uv.user_id,
                       uv.status AS user_voucher_status,
                       uv.expired_at,
                       v.id,
                       v.code,
                       v.name,
                       v.discount_type,
                       v.discount_value,
                       v.min_order,
                       v.quantity,
                       v.used,
                       v.start_date,
                       v.end_date,
                       v.status,
                       v.distribution_type,
                       v.target_user,
                       v.exchange_points,
                       v.created_at,
                       v.updated_at
                FROM user_vouchers uv
                JOIN vouchers v ON uv.voucher_id = v.id
                WHERE uv.user_voucher_id = ?
                """.replace("user_vouchers uv", "user_vouchers uv" + lockHint);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userVoucherId);
            try (ResultSet rs = ps.executeQuery()) {
                // Không có user_voucher tương ứng thì Customer không thể dùng.
                if (!rs.next()) {
                    return VoucherValidationResult.invalid("Voucher không tồn tại.");
                }

                // Voucher đổi điểm phải thuộc đúng Customer đang tạo booking.
                if (rs.getLong("user_id") != customerId) {
                    return VoucherValidationResult.invalid("Bạn không sở hữu voucher này.");
                }
                String userVoucherStatus = rs.getString("user_voucher_status");
                // Chỉ AVAILABLE mới được dùng để preview/reserve.
                if (!USER_VOUCHER_AVAILABLE.equalsIgnoreCase(userVoucherStatus)) {
                    // RESERVED cho biết voucher đang bị một booking HOLD khác giữ.
                    if (USER_VOUCHER_RESERVED.equalsIgnoreCase(userVoucherStatus)) {
                        return VoucherValidationResult.invalid("Voucher đang được giữ cho booking khác.");
                    }
                    return VoucherValidationResult.invalid("Voucher đã được sử dụng.");
                }
                LocalDateTime expiredAt = toLocalDateTime(rs.getTimestamp("expired_at"));
                // user_voucher hết hạn theo thời điểm Customer đã đổi voucher.
                if (expiredAt == null || expiredAt.isBefore(LocalDateTime.now())) {
                    return VoucherValidationResult.invalid("Voucher đã hết hạn.");
                }

                Voucher voucher = mapVoucher(rs);
                // user_voucher chỉ hợp lệ với loại phát hành REWARD_VOUCHER.
                if (!DISTRIBUTION_REWARD_VOUCHER.equalsIgnoreCase(voucher.getDistributionType())) {
                    return VoucherValidationResult.invalid("Voucher này không phải voucher đổi điểm.");
                }

                VoucherValidationResult commonResult = validateCommonVoucherRules(voucher, money(orderAmount), true, true);
                // Kiểm tra min order/thời hạn voucher gốc trước khi tính discount.
                if (!commonResult.isValid()) {
                    return commonResult;
                }

                // Khi reserve trong transaction, kiểm tra thêm có booking active nào đang giữ voucher này không.
                if (forUpdate && isUserVoucherHeldByActiveBooking(conn, userVoucherId)) {
                    return VoucherValidationResult.invalid("Voucher đang được giữ cho booking khác.");
                }

                return buildDiscountResult(voucher, userVoucherId, money(orderAmount));
            }
        }
    }

    /**
     * Danh sách voucher đổi điểm đang có thể hiển thị ở Voucher Center.
     * Filter ALL/MEMBER áp dụng target_user, không bao giờ áp dụng discount_type.
     */
    public List<Voucher> getAllExchangeVouchers(String targetUser, boolean activeVip) {
        // SQL: Dựng danh sách voucher đổi điểm còn hạn, còn lượt và đúng target user.
        StringBuilder sql = new StringBuilder("""
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
                       distribution_type,
                       target_user,
                       exchange_points,
                       created_at,
                       updated_at
                FROM vouchers
                WHERE status = 'ACTIVE'
                  AND distribution_type = 'REWARD_VOUCHER'
                  AND exchange_points > 0
                  AND quantity > used
                  AND start_date <= GETDATE()
                  AND end_date >= GETDATE()
                """);

        // VIP còn hạn được thấy cả voucher ALL và MEMBER.
        if (activeVip) {
            sql.append(" AND target_user IN ('ALL', 'MEMBER') ");
        } else {
            // Customer thường chỉ thấy voucher target ALL.
            sql.append(" AND target_user = 'ALL' ");
        }

        boolean hasTargetFilter = TARGET_ALL.equalsIgnoreCase(targetUser) || TARGET_MEMBER.equalsIgnoreCase(targetUser);
        // Filter tab ALL/MEMBER chỉ áp dụng khi tham số nằm trong whitelist.
        if (hasTargetFilter) {
            sql.append(" AND target_user = ? ");
        }
        sql.append(" ORDER BY exchange_points ASC, end_date ASC, id ASC");

        List<Voucher> list = new ArrayList<>();
        // Query voucher còn hạn, còn số lượng và đủ điều kiện đổi điểm.
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            // Bind target_user khi user chọn tab cụ thể.
            if (hasTargetFilter) {
                ps.setString(1, normalizeTargetUser(targetUser));
            }

            try (ResultSet rs = ps.executeQuery()) {
                // Map từng voucher để service chuyển thành DTO cho frontend.
                while (rs.next()) {
                    list.add(mapVoucher(rs));
                }
            }
        } catch (SQLException e) {
            // Service đang dùng RuntimeException cho API trả lỗi JSON chung.
            throw new RuntimeException("Lỗi khi lấy danh sách voucher đổi", e);
        }
        return list;
    }

    /**
     * Đổi voucher bằng điểm trong một transaction: khóa voucher, khóa user, trừ điểm, tạo user_vouchers và tăng used.
     */
    public VoucherRedeemResult redeemVoucher(long userId, long voucherId) {
        // SQL: Khóa voucher đổi điểm để kiểm tra lượt còn lại trước khi redeem.
        String getVoucherSql = baseSelectSql().replace("FROM vouchers", "FROM vouchers WITH (UPDLOCK, HOLDLOCK)") + """
                WHERE id = ?
                """;
        // SQL: Khóa user để trừ điểm reward an toàn trong transaction redeem.
        String getUserSql = """
                SELECT u.user_id,
                       u.available_reward_points,
                       u.is_vip,
                       u.vip_valid_until,
                       r.role_name
                FROM users u WITH (UPDLOCK, HOLDLOCK)
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.user_id = ?
                """;
        // SQL: Chặn Customer đổi trùng voucher khi còn bản AVAILABLE/RESERVED chưa hết hạn.
        String existingSql = """
                SELECT 1
                FROM user_vouchers WITH (UPDLOCK, HOLDLOCK)
                WHERE user_id = ?
                  AND voucher_id = ?
                  AND status IN ('AVAILABLE', 'RESERVED')
                  AND expired_at >= GETDATE()
                """;
        // SQL: Trừ điểm reward bằng điều kiện available_reward_points >= exchange_points.
        String updatePointSql = """
                UPDATE users
                SET available_reward_points = available_reward_points - ?
                WHERE user_id = ?
                  AND available_reward_points >= ?
                """;
        // SQL: Cấp user_voucher AVAILABLE cho Customer sau khi redeem thành công.
        String insertUserVoucherSql = """
                INSERT INTO user_vouchers
                    (user_id, voucher_id, status, received_at, expired_at)
                VALUES (?, ?, 'AVAILABLE', GETDATE(), ?)
                """;

        Connection conn = null;
        boolean originalAutoCommit = true;

        // Toàn bộ redeem nằm trong một transaction để không có trạng thái trừ điểm nhưng chưa cấp voucher.
        try {
            conn = DBContext.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            Voucher voucher;
            // Khóa voucher để hai khách hàng không cùng đổi lượt cuối cùng.
            try (PreparedStatement ps = conn.prepareStatement(getVoucherSql)) {
                ps.setLong(1, voucherId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return VoucherRedeemResult.failure("Voucher không tồn tại.");
                    }
                    voucher = mapVoucher(rs);
                }
            }

            // Validation nghiệp vụ chạy trong transaction sau khi voucher đã bị khóa.
            VoucherValidationResult common = validateRedeemableVoucher(voucher);
            // Voucher không còn đủ điều kiện đổi thì rollback và trả message nghiệp vụ.
            if (!common.isValid()) {
                conn.rollback();
                return VoucherRedeemResult.failure(common.getMessage());
            }

            int userPoint;
            boolean activeVip;
            String roleName;
            // Khóa dòng user để phép trừ điểm không bị chạy hai lần song song.
            try (PreparedStatement ps = conn.prepareStatement(getUserSql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return VoucherRedeemResult.failure("Bạn chưa đăng nhập.");
                    }
                    userPoint = rs.getInt("available_reward_points");
                    roleName = rs.getString("role_name");
                    LocalDateTime vipValidUntil = toLocalDateTime(rs.getTimestamp("vip_valid_until"));
                    activeVip = rs.getBoolean("is_vip")
                            && vipValidUntil != null
                            && vipValidUntil.isAfter(LocalDateTime.now());
                }
            }

            // Chỉ role CUSTOMER được đổi voucher bằng điểm.
            if (!"CUSTOMER".equalsIgnoreCase(roleName)) {
                conn.rollback();
                return VoucherRedeemResult.failure("Chỉ khách hàng mới được đổi voucher.");
            }
            // Voucher MEMBER chỉ cho Customer VIP còn hạn.
            if (TARGET_MEMBER.equalsIgnoreCase(voucher.getTargetUser()) && !activeVip) {
                conn.rollback();
                return VoucherRedeemResult.failure("Voucher chỉ dành cho thành viên VIP còn hạn.");
            }
            // Điểm hiện tại phải đủ theo exchange_points của voucher trong DB.
            if (userPoint < voucher.getExchangePoint()) {
                conn.rollback();
                return VoucherRedeemResult.failure("Bạn không đủ điểm để đổi voucher.");
            }

            // Chặn một Customer giữ nhiều bản AVAILABLE/RESERVED của cùng voucher khi business rule chỉ cho một lượt.
            try (PreparedStatement ps = conn.prepareStatement(existingSql)) {
                ps.setLong(1, userId);
                ps.setLong(2, voucherId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return VoucherRedeemResult.failure("Bạn đã đổi voucher này.");
                    }
                }
            }

            // Trừ điểm bằng điều kiện SQL để exchange_points âm/thiếu điểm không thể làm tăng điểm.
            try (PreparedStatement ps = conn.prepareStatement(updatePointSql)) {
                ps.setInt(1, voucher.getExchangePoint());
                ps.setLong(2, userId);
                ps.setInt(3, voucher.getExchangePoint());
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return VoucherRedeemResult.failure("Bạn không đủ điểm để đổi voucher.");
                }
            }

            // used của REWARD_VOUCHER là số lượt đã phát hành, tăng đúng một lần tại redeem.
            // Nếu used đã chạm quantity do transaction khác đổi trước thì rollback.
            if (!incrementUsed(voucher.getId(), conn)) {
                conn.rollback();
                return VoucherRedeemResult.failure("Voucher đã hết số lượng.");
            }

            // Cấp voucher cho Customer với hạn bằng end_date của voucher gốc tại thời điểm đổi.
            try (PreparedStatement ps = conn.prepareStatement(insertUserVoucherSql)) {
                ps.setLong(1, userId);
                ps.setLong(2, voucherId);
                ps.setTimestamp(3, Timestamp.valueOf(voucher.getEndDate()));
                ps.executeUpdate();
            }

            conn.commit();
            return VoucherRedeemResult.success("Đổi voucher thành công.");
        } catch (Exception e) {
            // Bất kỳ lỗi nào trong redeem đều rollback trước khi trả failure chung.
            if (conn != null) {
                try {
                    // Rollback toàn bộ để không có trạng thái đã trừ điểm nhưng chưa cấp voucher.
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            return VoucherRedeemResult.failure("Đã có lỗi xảy ra, vui lòng thử lại sau.");
        } finally {
            // Khôi phục autocommit và đóng connection dù redeem thành công hay thất bại.
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Lấy danh sách voucher của Customer, gồm AVAILABLE/RESERVED/USED và EXPIRED tính động.
     */
    public List<UserVoucherDTO> getUserVouchers(long userId, String status) {
        // SQL: Dựng danh sách user_vouchers và tính effective_status động cho trang Voucher của tôi.
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
                    held.booking_id AS reserved_booking_id,
                    held.booking_code AS reserved_booking_code,
                    CASE
                        WHEN uv.status = 'USED' THEN 'USED'
                        WHEN uv.status <> 'USED' AND uv.expired_at < SYSDATETIME() THEN 'EXPIRED'
                        ELSE uv.status
                    END AS effective_status
                FROM user_vouchers uv
                JOIN vouchers v ON uv.voucher_id = v.id
                OUTER APPLY (
                    SELECT TOP 1 b.booking_id, b.booking_code
                    FROM bookings b
                    WHERE b.user_voucher_id = uv.user_voucher_id
                      AND b.status IN ('HOLD', 'CONFIRMED', 'CHECKED_IN')
                    ORDER BY b.created_at DESC, b.booking_id DESC
                ) held
                WHERE uv.user_id = ?
                  AND v.distribution_type = 'REWARD_VOUCHER'
            ) t
            """);

        boolean hasFilter = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        // Filter status chỉ thêm WHERE khi Customer chọn tab cụ thể.
        if (hasFilter) {
            sql.append(" WHERE effective_status = ? ");
        }
        sql.append(" ORDER BY expired_at ASC, user_voucher_id DESC");

        List<UserVoucherDTO> result = new ArrayList<>();
        // Lấy voucher cá nhân và tính effective_status động trong SQL.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            ps.setLong(1, userId);
            // Bind status filter nếu khác ALL.
            if (hasFilter) {
                ps.setString(2, status.toUpperCase(Locale.ROOT));
            }

            try (ResultSet rs = ps.executeQuery()) {
                // Map từng user_voucher sang DTO cho trang Voucher của tôi.
                while (rs.next()) {
                    result.add(mapUserVoucherDTO(rs));
                }
            }
        } catch (SQLException e) {
            // API layer sẽ bắt RuntimeException và trả JSON lỗi chung.
            throw new RuntimeException("Lỗi khi lấy danh sách voucher của user", e);
        }
        return result;
    }

    /**
     * Lấy voucher AVAILABLE để hiển thị trong booking confirmation.
     * Voucher đã đổi vẫn được dùng nếu voucher gốc bị DISABLED sau khi phát hành.
     */
    public List<UserVoucherDTO> getAvailableUserVouchersForBooking(long userId, BigDecimal orderAmount) throws SQLException {
        // SQL: Lấy voucher đổi điểm AVAILABLE, còn hạn và đủ min_order để áp vào booking.
        String sql = """
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
                    CAST(NULL AS BIGINT) AS reserved_booking_id,
                    CAST(NULL AS NVARCHAR(50)) AS reserved_booking_code,
                    'AVAILABLE' AS effective_status
                FROM user_vouchers uv
                JOIN vouchers v ON uv.voucher_id = v.id
                WHERE uv.user_id = ?
                  AND uv.status = 'AVAILABLE'
                  AND uv.expired_at >= GETDATE()
                  AND v.distribution_type = 'REWARD_VOUCHER'
                  AND v.start_date <= GETDATE()
                  AND v.end_date >= GETDATE()
                  AND v.min_order <= ?
                ORDER BY uv.expired_at ASC, v.discount_value DESC
                """;

        List<UserVoucherDTO> result = new ArrayList<>();
        // Chỉ lấy voucher AVAILABLE, còn hạn và đạt min_order để dùng trong booking.
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setBigDecimal(2, money(orderAmount));
            try (ResultSet rs = ps.executeQuery()) {
                // Sắp xếp voucher sắp hết hạn trước để Customer dễ dùng.
                while (rs.next()) {
                    result.add(mapUserVoucherDTO(rs));
                }
            }
        }
        return result;
    }

    /**
     * Chuyển user_vouchers AVAILABLE sang RESERVED khi booking HOLD được tạo.
     */
    public VoucherValidationResult reserveOwnedRewardVoucher(
            Connection conn,
            long userVoucherId,
            long customerId,
            BigDecimal orderAmount
    ) throws SQLException {
        VoucherValidationResult validation = validateOwnedRewardVoucher(conn, userVoucherId, orderAmount, customerId, true);
        // Validate fail thì không update trạng thái user_vouchers.
        if (!validation.isValid()) {
            return validation;
        }

        // SQL: Reserve user_voucher bằng cách chuyển AVAILABLE sang RESERVED trong transaction tạo HOLD.
        String updateSql = """
                UPDATE user_vouchers
                SET status = 'RESERVED'
                WHERE user_voucher_id = ?
                  AND user_id = ?
                  AND status = 'AVAILABLE'
                  AND expired_at >= GETDATE()
                """;

        // Update AVAILABLE -> RESERVED; điều kiện status giúp chống double reserve.
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setLong(1, userVoucherId);
            ps.setLong(2, customerId);
            // Không update được đúng một row nghĩa là voucher đã bị giữ/đổi trạng thái.
            if (ps.executeUpdate() != 1) {
                return VoucherValidationResult.invalid("Voucher đang được giữ cho booking khác.");
            }
        }
        return validation;
    }

    /**
     * Trả voucher RESERVED về AVAILABLE khi booking bị hủy hoặc HOLD hết hạn.
     */
    public boolean releaseReservedUserVoucher(Connection conn, long userVoucherId, long customerId) throws SQLException {
        // Id không hợp lệ thì không cần gọi DB.
        if (userVoucherId <= 0 || customerId <= 0) {
            return false;
        }
        // SQL: Trả user_voucher RESERVED về AVAILABLE khi booking HOLD bị hủy/hết hạn.
        String sql = """
                UPDATE user_vouchers
                SET status = 'AVAILABLE'
                WHERE user_voucher_id = ?
                  AND user_id = ?
                  AND status = 'RESERVED'
                """;
        // Chỉ trả voucher đang RESERVED, không đụng voucher USED/AVAILABLE.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userVoucherId);
            ps.setLong(2, customerId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Đánh dấu voucher đã đổi là USED khi payment success.
     */
    public boolean markUserVoucherUsed(Connection conn, long userVoucherId, long customerId) throws SQLException {
        // Id không hợp lệ thì không được update user_vouchers.
        if (userVoucherId <= 0 || customerId <= 0) {
            return false;
        }
        // SQL: Chuyển user_voucher RESERVED/AVAILABLE sang USED khi payment success.
        String sql = """
                UPDATE user_vouchers
                SET status = 'USED',
                    used_at = COALESCE(used_at, GETDATE())
                WHERE user_voucher_id = ?
                  AND user_id = ?
                  AND status IN ('RESERVED', 'AVAILABLE')
                """;
        // Payment success chuyển voucher RESERVED/AVAILABLE sang USED.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userVoucherId);
            ps.setLong(2, customerId);
            // Update thành công thì voucher đã được ghi nhận dùng.
            if (ps.executeUpdate() == 1) {
                return true;
            }
        }

        // SQL: Kiểm tra idempotent nếu callback trước đó đã mark user_voucher USED.
        String checkSql = """
                SELECT 1
                FROM user_vouchers
                WHERE user_voucher_id = ?
                  AND user_id = ?
                  AND status = 'USED'
                """;
        // Idempotent: nếu callback trước đó đã mark USED thì vẫn xem là thành công.
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setLong(1, userVoucherId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Kiểm tra Customer đã dùng PUBLIC_CODE này chưa.
     */
    public boolean hasCustomerUsedVoucher(int voucherId, long customerId) throws SQLException {
        return hasCustomerUsedVoucher(voucherId, customerId, null);
    }

    public boolean hasCustomerUsedVoucher(int voucherId, long customerId, Connection conn) throws SQLException {
        // Id không hợp lệ thì mặc định chưa dùng để caller tự validate input chính.
        if (voucherId <= 0 || customerId <= 0) {
            return false;
        }

        // SQL: Kiểm tra Customer đã dùng PUBLIC_CODE này trong voucher_usages hay chưa.
        String sql = """
                SELECT 1
                FROM voucher_usages
                WHERE voucher_id = ?
                  AND customer_id = ?
                  AND user_voucher_id IS NULL
                """;

        // Nếu caller đang có transaction thì dùng chung connection để giữ lock/nhất quán.
        if (conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, voucherId);
                ps.setLong(2, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }

        // Không có transaction ngoài thì tự mở connection read-only.
        try (Connection ownConn = DBContext.getConnection();
             PreparedStatement ps = ownConn.prepareStatement(sql)) {
            ps.setInt(1, voucherId);
            ps.setLong(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Ghi usage idempotent cho booking payment success.
     * Kết quả INSERTED mới được phép tăng used cho PUBLIC_CODE; ALREADY_EXISTS không tăng lại.
     */
    public UsageInsertResult recordUsageIfAbsent(
            int voucherId,
            Long userVoucherId,
            long customerId,
            long bookingId,
            long paymentId,
            Connection conn
    ) throws SQLException {
        // Input không hợp lệ được xem là conflict để không ghi usage sai.
        if (voucherId <= 0 || customerId <= 0 || bookingId <= 0 || paymentId <= 0) {
            return UsageInsertResult.CONFLICT;
        }

        // SQL: Query insert usage được chọn theo PUBLIC_CODE hoặc REWARD_VOUCHER.
        String sql;
        // PUBLIC_CODE dùng user_voucher_id NULL và chặn dùng lại theo customer/voucher.
        if (userVoucherId == null) {
            // SQL: Insert usage cho PUBLIC_CODE nếu booking chưa có usage và Customer chưa dùng mã này.
            sql = """
                    INSERT INTO voucher_usages (
                        voucher_id,
                        user_voucher_id,
                        customer_id,
                        booking_id,
                        payment_id,
                        used_at
                    )
                    SELECT ?, NULL, ?, ?, ?, GETDATE()
                    WHERE NOT EXISTS (
                        SELECT 1 FROM voucher_usages WITH (UPDLOCK, HOLDLOCK)
                        WHERE booking_id = ?
                    )
                      AND NOT EXISTS (
                        SELECT 1 FROM voucher_usages WITH (UPDLOCK, HOLDLOCK)
                        WHERE voucher_id = ?
                          AND customer_id = ?
                          AND user_voucher_id IS NULL
                    )
                    """;
        } else {
            // REWARD_VOUCHER chặn dùng lại theo user_voucher_id duy nhất.
            // SQL: Insert usage cho REWARD_VOUCHER nếu booking/user_voucher chưa được ghi nhận.
            sql = """
                    INSERT INTO voucher_usages (
                        voucher_id,
                        user_voucher_id,
                        customer_id,
                        booking_id,
                        payment_id,
                        used_at
                    )
                    SELECT ?, ?, ?, ?, ?, GETDATE()
                    WHERE NOT EXISTS (
                        SELECT 1 FROM voucher_usages WITH (UPDLOCK, HOLDLOCK)
                        WHERE booking_id = ?
                    )
                      AND NOT EXISTS (
                        SELECT 1 FROM voucher_usages WITH (UPDLOCK, HOLDLOCK)
                        WHERE user_voucher_id = ?
                    )
                    """;
        }

        // Bind theo thứ tự khác nhau cho PUBLIC_CODE và REWARD_VOUCHER.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int index = 1;
            ps.setInt(index++, voucherId);
            // REWARD_VOUCHER có thêm userVoucherId trong INSERT.
            if (userVoucherId != null) {
                ps.setLong(index++, userVoucherId);
            }
            ps.setLong(index++, customerId);
            ps.setLong(index++, bookingId);
            ps.setLong(index++, paymentId);
            ps.setLong(index++, bookingId);
            // Public code kiểm tra trùng theo voucher/customer; reward voucher kiểm theo userVoucherId.
            if (userVoucherId == null) {
                ps.setInt(index++, voucherId);
                ps.setLong(index, customerId);
            } else {
                ps.setLong(index, userVoucherId);
            }
            // INSERT thành công thì caller mới được tăng used cho PUBLIC_CODE.
            if (ps.executeUpdate() == 1) {
                return UsageInsertResult.INSERTED;
            }
        }

        // Nếu booking đã có usage thì đây là callback lặp; ngược lại là conflict nghiệp vụ.
        return voucherUsageExistsForBooking(conn, bookingId)
                ? UsageInsertResult.ALREADY_EXISTS
                : UsageInsertResult.CONFLICT;
    }

    public int countAllStatusVoucher() {
        // SQL: Đếm voucher ACTIVE còn lượt và còn hạn cho dashboard.
        String sql = """
            SELECT COUNT(*)
            FROM vouchers
            WHERE status = 'ACTIVE'
              AND quantity > used
              AND start_date <= GETDATE()
              AND end_date >= GETDATE()
            """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Dashboard chỉ cần một giá trị đếm.
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            // Lỗi dashboard không làm hỏng page, trả 0 sau khi log.
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Hàm tính discount chung cho PUBLIC_CODE và REWARD_VOUCHER.
     */
    public static BigDecimal calculateDiscountAmount(String discountType, BigDecimal discountValue, BigDecimal orderAmount) {
        BigDecimal safeOrderAmount = toMoney(orderAmount);
        BigDecimal safeDiscountValue = toMoney(discountValue);
        // Discount value phải dương cho cả PERCENT và FIXED.
        if (safeDiscountValue.signum() <= 0) {
            throw new IllegalArgumentException("Giá trị voucher không hợp lệ.");
        }

        BigDecimal discountAmount;
        String type = discountType == null ? "" : discountType.trim().toUpperCase(Locale.ROOT);
        // PERCENT tính theo phần trăm giá gốc và không được vượt quá 100.
        if (TYPE_PERCENT.equals(type)) {
            // Percent không được vượt 100 để discount không vượt giá trị đơn.
            if (safeDiscountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new IllegalArgumentException("Voucher phần trăm không được vượt quá 100.");
            }
            discountAmount = safeOrderAmount
                    .multiply(safeDiscountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (TYPE_FIXED.equals(type)) {
            discountAmount = safeDiscountValue;
        } else {
            // Discount type lạ bị chặn để không tính sai số tiền.
            throw new IllegalArgumentException("Loại voucher không hợp lệ.");
        }

        // Discount không bao giờ làm tổng tiền âm.
        return discountAmount.compareTo(safeOrderAmount) > 0 ? safeOrderAmount : discountAmount;
    }

    private VoucherValidationResult validateRedeemableVoucher(Voucher voucher) {
        // Chỉ REWARD_VOUCHER mới được đổi bằng điểm trong Voucher Center.
        if (!DISTRIBUTION_REWARD_VOUCHER.equalsIgnoreCase(voucher.getDistributionType())) {
            return VoucherValidationResult.invalid("Voucher này không dùng để đổi điểm.");
        }
        // Voucher đổi điểm phải có exchange_points dương.
        if (voucher.getExchangePoint() <= 0) {
            return VoucherValidationResult.invalid("Điểm cần đổi không hợp lệ.");
        }
        return validateCommonVoucherRules(voucher, BigDecimal.ZERO, false, true);
    }

    private VoucherValidationResult validateCommonVoucherRules(
            Voucher voucher,
            BigDecimal orderAmount,
            boolean ownedRewardVoucher,
            boolean activeVip
    ) {
        // Voucher null nghĩa là lookup/validate trước đó không tìm thấy dữ liệu.
        if (voucher == null) {
            return VoucherValidationResult.invalid("Voucher không tồn tại.");
        }
        // PUBLIC_CODE/redeem mới phải ACTIVE; voucher đã sở hữu vẫn có thể dùng nếu Owner tắt sau khi phát hành.
        if (!ownedRewardVoucher && !STATUS_ACTIVE.equalsIgnoreCase(voucher.getStatus())) {
            return VoucherValidationResult.invalid("Voucher không còn hoạt động.");
        }
        LocalDateTime now = LocalDateTime.now();
        // Chưa tới start_date thì chưa cho áp/đổi voucher.
        if (voucher.getStartDate() == null || voucher.getStartDate().isAfter(now)) {
            return VoucherValidationResult.invalid("Voucher chưa đến thời gian sử dụng.");
        }
        // Quá end_date thì voucher hết hạn.
        if (voucher.getEndDate() == null || voucher.getEndDate().isBefore(now)) {
            return VoucherValidationResult.invalid("Voucher đã hết hạn.");
        }
        // Voucher chưa sở hữu phải còn lượt; owned reward voucher đã trừ lượt lúc redeem.
        if (!ownedRewardVoucher && voucher.getUsed() >= voucher.getQuantity()) {
            return VoucherValidationResult.invalid("Voucher đã hết lượt.");
        }
        // Voucher MEMBER chỉ hợp lệ với Customer VIP còn hạn.
        if (TARGET_MEMBER.equalsIgnoreCase(voucher.getTargetUser()) && !activeVip) {
            return VoucherValidationResult.invalid("Voucher chỉ dành cho thành viên VIP.");
        }
        BigDecimal minOrder = money(voucher.getMinOrder());
        // Chỉ kiểm min_order khi có orderAmount thực sự cần áp voucher.
        if (orderAmount != null && orderAmount.compareTo(BigDecimal.ZERO) > 0 && money(orderAmount).compareTo(minOrder) < 0) {
            return VoucherValidationResult.invalid("Giá trị đơn hàng chưa đạt mức tối thiểu.");
        }
        return VoucherValidationResult.valid(voucher, BigDecimal.ZERO, money(orderAmount));
    }

    private VoucherValidationResult buildDiscountResult(Voucher voucher, Long userVoucherId, BigDecimal orderAmount) {
        // Tính discount bằng helper chung để PUBLIC_CODE và REWARD_VOUCHER thống nhất.
        try {
            BigDecimal discountAmount = calculateDiscountAmount(voucher.getDiscountType(), voucher.getDiscountValue(), orderAmount);
            BigDecimal finalAmount = money(orderAmount).subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
            // userVoucherId null nghĩa là mã công khai.
            if (userVoucherId == null) {
                return VoucherValidationResult.valid(voucher, discountAmount, finalAmount);
            }
            return VoucherValidationResult.validOwned(voucher, userVoucherId, discountAmount, finalAmount);
        } catch (IllegalArgumentException e) {
            // Discount type/value sai được trả thành validation invalid cho UI.
            return VoucherValidationResult.invalid(e.getMessage());
        }
    }

    private boolean isUserVoucherHeldByActiveBooking(Connection conn, long userVoucherId) throws SQLException {
        // SQL: Kiểm tra user_voucher đang bị booking active giữ bằng lock để chống reserve trùng.
        String sql = """
                SELECT 1
                FROM bookings WITH (UPDLOCK, HOLDLOCK)
                WHERE user_voucher_id = ?
                  AND status IN ('HOLD', 'CONFIRMED', 'CHECKED_IN')
                """;
        // Lock các booking active đang dùng voucher để tránh reserve trùng.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userVoucherId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean voucherUsageExistsForBooking(Connection conn, long bookingId) throws SQLException {
        // SQL: Kiểm tra booking đã có voucher_usage để phân biệt callback lặp với conflict.
        String sql = "SELECT 1 FROM voucher_usages WHERE booking_id = ?";
        // Dùng để phân biệt callback lặp với conflict khi insert usage không thành công.
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String baseSelectSql() {
        // SQL: Select voucher dùng chung cho list, lookup, validate và redeem.
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
                       distribution_type,
                       target_user,
                       exchange_points,
                       created_at,
                       updated_at
                FROM vouchers
                """;
    }

    private void setVoucherStatement(PreparedStatement ps, Voucher voucher)
            throws SQLException {
        // Code được chuẩn hóa uppercase để unique index không có bản ghi trùng khác hoa/thường.
        ps.setString(1, normalizeCode(voucher.getCode()));
        ps.setString(2, trim(voucher.getName()));
        ps.setString(3, normalizeType(voucher.getDiscountType()));
        ps.setBigDecimal(4, money(voucher.getDiscountValue()));
        ps.setBigDecimal(5, money(voucher.getMinOrder()));
        ps.setInt(6, voucher.getQuantity());
        ps.setTimestamp(7, Timestamp.valueOf(voucher.getStartDate()));
        ps.setTimestamp(8, Timestamp.valueOf(voucher.getEndDate()));
        ps.setString(9, normalizeStatus(voucher.getStatus()));
        String distributionType = normalizeDistributionType(voucher.getDistributionType());
        ps.setString(10, distributionType);
        ps.setString(11, normalizeTargetUser(voucher.getTargetUser()));
        ps.setInt(12, DISTRIBUTION_PUBLIC_CODE.equals(distributionType) ? 0 : voucher.getExchangePoint());
    }

    private boolean updateUsedCounter(int voucherId, Connection conn, String sql) throws SQLException {
        // Voucher id không hợp lệ thì không update counter.
        if (voucherId <= 0) {
            return false;
        }

        // Nếu caller đã có transaction thì dùng chung connection để counter commit cùng nghiệp vụ.
        if (conn != null) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, voucherId);
                return ps.executeUpdate() == 1;
            }
        }

        // Không có transaction ngoài thì tự mở connection cho thao tác counter độc lập.
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
        voucher.setDistributionType(nullToDefault(rs.getString("distribution_type"), DISTRIBUTION_PUBLIC_CODE));
        voucher.setTargetUser(nullToDefault(rs.getString("target_user"), TARGET_ALL));
        voucher.setExchangePoint(rs.getInt("exchange_points"));
        voucher.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        voucher.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        return voucher;
    }

    private UserVoucherDTO mapUserVoucherDTO(ResultSet rs) throws SQLException {
        UserVoucherDTO dto = new UserVoucherDTO();
        dto.setUserVoucherId(rs.getLong("user_voucher_id"));
        dto.setVoucherId(rs.getLong("voucher_id"));
        dto.setVoucherCode(rs.getString("voucher_code"));
        dto.setVoucherName(rs.getString("voucher_name"));
        dto.setDiscountType(rs.getString("discount_type"));
        dto.setDiscountValue(rs.getBigDecimal("discount_value"));
        dto.setMinOrder(rs.getBigDecimal("min_order"));
        dto.setExchangePoints(rs.getInt("exchange_points"));
        dto.setReceivedAt(toLocalDateTime(rs.getTimestamp("received_at")));
        dto.setExpiredAt(toLocalDateTime(rs.getTimestamp("expired_at")));
        dto.setUsedAt(toLocalDateTime(rs.getTimestamp("used_at")));
        dto.setEffectiveStatus(rs.getString("effective_status"));
        long reservedBookingId = rs.getLong("reserved_booking_id");
        dto.setReservedBookingId(rs.wasNull() ? null : reservedBookingId);
        dto.setReservedBookingCode(rs.getString("reserved_booking_code"));
        return dto;
    }

    private boolean isVipCurrentlyValid(User user) {
        // Không có user id thì xem như không phải VIP.
        if (user == null || user.getUserId() == null) {
            return false;
        }
        // Nếu object đã có hạn VIP thì dùng trực tiếp để tránh query thừa.
        if (user.getVipValidUntil() != null) {
            return user.isVip() && user.getVipValidUntil().isAfter(LocalDateTime.now());
        }
        // Fallback đọc DB khi session User thiếu vip_valid_until.
        try {
            return new UserDAO().getUserById(user.getUserId())
                    .map(latest -> latest.isVip()
                            && latest.getVipValidUntil() != null
                            && latest.getVipValidUntil().isAfter(LocalDateTime.now()))
                    .orElse(false);
        } catch (RuntimeException e) {
            // Không xác minh được VIP thì fallback false để không cấp quyền voucher MEMBER sai.
            return false;
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private BigDecimal money(BigDecimal value) {
        return toMoney(value);
    }

    private static BigDecimal toMoney(BigDecimal value) {
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
        String value = status == null ? STATUS_ACTIVE : status.trim().toUpperCase(Locale.ROOT);
        // Status lạ được đưa về ACTIVE để tránh lưu giá trị ngoài domain.
        return STATUS_DISABLED.equals(value) ? STATUS_DISABLED : STATUS_ACTIVE;
    }

    private String normalizeDistributionType(String value) {
        String normalized = value == null ? DISTRIBUTION_PUBLIC_CODE : value.trim().toUpperCase(Locale.ROOT);
        // Chỉ cho hai loại phát hành; giá trị lạ fallback PUBLIC_CODE.
        return DISTRIBUTION_REWARD_VOUCHER.equals(normalized) ? DISTRIBUTION_REWARD_VOUCHER : DISTRIBUTION_PUBLIC_CODE;
    }

    private String normalizeTargetUser(String value) {
        String normalized = value == null ? TARGET_ALL : value.trim().toUpperCase(Locale.ROOT);
        // Target lạ fallback ALL để không vô tình khóa voucher khỏi mọi Customer.
        return TARGET_MEMBER.equals(normalized) ? TARGET_MEMBER : TARGET_ALL;
    }

    private String nullToDefault(String value, String defaultValue) {
        // Dữ liệu cũ có thể null/blank nên cần default domain value.
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
