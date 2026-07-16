<%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <%@ page import="com.swp.model.User" %>
<%@ page import="java.util.Map" %>
            <%@ page import="java.util.Collections" %>
                <%@ page import="java.time.LocalDateTime" %>
                    <%@ page import="java.time.format.DateTimeFormatter" %>
                        <% User currentUser=(User) request.getAttribute("currentUser"); if (currentUser==null) {
                    response.sendRedirect(request.getContextPath() + "/profile" ); return; } String
                    ctx=request.getContextPath(); String navRole=session.getAttribute("navRole") !=null ? (String)
                    session.getAttribute("navRole") : "guest" ; String displayName=currentUser.getFullName(); String
                    success=(String) request.getAttribute("success"); String error=(String)
                    request.getAttribute("error"); Map<String, String> errors = request.getAttribute("errors")
                    instanceof Map
                    ? (Map<String, String>) request.getAttribute("errors")
                        : Collections.emptyMap();

                        String fullNameClass = errors.containsKey("fullName") ? "is-invalid" : "";
                        String phoneClass = errors.containsKey("phone") ? "is-invalid" : "";
                        String emailClass = errors.containsKey("email") ? "is-invalid" : "";
                        String passwordClass = errors.containsKey("password") ? "is-invalid" : "";

                        %>
                        <!DOCTYPE html>
                        <html lang="vi">

                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
                                rel="stylesheet">
                            <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
                                rel="stylesheet">
                            <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
                            <title>Hồ sơ | Sport Field Booking</title>
                        </head>

                        <body>
                            <div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>"
                                data-name="<%= displayName %>" data-active="Hồ sơ"></div>

                            <main class="py-5">
                                <div class="container">
                                    <div class="row g-4">
                                        <aside class="col-lg-4">
                                            <div class="card soft-card p-4 text-center">
                                                <h4 class="mt-3 fw-bold">
                                                    <%= currentUser.getFullName() %>
                                                </h4>
                                                <p class="text-muted">
                                                    <%= currentUser.getRoleName() !=null ? currentUser.getRoleName()
                                                        : "Khách hàng" %>
                                                </p>

                                                <div class="row g-2 mt-2">
                                                    <div class="col">
                                                        <div class="stat-card p-3">
                                                            <strong class="fs-4 text-sf-primary">
                                                                <%= request.getAttribute("bookingCount") !=null ?
                                                                    request.getAttribute("bookingCount") : 0 %>
                                                            </strong>
                                                            <div class="small text-muted">Booking</div>
                                                        </div>
                                                    </div>
                                                </div>

                                                <div class="mt-4 text-start">
                                                    <%
                                                        boolean isCustomer = "Customer".equalsIgnoreCase(currentUser.getRoleName());
                                                        if (isCustomer) {
                                                    %>
                                                    <h6 class="fw-bold">Trạng thái Hội Viên</h6>
                                                    <%
                                                            boolean isVip = currentUser.isVip();
                                                            LocalDateTime validUntil = currentUser.getVipValidUntil();
                                                            boolean isVipActive = isVip && validUntil != null && validUntil.isAfter(LocalDateTime.now());
                                                    %>
                                                        <% if (isVipActive) { %>
                                                            <div class="alert alert-success p-2 mb-2 text-center">
                                                                <i class="bi bi-star-fill text-warning"></i> <strong>Thành viên VIP</strong>
                                                            </div>
                                                            <div class="small text-muted text-center mb-3">
                                                                Hiệu lực đến: <%= validUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) %>
                                                            </div>
                                                            <% if (validUntil.isBefore(LocalDateTime.now().plusDays(3))) { %>
                                                                <a href="<%= ctx %>/payment?action=method&type=membership" class="btn btn-warning w-100 fw-bold">
                                                                    <i class="bi bi-gem"></i> Gia hạn gói VIP (199k/30 ngày)
                                                                </a>
                                                            <% } %>
                                                        <% } else { %>
                                                            <div class="alert alert-secondary p-2 mb-2 text-center">
                                                                Thành viên thường
                                                            </div>
                                                            <a href="<%= ctx %>/payment?action=method&type=membership" class="btn btn-warning w-100 fw-bold">
                                                                <i class="bi bi-gem"></i> Đăng ký gói VIP (199k/30 ngày)
                                                            </a>
                                                        <% } %>
                                                    <% } %>
                                                </div>
                                            </div>
                                        </aside>

                                        <section class="col-lg-8">
                                            <div class="card soft-card p-4 p-lg-5">
                                                <h2 class="section-title mb-4">Thông tin cá nhân</h2>

                                                <% if (success !=null && !success.isEmpty()) { %>
                                                    <div class="alert alert-success alert-dismissible fade show"
                                                        role="alert">
                                                        <i class="bi bi-check-circle-fill me-2"></i>
                                                        <%= success %>
                                                            <button type="button" class="btn-close"
                                                                data-bs-dismiss="alert" aria-label="Close"></button>
                                                    </div>
                                                    <% } %>

                                                        <% if (error !=null && !error.isEmpty()) { %>
                                                            <div class="alert alert-danger alert-dismissible fade show"
                                                                role="alert">
                                                                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                                                                <%= error %>
                                                                    <button type="button" class="btn-close"
                                                                        data-bs-dismiss="alert"
                                                                        aria-label="Close"></button>
                                                            </div>
                                                            <% } %>

                                                                <form action="<%= ctx %>/profile" method="post"
                                                                    class="needs-validation" novalidate>
                                                                    <div class="row g-3">
                                                                        <div class="col-md-6">
                                                                            <label class="form-label fw-semibold"
                                                                                for="fullName">Họ tên <span
                                                                                    class="text-danger">*</span></label>
                                                                            <input id="fullName" name="fullName"
                                                                                class="form-control <%= fullNameClass %>"
                                                                                placeholder="Nguyễn Văn A"
                                                                                value="<%= currentUser.getFullName() %>"
                                                                                required>
                                                                            <div class="invalid-feedback">
                                                                                <%= errors.getOrDefault("fullName", "Họ tên không hợp lệ."
                                                                                    ) %>
                                                                            </div>
                                                                        </div>

                                                                        <div class="col-md-6">
                                                                            <label class="form-label fw-semibold"
                                                                                for="phone">Số điện thoại <span
                                                                                    class="text-danger">*</span></label>
                                                                            <input id="phone" name="phone"
                                                                                class="form-control <%= phoneClass %>"
                                                                                placeholder="0901234567"
                                                                                value="<%= currentUser.getPhone() %>"
                                                                                required>
                                                                            <div class="invalid-feedback">
                                                                                <%= errors.getOrDefault("phone", "Số điện thoại không hợp lệ."
                                                                                    ) %>
                                                                            </div>
                                                                        </div>

                                                                        <div class="col-12">
                                                                            <label class="form-label fw-semibold"
                                                                                for="email">Email <span
                                                                                    class="text-danger">*</span></label>
                                                                            <input id="email" name="email" type="email"
                                                                                class="form-control <%= emailClass %>"
                                                                                placeholder="name@example.com"
                                                                                value="<%= currentUser.getEmail() %>"
                                                                                required>
                                                                            <div class="invalid-feedback">
                                                                                <%= errors.getOrDefault("email", "Email không hợp lệ."
                                                                                    ) %>
                                                                            </div>
                                                                        </div>

                                                                        <div class="col-12">
                                                                            <label class="form-label fw-semibold"
                                                                                for="password">Mật khẩu mới (Để trống
                                                                                nếu không muốn đổi)</label>
                                                                            <div class="password-toggle-wrap">
                                                                                <input id="password" name="password"
                                                                                    type="password"
                                                                                    class="form-control <%= passwordClass %>"
                                                                                    placeholder="••••••••">
                                                                                <button type="button"
                                                                                    class="password-toggle-btn"
                                                                                    aria-label="Hiện mật khẩu"
                                                                                    aria-pressed="false">
                                                                                    <i class="bi bi-eye"></i>
                                                                                </button>
                                                                            </div>
                                                                            <div class="form-text">Ít nhất 6 ký tự, gồm
                                                                                cả chữ cái và chữ số.</div>
                                                                            <div class="invalid-feedback">
                                                                                <%= errors.getOrDefault("password", "Mật khẩu mới không hợp lệ."
                                                                                    ) %>
                                                                            </div>
                                                                        </div>
                                                                    </div>

                                                                    <div class="d-flex justify-content-end mt-4">
                                                                        <button type="submit"
                                                                            class="btn btn-sf-primary btn-lg px-4">Lưu
                                                                            thay đổi</button>
                                                                    </div>
                                                                </form>
                                            </div>
                                        </section>
                                    </div>
                                </div>
                            </main>

                            <div id="footer" data-root="<%= ctx %>/"></div>

                            <script
                                src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
                            <script src="<%= ctx %>/assets/js/app.js"></script>
                            <script src="<%= ctx %>/assets/js/password-toggle.js"></script>
                        </body>

                        </html>