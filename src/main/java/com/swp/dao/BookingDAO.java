package com.swp.dao;

import com.swp.model.Booking;
import com.swp.model.Field;
import com.swp.model.FieldMaintenanceSchedule;
import com.swp.model.dto.BookingView;
import com.swp.util.DBContext;

import java.awt.print.Book;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Cung cấp toàn bộ truy vấn và transaction cho booking của Customer: dựng lịch sân,
 * kiểm tra khả dụng, tính giá, tạo HOLD, tạo nhóm booking định kỳ, hủy booking và dọn HOLD hết hạn.
 */
/*
 * BookingDAO owns all SQL for booking screens, availability checks,
 * transactional HOLD creation, recurring groups, and cancellation updates.
 */
public class BookingDAO {

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String HOLD_EXPIRED_CANCEL_REASON =
            "Th\u1eddi gian gi\u1eef ch\u1ed7 \u0111\u00e3 h\u1ebft h\u1ea1n.";
    private static final int DEFAULT_CANCEL_BEFORE_HOURS = 24;

    public Long getComplexIdByFieldId(Long fieldId) throws SQLException {
        String sql = """
                SELECT complex_id
                FROM fields
                WHERE field_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("complex_id");
                }
            }
        }

        return null;
    }

    private FieldPricingContext getFieldPricingContext(Long fieldId) throws SQLException {
        String sql = """
                SELECT complex_id,
                       field_type_id
                FROM fields
                WHERE field_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FieldPricingContext(
                            rs.getLong("complex_id"),
                            rs.getInt("field_type_id"),
                            fieldId
                    );
                }
            }
        }

        return null;
    }

    public List<Field> getFieldsByComplex(Long complexId) throws SQLException {
        String sql = """
                SELECT field_id,
                       complex_id,
                       field_type_id,
                       field_name,
                       description,
                       status,
                       created_at,
                       updated_at
                FROM fields
                WHERE complex_id = ?
                ORDER BY field_name
                """;

        List<Field> fields = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, complexId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Field field = new Field();

                    field.setFieldId(rs.getLong("field_id"));
                    field.setComplexId(rs.getLong("complex_id"));
                    field.setFieldTypeId(rs.getInt("field_type_id"));
                    field.setFieldName(rs.getString("field_name"));
                    field.setDescription(rs.getString("description"));
                    field.setStatus(rs.getString("status"));
                    field.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    field.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));

                    fields.add(field);
                }
            }
        }

        return fields;
    }

    /**
     * Lấy các booking còn ảnh hưởng đến lịch trong một ngày của complex.
     * Trước khi đọc lịch, HOLD quá hạn được hủy để slot đã hết thời gian giữ chỗ có thể mở lại.
     */
    public List<Booking> getBookingsByComplexAndDate(Long complexId, LocalDate date) throws SQLException {
        cancelExpiredHolds();

        String sql = """
                SELECT booking_id,
                       booking_code,
                       customer_id,
                       complex_id,
                       field_id,
                       recurring_group_id,
                       start_time,
                       end_time,
                       voucher_id,
                       original_price,
                       discount_amount,
                       total_amount,
                       final_amount,
                       deposit_amount,
                       status,
                       hold_expires_at,
                       cancellation_reason,
                       cancelled_at,
                       qr_code,
                       created_at,
                       updated_at
                FROM bookings
                WHERE complex_id = ?
                  AND status NOT IN ('CANCELLED', 'EXPIRED', 'REJECTED')
                  AND start_time < ?
                  AND end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, complexId);
            ps.setTimestamp(2, Timestamp.valueOf(dayEnd));
            ps.setTimestamp(3, Timestamp.valueOf(dayStart));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBooking(rs));
                }
            }
        }

        return bookings;
    }

    /**
     * Lấy lịch bảo trì giao với ngày đang xem để đánh dấu slot không thể đặt.
     * Điều kiện overlap dùng cùng nguyên tắc với booking để không bỏ sót bảo trì vắt qua ngày.
     */
    public List<FieldMaintenanceSchedule> getMaintenanceByComplexAndDate(Long complexId, LocalDate date)
            throws SQLException {

        String sql = """
                SELECT m.maintenance_id,
                       m.field_id,
                       m.start_time,
                       m.end_time,
                       m.reason,
                       m.status,
                       m.created_at
                FROM field_maintenance_schedules m
                INNER JOIN fields f ON m.field_id = f.field_id
                WHERE f.complex_id = ?
                  AND m.status <> 'CANCELLED'
                  AND m.start_time < ?
                  AND m.end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<FieldMaintenanceSchedule> schedules = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, complexId);
            ps.setTimestamp(2, Timestamp.valueOf(dayEnd));
            ps.setTimestamp(3, Timestamp.valueOf(dayStart));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FieldMaintenanceSchedule schedule = new FieldMaintenanceSchedule();

                    schedule.setMaintenanceId(rs.getLong("maintenance_id"));
                    schedule.setFieldId(rs.getLong("field_id"));
                    schedule.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                    schedule.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                    schedule.setReason(rs.getString("reason"));
                    schedule.setStatus(rs.getString("status"));
                    schedule.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));

                    schedules.add(schedule);
                }
            }
        }

        return schedules;
    }

    /**
     * Chuẩn bị thông tin sân và Customer cho trang xác nhận trước khi booking thật được tạo.
     * Query chỉ trả về sân AVAILABLE để không preview một sân đã bị khóa.
     */
    public BookingView getBookingPreviewInfoByFieldId(Long fieldId, Long customerId) throws SQLException {
        String sql = """
                SELECT NULL AS booking_id,
                       NULL AS booking_code,
                       NULL AS recurring_group_id,
                       'NONE' AS repeat_type,
                       1 AS recurring_count,
                       u.user_id AS customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       u.email AS customer_email,
                       fa.complex_id,
                       fa.complex_name,
                       fa.address AS complex_address,
                       fa.hotline AS complex_address,
                       f.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       ft.number_of_players,
                       NULL AS start_time,
                       NULL AS end_time,
                       NULL AS voucher_id,
                       NULL AS voucher_code,
                       NULL AS original_price,
                       NULL AS discount_amount,
                       NULL AS total_amount,
                       NULL AS final_amount,
                       NULL AS deposit_amount,
                       NULL AS status,
                       NULL AS hold_expires_at,
                       NULL AS qr_code,
                       NULL AS created_at,
                       NULL AS updated_at,
                       NULL AS cancellation_reason,
                       NULL AS cancelled_at,
                       NULL AS payment_status,
                       NULL AS payment_method_name,
                       NULL AS paid_amount,
                       NULL AS paid_at,
                       NULL AS feedback_id,
                       0 AS reviewed
                FROM fields f
                INNER JOIN football_complexes fa ON f.complex_id = fa.complex_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                INNER JOIN users u ON u.user_id = ?
                WHERE f.field_id = ?
                  AND f.status = 'AVAILABLE'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);
            ps.setLong(2, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingView(rs);
                }
            }
        }

        return null;
    }

    /**
     * Kiểm tra một khung giờ có còn đặt được hay không.
     * Bản public dùng cho bước preview; bản trong transaction bên dưới mới là lớp bảo vệ cuối cùng khi ghi DB.
     */
    public boolean isFieldAvailable(Long fieldId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return isFieldAvailable(conn, fieldId, startTime, endTime);
        }
    }

    /**
     * Tính tiền thuê sân dựa trên price rule đang ACTIVE.
     * Nếu nhiều rule phủ cùng đoạn giờ, rule có priority/specific date/giá/id cao hơn được chọn cho đoạn đó.
     */
    public BigDecimal calculatePrice(Long fieldId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        FieldPricingContext pricingContext = getFieldPricingContext(fieldId);
        if (pricingContext == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new SQLException("Chưa hỗ trợ tính giá booking qua nhiều ngày.");
        }

        List<PriceRuleCandidate> rules = getPriceRuleCandidates(pricingContext, startTime, endTime);
        if (rules.isEmpty()) {
            throw new SQLException("Không tìm thấy price rule ACTIVE phù hợp cho complex_id="
                    + pricingContext.complexId()
                    + ", field_type_id=" + pricingContext.fieldTypeId()
                    + ", thời gian " + startTime + " - " + endTime + ".");
        }

        // Chia booking thành các đoạn nhỏ tại ranh giới của price rule để mỗi đoạn chỉ áp một đơn giá.
        List<LocalTime> boundaries = new ArrayList<>();
        LocalTime bookingStart = startTime.toLocalTime();
        LocalTime bookingEnd = endTime.toLocalTime();
        boundaries.add(bookingStart);
        boundaries.add(bookingEnd);

        for (PriceRuleCandidate rule : rules) {
            if (rule.startTime().isAfter(bookingStart) && rule.startTime().isBefore(bookingEnd)) {
                boundaries.add(rule.startTime());
            }
            if (rule.endTime().isAfter(bookingStart) && rule.endTime().isBefore(bookingEnd)) {
                boundaries.add(rule.endTime());
            }
        }

        boundaries = boundaries.stream()
                .distinct()
                .sorted()
                .toList();

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < boundaries.size() - 1; i++) {
            LocalTime segmentStartTime = boundaries.get(i);
            LocalTime segmentEndTime = boundaries.get(i + 1);

            PriceRuleCandidate rule = rules.stream()
                    .filter(candidate -> !candidate.startTime().isAfter(segmentStartTime)
                            && !candidate.endTime().isBefore(segmentEndTime))
                    .max(Comparator
                            .comparingInt(PriceRuleCandidate::priority)
                            .thenComparing(PriceRuleCandidate::exactSpecificDate)
                            .thenComparing(PriceRuleCandidate::price)
                            .thenComparingLong(PriceRuleCandidate::priceRuleId))
                    .orElseThrow(() -> new SQLException("Không tìm thấy price rule ACTIVE phù hợp cho complex_id="
                            + pricingContext.complexId()
                            + ", field_type_id=" + pricingContext.fieldTypeId()
                            + ", thời gian " + startTime.toLocalDate().atTime(segmentStartTime)
                            + " - " + startTime.toLocalDate().atTime(segmentEndTime) + "."));

            BigDecimal minutes = BigDecimal.valueOf(Duration.between(segmentStartTime, segmentEndTime).toMinutes());
            BigDecimal hours = minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            total = total.add(rule.price().multiply(hours));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<PriceRuleCandidate> getPriceRuleCandidates(
            FieldPricingContext pricingContext,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws SQLException {

        String sql = """
                SELECT pr.price_rule_id,
                       pr.start_time,
                       pr.end_time,
                       pr.price,
                       pr.priority,
                       CASE WHEN pr.specific_date = ? THEN 1 ELSE 0 END AS exact_specific_date
                FROM price_rules pr
                WHERE pr.status = 'ACTIVE'
                  AND pr.complex_id = ?
                  AND (pr.field_type_id = ? OR pr.field_type_id IS NULL)
                  AND (pr.field_id = ? OR pr.field_id IS NULL)
                  AND (pr.specific_date = ? OR pr.specific_date IS NULL)
                  AND (
                      pr.day_of_week IS NULL
                      OR UPPER(pr.day_of_week) = 'ALL'
                      OR UPPER(pr.day_of_week) = 'SPECIFICDATE'
                      OR UPPER(pr.day_of_week) = ?
                      OR (UPPER(pr.day_of_week) = 'WEEKDAY' AND ? = 1)
                      OR (UPPER(pr.day_of_week) = 'WEEKEND' AND ? = 1)
                  )
                  AND (pr.start_time IS NULL OR pr.start_time <= CAST(? AS time))
                  AND (pr.end_time IS NULL OR pr.end_time >= CAST(? AS time))
                ORDER BY
                  pr.priority DESC,
                  pr.price_rule_id DESC
                """;

        List<PriceRuleCandidate> rules = new ArrayList<>();
        boolean weekday = startTime.getDayOfWeek().getValue() <= 5;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(startTime.toLocalDate()));
            ps.setLong(2, pricingContext.complexId());
            ps.setInt(3, pricingContext.fieldTypeId());
            ps.setLong(4, pricingContext.fieldId());
            ps.setDate(5, java.sql.Date.valueOf(startTime.toLocalDate()));
            ps.setString(6, startTime.getDayOfWeek().name());
            ps.setInt(7, weekday ? 1 : 0);
            ps.setInt(8, weekday ? 0 : 1);
            ps.setString(9, endTime.toLocalTime().toString());
            ps.setString(10, startTime.toLocalTime().toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal price = rs.getBigDecimal("price");
                    if (price == null) {
                        continue;
                    }

                    java.sql.Time st = rs.getTime("start_time");
                    java.time.LocalTime ruleStartTime = st != null ? st.toLocalTime() : java.time.LocalTime.MIN;

                    java.sql.Time et = rs.getTime("end_time");
                    java.time.LocalTime ruleEndTime = et != null ? et.toLocalTime() : java.time.LocalTime.MAX;

                    rules.add(new PriceRuleCandidate(
                            rs.getLong("price_rule_id"),
                            ruleStartTime,
                            ruleEndTime,
                            price,
                            rs.getInt("priority"),
                            rs.getInt("exact_specific_date") == 1
                    ));
                }
            }
        }

        return rules;
    }

    /**
     * Tạo một booking HOLD và log trạng thái trong cùng transaction.
     * Việc kiểm tra khả dụng được lặp lại trong transaction để tránh hai request đặt cùng slot.
     */
    public long createBookingHold(Booking booking, Long changedBy, String note) throws SQLException {
        String insertBooking = """
                INSERT INTO bookings (
                    booking_code,
                    customer_id,
                    complex_id,
                    field_id,
                    start_time,
                    end_time,
                    voucher_id,
                    original_price,
                    discount_amount,
                    total_amount,
                    final_amount,
                    deposit_amount,
                    status,
                    hold_expires_at,
                    qr_code,
                    created_at,
                    updated_at
                )
                OUTPUT INSERTED.booking_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'HOLD', ?, ?, GETDATE(), GETDATE())
                """;

        String insertLog = """
                INSERT INTO booking_status_logs (
                    booking_id,
                    old_status,
                    new_status,
                    changed_by,
                    note,
                    created_at
                )
                VALUES (?, NULL, 'HOLD', ?, ?, GETDATE())
                """;

        Connection conn = null;
        boolean originalAutoCommit = true;

        // Bat dau transaction de tao booking va ghi log nhu mot thao tac atomic.
        try {
            conn = DBContext.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            // Kiem tra lai trang thai san trong transaction de tranh trung lich.
            if (!isFieldAvailable(conn, booking.getFieldId(), booking.getStartTime(), booking.getEndTime())) {
                throw new SQLException("Khung giờ đã được đặt hoặc sân đang bảo trì.");
            }

            long bookingId;
            // Insert booking HOLD va lay booking_id vua tao bang OUTPUT INSERTED.
            try (PreparedStatement ps = conn.prepareStatement(insertBooking)) {
                ps.setString(1, booking.getBookingCode());
                ps.setLong(2, booking.getCustomerId());
                ps.setLong(3, booking.getComplexId());
                ps.setLong(4, booking.getFieldId());
                ps.setTimestamp(5, Timestamp.valueOf(booking.getStartTime()));
                ps.setTimestamp(6, Timestamp.valueOf(booking.getEndTime()));
                setIntegerOrNull(ps, 7, booking.getVoucherId());
                ps.setBigDecimal(8, safeMoney(booking.getOriginalPrice()));
                ps.setBigDecimal(9, safeMoney(booking.getDiscountAmount()));
                ps.setBigDecimal(10, safeMoney(booking.getTotalAmount()));
                ps.setBigDecimal(11, safeMoney(firstNonNull(booking.getFinalAmount(), booking.getTotalAmount())));
                ps.setBigDecimal(12, safeMoney(booking.getDepositAmount()));
                ps.setTimestamp(13, Timestamp.valueOf(booking.getHoldExpiresAt()));
                ps.setString(14, booking.getQrCode());

                try (ResultSet rs = ps.executeQuery()) {
                    // Neu DB khong tra ve id thi booking chua duoc tao hop le.
                    if (!rs.next()) {
                        throw new SQLException("Không lấy được booking_id sau khi tạo booking.");
                    }
                    bookingId = rs.getLong(1);
                }
            }

            // Ghi lich su trang thai de audit duoc booking moi tao.
            try (PreparedStatement ps = conn.prepareStatement(insertLog)) {
                ps.setLong(1, bookingId);
                // changedBy co the null neu log duoc sinh boi he thong.
                if (changedBy == null) {
                    ps.setNull(2, Types.BIGINT);
                } else {
                    ps.setLong(2, changedBy);
                }
                ps.setString(3, note);
                ps.executeUpdate();
            }

            conn.commit();
            return bookingId;
        } catch (SQLException e) {
            // Co loi thi rollback de khong de lai booking/log lech nhau.
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            // Khoi phuc auto-commit va dong connection du co loi hay khong.
            if (conn != null) {
                conn.setAutoCommit(originalAutoCommit);
                conn.close();
            }
        }
    }

    /**
     * Lấy chi tiết booking theo cả booking_id và customer_id để bảo đảm Customer chỉ xem dữ liệu của mình.
     * HOLD hết hạn của Customer đó được xử lý trước để trạng thái trả về không bị cũ.
     */
    public BookingView getBookingDetailByIdAndCustomerId(Long bookingId, Long customerId) throws SQLException {
        cancelExpiredHolds(customerId);

        String sql = baseBookingViewSql() + """
                WHERE b.booking_id = ?
                  AND b.customer_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, bookingId);
            ps.setLong(2, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingView(rs);
                }
            }
        }

        return null;
    }

    /**
     * Tạo một nhóm booking định kỳ.
     * Tất cả slot phải còn trống trước khi insert; nếu một slot lỗi thì rollback toàn bộ nhóm để không tạo lịch dở dang.
     */
    public List<Long> createRecurringBookingHolds(
            List<Booking> bookings,
            String repeatType,
            LocalDate repeatUntil,
            Long changedBy,
            String note
    ) throws SQLException {
        String insertRecurringGroup = """
                INSERT INTO booking_recurring_groups (
                    customer_id,
                    repeat_type,
                    repeat_until,
                    created_at
                )
                OUTPUT INSERTED.recurring_group_id
                VALUES (?, ?, ?, GETDATE())
                """;

        String insertBooking = """
                INSERT INTO bookings (
                    booking_code,
                    customer_id,
                    complex_id,
                    field_id,
                    recurring_group_id,
                    start_time,
                    end_time,
                    voucher_id,
                    original_price,
                    discount_amount,
                    total_amount,
                    final_amount,
                    deposit_amount,
                    status,
                    hold_expires_at,
                    qr_code,
                    created_at,
                    updated_at
                )
                OUTPUT INSERTED.booking_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'HOLD', ?, ?, GETDATE(), GETDATE())
                """;

        String insertLog = """
                INSERT INTO booking_status_logs (
                    booking_id,
                    old_status,
                    new_status,
                    changed_by,
                    note,
                    created_at
                )
                VALUES (?, NULL, 'HOLD', ?, ?, GETDATE())
                """;

        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = DBContext.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            /*
             * Kiem tra thoi gian cho booking lap.
             * Tat ca slot phai con trong; chi can mot slot bi trung lich/bao tri
             * thi rollback ca nhom booking lap.
             */
            for (Booking booking : bookings) {
                // Kiem tra tung lan dat truoc khi insert bat ky booking nao.
                if (!isFieldAvailable(conn, booking.getFieldId(), booking.getStartTime(), booking.getEndTime())) {
                    throw new SQLException("Khung gio " + booking.getStartTime()
                            + " - " + booking.getEndTime()
                            + " da duoc dat hoac san dang bao tri.");
                }
            }
            Long recurringGroupId;
            Booking firstBooking = bookings.get(0);
            // Tao nhom recurring truoc de cac booking con tro ve cung mot group.
            try (PreparedStatement ps = conn.prepareStatement(insertRecurringGroup)) {
                ps.setLong(1, firstBooking.getCustomerId());
                ps.setString(2, repeatType);
                ps.setDate(3, java.sql.Date.valueOf(repeatUntil));

                try (ResultSet rs = ps.executeQuery()) {
                    // Neu khong lay duoc id group thi khong the gan booking con.
                    if (!rs.next()) {
                        throw new SQLException("Khong lay duoc recurring_group_id sau khi tao nhom lap.");
                    }
                    recurringGroupId = rs.getLong(1);
                }
            }

            List<Long> bookingIds = new ArrayList<>();
            // Insert tung booking HOLD thuoc nhom recurring.
            for (Booking booking : bookings) {
                // Luu booking con va lay booking_id vua tao.
                try (PreparedStatement ps = conn.prepareStatement(insertBooking)) {
                    ps.setString(1, booking.getBookingCode());
                    ps.setLong(2, booking.getCustomerId());
                    ps.setLong(3, booking.getComplexId());
                    ps.setLong(4, booking.getFieldId());
                    ps.setLong(5, recurringGroupId);
                    ps.setTimestamp(6, Timestamp.valueOf(booking.getStartTime()));
                    ps.setTimestamp(7, Timestamp.valueOf(booking.getEndTime()));
                    setIntegerOrNull(ps, 8, booking.getVoucherId());
                    ps.setBigDecimal(9, safeMoney(booking.getOriginalPrice()));
                    ps.setBigDecimal(10, safeMoney(booking.getDiscountAmount()));
                    ps.setBigDecimal(11, safeMoney(booking.getTotalAmount()));
                    ps.setBigDecimal(12, safeMoney(firstNonNull(booking.getFinalAmount(), booking.getTotalAmount())));
                    ps.setBigDecimal(13, safeMoney(booking.getDepositAmount()));
                    ps.setTimestamp(14, Timestamp.valueOf(booking.getHoldExpiresAt()));
                    ps.setString(15, booking.getQrCode());

                    try (ResultSet rs = ps.executeQuery()) {
                        // Moi booking con bat buoc phai tra ve id de ghi log dung.
                        if (!rs.next()) {
                            throw new SQLException("Khong lay duoc booking_id sau khi tao booking.");
                        }
                        bookingIds.add(rs.getLong(1));
                    }
                }

                // Ghi log HOLD cho booking con vua insert.
                try (PreparedStatement ps = conn.prepareStatement(insertLog)) {
                    ps.setLong(1, bookingIds.get(bookingIds.size() - 1));
                    // changedBy co the null neu log duoc sinh boi he thong.
                    if (changedBy == null) {
                        ps.setNull(2, Types.BIGINT);
                    } else {
                        ps.setLong(2, changedBy);
                    }
                    ps.setString(3, note);
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return bookingIds;
        } catch (SQLException e) {
            // Co loi o bat ky slot nao thi rollback ca nhom recurring.
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            // Luon tra connection ve trang thai ban dau truoc khi dong.
            if (conn != null) {
                conn.setAutoCommit(originalAutoCommit);
                conn.close();
            }
        }
    }

    public int getCancelBeforeHours() {
        String sql = """
                SELECT setting_value
                FROM system_settings
                WHERE setting_key = 'booking.cancel_before_hours'
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Neu co cau hinh trong DB thi dung gia tri do.
            if (rs.next()) {
                return Integer.parseInt(rs.getString("setting_value"));
            }
        } catch (Exception ignored) {
            // Loi doc cau hinh thi quay ve mac dinh de flow huy van chay duoc.
            return DEFAULT_CANCEL_BEFORE_HOURS;
        }

        return DEFAULT_CANCEL_BEFORE_HOURS;
    }

    /**
     * Hủy booking của Customer bằng transaction có khóa bản ghi.
     * Method kiểm tra ownership, trạng thái hiện tại và mốc giờ hủy trước khi đổi sang CANCELLED và ghi log.
     */
    public void cancelBooking(Long bookingId, Long customerId, String reason, Long changedBy) throws SQLException {
        String selectBooking = """
                SELECT status,
                       start_time
                FROM bookings WITH (UPDLOCK, HOLDLOCK)
                WHERE booking_id = ?
                  AND customer_id = ?
                """;

        String updateBooking = """
                UPDATE bookings
                SET status = ?,
                    cancellation_reason = ?,
                    cancelled_at = GETDATE(),
                    updated_at = GETDATE()
                WHERE booking_id = ?
                  AND customer_id = ?
                """;

        String insertLog = """
                INSERT INTO booking_status_logs (
                    booking_id,
                    old_status,
                    new_status,
                    changed_by,
                    note,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, GETDATE())
                """;

        Connection conn = null;
        boolean originalAutoCommit = true;

        // Bat dau transaction de khoa booking, cap nhat trang thai va ghi log cung luc.
        try {
            conn = DBContext.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            String oldStatus;
            LocalDateTime startTime;
            // Doc booking bang UPDLOCK/HOLDLOCK de tranh hai request huy cung luc.
            try (PreparedStatement ps = conn.prepareStatement(selectBooking)) {
                ps.setLong(1, bookingId);
                ps.setLong(2, customerId);

                try (ResultSet rs = ps.executeQuery()) {
                    // Khong thay booking nghia la booking khong ton tai hoac khong thuoc customer nay.
                    if (!rs.next()) {
                        throw new SQLException("Khong tim thay booking cua khach hang.");
                    }
                    oldStatus = rs.getString("status");
                    startTime = toLocalDateTime(rs.getTimestamp("start_time"));
                }
            }

            /*
             * Kiem tra quy tac huy booking.
             * Chi cho huy khi booking chua bat dau, chua o trang thai ket thuc,
             * va con truoc moc chan huy theo cau hinh.
             */
            int cancelBeforeHours = getCancelBeforeHours();
            LocalDateTime latestCancelTime = startTime.minusHours(cancelBeforeHours);
            LocalDateTime now = LocalDateTime.now();
            // Trang thai da ket thuc/dang check-in thi khong duoc huy.
            if (STATUS_CANCELLED.equals(oldStatus) || "COMPLETED".equals(oldStatus) || "CHECKED_IN".equals(oldStatus)) {
                throw new SQLException("Booking khong the huy voi trang thai hien tai.");
            }
            // Qua gio bat dau hoac qua moc cho phep huy thi chan request.
            if (!now.isBefore(startTime) || now.isAfter(latestCancelTime)) {
                throw new SQLException("Booking chi duoc huy truoc gio bat dau toi thieu "
                        + cancelBeforeHours + " gio.");
            }
            // Update booking sang CANCELLED va luu ly do huy.
            try (PreparedStatement ps = conn.prepareStatement(updateBooking)) {
                ps.setString(1, STATUS_CANCELLED);
                ps.setString(2, reason);
                ps.setLong(3, bookingId);
                ps.setLong(4, customerId);
                ps.executeUpdate();
            }

            // Ghi log chuyen trang thai de audit duoc ai da huy.
            try (PreparedStatement ps = conn.prepareStatement(insertLog)) {
                ps.setLong(1, bookingId);
                ps.setString(2, oldStatus);
                ps.setString(3, STATUS_CANCELLED);
                // changedBy co the null neu he thong thuc hien huy tu dong.
                if (changedBy == null) {
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(4, changedBy);
                }
                ps.setString(5, reason);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            // Co loi khi huy thi rollback de booking va log khong bi lech.
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            // Luon khoi phuc auto-commit va dong connection sau transaction.
            if (conn != null) {
                conn.setAutoCommit(originalAutoCommit);
                conn.close();
            }
        }
    }

    /**
     * Lấy lịch sử booking của Customer.
     * Với booking định kỳ, chỉ chọn booking đại diện để lịch sử không bị lặp nhiều dòng cho cùng một nhóm.
     */
    public List<BookingView> getBookingHistoryByCustomerId(Long customerId) throws SQLException {
        cancelExpiredHolds(customerId);

        String sql = baseBookingViewSql() + """
                WHERE b.customer_id = ?
                  AND (
                      b.recurring_group_id IS NULL
                      OR b.booking_id = (
                          SELECT MIN(representative.booking_id)
                          FROM bookings representative
                          WHERE representative.recurring_group_id = b.recurring_group_id
                      )
                  )
                ORDER BY b.created_at DESC,
                         b.booking_id DESC
                """;

        List<BookingView> bookings = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookings.add(mapBookingView(rs));
                }
            }
        }

        return bookings;
    }

    public int getBookingCountByCustomerId(Long customerId) throws SQLException {
        cancelExpiredHolds(customerId);

        String sql = """
                SELECT COUNT(*)
                FROM bookings
                WHERE customer_id = ?
                  AND status NOT IN ('CANCELLED', 'REJECTED')
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, customerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public int getBookingCountWithComplexId(long id) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM bookings
                WHERE complex_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e
            );
        }
    }

    public int getBookingCountWithFieldId(long id) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM bookings
                WHERE field_id = ?
                """;
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Lỗi khi cố gắng truy cập dữ liệu: " + e.getMessage(), e
            );
        }
    }

    public Booking getBookingById(Long bookingId) {
        String sql = """
                SELECT booking_id,
                       booking_code,
                       customer_id,
                       complex_id,
                       field_id,
                       recurring_group_id,
                       start_time,
                       end_time,
                       voucher_id,
                       original_price,
                       discount_amount,
                       total_amount,
                       final_amount,
                       deposit_amount,
                       status,
                       hold_expires_at,
                       cancellation_reason,
                       cancelled_at,
                       qr_code,
                       created_at,
                       updated_at
                FROM bookings
                WHERE booking_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Booking booking = new Booking();

                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setBookingCode(rs.getString("booking_code"));
                    booking.setCustomerId(rs.getLong("customer_id"));
                    booking.setComplexId(rs.getLong("complex_id"));
                    booking.setFieldId(rs.getLong("field_id"));

                    long recurringGroupId = rs.getLong("recurring_group_id");
                    booking.setRecurringGroupId(rs.wasNull() ? null : recurringGroupId);

                    Timestamp startTime = rs.getTimestamp("start_time");
                    if (startTime != null) {
                        booking.setStartTime(startTime.toLocalDateTime());
                    }

                    Timestamp endTime = rs.getTimestamp("end_time");
                    if (endTime != null) {
                        booking.setEndTime(endTime.toLocalDateTime());
                    }

                    int voucherId = rs.getInt("voucher_id");
                    booking.setVoucherId(rs.wasNull() ? null : voucherId);
                    booking.setOriginalPrice(rs.getBigDecimal("original_price"));
                    booking.setDiscountAmount(rs.getBigDecimal("discount_amount"));
                    booking.setTotalAmount(rs.getBigDecimal("total_amount"));
                    booking.setFinalAmount(rs.getBigDecimal("final_amount"));
                    booking.setDepositAmount(rs.getBigDecimal("deposit_amount"));
                    booking.setStatus(rs.getString("status"));
                    booking.setCancellationReason(rs.getString("cancellation_reason"));
                    booking.setQrCode(rs.getString("qr_code"));

                    Timestamp holdExpiresAt = rs.getTimestamp("hold_expires_at");
                    if (holdExpiresAt != null) {
                        booking.setHoldExpiresAt(holdExpiresAt.toLocalDateTime());
                    }

                    Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
                    if (cancelledAt != null) {
                        booking.setCancelledAt(cancelledAt.toLocalDateTime());
                    }

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        booking.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        booking.setUpdatedAt(updatedAt.toLocalDateTime());
                    }

                    return booking;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Kiểm tra khả dụng trong cùng connection transaction.
     * UPDLOCK/HOLDLOCK trên sân giúp serialize các request cùng tranh một sân, còn hai NOT EXISTS chặn booking/bảo trì overlap.
     */
    private boolean isFieldAvailable(Connection conn, Long fieldId, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {
        cancelExpiredHolds(conn, null);

        String sql = """
                SELECT
                    CASE
                        WHEN EXISTS (
                            SELECT 1
                            FROM fields f WITH (UPDLOCK, HOLDLOCK)
                            WHERE f.field_id = ?
                              AND f.status = 'AVAILABLE'
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM bookings b
                            WHERE b.field_id = ?
                              AND b.status NOT IN ('CANCELLED', 'EXPIRED', 'REJECTED')
                              AND b.start_time < ?
                              AND b.end_time > ?
                        )
                        AND NOT EXISTS (
                            SELECT 1
                            FROM field_maintenance_schedules m
                            WHERE m.field_id = ?
                              AND m.status <> 'CANCELLED'
                              AND m.start_time < ?
                              AND m.end_time > ?
                        )
                        THEN 1
                        ELSE 0
                    END AS available
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fieldId);
            ps.setLong(2, fieldId);
            ps.setTimestamp(3, Timestamp.valueOf(endTime));
            ps.setTimestamp(4, Timestamp.valueOf(startTime));
            ps.setLong(5, fieldId);
            ps.setTimestamp(6, Timestamp.valueOf(endTime));
            ps.setTimestamp(7, Timestamp.valueOf(startTime));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("available") == 1;
            }
        }
    }

    /**
     * SQL chung cho các màn hình booking.
     * OUTER APPLY gom tiền nhóm recurring và lấy payment mới nhất để view có thể hiển thị đúng trạng thái thanh toán.
     */
    private String baseBookingViewSql() {
        return """
                SELECT b.booking_id,
                       b.booking_code,
                       b.recurring_group_id,
                       COALESCE(rg.repeat_type, 'NONE') AS repeat_type,
                       grp.recurring_count,
                       b.customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       u.email AS customer_email,
                       b.complex_id,
                       fa.complex_name,
                       fa.address AS complex_address,
                       fa.hotline AS complex_address,
                       b.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       ft.number_of_players,
                       b.start_time,
                       b.end_time,
                       b.voucher_id,
                       v.code AS voucher_code,
                       grp.original_price,
                       grp.discount_amount,
                       grp.total_amount,
                       grp.final_amount,
                       grp.deposit_amount,
                       b.status,
                       b.hold_expires_at,
                       b.qr_code,
                       b.created_at,
                       b.updated_at,
                       b.cancellation_reason,
                       b.cancelled_at,
                       CASE
                           WHEN b.status = 'HOLD'
                                AND b.hold_expires_at > GETDATE()
                                AND (lp.payment_status IS NULL OR lp.payment_status = 'FAILED')
                           THEN 'PENDING'
                           ELSE lp.payment_status
                       END AS payment_status,
                       lp.payment_method_name,
                       CASE WHEN lp.payment_status = 'SUCCESS' THEN lp.paid_amount ELSE NULL END AS paid_amount,
                       lp.paid_at,
                       fb.feedback_id,
                       IIF(fb.feedback_id IS NULL, 0, 1) AS reviewed
                FROM bookings b
                LEFT JOIN booking_recurring_groups rg ON b.recurring_group_id = rg.recurring_group_id
                INNER JOIN users u ON b.customer_id = u.user_id
                INNER JOIN football_complexes fa ON b.complex_id = fa.complex_id
                INNER JOIN fields f ON b.field_id = f.field_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                LEFT JOIN vouchers v ON b.voucher_id = v.id
                LEFT JOIN feedbacks fb
                    ON fb.booking_id = b.booking_id
                   AND fb.status = 'ACTIVE'
                OUTER APPLY (
                    SELECT COUNT(*) AS recurring_count,
                           SUM(sb.original_price) AS original_price,
                           SUM(sb.discount_amount) AS discount_amount,
                           SUM(sb.total_amount) AS total_amount,
                           SUM(COALESCE(sb.final_amount, sb.total_amount)) AS final_amount,
                           SUM(sb.deposit_amount) AS deposit_amount
                    FROM bookings sb
                    WHERE sb.customer_id = b.customer_id
                      AND (
                          (b.recurring_group_id IS NOT NULL AND sb.recurring_group_id = b.recurring_group_id)
                          OR (b.recurring_group_id IS NULL AND sb.booking_id = b.booking_id)
                      )
                ) grp
                OUTER APPLY (
                    SELECT TOP 1
                           p.status AS payment_status,
                           pm.method_name AS payment_method_name,
                           p.amount AS paid_amount,
                           p.paid_at
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE EXISTS (
                        SELECT 1
                        FROM bookings paid_booking
                        WHERE paid_booking.booking_id = p.booking_id
                          AND paid_booking.customer_id = b.customer_id
                          AND (
                              (b.recurring_group_id IS NOT NULL AND paid_booking.recurring_group_id = b.recurring_group_id)
                              OR (b.recurring_group_id IS NULL AND paid_booking.booking_id = b.booking_id)
                          )
                    )
                    ORDER BY
                        CASE WHEN p.status = 'SUCCESS' THEN 0 ELSE 1 END,
                        p.paid_at DESC,
                        p.created_at DESC,
                        p.payment_id DESC
                ) lp
                """;
    }

    private int cancelExpiredHolds() throws SQLException {
        return cancelExpiredHolds(null);
    }

    /**
     * Hủy các booking HOLD đã quá hạn mà chưa có payment SUCCESS.
     * Có thể giới hạn theo Customer để các màn hình cá nhân chỉ dọn dữ liệu liên quan tới người đang xem.
     */
    private int cancelExpiredHolds(Long customerId) throws SQLException {
        Connection conn = null;
        boolean originalAutoCommit = true;

        try {
            conn = DBContext.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            int updatedRows = cancelExpiredHolds(conn, customerId);
            conn.commit();
            return updatedRows;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(originalAutoCommit);
                conn.close();
            }
        }
    }

    /**
     * Cập nhật trạng thái HOLD quá hạn và ghi log bằng một câu SQL dùng bảng tạm.
     * Cách này giữ danh sách booking vừa bị hủy để log chính xác trong cùng transaction.
     */
    private int cancelExpiredHolds(Connection conn, Long customerId) throws SQLException {
        String customerFilter = customerId == null ? "" : "                  AND b.customer_id = ?\n";
        String sql = """
                DECLARE @ExpiredBookings TABLE (
                    booking_id BIGINT NOT NULL
                );
                
                UPDATE b
                SET b.status = 'CANCELLED',
                    b.cancellation_reason = ?,
                    b.cancelled_at = GETDATE(),
                    b.hold_expires_at = NULL,
                    b.updated_at = GETDATE()
                OUTPUT INSERTED.booking_id INTO @ExpiredBookings (booking_id)
                FROM bookings b
                WHERE b.status = 'HOLD'
                  AND b.hold_expires_at IS NOT NULL
                  AND b.hold_expires_at <= GETDATE()
                """ + customerFilter + """
                  AND NOT EXISTS (
                      SELECT 1
                      FROM payments p
                      WHERE p.booking_id = b.booking_id
                        AND p.status = 'SUCCESS'
                  )
                
                INSERT INTO booking_status_logs (
                    booking_id,
                    old_status,
                    new_status,
                    changed_by,
                    note,
                    created_at
                )
                SELECT booking_id,
                       'HOLD',
                       'CANCELLED',
                       CAST(NULL AS BIGINT),
                       ?,
                       GETDATE()
                FROM @ExpiredBookings
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, HOLD_EXPIRED_CANCEL_REASON);
            if (customerId != null) {
                ps.setLong(2, customerId);
            }
            ps.setString(customerId == null ? 2 : 3, HOLD_EXPIRED_CANCEL_REASON);
            return ps.executeUpdate();
        }
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();

        booking.setBookingId(rs.getLong("booking_id"));
        booking.setBookingCode(rs.getString("booking_code"));
        booking.setCustomerId(rs.getLong("customer_id"));
        booking.setComplexId(rs.getLong("complex_id"));
        booking.setFieldId(rs.getLong("field_id"));

        Object recurringGroupId = rs.getObject("recurring_group_id");
        if (recurringGroupId != null) {
            booking.setRecurringGroupId(rs.getLong("recurring_group_id"));
        }

        booking.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        booking.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        booking.setVoucherId(getIntegerOrNull(rs, "voucher_id"));
        booking.setOriginalPrice(rs.getBigDecimal("original_price"));
        booking.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        booking.setTotalAmount(rs.getBigDecimal("total_amount"));
        booking.setFinalAmount(rs.getBigDecimal("final_amount"));
        booking.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        booking.setStatus(rs.getString("status"));
        booking.setHoldExpiresAt(toLocalDateTime(rs.getTimestamp("hold_expires_at")));
        booking.setCancellationReason(rs.getString("cancellation_reason"));
        booking.setCancelledAt(toLocalDateTime(rs.getTimestamp("cancelled_at")));
        booking.setQrCode(rs.getString("qr_code"));
        booking.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        booking.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));

        return booking;
    }

    private BookingView mapBookingView(ResultSet rs) throws SQLException {
        BookingView view = new BookingView();

        view.setBookingId(getLongOrNull(rs, "booking_id"));
        view.setBookingCode(rs.getString("booking_code"));
        view.setRecurringGroupId(getLongOrNull(rs, "recurring_group_id"));
        view.setRepeatType(rs.getString("repeat_type"));
        view.setRecurringCount(getIntegerOrNull(rs, "recurring_count"));
        view.setCustomerId(getLongOrNull(rs, "customer_id"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setCustomerEmail(rs.getString("customer_email"));
        view.setComplexId(getLongOrNull(rs, "complex_id"));
        view.setComplexName(rs.getString("complex_name"));
        view.setComplexAddress(rs.getString("complex_address"));
        view.setComplexHotline(rs.getString("complex_address"));
        view.setFieldId(getLongOrNull(rs, "field_id"));
        view.setFieldName(rs.getString("field_name"));
        view.setFieldTypeName(rs.getString("field_type_name"));
        view.setNumberOfPlayers(getIntegerOrNull(rs, "number_of_players"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setVoucherId(getIntegerOrNull(rs, "voucher_id"));
        view.setVoucherCode(rs.getString("voucher_code"));
        view.setOriginalPrice(rs.getBigDecimal("original_price"));
        view.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
        view.setFinalAmount(rs.getBigDecimal("final_amount"));
        view.setDepositAmount(rs.getBigDecimal("deposit_amount"));
        view.setStatus(rs.getString("status"));
        view.setHoldExpiresAt(toLocalDateTime(rs.getTimestamp("hold_expires_at")));
        view.setQrCode(rs.getString("qr_code"));
        view.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        view.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        view.setCancellationReason(rs.getString("cancellation_reason"));
        view.setCancelledAt(toLocalDateTime(rs.getTimestamp("cancelled_at")));
        view.setPaymentStatus(rs.getString("payment_status"));
        view.setPaymentMethodName(rs.getString("payment_method_name"));
        view.setPaidAmount(rs.getBigDecimal("paid_amount"));
        view.setPaidAt(toLocalDateTime(rs.getTimestamp("paid_at")));
        view.setFeedbackId(getLongOrNull(rs, "feedback_id"));
        view.setReviewed(rs.getBoolean("reviewed"));

        return view;
    }

    private Long getLongOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntegerOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private void setIntegerOrNull(PreparedStatement ps, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, Types.INTEGER);
        } else {
            ps.setInt(parameterIndex, value);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private record FieldPricingContext(Long complexId, Integer fieldTypeId, Long fieldId) {
    }

    private record PriceRuleCandidate(
            Long priceRuleId,
            LocalTime startTime,
            LocalTime endTime,
            BigDecimal price,
            Integer priority,
            Boolean exactSpecificDate
    ) {
    }
}
