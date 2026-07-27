# 1. Tổng quan

- Tổng số file đã kiểm tra trong phạm vi yêu cầu: 180 file Java/JSP/JavaScript.
- Tổng số file đã thêm comment: 20 file source code, cộng file báo cáo này.
- Tổng số method đã thêm Javadoc/comment: 12 method.
- Tổng số dòng comment logic/Business Rule đã thêm: 225 dòng.
- Tổng số dòng comment Business Rule đã thêm: 144 dòng.
- Tổng số Business Rule trong `D:\SWP\Group5_SRS.docx` đã được gắn comment hoặc TODO mapping: 39/39 rule.
- BR-16 đã được đánh dấu TODO vì code hiện tại chưa chặn checkout sớm theo SRS.

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
| BR-26 | Mâu thuẫn với SRS | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | SRS yêu cầu họ tên 2-50 ký tự và chỉ chữ/khoảng trắng; code hiện kiểm tra 2-100 và pattern còn cho phép dấu nháy/dấu chấm. |
| BR-27 | Có một phần | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | SRS yêu cầu email đúng chuẩn quốc tế và tối đa 50 ký tự; code hiện kiểm tra max 100 và regex email cơ bản. |
| BR-28 | Mâu thuẫn với SRS | `RegisterValidator.java`, `ProfileServlet.java`, `register-validation.js` | `validate`, `doPost` | Profile chấp nhận 10-11 số bắt đầu 0, nhưng Register/frontend đang dùng regex số di động VN 10 số và prefix 03/05/07/08/09. |
| BR-29 | Đã có code thực thi | `index.jsp` | JSP render condition | Khối ưu đãi/VIP chỉ hiển thị khi `navRole` là guest hoặc customer. |
| BR-30 | Đã có code thực thi | `OwnerAuthFilter.java`, `VoucherManagementServlet.java` | `doFilter`, class-level servlet | Chỉ user đăng nhập với role OWNER được vào `/owner/vouchers` để xem, tạo, sửa, bật/tắt voucher. |
| BR-31 | Đã có code thực thi | `VoucherManagementServlet.java`, `VoucherDAO.java`, `form.jsp` | `parseVoucher`, `ensureUniqueCode`, `setVoucherStatement` | Code/name bắt buộc; code được trim/uppercase, UI giới hạn 50 ký tự và servlet kiểm tra unique trên toàn bộ voucher. |
| BR-32 | Có một phần | `VoucherManagementServlet.java`, `form.jsp` | `parseVoucher`, JSP input | Name bắt buộc và UI giới hạn 255 ký tự; servlet hiện chưa tự check max length ngoài form. |
| BR-33 | Đã có code thực thi | `VoucherManagementServlet.java`, `form.jsp` | `validateVoucher`, JSP input | Discount type chỉ PERCENT/FIXED, discount value > 0 và PERCENT không vượt 100. |
| BR-34 | Đã có code thực thi | `VoucherManagementServlet.java`, `form.jsp` | `parseVoucher`, `validateVoucher`, JSP input | Min order không âm và quantity phải là số nguyên dương. |
| BR-35 | Đã có code thực thi | `VoucherManagementServlet.java`, `form.jsp` | `validateVoucher`, JSP input | Start date không được sau end date. |
| BR-36 | Đã có code thực thi | `VoucherManagementServlet.java`, `VoucherDAO.java` | `doPost`, `createVoucher` | Voucher mới luôn có `used = 0`, không lấy used từ request. |
| BR-37 | Đã có code thực thi | `VoucherManagementServlet.java`, `VoucherDAO.java` | `doPost`, `validateEditableQuantity`, `updateVoucher` | Khi edit giữ nguyên used hiện tại và chặn quantity mới thấp hơn used. |
| BR-38 | Đã có code thực thi | `VoucherManagementServlet.java`, `VoucherDAO.java`, `form.jsp` | `validateVoucher`, `updateStatus`, `toggle-status` | Status chỉ ACTIVE/DISABLED; bật/tắt chỉ đổi status và updated_at, không đổi usage history. |
| BR-39 | Đã có code thực thi | `VoucherManagementServlet.java`, `VoucherDAO.java`, `list.jsp` | `toggle-status`, `updateStatus`, action buttons | Manage Voucher không xóa vĩnh viễn; Owner dùng DISABLED để dừng sử dụng và giữ lịch sử tham chiếu. |

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
- Yêu cầu trong SRS: họ tên dài 2-50 ký tự, chỉ chứa chữ cái và khoảng trắng, không có ký tự đặc biệt hoặc số.
- Đề xuất sửa: thống nhất giới hạn 2-50 và regex họ tên theo chữ cái Unicode/khoảng trắng, nhưng không thực hiện trong nhiệm vụ comment này.

## BR-27

- File: `src/main/java/com/swp/util/RegisterValidator.java`, `src/main/java/com/swp/controller/auth/ProfileServlet.java`, `src/main/webapp/assets/js/register-validation.js`.
- Class/method: `RegisterValidator.validate`, `ProfileServlet.doPost`, frontend validate.
- Logic hiện tại: register/profile/frontend đang kiểm tra email tối đa 100 ký tự và regex email cơ bản.
- Yêu cầu trong SRS: email đúng định dạng quốc tế và tối đa 50 ký tự.
- Đề xuất sửa: đồng bộ giới hạn email về 50 ký tự và dùng regex/validator phù hợp hơn, nhưng không thực hiện trong nhiệm vụ comment này.

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

# 6. Bổ sung theo yêu cầu "chỉ thêm, không xoá" - 2026-07-24 09:07 +07:00

## 6.1. Phạm vi bổ sung

- Không tìm thấy `SWP391-SE2010Net-Team5(8).zip` và `Group5_SRS(8).docx` trong workspace hoặc thư mục attachments hiện tại; phần BR-01 đến BR-29 tiếp tục dựa trên mapping đã có trong báo cáo này.
- Chỉ thêm comment/Javadoc tiếng Việt vào các file đã chỉnh sửa; không chủ động thay đổi điều kiện, SQL, trạng thái, công thức tính tiền, endpoint hoặc tên biến.
- Không sửa/xoá comment cũ dù có comment cũ bằng tiếng Anh hoặc comment còn chưa chuẩn chính tả, để tuân thủ yêu cầu chỉ thêm.

## 6.2. File được bổ sung comment trong lượt này

- `src/main/java/com/swp/controller/customer/BookingController.java`
- `src/main/java/com/swp/dao/PriceRuleDAO.java`
- `src/main/java/com/swp/dao/SystemSettingDAO.java`
- `src/main/java/com/swp/dao/UserDAO.java`
- `src/main/java/com/swp/dao/WorkShiftDAO.java`
- `src/main/java/com/swp/filter/AdminAuthFilter.java`
- `src/main/java/com/swp/filter/AuthFilter.java`
- `src/main/java/com/swp/filter/CustomerAuthFilter.java`
- `src/main/java/com/swp/filter/MaintenanceFilter.java`
- `src/main/java/com/swp/filter/OwnerAuthFilter.java`
- `src/main/java/com/swp/filter/StaffAuthFilter.java`
- `src/main/java/com/swp/service/CloudinaryService.java`
- `src/main/java/com/swp/service/FeedbackService.java`
- `src/main/java/com/swp/service/FieldService.java`
- `src/main/java/com/swp/service/FootballComplexService.java`
- `src/main/java/com/swp/service/GoogleOAuthService.java`
- `src/main/java/com/swp/service/OwnerDashboardService.java`
- `src/main/java/com/swp/service/VoucherUserService.java`
- `src/main/java/com/swp/util/AuthUtil.java`
- `src/main/java/com/swp/util/DBContext.java`
- `src/main/java/com/swp/util/LoginAttemptUtil.java`
- `src/main/java/com/swp/util/PasswordUtil.java`
- `src/main/java/com/swp/util/PriceCalculator.java`
- `src/main/java/com/swp/util/RecaptchaUtil.java`
- `src/main/java/com/swp/util/RememberMeUtil.java`
- `src/main/java/com/swp/util/VNPayConfig.java`
- `src/main/webapp/assets/js/app.js`
- `src/main/webapp/assets/js/customer/feedback.js`
- `src/main/webapp/assets/js/customer/field-detail.js`
- `src/main/webapp/assets/js/customer/matchmaking.js`
- `src/main/webapp/assets/js/customer/search.js`
- `src/main/webapp/assets/js/customer/voucher.js`
- `src/main/webapp/assets/js/owner/complex.js`
- `src/main/webapp/assets/js/owner/dashboard.js`
- `src/main/webapp/assets/js/owner/field.js`

## 6.3. Ghi chú về diff và kiểm tra

- `git diff --numstat`: ghi nhận 636 dòng thêm mới. Một số file JS vốn không có newline cuối file nên Git hiển thị marker `No newline at end of file` như một thay đổi dòng cuối; không có logic JS bị sửa.
- Trước khi bổ sung comment đã tồn tại một thay đổi whitespace trong `BookingController.java`; sau lượt này `git diff --check` vẫn báo `src/main/java/com/swp/controller/customer/BookingController.java:494: trailing whitespace`. Không tự xoá whitespace đó để giữ đúng yêu cầu chỉ thêm.
- `mvn -DskipTests package`: không chạy được vì `mvn` không có trong PATH (`CommandNotFoundException`).
- `mvn test`: không chạy được vì `mvn` không có trong PATH (`CommandNotFoundException`).
- Fallback `javac` trong sandbox:
  - Lần dùng classpath cũ thiếu các jar `jakarta.mail`, Cloudinary, OpenPDF, BCrypt nên không phản ánh lỗi do comment.
  - Lần dùng jar trong `target/WEB-INF/lib` thiếu `jakarta.servlet-api` vì servlet dependency là provided.
  - Lệnh compile ngoài sandbox để đọc thêm jar servlet trong local Maven cache đã bị hệ thống từ chối do giới hạn usage, nên chưa thể xác nhận full compile trong lượt bổ sung này.

# 7. Bổ sung theo `D:\SWP\Group5_SRS.docx` - 2026-07-27

## 7.1. Kết quả đối chiếu mới

- SRS mới có thêm BR-30 đến BR-39 cho Manage Voucher của Owner.
- Trước lượt bổ sung này, audit comment còn thiếu các ID: BR-16, BR-30, BR-31, BR-32, BR-33, BR-34, BR-35, BR-36, BR-37, BR-38, BR-39.
- Sau khi bổ sung, audit bằng regex `Business Rule BR-xx` trên `src/main/java` và `src/main/webapp` đã nhận diện đủ BR-01 đến BR-39.
- BR-16 được thêm dưới dạng TODO comment ở `StaffBillingServlet.showCheckout` và `StaffBillingDAO.completeCheckout`, vì code hiện tại chưa enforce `now >= booking.endTime()`.
- Các comment checkout bị gắn nhầm BR-26/BR-27 đã được sửa: CASH online không còn gắn BR-26; cash checkout gắn BR-21; online checkout request gắn BR-20.
- BR-26/BR-27/BR-28 được chỉnh comment để nêu rõ yêu cầu SRS mới và gap hiện tại, không thay đổi regex/logic validate.

## 7.2. File được bổ sung hoặc chỉnh comment trong lượt này

- `BUSINESS_RULE_COMMENT_REPORT.md`
- `src/main/java/com/swp/filter/OwnerAuthFilter.java`
- `src/main/java/com/swp/controller/owner/VoucherManagementServlet.java`
- `src/main/java/com/swp/dao/VoucherDAO.java`
- `src/main/webapp/WEB-INF/owner/vouchers/form.jsp`
- `src/main/webapp/WEB-INF/owner/vouchers/list.jsp`
- `src/main/java/com/swp/controller/staff/StaffBillingServlet.java`
- `src/main/java/com/swp/dao/StaffBillingDAO.java`
- `src/main/java/com/swp/controller/customer/PaymentController.java`
- `src/main/java/com/swp/util/RegisterValidator.java`
- `src/main/java/com/swp/controller/auth/ProfileServlet.java`
- `src/main/webapp/assets/js/register-validation.js`

## 7.3. Kiểm tra sau chỉnh sửa

- `git diff --check`: pass, chỉ có cảnh báo LF sẽ được đổi thành CRLF khi Git chạm file.
- `mvn -DskipTests package`: không chạy được vì `mvn` không có trong PATH (`CommandNotFoundException`).
- Fallback `javac` lần đầu thiếu dependency nên fail không đại diện.
- Fallback `javac` lần hai với classpath từ `target/SWP391-1.0-SNAPSHOT/WEB-INF/lib` cộng servlet/JSP API trong local Maven cache: pass.
