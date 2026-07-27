<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.Notification" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Admin";
    
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    String msgSuccess = (String) session.getAttribute("msgSuccess");
    String msgError = (String) session.getAttribute("msgError");
    session.removeAttribute("msgSuccess");
    session.removeAttribute("msgError");
%>
<%!
    // Helper method to escape string for JS
    private String escapeForJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("'", "\\'")
                  .replace("\"", "\\\"")
                  .replace("\r", "\\r")
                  .replace("\n", "\\n");
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
    <title>Quản lý thông báo | Admin | Sport Field Booking</title>
</head>
<body class="bg-light">
    <div id="navbar" data-root="<%= ctx %>/" data-role="admin" data-name="<%= currentName %>" data-active="Thông báo"></div>

    <main class="dashboard-shell py-4">
        <div class="container">
            <div class="d-flex justify-content-between align-items-center mb-4">
                <div>
                    <h1 class="section-title mb-1">Quản lý thông báo</h1>
                    <p class="text-muted">Xem và xóa các thông báo hệ thống đã gửi.</p>
                </div>
            </div>

            <% if (msgSuccess != null) { %>
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <i class="bi bi-check-circle me-2"></i> <%= msgSuccess %>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            <% } %>
            <% if (msgError != null) { %>
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <i class="bi bi-exclamation-triangle me-2"></i> <%= msgError %>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            <% } %>

            <div class="card soft-card border-0 shadow-sm">
                <div class="card-body">
                    <div class="table-responsive">
                        <table class="table align-middle">
                            <thead class="table-light">
                                <tr>
                                    <th>Thời gian (mới nhất)</th>
                                    <th>Tiêu đề</th>
                                    <th>Nội dung</th>
                                    <th>Loại</th>
                                    <th class="text-end">Hành động</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% if (notifications != null && !notifications.isEmpty()) { 
                                    for (Notification n : notifications) { 
                                        String time = n.getCreatedAt() != null ? n.getCreatedAt().format(dtf) : "";
                                %>
                                    <tr>
                                        <td><%= time %></td>
                                        <td class="fw-bold"><%= n.getTitle() %></td>
                                        <td><%= n.getMessage() %></td>
                                        <td>
                                            <span class="badge bg-secondary"><%= n.getNotificationType() %></span>
                                        </td>
                                        <td class="text-end">
                                            <form action="<%= ctx %>/admin/notifications" method="post" class="d-flex justify-content-end mb-0" onsubmit="return confirm('Bạn có chắc chắn muốn xóa tất cả thông báo này không? Các user khác cũng sẽ bị xóa thông báo này khỏi danh sách.');">
                                                <input type="hidden" name="action" value="delete">
                                                <input type="hidden" name="title" value="<%= n.getTitle() %>">
                                                <input type="hidden" name="message" value="<%= n.getMessage() %>">
                                                <input type="hidden" name="type" value="<%= n.getNotificationType() %>">
                                                <% if ("BOOKING".equals(n.getNotificationType()) && n.getReferenceId() != null) { %>
                                                <a href="<%= ctx %>/admin/booking-detail?id=<%= n.getReferenceId() %>" class="btn btn-sm btn-outline-info me-1">
                                                    <i class="bi bi-eye"></i> Xem
                                                </a>
                                                <% } else { %>
                                                <button type="button" class="btn btn-sm btn-outline-info me-1" onclick="showNotificationDetail('<%= escapeForJs(n.getTitle()) %>', '<%= escapeForJs(n.getMessage()) %>')">
                                                    <i class="bi bi-eye"></i> Xem
                                                </button>
                                                <% } %>
                                                <button type="submit" class="btn btn-sm btn-outline-danger">
                                                    <i class="bi bi-trash"></i> Xóa
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                <%  } 
                                } else { %>
                                    <tr>
                                        <td colspan="5" class="text-center text-muted py-4">Chưa có thông báo nào được gửi.</td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <div id="footer" data-root="<%= ctx %>/"></div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>
