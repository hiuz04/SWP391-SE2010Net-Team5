<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ page import="com.swp.model.Field" %>
<%@ page import="com.swp.model.FieldType" %>
<%@ page import="com.swp.model.dto.FieldScheduleSlot" %>
<%@ page import="com.swp.model.User" %>

<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.LocalTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.LinkedHashSet" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Set" %>

<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean supportedFieldType(FieldType fieldType) {
        if (fieldType == null) return false;
        Integer players = fieldType.getNumberOfPlayers();
        if (players != null && (players == 5 || players == 7 || players == 11)) {
            return true;
        }

        String typeName = fieldType.getTypeName();
        return "Sân 5".equalsIgnoreCase(typeName)
                || "Sân 7".equalsIgnoreCase(typeName)
                || "Sân 11".equalsIgnoreCase(typeName);
    }
%>

<%
    String ctx = request.getContextPath();

    Long complexId = (Long) request.getAttribute("complexId");
    LocalDate selectedDate = (LocalDate) request.getAttribute("selectedDate");
    LocalDate maxBookingDate = (LocalDate) request.getAttribute("maxBookingDate");
    String error = (String) request.getAttribute("error");

    List<Field> fields = (List<Field>) request.getAttribute("fields");
    List<FieldType> fieldTypes = (List<FieldType>) request.getAttribute("fieldTypes");
    Map<Long, String> fieldTypeNameByFieldId =
            (Map<Long, String>) request.getAttribute("fieldTypeNameByFieldId");
    List<String> timeHeaders = (List<String>) request.getAttribute("timeHeaders");
    Map<Long, List<FieldScheduleSlot>> scheduleMap =
            (Map<Long, List<FieldScheduleSlot>>) request.getAttribute("scheduleMap");

    if (selectedDate == null) {
        selectedDate = LocalDate.now();
    }
    if (maxBookingDate == null) {
        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        maxBookingDate = nextMonth.withDayOfMonth(nextMonth.lengthOfMonth());
    }

    if (fields == null) {
        fields = new ArrayList<>();
    }

    if (fieldTypes == null) {
        fieldTypes = new ArrayList<>();
    }

    if (fieldTypeNameByFieldId == null) {
        fieldTypeNameByFieldId = new LinkedHashMap<>();
    }

    if (timeHeaders == null) {
        timeHeaders = new ArrayList<>();

        LocalTime current = LocalTime.of(5, 0);
        LocalTime last = LocalTime.of(20, 30);

        while (!current.isAfter(last)) {
            timeHeaders.add(current.toString());
            current = current.plusMinutes(30);
        }
    }

    if (scheduleMap == null) {
        scheduleMap = new LinkedHashMap<>();
    }

    User currentUser = (User) session.getAttribute("user");

    String currentName = "Người dùng";
    if (currentUser != null && currentUser.getFullName() != null) {
        currentName = currentUser.getFullName();
    }

    DateTimeFormatter slotFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
%>

<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <title>Chọn giờ đặt sân</title>

    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">

    <style>
        body {
            background: #f3f6f9;
        }

        .booking-card {
            border-radius: 18px;
            border: 1px solid #dde5ec;
            background: #ffffff;
            padding: 24px;
            box-shadow: 0 8px 20px rgba(15, 23, 42, 0.05);
        }

        .page-title {
            font-size: 34px;
            font-weight: 800;
            margin-bottom: 4px;
        }

        .schedule-wrapper {
            overflow-x: auto;
            border: 1px solid #92cfd2;
            border-radius: 12px;
            background: #eefaf4;
        }

        .schedule-table {
            border-collapse: collapse;
            min-width: 1500px;
            width: max-content;
            font-size: 13px;
        }

        .schedule-table th,
        .schedule-table td {
            border: 1px solid #777;
            height: 42px;
            text-align: center;
            vertical-align: middle;
        }

        .field-name-col {
            min-width: 135px;
            width: 135px;
            position: sticky;
            left: 0;
            z-index: 5;
            background: #e1faee;
            font-weight: 600;
            color: #007444;
        }

        .field-name-text,
        .field-type-label {
            display: block;
            line-height: 1.2;
        }

        .field-type-label {
            margin-top: 2px;
            color: #64748b;
            font-size: 11px;
            font-weight: 500;
        }

        .time-header {
            min-width: 52px;
            width: 52px;
            background: #b9f3f5;
            color: #004c50;
            font-weight: 700;
            font-size: 12px;
        }

        .slot-cell {
            min-width: 52px;
            width: 52px;
            cursor: pointer;
            user-select: none;
            transition: 0.15s;
        }

        .slot-available {
            background: #f8fff8;
        }

        .slot-available:hover {
            background: #c7f7d3;
        }

        .slot-booked {
            background: #bd4242;
            cursor: not-allowed;
        }

        .slot-maintenance {
            background: #b5b5b5;
            cursor: not-allowed;
        }

        .slot-disabled {
            background: #b5b5b5;
            cursor: not-allowed;
        }

        .selected-range {
            background: #9d5cab !important;
            color: white;
        }

        .selected-start,
        .selected-end {
            outline: 4px solid #ffd12f;
            outline-offset: -4px;
        }

        .legend-dot {
            display: inline-block;
            width: 18px;
            height: 18px;
            border-radius: 4px;
            margin-right: 6px;
            vertical-align: middle;
            border: 1px solid #9ca3af;
        }

        .legend-available {
            background: #f8fff8;
        }

        .legend-selected {
            background: #9d5cab;
        }

        .legend-booked {
            background: #bd4242;
        }

        .legend-maintenance {
            background: #b5b5b5;
        }

        .selected-info {
            background: #c9f4fb;
            border: 1px solid #8bddec;
            color: #075985;
            border-radius: 6px;
            padding: 14px 16px;
        }

        .btn-green {
            background: #16a34a;
            color: white;
            border: none;
            border-radius: 8px;
            padding: 11px 20px;
            font-weight: 600;
        }

        .btn-green:hover {
            background: #15803d;
            color: white;
        }

        .btn-back {
            border-radius: 8px;
            padding: 11px 20px;
        }
    </style>
</head>

<body>

<div id="navbar"
     data-root="<%= ctx %>/"
     data-role="customer"
     data-name="<%= currentName %>"
     data-active="Tìm sân"></div>

<main class="py-4">
    <div class="container-fluid px-4">
        <div class="booking-card">

            <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-3">
                <div>
                    <h1 class="page-title">Chọn giờ đặt sân</h1>
                    <p class="text-muted mb-0">
                        Click vào ô trống để chọn giờ bắt đầu và giờ kết thúc.
                    </p>
                </div>

                <div class="d-flex gap-3 flex-wrap">
                    <div>
                        <label for="repeatType" class="form-label mb-1">Loại thuê</label>
                        <select id="repeatType" class="form-select">
                            <option value="NONE">Thuê đơn lẻ</option>
                            <option value="MONTHLY">Thuê theo tháng</option>
                        </select>
                    </div>

                    <div>
                        <label for="fieldTypeFilter" class="form-label mb-1">Loại sân</label>
                        <select id="fieldTypeFilter" class="form-select">
                            <option value="">Tất cả loại sân</option>
                            <%
                                Set<String> renderedFieldTypes = new LinkedHashSet<>();
                                for (FieldType fieldType : fieldTypes) {
                                    if (!supportedFieldType(fieldType)) {
                                        continue;
                                    }

                                    Integer players = fieldType.getNumberOfPlayers();
                                    String typeName = fieldType.getTypeName();
                                    if ((typeName == null || typeName.isBlank()) && players != null) {
                                        typeName = "Sân " + players;
                                    }

                                    if (typeName != null && !typeName.isBlank()
                                            && renderedFieldTypes.add(typeName)) {
                            %>
                            <option value="<%= esc(typeName) %>"><%= esc(typeName) %></option>
                            <%
                                    }
                                }

                                if (renderedFieldTypes.isEmpty()) {
                                    for (String typeName : fieldTypeNameByFieldId.values()) {
                                        if (typeName != null && !typeName.isBlank()
                                                && renderedFieldTypes.add(typeName)) {
                            %>
                            <option value="<%= esc(typeName) %>"><%= esc(typeName) %></option>
                            <%
                                        }
                                    }
                                }
                            %>
                        </select>
                    </div>

                    <div>
                        <label for="bookingDate" class="form-label mb-1">Chọn ngày</label>
                        <input type="date"
                               id="bookingDate"
                               class="form-control"
                               min="<%= LocalDate.now() %>"
                               max="<%= maxBookingDate %>"
                               value="<%= selectedDate %>">
                    </div>
                </div>
            </div>

            <div class="d-flex gap-3 flex-wrap mb-3">
                <% if (error != null && !error.isBlank()) { %>
                <div class="alert alert-danger w-100 mb-0"><%= error %></div>
                <% } %>

                <span>
                    <span class="legend-dot legend-available"></span>Còn trống
                </span>

                <span>
                    <span class="legend-dot legend-selected"></span>Đang chọn
                </span>

                <span>
                    <span class="legend-dot legend-booked"></span>Đã đặt
                </span>

                <span>
                    <span class="legend-dot legend-maintenance"></span>Bảo trì / khóa sân
                </span>
            </div>

            <div class="schedule-wrapper">
                <table class="schedule-table">
                    <thead>
                    <tr>
                        <th class="field-name-col">Sân</th>

                        <% for (String time : timeHeaders) { %>
                        <th class="time-header"><%= time %></th>
                        <% } %>
                    </tr>
                    </thead>

                    <tbody>
                    <%
                        if (fields.isEmpty()) {
                    %>
                    <tr>
                        <td class="field-name-col">Không có sân</td>
                        <td colspan="<%= timeHeaders.size() %>" class="text-muted">
                            Không có dữ liệu sân cho cơ sở này.
                        </td>
                    </tr>
                    <%
                    } else {
                        for (Field field : fields) {
                            String fieldTypeName = fieldTypeNameByFieldId.get(field.getFieldId());
                            if (fieldTypeName == null || fieldTypeName.isBlank()) {
                                fieldTypeName = "Chưa xác định";
                            }

                            List<FieldScheduleSlot> slots = scheduleMap.get(field.getFieldId());

                            if (slots == null) {
                                slots = new ArrayList<>();
                            }
                    %>

                    <tr data-field-id="<%= field.getFieldId() %>"
                        data-field-type="<%= esc(fieldTypeName) %>">
                        <td class="field-name-col">
                            <span class="field-name-text"><%= esc(field.getFieldName()) %></span>
                            <span class="field-type-label">Loại sân: <%= esc(fieldTypeName) %></span>
                        </td>

                        <%
                            for (FieldScheduleSlot slot : slots) {
                                String status = slot.getStatus();

                                String cssClass = "slot-disabled";
                                String title = "Không khả dụng";

                                if ("AVAILABLE".equals(status)) {
                                    cssClass = "slot-available";
                                    title = "Còn trống";
                                } else if ("BOOKED".equals(status)) {
                                    cssClass = "slot-booked";
                                    title = "Đã đặt";
                                } else if ("MAINTENANCE".equals(status)) {
                                    cssClass = "slot-maintenance";
                                    title = "Bảo trì / khóa sân";
                                }

                                String startValue = slot.getSlotStart().format(slotFormatter);
                                String endValue = slot.getSlotEnd().format(slotFormatter);
                                String startLabel = slot.getSlotStart().toLocalTime().format(timeFormatter);
                                String endLabel = slot.getSlotEnd().toLocalTime().format(timeFormatter);
                        %>

                        <td class="slot-cell <%= cssClass %>"
                            title="<%= title %>"
                            data-status="<%= status %>"
                            data-field-id="<%= slot.getFieldId() %>"
                            data-field-name="<%= esc(slot.getFieldName()) %>"
                            data-field-type="<%= esc(fieldTypeName) %>"
                            data-start="<%= startValue %>"
                            data-end="<%= endValue %>"
                            data-start-label="<%= startLabel %>"
                            data-end-label="<%= endLabel %>">
                        </td>

                        <%
                            }
                        %>
                    </tr>

                    <%
                            }
                        }
                    %>
                    </tbody>
                </table>
            </div>

            <div class="mt-4">
                <div class="selected-info mb-3" id="selectedInfo">
                    Bạn chưa chọn khung giờ.
                </div>

                <div class="d-flex gap-2">
                    <button type="button" class="btn btn-green btn-lg" id="continueButton">
                        Tiếp tục xác nhận
                    </button>

                    <a href="<%= ctx %>/" class="btn btn-outline-secondary btn-lg btn-back">
                        Quay lại
                    </a>
                </div>
            </div>

        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>

<script>
    const ctx = '<%= ctx %>';
    const complexId = '<%= complexId == null ? "" : complexId %>';
    const selectedDateText = '<%= selectedDate %>';

    const bookingDateInput = document.getElementById('bookingDate');
    const repeatTypeInput = document.getElementById('repeatType');
    const fieldTypeFilter = document.getElementById('fieldTypeFilter');
    const selectedInfo = document.getElementById('selectedInfo');
    const continueButton = document.getElementById('continueButton');

    let startCell = null;
    let endCell = null;

    bookingDateInput.addEventListener('change', function () {
        if (!complexId) {
            alert('Không tìm thấy complexId.');
            return;
        }

        window.location.href =
            ctx + '/booking?action=create&complexId=' + complexId + '&date=' + this.value;
    });

    repeatTypeInput.addEventListener('change', function () {
        if (startCell !== null && endCell !== null) {
            updateSelectedInfo();
        }
    });

    if (fieldTypeFilter) {
        fieldTypeFilter.addEventListener('change', applyFieldTypeFilter);
    }

    document.querySelectorAll('.slot-cell').forEach(cell => {
        cell.addEventListener('click', function () {
            if (this.dataset.status !== 'AVAILABLE') {
                return;
            }

            if (
                startCell === null ||
                this.dataset.fieldId !== startCell.dataset.fieldId ||
                this.dataset.start < startCell.dataset.start
            ) {
                setRange(this, this);
                return;
            }

            setRange(startCell, this);
        });
    });

    function setRange(start, end) {
        const row = start.closest('tr');
        const cells = Array.from(row.querySelectorAll('.slot-cell'));

        const startIndex = cells.indexOf(start);
        const endIndex = cells.indexOf(end);

        const from = Math.min(startIndex, endIndex);
        const to = Math.max(startIndex, endIndex);

        for (let i = from; i <= to; i++) {
            if (cells[i].dataset.status !== 'AVAILABLE') {
                alert('Khoảng giờ bạn chọn có ô đã bận hoặc sân đang bảo trì.');
                return;
            }
        }

        clearSelection();

        startCell = cells[from];
        endCell = cells[to];

        for (let i = from; i <= to; i++) {
            cells[i].classList.add('selected-range');
        }

        startCell.classList.add('selected-start');
        endCell.classList.add('selected-end');

        updateSelectedInfo();
    }

    function updateSelectedInfo() {
        const repeatNote = repeatTypeInput.value === 'MONTHLY'
            ? ' | Thuê theo tháng: hệ thống sẽ tự chọn các tuần tiếp theo trong tháng này và tháng sau, rồi kiểm tra trùng lịch.'
            : ' | Thuê đơn lẻ';

        selectedInfo.innerHTML =
            '<strong>Đã chọn:</strong> '
            + startCell.dataset.fieldName
            + ' | '
            + startCell.dataset.startLabel
            + ' - '
            + endCell.dataset.endLabel
            + ' | Ngày '
            + selectedDateText
            + repeatNote;
    }

    function clearSelection() {
        document.querySelectorAll('.slot-cell').forEach(cell => {
            cell.classList.remove('selected-range', 'selected-start', 'selected-end');
        });
        startCell = null;
        endCell = null;
    }

    function resetSelectedInfo() {
        selectedInfo.textContent = 'Bạn chưa chọn khung giờ.';
    }

    function applyFieldTypeFilter() {
        const selectedFieldType = fieldTypeFilter.value;
        let selectedRowHidden = false;

        document.querySelectorAll('.schedule-table tbody tr[data-field-id]').forEach(row => {
            const matches = !selectedFieldType || row.dataset.fieldType === selectedFieldType;
            row.classList.toggle('d-none', !matches);

            if (!matches && startCell !== null && row === startCell.closest('tr')) {
                selectedRowHidden = true;
            }
        });

        if (selectedRowHidden) {
            clearSelection();
            resetSelectedInfo();
        }
    }

    continueButton.addEventListener('click', function () {
        if (startCell !== null && endCell !== null) {
            window.location.href =
                ctx
                + '/booking?action=confirm'
                + '&fieldId=' + encodeURIComponent(startCell.dataset.fieldId)
                + '&startTime=' + encodeURIComponent(startCell.dataset.start)
                + '&endTime=' + encodeURIComponent(endCell.dataset.end)
                + '&repeatType=' + encodeURIComponent(repeatTypeInput.value);
            return;
        }

        if (startCell === null || endCell === null) {
            alert('Vui lòng chọn khung giờ trước.');
            return;
        }
    });
</script>

</body>
</html>

