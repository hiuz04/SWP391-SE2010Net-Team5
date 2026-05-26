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
    <label>Username:</label>
    <input type="text" name="username" placeholder="Nhập username">

    <br><br>

    <label>Password:</label>
    <input type="password" name="password" placeholder="Nhập password">

    <br><br>

    <button type="submit">Login</button>
</form>

<p>Tài khoản test:</p>
<p>Username: <b>admin</b></p>
<p>Password: <b>123</b></p>

</body>
</html>