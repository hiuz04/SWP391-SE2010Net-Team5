# ⚽ Football Field Booking (Nhóm 5 - SE2010-NET)
Football Field Booking là hệ thống web quản lý và đặt sân bóng đá hiện đại, bảo mật và tối ưu cho cả người dùng lẫn chủ sân.
Dự án sử dụng Java Servlet/JSP, tích hợp Google OAuth, gửi email tự động và quản lý dữ liệu chuyên nghiệp.
---
# 🎯 Mục tiêu dự án
* Chuyển đổi toàn bộ quy trình đặt sân, check-in, gọi dịch vụ và quyết toán từ thủ công sang nền tảng số tự động.
* Hỗ trợ chủ sân lấp đầy các khung giờ trống bằng các chương trình khuyến mãi hoặc điều chỉnh giá linh hoạt theo khung giờ (giờ vàng/giờ thấp điểm).
* Xây dựng kiến trúc hệ thống vững chắc, xử lý mượt mà hàng trăm yêu cầu đặt sân cùng lúc mà không xảy ra hiện tượng trùng lịch.
---
# ✨ Tính năng nổi bật
## 🔍 Dành cho Khách hàng & Khách vãng lai
* Tra cứu sân bóng theo khu vực địa lý, loại sân (sân 5, sân 7, sân 11) và khoảng thời gian trống mong muốn.
* Người chơi có thể đặt lịch đá một trận đơn lẻ hoặc đăng ký đặt lịch cố định theo tuần/tháng dành cho các câu lạc bộ duy trì sinh hoạt đều đặn.
* Tích hợp đa dạng cổng thanh toán hiện đại như **VNPay, MoMo, VietQR** hoặc lựa chọn trả tiền mặt trực tiếp tại quầy.

## 🏟️ Dành cho Chủ sân & Nhân viên
* Cấu hình danh sách sân, sơ đồ sân, quản lý trạng thái bảo trì và thiết lập bảng giá động theo khung giờ/ngày lễ.
* Nhân viên tại quầy thực hiện check-in cho khách khi đến sân và in hóa đơn/quyết toán tự động.
* Biểu đồ trực quan giúp chủ sân theo dõi doanh thu theo ngày/tuần/tháng, tỷ lệ lấp đầy sân.

## 🛡️ Dành cho Quản trị viên hệ thống
* Quản lý tài khoản người dùng, phê duyệt thông tin các chủ sân mới gia nhập hệ thống, kiểm soát luồng tiền và xử lý khiếu nại nếu có.
* Đảm bảo tính bảo mật và toàn vẹn dữ liệu giữa các phân hệ: Admin, Chủ sân, Nhân viên, Khách hàng.
---
# 🛠️ Công Nghệ Sử Dụng
| Thành phần | Công nghệ |
| :--- | :------- |
| Backend | Java Servlet |
| Frontend | HTML5, CSS3, Bootstrap 5, Javascript |
| CSDL | SQL Server 20 |
| Server | Apache Tomcat 10.1 |
| Payment | VNPay, MoMo, VietQR |
---
# 💡 Các module tích hợp
* Đăng nhập/đăng ký + xác minh email.
* Quản lý đặt sân.
* Quản lý thanh toán, thống kê doanh thu.
* Quản lý bài viết tìm đối.
* Quản lý người dùng (Admin, Owner, Staff, Customer).
