<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.Map" %>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Admin";
            
    @SuppressWarnings("unchecked")
    Map<String, String> settingsMap = (Map<String, String>) request.getAttribute("settings");
    if (settingsMap == null) {
        settingsMap = java.util.Collections.emptyMap();
    }
%>
<%! 
    private String getVal(Map<String, String> map, String key) {
        if (map == null || !map.containsKey(key)) return "";
        String val = map.get(key);
        return val == null ? "" : val;
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Cài đặt hệ thống | Sport Field Booking</title>
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= currentName %>" data-active="Cài đặt"></div>

<main class="py-4">
    <div class="container">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h1 class="section-title mb-1">Cài đặt Hệ thống</h1>
                <p class="text-muted mb-0">Quản lý các cấu hình chung, quy định đặt sân và tài chính.</p>
            </div>
        </div>

        <% if ("1".equals(request.getParameter("success"))) { %>
        <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i> Đã lưu cài đặt thành công!
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
        <% } %>

        <form method="POST" action="<%= ctx %>/admin/settings">
            <div class="row g-4">
                <!-- Cấu hình Chung -->
                <div class="col-lg-6">
                    <div class="card soft-card border-0 shadow-sm h-100">
                        <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                            <h5 class="fw-bold mb-0"><i class="bi bi-gear-fill text-sf-primary me-2"></i>Cấu hình chung</h5>
                        </div>
                        <div class="card-body">
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Bảo trì hệ thống</label>
                                <div class="form-check form-switch fs-5">
                                    <input class="form-check-input" type="checkbox" role="switch" id="maintenanceMode" name="MAINTENANCE_MODE" <%= "true".equals(getVal(settingsMap, "MAINTENANCE_MODE")) ? "checked" : "" %>>
                                    <label class="form-check-label fs-6 text-muted ms-2" for="maintenanceMode">Tạm dừng cho phép khách hàng đặt sân để bảo trì</label>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Email liên hệ</label>
                                <input type="email" class="form-control" name="CONTACT_EMAIL" value="<%= getVal(settingsMap, "CONTACT_EMAIL") %>" placeholder="ví dụ: hotro@sportfield.vn">
                                <div class="form-text">Hiển thị ở khu vực chân trang (footer).</div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Số điện thoại hỗ trợ</label>
                                <input type="text" class="form-control" name="CONTACT_PHONE" value="<%= getVal(settingsMap, "CONTACT_PHONE") %>" placeholder="ví dụ: 1900 1234">
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cấu hình Đặt sân & Tài chính -->
                <div class="col-lg-6">
                    <div class="row g-4">
                        <div class="col-12">
                            <div class="card soft-card border-0 shadow-sm">
                                <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                                    <h5 class="fw-bold mb-0"><i class="bi bi-calendar-check-fill text-success me-2"></i>Quy định Đặt sân & Hủy</h5>
                                </div>
                                <div class="card-body">
                                    <div class="mb-3">
                                        <label class="form-label fw-semibold">Thời gian đặt trước tối đa (Ngày)</label>
                                        <input type="number" class="form-control" name="MAX_BOOKING_DAYS_AHEAD" value="<%= getVal(settingsMap, "MAX_BOOKING_DAYS_AHEAD") %>" min="1" max="365">
                                        <div class="form-text">Giới hạn khách hàng chỉ được đặt sân trước tối đa bao nhiêu ngày.</div>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-semibold">Thời gian hủy tối thiểu (Giờ)</label>
                                        <input type="number" class="form-control" name="MIN_CANCELLATION_HOURS" value="<%= getVal(settingsMap, "MIN_CANCELLATION_HOURS") %>" min="0">
                                        <div class="form-text">Khách hàng được phép hủy không mất phí nếu cách giờ đá hơn số giờ này.</div>
                                    </div>
                                    <div class="mb-3 d-none">
                                        <label class="form-label fw-semibold">Tỉ lệ cọc mặc định (%)</label>
                                        <input type="number" class="form-control" name="DEPOSIT_PERCENTAGE" value="<%= getVal(settingsMap, "DEPOSIT_PERCENTAGE") %>" min="0" max="100">
                                        <div class="form-text">Phần trăm số tiền khách phải cọc khi đặt sân.</div>
                                    </div>
                                    <div class="mb-3 d-none">
                                        <label class="form-label fw-semibold">Thời gian giữ chỗ (Phút)</label>
                                        <input type="number" class="form-control" name="BOOKING_HOLD_MINUTES" value="<%= getVal(settingsMap, "BOOKING_HOLD_MINUTES") %>" min="1">
                                        <div class="form-text">Thời gian chờ thanh toán trước khi tự động hủy hóa đơn.</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="col-12">
                            <div class="card soft-card border-0 shadow-sm mt-4">
                                <div class="card-header bg-white border-bottom-0 pt-4 pb-0">
                                    <h5 class="fw-bold mb-0"><i class="bi bi-star-fill text-warning me-2"></i>Tài chính & Ưu đãi VIP</h5>
                                </div>
                                <div class="card-body">
                                    <div class="mb-3">
                                        <label class="form-label fw-semibold">Giá gói VIP 1 tháng (VNĐ)</label>
                                        <input type="number" class="form-control" name="VIP_SUBSCRIPTION_PRICE_MONTHLY" value="<%= getVal(settingsMap, "VIP_SUBSCRIPTION_PRICE_MONTHLY") %>" min="0">
                                        <div class="form-text">Giá tiền để đăng ký/nâng cấp gói hội viên VIP.</div>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label fw-semibold">Giảm giá VIP (%)</label>
                                        <input type="number" class="form-control" name="VIP_DISCOUNT_PERCENTAGE" value="<%= getVal(settingsMap, "VIP_DISCOUNT_PERCENTAGE") %>" min="0" max="100">
                                        <div class="form-text">Phần trăm giảm giá dành riêng cho hội viên VIP mỗi khi đặt sân.</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>

                <div class="col-12 text-end mt-4">
                    <button type="submit" class="btn btn-sf-primary btn-lg px-5 shadow-sm">
                        <i class="bi bi-save me-2"></i> Lưu thay đổi
                    </button>
                </div>
            </div>
        </form>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>
