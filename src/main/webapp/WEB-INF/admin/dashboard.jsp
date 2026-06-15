<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
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
                    <h3 class="fw-bold mt-2">12,450,000₫</h3>
                    <span class="text-success small"><i class="bi bi-arrow-up-right"></i> +15% so với hôm qua</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #0dcaf0;">
                    <i class="bi bi-calendar-check stat-icon"></i>
                    <div class="text-muted fw-semibold">Lượt đặt sân</div>
                    <h3 class="fw-bold mt-2">48</h3>
                    <span class="text-success small"><i class="bi bi-arrow-up-right"></i> +5 lượt</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #ffc107;">
                    <i class="bi bi-people stat-icon"></i>
                    <div class="text-muted fw-semibold">Khách hàng mới</div>
                    <h3 class="fw-bold mt-2">12</h3>
                    <span class="text-muted small">Trong tuần này</span>
                </div>
            </div>
            <div class="col-md-3">
                <div class="stat-card p-4 position-relative h-100" style="border-left-color: #dc3545;">
                    <i class="bi bi-percent stat-icon"></i>
                    <div class="text-muted fw-semibold">Tỉ lệ lấp đầy</div>
                    <h3 class="fw-bold mt-2">68%</h3>
                    <span class="text-danger small"><i class="bi bi-arrow-down-right"></i> -2% so với tháng trước</span>
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
                                    <tr>
                                        <td><strong>Nguyễn Văn A</strong><br><small class="text-muted">0901234567</small></td>
                                        <td>Sân 5A (Cơ sở 1)</td>
                                        <td>18:00 - 19:30<br><small class="text-muted">Hôm nay</small></td>
                                        <td>450,000₫</td>
                                        <td><span class="badge bg-success">Đã xác nhận</span></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Trần Thị B</strong><br><small class="text-muted">0912345678</small></td>
                                        <td>Sân 7B (Cơ sở 2)</td>
                                        <td>20:00 - 21:30<br><small class="text-muted">Hôm nay</small></td>
                                        <td>600,000₫</td>
                                        <td><span class="badge bg-warning text-dark">Chờ thanh toán</span></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Lê Hoàng C</strong><br><small class="text-muted">0987654321</small></td>
                                        <td>Sân 11 (Cơ sở 1)</td>
                                        <td>16:00 - 18:00<br><small class="text-muted">Ngày mai</small></td>
                                        <td>1,200,000₫</td>
                                        <td><span class="badge bg-success">Đã thanh toán</span></td>
                                    </tr>
                                    <tr>
                                        <td><strong>Phạm Văn D</strong><br><small class="text-muted">0934567890</small></td>
                                        <td>Sân 5C (Cơ sở 3)</td>
                                        <td>19:00 - 20:30<br><small class="text-muted">Hôm qua</small></td>
                                        <td>400,000₫</td>
                                        <td><span class="badge bg-secondary">Hoàn thành</span></td>
                                    </tr>
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
                        <p class="text-muted mb-3">Có <strong>3</strong> nhân viên/chủ sân mới đăng ký.</p>
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

<!-- Initialize Charts -->
<script>
    document.addEventListener("DOMContentLoaded", function() {
        // Revenue Chart
        const revCtx = document.getElementById('revenueChart').getContext('2d');
        new Chart(revCtx, {
            type: 'line',
            data: {
                labels: ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'],
                datasets: [{
                    label: 'Doanh thu (VNĐ)',
                    data: [8000000, 9500000, 7200000, 11000000, 15000000, 22000000, 18000000],
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
                labels: ['Sân 5', 'Sân 7', 'Sân 11'],
                datasets: [{
                    data: [55, 30, 15],
                    backgroundColor: ['#198754', '#0dcaf0', '#ffc107'],
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
