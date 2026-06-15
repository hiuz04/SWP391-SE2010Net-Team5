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
    <title>Quản lý người dùng | Sport Field Booking</title>
    <style>
        .avatar-circle {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
            color: white;
        }
        .bg-gradient-primary { background: linear-gradient(135deg, #0d6efd, #0dcaf0); }
        .bg-gradient-success { background: linear-gradient(135deg, #198754, #20c997); }
        .bg-gradient-warning { background: linear-gradient(135deg, #ffc107, #fd7e14); }
        .bg-gradient-danger { background: linear-gradient(135deg, #dc3545, #f87171); }
        .bg-gradient-secondary { background: linear-gradient(135deg, #6c757d, #adb5bd); }
        
        .table-hover tbody tr:hover {
            background-color: #f8f9fa;
        }
        .action-btn {
            width: 32px;
            height: 32px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border-radius: 6px;
            transition: all 0.2s;
        }
        .action-btn:hover {
            background-color: #e9ecef;
        }
    </style>
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= esc(currentName) %>" data-active="Người dùng"></div>

<main class="py-4">
    <div class="container">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h1 class="section-title mb-1">Quản lý Người dùng</h1>
                <p class="text-muted mb-0">Quản lý tất cả tài khoản Khách hàng, Chủ sân, và Nhân viên.</p>
            </div>
            <button class="btn btn-success" data-bs-toggle="modal" data-bs-target="#userModal">
                <i class="bi bi-person-plus-fill me-2"></i>Thêm người dùng mới
            </button>
        </div>

        <!-- Filters & Toolbar -->
        <div class="card soft-card border-0 shadow-sm mb-4">
            <div class="card-body p-3">
                <div class="row g-3">
                    <div class="col-md-4">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" class="form-control border-start-0 ps-0" placeholder="Tìm theo tên, email, SĐT...">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <select class="form-select">
                            <option value="">Tất cả vai trò</option>
                            <option value="Admin">Admin</option>
                            <option value="Manager">Quản lý (Manager)</option>
                            <option value="Staff">Nhân viên (Staff)</option>
                            <option value="Owner">Chủ sân (Owner)</option>
                            <option value="Customer">Khách hàng (Customer)</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select class="form-select">
                            <option value="">Tất cả trạng thái</option>
                            <option value="Active">Đang hoạt động</option>
                            <option value="Pending">Chờ xác minh</option>
                            <option value="Banned">Bị khóa</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button class="btn btn-outline-secondary w-100"><i class="bi bi-funnel me-1"></i> Lọc</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- User Table -->
        <div class="card soft-card border-0 shadow-sm mb-4">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">Người dùng</th>
                            <th>Liên hệ</th>
                            <th>Vai trò</th>
                            <th>Ngày tham gia</th>
                            <th>Trạng thái</th>
                            <th class="text-end pe-4">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- Row 1: Admin -->
                        <tr>
                            <td class="ps-4">
                                <div class="d-flex align-items-center">
                                    <div class="avatar-circle bg-gradient-danger me-3">H</div>
                                    <div>
                                        <h6 class="mb-0 fw-bold">Hệ Thống Admin</h6>
                                        <small class="text-muted">ID: #USR-001</small>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div class="small"><i class="bi bi-envelope me-1 text-muted"></i> admin@system.com</div>
                                <div class="small mt-1"><i class="bi bi-telephone me-1 text-muted"></i> 0900 123 456</div>
                            </td>
                            <td><span class="badge bg-danger">Admin</span></td>
                            <td><span class="text-muted small">01/01/2023</span></td>
                            <td><span class="badge rounded-pill bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2">Hoạt động</span></td>
                            <td class="text-end pe-4">
                                <a href="<%= ctx %>/admin/user-details" class="action-btn text-primary" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                <button class="action-btn text-secondary" title="Sửa" data-bs-toggle="modal" data-bs-target="#userModal"><i class="bi bi-pencil"></i></button>
                            </td>
                        </tr>
                        <!-- Row 2: Customer -->
                        <tr>
                            <td class="ps-4">
                                <div class="d-flex align-items-center">
                                    <div class="avatar-circle bg-gradient-primary me-3">N</div>
                                    <div>
                                        <h6 class="mb-0 fw-bold">Nguyễn Văn Khách</h6>
                                        <small class="text-muted">ID: #USR-042</small>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div class="small"><i class="bi bi-envelope me-1 text-muted"></i> khach.nguyen@example.com</div>
                                <div class="small mt-1"><i class="bi bi-telephone me-1 text-muted"></i> 0912 345 678</div>
                            </td>
                            <td><span class="badge bg-primary">Khách hàng</span></td>
                            <td><span class="text-muted small">15/05/2023</span></td>
                            <td><span class="badge rounded-pill bg-success bg-opacity-10 text-success border border-success border-opacity-25 px-2">Hoạt động</span></td>
                            <td class="text-end pe-4">
                                <a href="<%= ctx %>/admin/user-details" class="action-btn text-primary" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                <button class="action-btn text-secondary" title="Sửa" data-bs-toggle="modal" data-bs-target="#userModal"><i class="bi bi-pencil"></i></button>
                                <button class="action-btn text-danger" title="Khóa" data-bs-toggle="modal" data-bs-target="#banModal"><i class="bi bi-slash-circle"></i></button>
                            </td>
                        </tr>
                        <!-- Row 3: Owner Pending -->
                        <tr>
                            <td class="ps-4">
                                <div class="d-flex align-items-center">
                                    <div class="avatar-circle bg-gradient-warning me-3">T</div>
                                    <div>
                                        <h6 class="mb-0 fw-bold">Trần Chủ Sân</h6>
                                        <small class="text-muted">ID: #USR-105</small>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div class="small"><i class="bi bi-envelope me-1 text-muted"></i> chu.tran@sport.vn</div>
                                <div class="small mt-1"><i class="bi bi-telephone me-1 text-muted"></i> 0988 765 432</div>
                            </td>
                            <td><span class="badge bg-warning text-dark">Chủ sân</span></td>
                            <td><span class="text-muted small">Vừa xong</span></td>
                            <td><span class="badge rounded-pill bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25 px-2">Chờ duyệt</span></td>
                            <td class="text-end pe-4">
                                <button class="btn btn-sm btn-outline-success rounded-pill me-1"><i class="bi bi-check-circle me-1"></i> Duyệt</button>
                                <a href="<%= ctx %>/admin/user-details" class="action-btn text-primary" title="Chi tiết"><i class="bi bi-eye"></i></a>
                            </td>
                        </tr>
                        <!-- Row 4: Banned User -->
                        <tr>
                            <td class="ps-4">
                                <div class="d-flex align-items-center">
                                    <div class="avatar-circle bg-gradient-secondary me-3">L</div>
                                    <div>
                                        <h6 class="mb-0 fw-bold text-muted text-decoration-line-through">Lê Bị Khóa</h6>
                                        <small class="text-muted">ID: #USR-088</small>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div class="small"><i class="bi bi-envelope me-1 text-muted"></i> le.spam@spam.com</div>
                                <div class="small mt-1"><i class="bi bi-telephone me-1 text-muted"></i> 0999 999 999</div>
                            </td>
                            <td><span class="badge bg-primary opacity-50">Khách hàng</span></td>
                            <td><span class="text-muted small">01/02/2023</span></td>
                            <td><span class="badge rounded-pill bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25 px-2">Đã khóa</span></td>
                            <td class="text-end pe-4">
                                <button class="action-btn text-success" title="Mở khóa"><i class="bi bi-unlock"></i></button>
                                <button class="action-btn text-danger" title="Xóa vĩnh viễn"><i class="bi bi-trash"></i></button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            
            <!-- Pagination -->
            <div class="card-footer bg-white border-top p-3">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="text-muted small">Hiển thị 1 đến 4 trong số 128 người dùng</span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item disabled"><a class="page-link" href="#">Trước</a></li>
                            <li class="page-item active"><a class="page-link" href="#">1</a></li>
                            <li class="page-item"><a class="page-link" href="#">2</a></li>
                            <li class="page-item"><a class="page-link" href="#">3</a></li>
                            <li class="page-item"><a class="page-link" href="#">Sau</a></li>
                        </ul>
                    </nav>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- Modal: Thêm / Sửa Người Dùng -->
<div class="modal fade" id="userModal" tabindex="-1" aria-labelledby="userModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header border-bottom-0 pb-0">
                <h5 class="modal-title fw-bold" id="userModalLabel">Thông tin người dùng</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <form>
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" placeholder="Nhập họ tên">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" placeholder="Nhập SĐT">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Email <span class="text-danger">*</span></label>
                            <input type="email" class="form-control" placeholder="Nhập Email">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Vai trò <span class="text-danger">*</span></label>
                            <select class="form-select">
                                <option value="Customer">Khách hàng</option>
                                <option value="Staff">Nhân viên</option>
                                <option value="Manager">Quản lý</option>
                                <option value="Owner">Chủ sân</option>
                                <option value="Admin">Admin</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Trạng thái</label>
                            <select class="form-select">
                                <option value="Active">Đang hoạt động</option>
                                <option value="Pending">Chờ xác minh</option>
                                <option value="Banned">Khóa</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-semibold">Mật khẩu mới (Nếu có)</label>
                            <input type="password" class="form-control" placeholder="Để trống nếu không muốn đổi">
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                <button type="button" class="btn btn-success">Lưu thông tin</button>
            </div>
        </div>
    </div>
</div>

<!-- Modal: Xác nhận Khóa (Ban) -->
<div class="modal fade" id="banModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 shadow text-center p-4">
            <div class="mb-3 text-danger">
                <i class="bi bi-exclamation-triangle-fill display-1"></i>
            </div>
            <h5 class="fw-bold mb-2">Xác nhận khóa tài khoản</h5>
            <p class="text-muted small mb-4">Tài khoản này sẽ không thể đăng nhập và đặt sân được nữa. Bạn có chắc chắn?</p>
            <div class="d-flex justify-content-center gap-2">
                <button type="button" class="btn btn-light w-50" data-bs-dismiss="modal">Hủy</button>
                <button type="button" class="btn btn-danger w-50">Khóa ngay</button>
            </div>
        </div>
    </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>

</body>
</html>
