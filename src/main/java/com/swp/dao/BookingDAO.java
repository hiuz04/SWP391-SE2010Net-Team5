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

/*
 * BookingDAO owns all SQL for booking screens, availability checks,
 * transactional HOLD creation, recurring groups, and cancellation updates.
 */
public class BookingDAO {

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final int DEFAULT_CANCEL_BEFORE_HOURS = 24;

    public Long getFacilityIdByFieldId(Long fieldId) throws SQLException {
        String sql = """
                SELECT facility_id
                FROM fields
                WHERE field_id = ?
                """;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, fieldId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("facility_id");
                }
            }
        }

        return null;
    }

    private FieldPricingContext getFieldPricingContext(Long fieldId) throws SQLException {
        String sql = """
                SELECT facility_id,
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
                            rs.getLong("facility_id"),
                            rs.getInt("field_type_id")
                    );
                }
            }
        }

        return null;
    }

    public List<Field> getFieldsByFacility(Long facilityId) throws SQLException {
        String sql = """
                SELECT field_id,
                       facility_id,
                       field_type_id,
                       field_name,
                       description,
                       status,
                       created_at,
                       updated_at
                FROM fields
                WHERE facility_id = ?
                ORDER BY field_name
                """;

        List<Field> fields = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Field field = new Field();

                    field.setFieldId(rs.getLong("field_id"));
                    field.setFacilityId(rs.getLong("facility_id"));
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

    public List<Booking> getBookingsByFacilityAndDate(Long facilityId, LocalDate date) throws SQLException {
        String sql = """
                SELECT booking_id,
                       booking_code,
                       customer_id,
                       facility_id,
                       field_id,
                       recurring_group_id,
                       start_time,
                       end_time,
                       original_price,
                       discount_amount,
                       total_amount,
                       deposit_amount,
                       status,
                       hold_expires_at,
                       cancellation_reason,
                       cancelled_at,
                       qr_code,
                       created_at,
                       updated_at
                FROM bookings
                WHERE facility_id = ?
                  AND status NOT IN ('CANCELLED', 'EXPIRED', 'REJECTED')
                  AND start_time < ?
                  AND end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Booking> bookings = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);
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

    public List<FieldMaintenanceSchedule> getMaintenanceByFacilityAndDate(Long facilityId, LocalDate date)
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
                WHERE f.facility_id = ?
                  AND m.status <> 'CANCELLED'
                  AND m.start_time < ?
                  AND m.end_time > ?
                """;

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<FieldMaintenanceSchedule> schedules = new ArrayList<>();

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, facilityId);
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

    public BookingView getBookingPreviewInfoByFieldId(Long fieldId, Long customerId) throws SQLException {
        String sql = """
                SELECT NULL AS booking_id,
                       NULL AS booking_code,
                       u.user_id AS customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       u.email AS customer_email,
                       fa.facility_id,
                       fa.facility_name,
                       fa.address AS facility_address,
                       fa.hotline AS facility_hotline,
                       f.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       ft.number_of_players,
                       NULL AS start_time,
                       NULL AS end_time,
                       NULL AS original_price,
                       NULL AS discount_amount,
                       NULL AS total_amount,
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
                       NULL AS paid_at
                FROM fields f
                INNER JOIN facilities fa ON f.facility_id = fa.facility_id
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

    public boolean isFieldAvailable(Long fieldId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        try (Connection conn = DBContext.getConnection()) {
            return isFieldAvailable(conn, fieldId, startTime, endTime);
        }
    }

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
            throw new SQLException("Không tìm thấy price rule ACTIVE phù hợp cho facility_id="
                    + pricingContext.facilityId()
                    + ", field_type_id=" + pricingContext.fieldTypeId()
                    + ", thời gian " + startTime + " - " + endTime + ".");
        }

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
                            .thenComparingLong(PriceRuleCandidate::priceRuleId))
                    .orElseThrow(() -> new SQLException("Không tìm thấy price rule ACTIVE phù hợp cho facility_id="
                            + pricingContext.facilityId()
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
                  AND pr.facility_id = ?
                  AND pr.field_type_id = ?
                  AND (pr.specific_date = ? OR pr.specific_date IS NULL)
                  AND (
                      pr.day_of_week IS NULL
                      OR UPPER(pr.day_of_week) = 'ALL'
                      OR UPPER(pr.day_of_week) = ?
                      OR (UPPER(pr.day_of_week) = 'WEEKDAY' AND ? = 1)
                      OR (UPPER(pr.day_of_week) = 'WEEKEND' AND ? = 1)
                  )
                  AND pr.start_time < CAST(? AS time)
                  AND pr.end_time > CAST(? AS time)
                ORDER BY
                  pr.priority DESC,
                  pr.price_rule_id DESC
                """;

        List<PriceRuleCandidate> rules = new ArrayList<>();
        boolean weekday = startTime.getDayOfWeek().getValue() <= 5;

        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(startTime.toLocalDate()));
            ps.setLong(2, pricingContext.facilityId());
            ps.setInt(3, pricingContext.fieldTypeId());
            ps.setDate(4, java.sql.Date.valueOf(startTime.toLocalDate()));
            ps.setString(5, startTime.getDayOfWeek().name());
            ps.setInt(6, weekday ? 1 : 0);
            ps.setInt(7, weekday ? 0 : 1);
            ps.setString(8, endTime.toLocalTime().toString());
            ps.setString(9, startTime.toLocalTime().toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal price = rs.getBigDecimal("price");
                    if (price == null) {
                        continue;
                    }

                    rules.add(new PriceRuleCandidate(
                            rs.getLong("price_rule_id"),
                            rs.getTime("start_time").toLocalTime(),
                            rs.getTime("end_time").toLocalTime(),
                            price,
                            rs.getInt("priority"),
                            rs.getInt("exact_specific_date") == 1
                    ));
                }
            }
        }

        return rules;
    }

    public long createBookingHold(Booking booking, Long changedBy, String note) throws SQLException {
        String insertBooking = """
                INSERT INTO bookings (
                    booking_code,
                    customer_id,
                    facility_id,
                    field_id,
                    start_time,
                    end_time,
                    original_price,
                    discount_amount,
                    total_amount,
                    deposit_amount,
                    status,
                    hold_expires_at,
                    qr_code,
                    created_at,
                    updated_at
                )
                OUTPUT INSERTED.booking_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'HOLD', ?, ?, GETDATE(), GETDATE())
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
                ps.setLong(3, booking.getFacilityId());
                ps.setLong(4, booking.getFieldId());
                ps.setTimestamp(5, Timestamp.valueOf(booking.getStartTime()));
                ps.setTimestamp(6, Timestamp.valueOf(booking.getEndTime()));
                ps.setBigDecimal(7, safeMoney(booking.getOriginalPrice()));
                ps.setBigDecimal(8, safeMoney(booking.getDiscountAmount()));
                ps.setBigDecimal(9, safeMoney(booking.getTotalAmount()));
                ps.setBigDecimal(10, safeMoney(booking.getDepositAmount()));
                ps.setTimestamp(11, Timestamp.valueOf(booking.getHoldExpiresAt()));
                ps.setString(12, booking.getQrCode());

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

    public BookingView getBookingDetailByIdAndCustomerId(Long bookingId, Long customerId) throws SQLException {
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
                    facility_id,
                    field_id,
                    recurring_group_id,
                    start_time,
                    end_time,
                    original_price,
                    discount_amount,
                    total_amount,
                    deposit_amount,
                    status,
                    hold_expires_at,
                    qr_code,
                    created_at,
                    updated_at
                )
                OUTPUT INSERTED.booking_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'HOLD', ?, ?, GETDATE(), GETDATE())
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
                    ps.setLong(3, booking.getFacilityId());
                    ps.setLong(4, booking.getFieldId());
                    ps.setLong(5, recurringGroupId);
                    ps.setTimestamp(6, Timestamp.valueOf(booking.getStartTime()));
                    ps.setTimestamp(7, Timestamp.valueOf(booking.getEndTime()));
                    ps.setBigDecimal(8, safeMoney(booking.getOriginalPrice()));
                    ps.setBigDecimal(9, safeMoney(booking.getDiscountAmount()));
                    ps.setBigDecimal(10, safeMoney(booking.getTotalAmount()));
                    ps.setBigDecimal(11, safeMoney(booking.getDepositAmount()));
                    ps.setTimestamp(12, Timestamp.valueOf(booking.getHoldExpiresAt()));
                    ps.setString(13, booking.getQrCode());

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

    public List<BookingView> getBookingHistoryByCustomerId(Long customerId) throws SQLException {
        String sql = baseBookingViewSql() + """
                WHERE b.customer_id = ?
                ORDER BY b.start_time DESC
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

    public int getBookingCountWithFacilityId(long id) {
        String sql = """
                    SELECT COUNT(*) AS total
                    FROM bookings
                    WHERE facility_id = ?
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

    private boolean isFieldAvailable(Connection conn, Long fieldId, LocalDateTime startTime, LocalDateTime endTime)
            throws SQLException {

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

    private String baseBookingViewSql() {
        return """
                SELECT b.booking_id,
                       b.booking_code,
                       b.customer_id,
                       u.full_name AS customer_name,
                       u.phone AS customer_phone,
                       u.email AS customer_email,
                       b.facility_id,
                       fa.facility_name,
                       fa.address AS facility_address,
                       fa.hotline AS facility_hotline,
                       b.field_id,
                       f.field_name,
                       ft.type_name AS field_type_name,
                       ft.number_of_players,
                       b.start_time,
                       b.end_time,
                       b.original_price,
                       b.discount_amount,
                       b.total_amount,
                       b.deposit_amount,
                       b.status,
                       b.hold_expires_at,
                       b.qr_code,
                       b.created_at,
                       b.updated_at,
                       b.cancellation_reason,
                       b.cancelled_at,
                       lp.payment_status,
                       lp.payment_method_name,
                       lp.paid_amount,
                       lp.paid_at
                FROM bookings b
                INNER JOIN users u ON b.customer_id = u.user_id
                INNER JOIN facilities fa ON b.facility_id = fa.facility_id
                INNER JOIN fields f ON b.field_id = f.field_id
                INNER JOIN field_types ft ON f.field_type_id = ft.field_type_id
                OUTER APPLY (
                    SELECT TOP 1
                           p.status AS payment_status,
                           pm.method_name AS payment_method_name,
                           p.amount AS paid_amount,
                           p.paid_at
                    FROM payments p
                    LEFT JOIN payment_methods pm ON p.payment_method_id = pm.payment_method_id
                    WHERE p.booking_id = b.booking_id
                    ORDER BY
                        CASE WHEN p.status = 'SUCCESS' THEN 0 ELSE 1 END,
                        p.paid_at DESC,
                        p.created_at DESC,
                        p.payment_id DESC
                ) lp
                """;
    }

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking booking = new Booking();

        booking.setBookingId(rs.getLong("booking_id"));
        booking.setBookingCode(rs.getString("booking_code"));
        booking.setCustomerId(rs.getLong("customer_id"));
        booking.setFacilityId(rs.getLong("facility_id"));
        booking.setFieldId(rs.getLong("field_id"));

        Object recurringGroupId = rs.getObject("recurring_group_id");
        if (recurringGroupId != null) {
            booking.setRecurringGroupId(rs.getLong("recurring_group_id"));
        }

        booking.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        booking.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        booking.setOriginalPrice(rs.getBigDecimal("original_price"));
        booking.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        booking.setTotalAmount(rs.getBigDecimal("total_amount"));
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
        view.setCustomerId(getLongOrNull(rs, "customer_id"));
        view.setCustomerName(rs.getString("customer_name"));
        view.setCustomerPhone(rs.getString("customer_phone"));
        view.setCustomerEmail(rs.getString("customer_email"));
        view.setFacilityId(getLongOrNull(rs, "facility_id"));
        view.setFacilityName(rs.getString("facility_name"));
        view.setFacilityAddress(rs.getString("facility_address"));
        view.setFacilityHotline(rs.getString("facility_hotline"));
        view.setFieldId(getLongOrNull(rs, "field_id"));
        view.setFieldName(rs.getString("field_name"));
        view.setFieldTypeName(rs.getString("field_type_name"));
        view.setNumberOfPlayers(getIntegerOrNull(rs, "number_of_players"));
        view.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
        view.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
        view.setOriginalPrice(rs.getBigDecimal("original_price"));
        view.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        view.setTotalAmount(rs.getBigDecimal("total_amount"));
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

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record FieldPricingContext(Long facilityId, Integer fieldTypeId) {
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
