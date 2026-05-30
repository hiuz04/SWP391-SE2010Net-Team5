<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head>
    <title>Demo Servlet</title>
</head>
<body>

<h1>Demo Java Servlet</h1>

<hr>

<h2>Test GET Servlet</h2>

<a href="hello">Bấm vào đây để gọi HelloServlet</a>

<hr>

<h2>Test POST Servlet</h2>

<form action="login" method="post">
    <label>Email:</label>
    <input type="email" name="email" placeholder="Nhập email" required>

    <br><br>

    <label>Password:</label>
    <input type="password" name="password" placeholder="Nhập mật khẩu" required>

    <br><br>

    <button type="submit">Login</button>
</form>

<p><a href="db-test">Kiểm tra kết nối database</a></p>

<p>Dữ liệu đăng nhập lấy từ bảng <b>users</b> trong SQL Server (FootballBookingSystem).</p>

</body>
</html>