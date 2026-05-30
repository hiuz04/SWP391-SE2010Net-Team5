# Hướng dẫn kết nối SQL Server — FootballBookingSystem

## 1. Yêu cầu

- **SQL Server** đã cài và đang chạy (SQL Server Express hoặc Developer Edition).
- Database **`FootballBookingSystem`** đã tạo bằng script schema của team.
- **JDK 21**, **Maven**, **Tomcat 10+**.

## 2. Bật SQL Server và xác nhận database

1. Mở **SQL Server Management Studio (SSMS)**.
2. Kết nối tới instance (thường `localhost` hoặc `localhost\SQLEXPRESS`).
3. Chạy script tạo bảng (schema team đã có).
4. Kiểm tra database tồn tại:

```sql
USE FootballBookingSystem;
GO
SELECT name FROM sys.tables;
```

## 3. Cấu hình đăng nhập SQL Server

### Cách A: SQL Server Authentication (khuyên dùng khi dev)

1. SSMS → chuột phải server → **Properties** → **Security**.
2. Chọn **SQL Server and Windows Authentication mode**.
3. Restart service **SQL Server**.
4. **Security** → **Logins** → chuột phải `sa` → **Properties** → đặt mật khẩu, bật login.

### Cách B: Windows Authentication

Trong `db.properties` dùng integratedSecurity (cần thêm dependency `com.microsoft.sqlserver:mssql-jdbc_auth` trên Windows):

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=FootballBookingSystem;encrypt=true;trustServerCertificate=true;integratedSecurity=true
```

## 4. Cấu hình project Java

### Bước 1: Tạo file `db.properties`

```bash
copy src\main\resources\db.properties.example src\main\resources\db.properties
```

Chỉnh sửa theo máy bạn:

| Thuộc tính | Ví dụ | Ghi chú |
|------------|--------|---------|
| `db.url` | `jdbc:sqlserver://localhost:1433;databaseName=FootballBookingSystem;encrypt=true;trustServerCertificate=true` | Đổi port nếu khác 1433 |
| `db.username` | `sa` | Hoặc user SQL bạn tạo |
| `db.password` | `MatKhauCuaBan` | Mật khẩu SQL Server |

**Instance tên SQLEXPRESS:**

```properties
db.url=jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=FootballBookingSystem;encrypt=true;trustServerCertificate=true
```

### Bước 2: Reload Maven

Trong IntelliJ: chuột phải `pom.xml` → **Maven** → **Reload project**.

Hoặc terminal:

```bash
mvn clean package
```

## 5. Thêm dữ liệu test đăng nhập

```sql
USE FootballBookingSystem;
GO

INSERT INTO roles (role_name) VALUES ('ADMIN');
INSERT INTO roles (role_name) VALUES ('CUSTOMER');

INSERT INTO users (role_id, full_name, email, phone, password_hash, status)
VALUES (
    1,
    N'Quản trị viên',
    'admin@football.com',
    '0900000000',
    '123456',
    'ACTIVE'
);
```

> **Lưu ý:** Mật khẩu đang lưu dạng plain text chỉ để test. Production nên hash bằng BCrypt trước khi lưu vào `password_hash`.

Đăng nhập trên web:

- **Email:** `admin@football.com`
- **Password:** `123456`

## 6. Kiểm tra kết nối

1. Deploy/chạy ứng dụng trên Tomcat.
2. Mở: `http://localhost:8080/<tên-app>/db-test`
3. Nếu thấy **"Kết nối thành công"** → JDBC hoạt động.
4. Thử login tại `index.jsp`.

## 7. Cấu trúc code trong project

```
src/main/resources/
  db.properties          ← cấu hình (không commit Git)
  db.properties.example  ← mẫu cho team

src/main/java/com/swp/
  util/DBContext.java    ← mở Connection JDBC
  dao/UserDAO.java       ← truy vấn bảng users
  controller/
    DbTestServlet.java   ← /db-test
    LoginServlet.java    ← đăng nhập qua DB
```

## 8. Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|-----|-------------|------------|
| `Login failed for user 'sa'` | Sai user/password | Sửa `db.properties`, bật SQL Auth |
| `Cannot open database "FootballBookingSystem"` | DB chưa tạo | Chạy lại script CREATE DATABASE |
| `Connection refused` | SQL Server chưa chạy | Services → start **SQL Server** |
| `encrypt=true` / SSL | Certificate | Giữ `trustServerCertificate=true` khi dev |
| `ClassNotFoundException` SQLServerDriver | Thiếu dependency | `mvn clean install`, reload Maven |
| `Không tìm thấy db.properties` | Chưa copy file | Copy từ `.example` |

## 9. Mở rộng DAO cho các bảng khác

Ví dụ truy vấn `facilities`:

```java
String sql = "SELECT facility_id, facility_name, address FROM facilities WHERE status = 'ACTIVE'";
try (Connection conn = DBContext.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql);
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
        // map sang model Facility
    }
}
```

Luôn dùng `PreparedStatement` và đóng resource bằng try-with-resources.
