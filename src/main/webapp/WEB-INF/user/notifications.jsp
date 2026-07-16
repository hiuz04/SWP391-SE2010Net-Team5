<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="com.swp.model.Notification" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    private String esc(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String notificationLink(String ctx, Notification notif) {
        if (notif == null || notif.getReferenceId() == null) return "#";
        String type = notif.getNotificationType();
        Long ref = notif.getReferenceId();
        if ("CHECKOUT_PAYMENT".equals(type) || "CHECKOUT_PAYMENT_SUCCESS".equals(type)) {
            return ctx + "/customer/checkout-invoice?id=" + ref;
        }
        if ("BOOKING".equals(type) || "REMINDER".equals(type)) {
            return ctx + "/booking?action=detail&id=" + ref;
        }
        return "#";
    }
%>
<%
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();

    List<Notification> notifications = (List<Notification>) request.getAttribute("notifications");
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
</head>
<body class="bg-light">
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= esc(displayName) %>" data-active=""></div>

<main class="py-5">
    <div class="container" style="max-width: 800px;">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h2 class="fw-bold mb-0">Thông báo của bạn</h2>
            <% if (notifications != null && !notifications.isEmpty()) { %>
            <form action="<%= ctx %>/notifications" method="POST" class="m-0">
                <input type="hidden" name="action" value="mark_all">
                <button type="submit" class="btn btn-outline-success btn-sm"><i class="bi bi-check2-all me-1"></i>Đánh dấu tất cả đã đọc</button>
            </form>
            <% } %>
        </div>

        <div class="card border-0 shadow-sm rounded-4">
            <div class="card-body p-0">
                <ul class="list-group list-group-flush rounded-4">
                    <% if (notifications != null && !notifications.isEmpty()) {
                        for (Notification notif : notifications) {
                            String bgClass = Boolean.TRUE.equals(notif.getIsRead()) ? "bg-white" : "bg-light";
                            String href = notificationLink(ctx, notif);
                            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");
                            String createdText = notif.getCreatedAt() != null ? notif.getCreatedAt().format(formatter) : "";
                    %>
                    <li class="list-group-item p-0 <%= bgClass %>" id="notif-<%= notif.getNotificationId() %>">
                        <a href="<%= esc(href) %>" class="d-block p-3 text-decoration-none text-dark"
                           onclick="openNotification(event, <%= notif.getNotificationId() %>, '<%= esc(href) %>', '<%= esc(notif.getTitle()).replace("'", "\\'").replace("\n", "\\n") %>', '<%= esc(notif.getMessage()).replace("'", "\\'").replace("\n", "\\n") %>')">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <h6 class="mb-1 fw-bold <%= Boolean.TRUE.equals(notif.getIsRead()) ? "text-secondary" : "text-dark" %>">
                                    <% if (!Boolean.TRUE.equals(notif.getIsRead())) { %><span class="text-danger me-1">●</span><% } %>
                                    <%= esc(notif.getTitle()) %>
                                </h6>
                                <small class="text-muted text-nowrap"><%= esc(createdText) %></small>
                            </div>
                            <p class="mb-1 mt-2 <%= Boolean.TRUE.equals(notif.getIsRead()) ? "text-muted" : "text-dark" %>" style="font-size: 0.95rem;">
                                <%= esc(notif.getMessage()) %>
                            </p>
                        </a>
                    </li>
                    <% } } else { %>
                    <li class="list-group-item p-5 text-center text-muted">
                        <i class="bi bi-bell-slash fs-1 text-light mb-3 d-block" style="font-size: 3rem;"></i>
                        Bạn không có thông báo nào.
                    </li>
                    <% } %>
                </ul>
            </div>
        </div>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
  function openNotification(event, id, href, title, message) {
    event.preventDefault();
    fetch('<%= ctx %>/api/notifications', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'action=mark_read&id=' + id
    }).then(() => {
      document.getElementById('notif-' + id).classList.remove('bg-light');
      if (href && href !== '#') {
        window.location.href = href;
      } else if (title && message) {
        if (typeof window.showNotificationDetail === 'function') {
            window.showNotificationDetail(title, message);
        }
      }
    });
  }
</script>
</body>
</html>
