<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.*" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%!
    private String esc(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null
            ? currentUser.getFullName()
            : "Admin";
            
    Map<String, Object> kpis = (Map<String, Object>) request.getAttribute("kpis");
    List<Map<String, Object>> revChart = (List<Map<String, Object>>) request.getAttribute("revChart");
    List<Map<String, Object>> typeChart = (List<Map<String, Object>>) request.getAttribute("typeChart");
    List<Map<String, Object>> recentBookings = (List<Map<String, Object>>) request.getAttribute("recentBookings");
    
    NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    
    BigDecimal todayRevenue = kpis != null ? (BigDecimal) kpis.get("todayRevenue") : BigDecimal.ZERO;
    int todayBookings = kpis != null ? (Integer) kpis.get("todayBookings") : 0;
    int newCustomers = kpis != null ? (Integer) kpis.get("newCustomers") : 0;
    int pendingUsers = kpis != null ? (Integer) kpis.get("pendingUsers") : 0;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Admin Dashboard | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= esc(currentName) %>" data-active="Dashboard"></div>

<main class="dashboard-shell py-4">
    <div class="container">
        <div class="d-flex justify-content-between items-center mb-4">
            <div>
                <h1 class="section-title mb-1">Admin Dashboard</h1>
                <p class="text-muted">Tổng quan tình hình hoạt động của toàn hệ thống.</p>
            </div>
            <div>
                <button class="btn btn-outline-success"><i class="bi bi-download"></i> Tải báo cáo</button>
            </div>
        </div>

        <!-- 1. KPI Cards -->
        <div class="row g-4 mb-4">
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100">
                    <i class="bi bi-currency-dollar stat-icon"></i>
                    <div class="text-muted fw-semibold">Doanh thu hôm nay</div>
                    <h3 class="fw-bold mt-2"><%= currencyFormat.format(todayRevenue) %>₫</h3>
                    <span class="text-success small"></span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #0dcaf0;">
                    <i class="bi bi-calendar-check stat-icon"></i>
                    <div class="text-muted fw-semibold">Lượt đặt sân hôm nay</div>
                    <h3 class="fw-bold mt-2"><%= todayBookings %></h3>
                    <span class="text-success small"></span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #ffc107;">
                    <i class="bi bi-people stat-icon"></i>
                    <div class="text-muted fw-semibold">Khách hàng mới</div>
                    <h3 class="fw-bold mt-2"><%= newCustomers %></h3>
                    <span class="text-muted small">Trong 7 ngày qua</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #dc3545;">
                    <i class="bi bi-percent stat-icon"></i>
                    <div class="text-muted fw-semibold">Tài khoản chờ duyệt</div>
                    <h3 class="fw-bold mt-2"><%= pendingUsers %></h3>
                    <span class="text-danger small"></span>
                </div>
            </div>
        </div>

        <!-- 2. Charts Section -->
        <div class="row g-4 mb-4">
            <div class="col-lg-8">
                <div class="card soft-card h-100 border-0 shadow-sm">
                    <div class="card-header bg-white border-0 pt-4 pb-0">
                        <h5 class="fw-bold">Biểu đồ doanh thu 7 ngày qua</h5>
                    </div>
                    <div class="card-body">
                        <canvas id="revenueChart" height="100"></canvas>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card soft-card h-100 border-0 shadow-sm">
                    <div class="card-header bg-white border-0 pt-4 pb-0">
                        <h5 class="fw-bold">Tỉ lệ đặt theo loại sân</h5>
                    </div>
                    <div class="card-body d-flex align-items-center justify-content-center">
                        <canvas id="fieldTypeChart" height="200"></canvas>
                    </div>
                </div>
            </div>
        </div>

        <!-- 3. Recent Activities & Pending Approvals -->
        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card soft-card border-0 shadow-sm h-100">
                    <div class="card-header bg-white border-0 pt-4 pb-0 d-flex justify-content-between align-items-center">
                        <h5 class="fw-bold m-0">Lượt đặt sân gần đây</h5>
                        <a href="<%= ctx %>/admin/bookings" class="text-success text-decoration-none small">Xem tất cả</a>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table recent-activity-table align-middle">
                                <thead>
                                    <tr>
                                        <th>Khách hàng</th>
                                        <th>Sân</th>
                                        <th>Thời gian</th>
                                        <th>Tổng tiền</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                <%
                                    if (recentBookings != null && !recentBookings.isEmpty()) {
                                        for (Map<String, Object> rb : recentBookings) {
                                            String startT = rb.get("startTime") != null ? rb.get("startTime").toString().substring(0,16) : "";
                                            String endT = rb.get("endTime") != null ? rb.get("endTime").toString().substring(11,16) : "";
                                            BigDecimal totalAmt = rb.get("totalAmount") != null ? (BigDecimal) rb.get("totalAmount") : BigDecimal.ZERO;
                                            String st = rb.get("status") != null ? rb.get("status").toString() : "";
                                            String badgeClass = "bg-secondary";
                                            String stText = st;
                                            if ("CONFIRMED".equalsIgnoreCase(st)) { badgeClass = "bg-success"; stText = "Đã xác nhận"; }
                                            else if ("PENDING".equalsIgnoreCase(st)) { badgeClass = "bg-warning text-dark"; stText = "Chờ thanh toán"; }
                                            else if ("CHECKED_IN".equalsIgnoreCase(st)) { badgeClass = "bg-info"; stText = "Đang đá"; }
                                            else if ("COMPLETED".equalsIgnoreCase(st)) { badgeClass = "bg-primary"; stText = "Hoàn thành"; }
                                            else if ("CANCELLED".equalsIgnoreCase(st)) { badgeClass = "bg-danger"; stText = "Đã hủy"; }
                                %>
                                    <tr>
                                        <td><strong><%= esc((String)rb.get("customerName")) %></strong><br><small class="text-muted"><%= esc((String)rb.get("customerPhone")) %></small></td>
                                        <td><%= esc((String)rb.get("fieldName")) %> (<%= esc((String)rb.get("facilityName")) %>)</td>
                                        <td><%= startT %> - <%= endT %></td>
                                        <td><%= currencyFormat.format(totalAmt) %>₫</td>
                                        <td><span class="badge <%= badgeClass %>"><%= stText %></span></td>
                                    </tr>
                                <%
                                        }
                                    } else {
                                %>
                                    <tr><td colspan="5" class="text-center text-muted py-3">Chưa có lượt đặt sân nào</td></tr>
                                <% } %>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-4">
                <div class="card soft-card border-0 shadow-sm mb-4">
                    <div class="card-body text-center p-4">
                        <div class="display-1 text-warning mb-2"><i class="bi bi-clock-history"></i></div>
                        <h5 class="fw-bold">Yêu cầu chờ duyệt</h5>
                        <p class="text-muted mb-3">Có <strong>7</strong> yêu cầu chờ xử lý (đặt sân, đối soát).</p>
                        <a href="<%= ctx %>/admin/owner-approval" class="btn btn-warning text-dark w-100">Xử lý ngay</a>
                    </div>
                </div>
                <div class="card soft-card border-0 shadow-sm">
                    <div class="card-body text-center p-4">
                        <div class="display-1 text-success mb-2"><i class="bi bi-person-plus"></i></div>
                        <h5 class="fw-bold">Tài khoản cần xác minh</h5>
                        <p class="text-muted mb-3">Có <strong><%= pendingUsers %></strong> người dùng chờ duyệt.</p>
                        <a href="<%= ctx %>/admin/users" class="btn btn-outline-success w-100">Quản lý người dùng</a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>

<!-- Prepare JSON Data -->
<%
    StringBuilder revLabels = new StringBuilder("[");
    StringBuilder revData = new StringBuilder("[");
    if (revChart != null) {
        for (int i=0; i<revChart.size(); i++) {
            revLabels.append("'").append(revChart.get(i).get("date")).append("'");
            revData.append(revChart.get(i).get("total"));
            if (i < revChart.size() - 1) {
                revLabels.append(", ");
                revData.append(", ");
            }
        }
    }
    revLabels.append("]");
    revData.append("]");
    
    StringBuilder typeLabels = new StringBuilder("[");
    StringBuilder typeData = new StringBuilder("[");
    if (typeChart != null) {
        for (int i=0; i<typeChart.size(); i++) {
            typeLabels.append("'").append(typeChart.get(i).get("typeName")).append("'");
            typeData.append(typeChart.get(i).get("count"));
            if (i < typeChart.size() - 1) {
                typeLabels.append(", ");
                typeData.append(", ");
            }
        }
    }
    typeLabels.append("]");
    typeData.append("]");
%>

<!-- Initialize Charts -->
<script>
    document.addEventListener("DOMContentLoaded", function() {
        // Revenue Chart
        const revCtx = document.getElementById('revenueChart').getContext('2d');
        new Chart(revCtx, {
            type: 'line',
            data: {
                labels: <%= revLabels.toString() %>,
                datasets: [{
                    label: 'Doanh thu (VNĐ)',
                    data: <%= revData.toString() %>,
                    borderColor: '#198754',
                    backgroundColor: 'rgba(25, 135, 84, 0.1)',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true }
                }
            }
        });

        // Field Type Chart
        const fieldCtx = document.getElementById('fieldTypeChart').getContext('2d');
        new Chart(fieldCtx, {
            type: 'doughnut',
            data: {
                labels: <%= typeLabels.toString() %>,
                datasets: [{
                    data: <%= typeData.toString() %>,
                    backgroundColor: ['#198754', '#0dcaf0', '#ffc107', '#dc3545', '#6c757d'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                cutout: '70%',
                plugins: {
                    legend: { position: 'bottom' }
                }
            }
        });
    });
</script>
</body>
</html>
