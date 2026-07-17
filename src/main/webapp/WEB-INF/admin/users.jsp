<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.List" %>
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

<%
    String successMessage = (String) session.getAttribute("successMessage");
    String errorMessage = (String) session.getAttribute("errorMessage");
    if (successMessage != null) {
%>
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <%= esc(successMessage) %>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
<%
        session.removeAttribute("successMessage");
    }
    if (errorMessage != null) {
%>
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <%= esc(errorMessage) %>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
<%
        session.removeAttribute("errorMessage");
    }
    
    String searchVal = request.getAttribute("search") != null ? (String) request.getAttribute("search") : "";
    String roleVal = request.getAttribute("role") != null ? (String) request.getAttribute("role") : "";
    String statusVal = request.getAttribute("status") != null ? (String) request.getAttribute("status") : "";
%>

        <!-- Filters & Toolbar -->
        <div class="card soft-card border-0 shadow-sm mb-4">
            <div class="card-body p-3">
                <form method="GET" action="<%= ctx %>/admin/users">
                <div class="row g-3">
                    <div class="col-md-4">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" value="<%= esc(searchVal) %>" class="form-control border-start-0 ps-0" placeholder="Tìm theo tên, email, SĐT...">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <select name="role" class="form-select">
                            <option value="">Tất cả vai trò</option>
                            <option value="Admin" <%= "Admin".equals(roleVal) ? "selected" : "" %>>Admin</option>
                            <option value="Staff" <%= "Staff".equals(roleVal) ? "selected" : "" %>>Nhân viên (Staff)</option>
                            <option value="Owner" <%= "Owner".equals(roleVal) ? "selected" : "" %>>Chủ sân (Owner)</option>
                            <option value="Customer" <%= "Customer".equals(roleVal) ? "selected" : "" %>>Khách hàng (Customer)</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select name="status" class="form-select">
                            <option value="">Tất cả trạng thái</option>
                            <option value="ACTIVE" <%= "ACTIVE".equals(statusVal) ? "selected" : "" %>>Đang hoạt động</option>
                            <option value="PENDING" <%= "PENDING".equals(statusVal) ? "selected" : "" %>>Chờ xác minh</option>
                            <option value="BANNED" <%= "BANNED".equals(statusVal) ? "selected" : "" %>>Bị khóa</option>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-outline-secondary w-100"><i class="bi bi-funnel me-1"></i> Lọc</button>
                    </div>
                </div>
                </form>
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
<%
    List<User> userList = (List<User>) request.getAttribute("userList");
    if (userList != null && !userList.isEmpty()) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (User u : userList) {
            String roleBadgeClass = "bg-secondary";
            String roleName = u.getRoleName() != null ? u.getRoleName() : "Khách hàng";
            if ("Admin".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-danger";
            else if ("Customer".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-primary";
            else if ("Owner".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-warning text-dark";
            else if ("Staff".equalsIgnoreCase(roleName)) roleBadgeClass = "bg-info";

            String statusBadgeClass = "bg-secondary";
            String statusText = u.getStatus() != null ? u.getStatus() : "ACTIVE";
            if ("ACTIVE".equalsIgnoreCase(statusText)) {
                statusBadgeClass = "bg-success bg-opacity-10 text-success border border-success border-opacity-25";
                statusText = "Hoạt động";
            } else if ("PENDING".equalsIgnoreCase(statusText)) {
                statusBadgeClass = "bg-warning bg-opacity-10 text-warning border border-warning border-opacity-25";
                statusText = "Chờ duyệt";
            } else if ("BANNED".equalsIgnoreCase(statusText)) {
                statusBadgeClass = "bg-danger bg-opacity-10 text-danger border border-danger border-opacity-25";
                statusText = "Đã khóa";
            }

            String firstLetter = u.getFullName() != null && u.getFullName().length() > 0 ? u.getFullName().substring(0, 1).toUpperCase() : "U";
            String avatarClass = "bg-gradient-secondary";
            if ("Admin".equalsIgnoreCase(roleName)) avatarClass = "bg-gradient-danger";
            else if ("Customer".equalsIgnoreCase(roleName)) avatarClass = "bg-gradient-primary";
            else if ("Owner".equalsIgnoreCase(roleName)) avatarClass = "bg-gradient-warning";
%>
                        <tr>
                            <td class="ps-4">
                                <div class="d-flex align-items-center">
                                    <div class="avatar-circle <%= avatarClass %> me-3"><%= esc(firstLetter) %></div>
                                    <div>
                                        <h6 class="mb-0 fw-bold <%= "BANNED".equalsIgnoreCase(u.getStatus()) ? "text-muted text-decoration-line-through" : "" %>"><%= esc(u.getFullName()) %></h6>
                                        <small class="text-muted">ID: #USR-<%= String.format("%03d", u.getUserId()) %></small>
                                    </div>
                                </div>
                            </td>
                            <td>
                                <div class="small"><i class="bi bi-envelope me-1 text-muted"></i> <%= esc(u.getEmail()) %></div>
                                <div class="small mt-1"><i class="bi bi-telephone me-1 text-muted"></i> <%= esc(u.getPhone()) %></div>
                            </td>
                            <td><span class="badge <%= roleBadgeClass %>"><%= esc(roleName) %></span></td>
                            <td><span class="text-muted small"><%= u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : "" %></span></td>
                            <td><span class="badge rounded-pill <%= statusBadgeClass %> px-2"><%= statusText %></span></td>
                            <td class="text-end pe-4">
                                <% if ("PENDING".equalsIgnoreCase(u.getStatus()) && "Owner".equalsIgnoreCase(roleName)) { %>
                                <form method="post" action="<%= ctx %>/admin/users" class="d-inline">
                                    <input type="hidden" name="action" value="approve">
                                    <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-success rounded-pill me-1"><i class="bi bi-check-circle me-1"></i> Duyệt</button>
                                </form>
                                <% } %>
                                <a href="<%= ctx %>/admin/user-details?id=<%= u.getUserId() %>" class="action-btn text-primary" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                <button class="action-btn text-secondary" title="Sửa" onclick="openEditModal('<%= u.getUserId() %>', '<%= esc(u.getFullName()) %>', '<%= esc(u.getPhone()) %>', '<%= esc(u.getEmail()) %>', '<%= esc(roleName) %>', '<%= esc(u.getStatus()) %>')"><i class="bi bi-pencil"></i></button>
                                <% if (!"BANNED".equalsIgnoreCase(u.getStatus())) { %>
                                <button class="action-btn text-danger" title="Khóa" onclick="openBanModal('<%= u.getUserId() %>')"><i class="bi bi-slash-circle"></i></button>
                                <% } else { %>
                                <form method="post" action="<%= ctx %>/admin/users" class="d-inline">
                                    <input type="hidden" name="action" value="unban">
                                    <input type="hidden" name="userId" value="<%= u.getUserId() %>">
                                    <button type="submit" class="action-btn text-success" title="Mở khóa"><i class="bi bi-unlock"></i></button>
                                </form>
                                <% } %>
                            </td>
                        </tr>
<%
        }
    } else {
%>
                        <tr>
                            <td colspan="6" class="text-center py-4 text-muted">Không có dữ liệu người dùng</td>
                        </tr>
<%
    }
%>
                    </tbody>
                </table>
            </div>
            
            <!-- Pagination -->
<%
    Integer currentPage = (Integer) request.getAttribute("currentPage");
    if (currentPage == null) currentPage = 1;
    Integer totalPages = (Integer) request.getAttribute("totalPages");
    if (totalPages == null) totalPages = 1;
    if (totalPages == 0) totalPages = 1; // display at least page 1
    
    String searchParam = searchVal != null && !searchVal.isEmpty() ? "&search=" + esc(searchVal) : "";
    String roleParam = roleVal != null && !roleVal.isEmpty() ? "&role=" + esc(roleVal) : "";
    String statusParam = statusVal != null && !statusVal.isEmpty() ? "&status=" + esc(statusVal) : "";
    String queryParams = searchParam + roleParam + statusParam;
%>
            <div class="card-footer bg-white border-top p-3">
                <div class="d-flex justify-content-between align-items-center">
                    <span class="text-muted small">Hiển thị dữ liệu trang <%= currentPage %> / <%= totalPages %></span>
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item <%= currentPage <= 1 ? "disabled" : "" %>">
                                <a class="page-link" href="?page=<%= currentPage - 1 %><%= queryParams %>">Trước</a>
                            </li>
                            <% for(int i = 1; i <= totalPages; i++) { %>
                            <li class="page-item <%= i == currentPage ? "active" : "" %>">
                                <a class="page-link" href="?page=<%= i %><%= queryParams %>"><%= i %></a>
                            </li>
                            <% } %>
                            <li class="page-item <%= currentPage >= totalPages ? "disabled" : "" %>">
                                <a class="page-link" href="?page=<%= currentPage + 1 %><%= queryParams %>">Sau</a>
                            </li>
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
                <form id="userForm" method="post" action="<%= ctx %>/admin/users">
                    <input type="hidden" name="action" id="userFormAction" value="add">
                    <input type="hidden" name="userId" id="userIdInput" value="">
                    <div class="row g-3">
                        <div class="col-12">
                            <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="fullName" id="fullNameInput" placeholder="Nhập họ tên" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Số điện thoại <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="phone" id="phoneInput" placeholder="Nhập SĐT" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Email <span class="text-danger">*</span></label>
                            <input type="email" class="form-control" name="email" id="emailInput" placeholder="Nhập Email" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Vai trò <span class="text-danger">*</span></label>
                            <select class="form-select" name="roleName" id="roleNameSelect">
                                <option value="Customer">Khách hàng</option>
                                <option value="Staff">Nhân viên</option>
                                <option value="Owner">Chủ sân</option>
                                <option value="Admin">Admin</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label small fw-semibold">Trạng thái</label>
                            <select class="form-select" name="status" id="statusSelect">
                                <option value="ACTIVE">Đang hoạt động</option>
                                <option value="PENDING">Chờ xác minh</option>
                                <option value="BANNED">Khóa</option>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-semibold">Mật khẩu mới (Nếu có)</label>
                            <input type="password" class="form-control" name="password" id="passwordInput" placeholder="Để trống nếu không muốn đổi">
                        </div>
                    </div>
                </form>
            </div>
            <div class="modal-footer border-top-0 pt-0">
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
                <button type="submit" form="userForm" class="btn btn-success">Lưu thông tin</button>
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
            <form method="post" action="<%= ctx %>/admin/users">
                <input type="hidden" name="action" value="ban">
                <input type="hidden" name="userId" id="banUserId" value="">
                <div class="d-flex justify-content-center gap-2">
                    <button type="button" class="btn btn-light w-50" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-danger w-50">Khóa ngay</button>
                </div>
            </form>
        </div>
    </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>

<script>
    function openEditModal(id, name, phone, email, role, status) {
        document.getElementById('userModalLabel').innerText = 'Sửa thông tin người dùng';
        document.getElementById('userFormAction').value = 'edit';
        document.getElementById('userIdInput').value = id;
        document.getElementById('fullNameInput').value = name;
        document.getElementById('phoneInput').value = phone;
        document.getElementById('emailInput').value = email;
        // Ensure case-insensitive match for roleNameSelect
        var selectOptions = document.getElementById('roleNameSelect').options;
        for (var i = 0; i < selectOptions.length; i++) {
            if (selectOptions[i].value.toLowerCase() === role.toLowerCase()) {
                document.getElementById('roleNameSelect').selectedIndex = i;
                break;
            }
        }
        document.getElementById('statusSelect').value = status;
        document.getElementById('passwordInput').placeholder = 'Để trống nếu không muốn đổi';
        
        clearValidationErrors();
        
        var userModal = new bootstrap.Modal(document.getElementById('userModal'));
        userModal.show();
    }

    function openBanModal(id) {
        document.getElementById('banUserId').value = id;
        var banModal = new bootstrap.Modal(document.getElementById('banModal'));
        banModal.show();
    }
    
    // Reset form when clicking Add user button
    document.querySelector('button[data-bs-target="#userModal"]').addEventListener('click', function() {
        if(this.getAttribute('title') !== 'Sửa') {
            document.getElementById('userModalLabel').innerText = 'Thêm người dùng mới';
            document.getElementById('userForm').reset();
            document.getElementById('userFormAction').value = 'add';
            document.getElementById('userIdInput').value = '';
            document.getElementById('passwordInput').placeholder = 'Mật khẩu mặc định: 123456';
            clearValidationErrors();
        }
    });

    // Real-time validation
    const fullNameInput = document.getElementById('fullNameInput');
    const phoneInput = document.getElementById('phoneInput');
    const emailInput = document.getElementById('emailInput');
    const passwordInput = document.getElementById('passwordInput');

    function createErrorMsg(inputElement, msg) {
        let err = inputElement.nextElementSibling;
        if (!err || !err.classList.contains('text-danger-msg')) {
            err = document.createElement('div');
            err.className = 'text-danger-msg text-danger small mt-1';
            inputElement.parentNode.insertBefore(err, inputElement.nextSibling);
        }
        err.innerText = msg;
        inputElement.classList.add('is-invalid');
    }

    function removeErrorMsg(inputElement) {
        let err = inputElement.nextElementSibling;
        if (err && err.classList.contains('text-danger-msg')) {
            err.remove();
        }
        inputElement.classList.remove('is-invalid');
    }

    function clearValidationErrors() {
        [fullNameInput, phoneInput, emailInput, passwordInput].forEach(removeErrorMsg);
    }

    function validateName() {
        if (!fullNameInput.value.trim()) {
            createErrorMsg(fullNameInput, 'Họ tên không được để trống');
            return false;
        } else {
            removeErrorMsg(fullNameInput);
            return true;
        }
    }

    function validatePhone() {
        const phoneRegex = /^[0-9]{10}$/;
        if (!phoneRegex.test(phoneInput.value.trim())) {
            createErrorMsg(phoneInput, 'Số điện thoại phải có 10 chữ số');
            return false;
        } else {
            removeErrorMsg(phoneInput);
            return true;
        }
    }

    function validateEmail() {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(emailInput.value.trim())) {
            createErrorMsg(emailInput, 'Email không hợp lệ');
            return false;
        } else {
            removeErrorMsg(emailInput);
            return true;
        }
    }

    function validatePassword() {
        const action = document.getElementById('userFormAction').value;
        if (action === 'add' && passwordInput.value && passwordInput.value.length < 6) {
            createErrorMsg(passwordInput, 'Mật khẩu phải có ít nhất 6 ký tự');
            return false;
        } else if (action === 'edit' && passwordInput.value && passwordInput.value.length < 6) {
            createErrorMsg(passwordInput, 'Mật khẩu phải có ít nhất 6 ký tự');
            return false;
        } else {
            removeErrorMsg(passwordInput);
            return true;
        }
    }

    fullNameInput.addEventListener('input', validateName);
    phoneInput.addEventListener('input', validatePhone);
    emailInput.addEventListener('input', validateEmail);
    passwordInput.addEventListener('input', validatePassword);

    document.getElementById('userForm').addEventListener('submit', function(e) {
        const isNameValid = validateName();
        const isPhoneValid = validatePhone();
        const isEmailValid = validateEmail();
        const isPasswordValid = validatePassword();
        if (!isNameValid || !isPhoneValid || !isEmailValid || !isPasswordValid) {
            e.preventDefault();
        }
    });
</script>

</body>
</html>
