<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.Notification" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%
    String ctx = request.getContextPath();
    User currentUser = (User) session.getAttribute("user");
    String currentName = currentUser != null && currentUser.getFullName() != null ? currentUser.getFullName() : "Người dùng";
    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Thông báo của bạn | Sport Field Booking</title>
    <style>
        .notification-item {
            transition: all 0.2s ease;
            border-left: 4px solid transparent;
        }
        .notification-item:hover {
            background-color: #f8f9fa;
        }
        .notification-item.unread {
            background-color: #f0f7ff;
            border-left-color: #0d6efd;
        }
        .notification-icon {
            width: 48px;
            height: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            font-size: 1.5rem;
        }
        .icon-BOOKING { background-color: #e8f5e9; color: #198754; }
        .icon-MATCHING { background-color: #fff3cd; color: #ffc107; }
        .icon-SYSTEM { background-color: #e2e3e5; color: #6c757d; }
    </style>
</head>
<body class="bg-light">
    <!-- Component Navbar sẽ được load qua app.js -->
    <div id="navbar" data-root="<%= ctx %>/" data-role="<%= currentUser != null && currentUser.getRoleName() != null ? currentUser.getRoleName().toLowerCase() : "customer" %>" data-name="<%= currentName %>" data-active="Notifications"></div>

    <main class="container py-4">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card border-0 shadow-sm">
                    <div class="card-header bg-white border-0 pt-4 pb-0 d-flex justify-content-between align-items-center">
                        <h4 class="fw-bold m-0"><i class="bi bi-bell text-primary me-2"></i>Thông báo của bạn</h4>
                        <button id="btnMarkAllRead" class="btn btn-sm btn-outline-secondary">Đánh dấu tất cả đã đọc</button>
                    </div>
                    <div class="card-body p-0 mt-3">
                        <div class="list-group list-group-flush" id="notificationList">
                            <% if (notifications != null && !notifications.isEmpty()) { %>
                                <% for (Notification n : notifications) { 
                                    String iconClass = "bi-bell";
                                    String bgClass = "icon-SYSTEM";
                                    if ("BOOKING".equals(n.getNotificationType())) {
                                        iconClass = "bi-calendar-check";
                                        bgClass = "icon-BOOKING";
                                    } else if ("MATCHING".equals(n.getNotificationType())) {
                                        iconClass = "bi-people";
                                        bgClass = "icon-MATCHING";
                                    }
                                %>
                                <div class="list-group-item notification-item <%= (n.getIsRead() != null && !n.getIsRead()) ? "unread" : "" %> p-4" data-id="<%= n.getNotificationId() %>">
                                    <div class="d-flex align-items-start">
                                        <div class="notification-icon <%= bgClass %> flex-shrink-0 me-3">
                                            <i class="bi <%= iconClass %>"></i>
                                        </div>
                                        <div class="flex-grow-1">
                                            <div class="d-flex justify-content-between align-items-start">
                                                <h6 class="fw-bold mb-1 <%= (n.getIsRead() != null && !n.getIsRead()) ? "text-dark" : "text-muted" %>"><%= n.getTitle() %></h6>
                                                <small class="text-muted text-nowrap ms-2"><%= n.getCreatedAt().format(formatter) %></small>
                                            </div>
                                            <p class="mb-2 text-secondary"><%= n.getMessage() %></p>
                                            <% if (n.getIsRead() != null && !n.getIsRead()) { %>
                                                <button class="btn btn-sm btn-link text-decoration-none p-0 mark-read-btn" data-id="<%= n.getNotificationId() %>">Đánh dấu đã đọc</button>
                                            <% } %>
                                        </div>
                                    </div>
                                </div>
                                <% } %>
                            <% } else { %>
                                <div class="p-5 text-center text-muted">
                                    <i class="bi bi-bell-slash display-1 text-light mb-3"></i>
                                    <h5>Bạn chưa có thông báo nào</h5>
                                    <p>Các thông báo mới từ hệ thống sẽ xuất hiện ở đây.</p>
                                </div>
                            <% } %>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Component Footer -->
    <div id="footer" data-root="<%= ctx %>/"></div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="<%= ctx %>/assets/js/app.js"></script>
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // Xử lý đánh dấu 1 thông báo đã đọc
            const readBtns = document.querySelectorAll('.mark-read-btn');
            readBtns.forEach(btn => {
                btn.addEventListener('click', function() {
                    const notifId = this.getAttribute('data-id');
                    const item = this.closest('.notification-item');
                    
                    fetch('<%= ctx %>/api/notifications/mark-read', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded',
                        },
                        body: 'id=' + notifId
                    })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            item.classList.remove('unread');
                            this.remove();
                            item.querySelector('h6').classList.remove('text-dark');
                            item.querySelector('h6').classList.add('text-muted');
                            // Cập nhật lại số đếm ở navbar nếu có hàm updateNotificationCount trong app.js
                            if(typeof updateNotificationCount === 'function') {
                                updateNotificationCount();
                            }
                        }
                    });
                });
            });

            // Xử lý đánh dấu tất cả đã đọc
            const btnMarkAll = document.getElementById('btnMarkAllRead');
            if (btnMarkAll) {
                btnMarkAll.addEventListener('click', function() {
                    fetch('<%= ctx %>/api/notifications/mark-all-read', {
                        method: 'POST'
                    })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            document.querySelectorAll('.notification-item.unread').forEach(item => {
                                item.classList.remove('unread');
                                const btn = item.querySelector('.mark-read-btn');
                                if (btn) btn.remove();
                                item.querySelector('h6').classList.remove('text-dark');
                                item.querySelector('h6').classList.add('text-muted');
                            });
                            if(typeof updateNotificationCount === 'function') {
                                updateNotificationCount();
                            }
                        }
                    });
                });
            }
        });
    </script>
</body>
</html>
