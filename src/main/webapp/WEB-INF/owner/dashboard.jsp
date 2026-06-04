<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
%>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
    <title>Owner Dashboard</title>
</head>
<body>
    <div>Owner Dashboard</div>
    <button onclick="window.location.href='${pageContext.request.contextPath}/owner/field-list'">Xem danh sách sân</button>
</body>
</html>
