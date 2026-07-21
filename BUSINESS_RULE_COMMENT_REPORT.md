# 1. Tổng quan

- Tổng số file đã kiểm tra trong phạm vi yêu cầu: 180 file Java/JSP/JavaScript.
- Tổng số file đã thêm comment: 20 file source code, cộng file báo cáo này.
- Tổng số method đã thêm Javadoc/comment: 12 method.
- Tổng số dòng comment logic/Business Rule đã thêm: 225 dòng.
- Tổng số dòng comment Business Rule đã thêm: 144 dòng.
- Tổng số Business Rule tìm thấy và gắn đúng vị trí code thực thi: 28/29 rule.
- Business Rule chưa gắn comment vì code hiện tại chưa thực thi đúng rule: BR-16.

# 2. Business Rule Mapping

| Business Rule | Trạng thái | File | Class/Method | Mô tả vị trí thực thi |
|---------------|------------|------|--------------|-----------------------|
| BR-01 | Đã có code thực thi | `AuthFilter.java`, `CustomerAuthFilter.java`, `BookingController.java`, `PaymentController.java`, `VoucherAPIController.java`, `VoucherController.java`, `MatchmakingAPIController.java` | `doFilter`, `requireLogin`, `doPost`, `doGet` | Chặn hoặc khôi phục đăng nhập trước khi Customer tạo booking, thanh toán, dùng voucher, xem lịch sử và tạo/phản hồi bài tìm đối. |
| BR-02 | Đã có code thực thi | `BookingController.java`, `create-booking.jsp` | `validateBookingTime`, `isOnThirtyMinuteBlock`, fallback slot grid | Lưới đặt sân dùng slot 30 phút, start/end time được gửi sang backend để tính thời lượng. |
| BR-03 | Đã có code thực thi | `BookingController.java`, `BookingDAO.java`, `create-booking.jsp` | `buildConfirmationContext`, `isFieldAvailable`, `createBookingHold`, `createRecurringBookingHolds` | Kiểm tra sân AVAILABLE, không bảo trì và không overlap trước khi preview và trong transaction tạo HOLD. |
| BR-04 | Đã có code thực thi | `BookingController.java`, `BookingDAO.java` | `buildConfirmationContext`, `createBookingHold`, `createRecurringBookingHolds` | Tạo booking HOLD với `hold_expires_at = now + 15 minutes`. |
| BR-05 | Đã có code thực thi | `BookingDAO.java`, `PaymentDAO.java` | `cancelExpiredHolds`, `createPendingDepositPayment` | HOLD hết hạn bị chuyển CANCELLED nếu chưa có payment SUCCESS; payment mới bị chặn khi hold hết hạn. |
| BR-06 | Đã có code thực thi | `BookingController.java`, `PaymentDAO.java` | `calculateDepositAmount`, `createPendingDepositPayment` | Booking thường tính cọc 30%; booking định kỳ dùng full payment theo flow hiện tại. |
| BR-07 | Đã có code thực thi | `BookingController.java`, `BookingDAO.java` | `applyCancellationRule`, `cancelBooking` | Customer chỉ được hủy khi trạng thái và hạn hủy còn hợp lệ. |
| BR-08 | Đã có code thực thi | `PaymentController.java`, `PaymentDAO.java` | `processSimulatedPayment`, `markPaymentSuccessAndConfirmBooking` | Payment success cho booking HOLD chuyển booking sang CONFIRMED. |
| BR-09 | Đã có code thực thi | `VoucherDAO.java`, `BookingController.java`, `PaymentDAO.java` | `validateVoucher`, `buildConfirmationContext`, `createPendingDepositPayment` | Voucher phải tồn tại, ACTIVE, trong hạn, còn lượt, đạt min order và chưa dùng bởi Customer. |
| BR-10 | Đã có code thực thi | `BookingController.java`, `VoucherDAO.java` | `applyVoucher`, `createBookingAmounts`, `validateVoucher` | Mỗi booking chỉ nhận một voucher; discount bị giới hạn để final amount không âm. |
| BR-11 | Đã có code thực thi | `PaymentDAO.java`, `VoucherDAO.java` | `recordVoucherUsage`, `recordUsage`, `incrementUsed` | Chỉ sau payment SUCCESS mới ghi voucher usage và tăng `used`. |
| BR-12 | Đã có code thực thi | `StaffActionServlet.java`, `StaffBillingServlet.java`, `StaffBillingDAO.java` | `ensureActiveShift`, `handleCheckin`, `completeCheckout`, `hasActiveShiftForComplex` | Staff chỉ check-in/checkout tại complex có ca trực đang hoạt động; Owner không bị giới hạn theo staff shift. |
| BR-13 | Đã có code thực thi | `StaffActionServlet.java`, `StaffDashboardDAO.java` | `handleCheckin`, `checkinBooking` | Chỉ booking CONFIRMED được chuyển CHECKED_IN và ghi check-in log. |
| BR-14 | Đã có code thực thi | `StaffBillingDAO.java` | `cancelLateNoShowBooking` | Booking CONFIRMED có thể bị hủy no-show sau 30 phút từ giờ bắt đầu nếu chưa check-in. |
| BR-15 | Đã có code thực thi | `StaffBillingServlet.java`, `StaffBillingDAO.java` | `showCheckout`, `completeCheckout` | Chỉ booking CHECKED_IN được checkout. |
| BR-16 | Mâu thuẫn với SRS | `StaffBillingServlet.java`, `StaffBillingDAO.java` | `showCheckout`, `completeCheckout` | SRS yêu cầu chỉ checkout khi current time >= booking end time, nhưng backend hiện chưa chặn checkout sớm. |
| BR-17 | Đã có code thực thi | `StaffBillingDAO.java` | `calculateOvertimeMinutes`, `completeCheckout`, `enrichCheckoutAmounts` | Phí quá giờ được tính theo số phút sau giờ kết thúc booking. |
| BR-18 | Đã có code thực thi | `StaffBillingDAO.java` | `completeCheckout`, `enrichCheckoutAmounts`, `maxZero` | Số tiền còn lại = max(field fee + overtime fee - deposit, 0). |
| BR-19 | Đã có code thực thi | `StaffBillingDAO.java` | `completeCheckout` | Nếu còn phải trả bằng 0 thì invoice PAID và booking COMPLETED ngay. |
| BR-20 | Đã có code thực thi | `StaffBillingDAO.java`, `PaymentDAO.java`, `PaymentController.java` | `completeCheckout`, `createPendingCheckoutPayment`, `showCheckoutPaymentMethod` | Nếu còn tiền thì invoice PENDING, booking PENDING_CHECKOUT_PAYMENT và Customer phải thanh toán checkout. |
| BR-21 | Đã có code thực thi | `PaymentDAO.java`, `PaymentController.java` | `markCheckoutPaymentSuccess`, `processCheckoutSimulatedPayment` | Checkout payment thành công cập nhật payment SUCCESS, invoice PAID, booking COMPLETED và giải phóng sân. |
| BR-22 | Đã có code thực thi | `VNPayUtil.java`, `PaymentController.java`, `PaymentDAO.java` | `verifySignatureDebug`, `handleVNPayReturn`, `handleVNPayIpn`, `findPaymentByTransactionRef` | Callback VNPay kiểm chữ ký/hash, amount và trạng thái payment/booking/invoice trước khi cập nhật. |
| BR-23 | Đã có code thực thi | `PaymentDAO.java`, `PaymentController.java` | `markPaymentSuccessAndConfirmBooking`, `markPaymentFailed`, `createPendingDepositPayment` | Callback hoặc submit lặp được xử lý idempotent, không nhân đôi status change/voucher usage. |
| BR-24 | Đã có code thực thi | `BookingController.java`, `BookingDAO.java`, `StaffBillingDAO.java` | `setStatus`, `insertLog`, `updateBookingStatus`, `cancelExpiredHolds` | Flow dùng các trạng thái HOLD, CONFIRMED, CHECKED_IN, PENDING_CHECKOUT_PAYMENT, COMPLETED và CANCELLED. |
| BR-25 | Đã có code thực thi | `PaymentDAO.java` | `markPaymentSuccessAndConfirmBooking`, `markCheckoutPaymentSuccess`, `markPaymentFailed` | Payment dùng PENDING, SUCCESS và FAILED trong các update chính. |
| BR-26 | Mâu thuẫn với SRS | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | Code có kiểm tra độ dài 2-100 nhưng regex hiện cho phép dấu nháy/dấu chấm, trong khi SRS ghi chỉ chữ cái và khoảng trắng. |
| BR-27 | Có một phần | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | Code kiểm tra max 100 và định dạng email cơ bản; regex chưa bao phủ đầy đủ mọi định dạng email chuẩn quốc tế. |
| BR-28 | Mâu thuẫn với SRS | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | Profile chấp nhận 10-11 số bắt đầu 0, nhưng Register/frontend đang dùng regex số di động VN 10 số và prefix 03/05/07/08/09. |
| BR-29 | Đã có code thực thi | `index.jsp` | JSP render condition | Khối ưu đãi/VIP chỉ hiển thị khi `navRole` là guest hoặc customer. |

# 3. Các điểm mâu thuẫn giữa code và SRS

## BR-16

- File: `src/main/java/com/swp/controller/staff/StaffBillingServlet.java`, `src/main/java/com/swp/dao/StaffBillingDAO.java`.
- Class/method: `StaffBillingServlet.showCheckout`, `StaffBillingDAO.completeCheckout`.
- Logic hiện tại: backend chỉ kiểm tra booking đang `CHECKED_IN`, chưa kiểm tra `LocalDateTime.now()` đã bằng hoặc sau `booking.endTime()`.
- Yêu cầu trong SRS: checkout chỉ được phép khi thời gian hiện tại đã bằng hoặc vượt quá thời gian kết thúc booking.
- Đề xuất sửa: thêm validate `now.isBefore(booking.endTime())` ở `completeCheckout` và phản hồi lỗi ở `showCheckout`, nhưng không thực hiện trong nhiệm vụ comment này.

## BR-26

- File: `src/main/java/com/swp/util/RegisterValidator.java`, `src/main/java/com/swp/controller/auth/ProfileServlet.java`, `src/main/webapp/assets/js/register-validation.js`.
- Class/method: `RegisterValidator.validate`, `ProfileServlet.doPost`, frontend validate.
- Logic hiện tại: regex họ tên cho phép thêm dấu nháy `'` và dấu chấm `.` trong một số trường hợp.
- Yêu cầu trong SRS: họ tên chỉ chứa chữ cái và khoảng trắng, không có ký tự đặc biệt hoặc số.
- Đề xuất sửa: thống nhất regex họ tên theo chữ cái Unicode và khoảng trắng, nhưng không sửa trong nhiệm vụ comment này.

## BR-28

- File: `src/main/java/com/swp/util/RegisterValidator.java`, `src/main/java/com/swp/controller/auth/ProfileServlet.java`, `src/main/webapp/assets/js/register-validation.js`.
- Class/method: `RegisterValidator.validate`, `ProfileServlet.doPost`, frontend validate.
- Logic hiện tại: đăng ký/frontend yêu cầu số điện thoại dạng `^0[35789]\d{8}$` (10 số, prefix di động VN), còn profile dùng `^0\d{9,10}$`.
- Yêu cầu trong SRS: số điện thoại bắt đầu bằng 0 và dài 10 đến 11 chữ số.
- Đề xuất sửa: đồng bộ regex đăng ký/frontend/profile theo `^0\d{9,10}$`, nhưng không sửa trong nhiệm vụ comment này.

# 4. Các file đã chỉnh sửa

- `BUSINESS_RULE_COMMENT_REPORT.md`
- `src/main/java/com/swp/controller/api/MatchmakingAPIController.java`
- `src/main/java/com/swp/controller/auth/ProfileServlet.java`
- `src/main/java/com/swp/controller/customer/BookingController.java`
- `src/main/java/com/swp/controller/customer/PaymentController.java`
- `src/main/java/com/swp/controller/customer/VoucherAPIController.java`
- `src/main/java/com/swp/controller/customer/VoucherController.java`
- `src/main/java/com/swp/controller/staff/StaffActionServlet.java`
- `src/main/java/com/swp/controller/staff/StaffBillingServlet.java`
- `src/main/java/com/swp/dao/BookingDAO.java`
- `src/main/java/com/swp/dao/PaymentDAO.java`
- `src/main/java/com/swp/dao/StaffBillingDAO.java`
- `src/main/java/com/swp/dao/StaffDashboardDAO.java`
- `src/main/java/com/swp/dao/VoucherDAO.java`
- `src/main/java/com/swp/filter/AuthFilter.java`
- `src/main/java/com/swp/filter/CustomerAuthFilter.java`
- `src/main/java/com/swp/util/RegisterValidator.java`
- `src/main/java/com/swp/util/VNPayUtil.java`
- `src/main/webapp/WEB-INF/booking/create-booking.jsp`
- `src/main/webapp/assets/js/register-validation.js`
- `src/main/webapp/index.jsp`

# 5. Kết quả kiểm tra sau chỉnh sửa

- `git diff --check`: không phát hiện whitespace error; Git chỉ cảnh báo line ending LF sẽ được đổi thành CRLF khi Git chạm file.
- `mvn -DskipTests package`: không chạy được vì môi trường hiện tại không có `mvn` trong PATH (`CommandNotFoundException`).
- `mvn test`: không chạy được vì môi trường hiện tại không có `mvn` trong PATH (`CommandNotFoundException`).
- Fallback compile: đã chạy `javac -encoding UTF-8 --release 17` cho toàn bộ `src/main/java` với các dependency jar trong local Maven cache; kết quả pass, không phát sinh lỗi compile sau khi thêm comment.
