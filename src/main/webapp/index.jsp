<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.TopFieldSummary" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Collections" %>
<%@ page import="com.swp.dao.SystemSettingDAO" %>
<%@ page import="com.swp.model.SystemSetting" %>
<%@ page import="java.util.Optional" %>
<%
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();

    @SuppressWarnings("unchecked")
    List<TopFieldSummary> topFields = (List<TopFieldSummary>) request.getAttribute("topFields");
    if (topFields == null) topFields = Collections.emptyList();

    boolean isVipActive = false;
    boolean showVipRenew = false;
    if (sessionUser != null) {
        boolean isVip = sessionUser.isVip();
        java.time.LocalDateTime validUntil = sessionUser.getVipValidUntil();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        isVipActive = isVip && validUntil != null && validUntil.isAfter(now);
        if (isVipActive) {
            // Hiển thị nút gia hạn nếu hạn sử dụng <= 3 ngày (tính từ thời điểm hiện tại)
            showVipRenew = validUntil.isBefore(now.plusDays(3)) || validUntil.isEqual(now.plusDays(3));
        }
    }
    
    SystemSettingDAO sysDao = new SystemSettingDAO();
    Optional<SystemSetting> vipDiscountSetting = sysDao.getSettingByKey("VIP_DISCOUNT_PERCENTAGE");
    String vipDiscountStr = "5";
    if (vipDiscountSetting.isPresent() && vipDiscountSetting.get().getSettingValue() != null) {
        vipDiscountStr = vipDiscountSetting.get().getSettingValue().replaceAll("[^0-9.]", "");
    }

    Optional<SystemSetting> vipSetting = sysDao.getSettingByKey("VIP_SUBSCRIPTION_PRICE_MONTHLY");
    String vipPriceStr = "199.000";
    if (vipSetting.isPresent() && vipSetting.get().getSettingValue() != null) {
        String val = vipSetting.get().getSettingValue();
        try {
            long p = Long.parseLong(val.replaceAll("[^0-9]", ""));
            vipPriceStr = String.format("%,d", p).replace(',', '.');
        } catch(Exception e) {
            vipPriceStr = val;
        }
    }
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="https://unpkg.com/aos@2.3.1/dist/aos.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Trang chủ | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Trang chủ"></div>

<header class="hero">
    <div class="container" data-aos="fade-up" data-aos-duration="1000">
        <div class="row align-items-center g-5">
            <div class="col-lg-8">
                <span class="badge text-bg-light text-success mb-3 px-3 py-2 shadow-sm">Hệ thống đặt sân bóng online</span>
                <h1 class="display-4 fw-bold mb-3">Đặt sân bóng đá dễ dàng, nhanh chóng</h1>
                <p class="lead mb-4 text-white-50">Tìm sân, chọn khung giờ, thanh toán và quản lý lịch đặt chỉ trong vài thao tác.</p>
                <div class="card hero-card p-3 p-lg-4">
                    <form class="row g-3 align-items-end" action="<%= ctx %>/search" method="get">
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Tỉnh / Thành phố</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-geo-alt"></i></span>
                                <select class="form-select" name="province">
                                    <option value="" selected>Tất cả</option>
                                    <%
                                        @SuppressWarnings("unchecked")
                                        java.util.List<String> cities = (java.util.List<String>) request.getAttribute("cities");
                                        if (cities != null) {
                                            for (String c : cities) {
                                    %>
                                    <option value="<%= c %>"><%= c %></option>
                                    <%
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Phường / Xã</label>
                            <div class="input-group">
                                <span class="input-group-text"><i class="bi bi-geo-alt-fill"></i></span>
                                <select class="form-select" name="ward">
                                    <option value="" selected>Tất cả</option>
                                    <%
                                        @SuppressWarnings("unchecked")
                                        java.util.List<String> wards = (java.util.List<String>) request.getAttribute("wards");
                                        if (wards != null) {
                                            for (String w : wards) {
                                    %>
                                    <option value="<%= w %>"><%= w %></option>
                                    <%
                                            }
                                        }
                                    %>
                                </select>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label text-dark fw-semibold">Loại sân</label>
                            <select class="form-select" name="type">
                                <option value="" selected>Tất cả</option>
                                <option value="1">Sân 5</option>
                                <option value="2">Sân 7</option>
                                <option value="3">Sân 11</option>
                            </select>
                        </div>
                        <div class="col-md-3 d-grid">
                            <button type="submit" class="btn btn-sf-accent btn-lg">
                                <i class="bi bi-search me-1"></i>Tìm sân
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</header>

<main>
    <section class="py-5 bg-white">
        <div class="container">
            <div class="row g-4 text-center">
                <div class="col-md-4" data-aos="fade-up" data-aos-delay="100">
                    <a href="<%= ctx %>/search" class="text-decoration-none text-dark d-block h-100">
                        <div class="soft-card p-4 h-100 hover-scale">
                            <i class="bi bi-search display-5 text-sf-primary"></i>
                            <h5 class="mt-3">Tìm kiếm dễ dàng</h5>
                            <p class="text-muted mb-0">Lọc theo vị trí, loại sân, khung giờ và mức giá.</p>
                        </div>
                    </a>
                </div>
                <div class="col-md-4" data-aos="fade-up" data-aos-delay="200">
                    <a href="<%= ctx %>/booking?action=history" class="text-decoration-none text-dark d-block h-100">
                        <div class="soft-card p-4 h-100 hover-scale">
                            <i class="bi bi-clock-history display-5 text-sf-primary"></i>
                            <h5 class="mt-3">Lịch sử đặt sân</h5>
                            <p class="text-muted mb-0">Theo dõi, quản lý danh sách các sân bạn đã đặt.</p>
                        </div>
                    </a>
                </div>
                <div class="col-md-4" data-aos="fade-up" data-aos-delay="300">
                    <a href="<%= ctx %>/matchmaking" class="text-decoration-none text-dark d-block h-100">
                        <div class="soft-card p-4 h-100 hover-scale">
                            <i class="bi bi-people display-5 text-sf-primary"></i>
                            <h5 class="mt-3">Tìm đối</h5>
                            <p class="text-muted mb-0">Kết nối và giao lưu với các đội bóng khác.</p>
                        </div>
                    </a>
                </div>
            </div>
        </div>
    </section>

    <section class="py-5 bg-light overflow-hidden">
        <div class="container">
            <div class="row align-items-center g-5">
                <div class="col-lg-6" data-aos="fade-right">
                    <span class="badge text-bg-success mb-3 px-3 py-2">Dịch vụ chuẩn 5 sao</span>
                    <h2 class="fw-bold mb-4">Trải nghiệm tiện ích đẳng cấp</h2>
                    <p class="lead text-muted mb-4">Cơ sở vật chất hiện đại, mặt sân đạt chuẩn cùng hệ thống chiếu sáng chuyên nghiệp. Nâng tầm mọi trận đấu của bạn với những trải nghiệm tuyệt vời nhất.</p>
                    <ul class="list-unstyled mb-4">
                        <li class="mb-3"><i class="bi bi-check-circle-fill text-success me-2"></i> Mặt cỏ nhân tạo chất lượng FIFA</li>
                        <li class="mb-3"><i class="bi bi-check-circle-fill text-success me-2"></i> Hệ thống đèn chiếu sáng ban đêm chống chói</li>
                        <li class="mb-3"><i class="bi bi-check-circle-fill text-success me-2"></i> Khu vực nghỉ ngơi, phòng thay đồ tiện nghi, sạch sẽ</li>
                    </ul>
                    <a href="<%= ctx %>/search" class="btn btn-sf-accent btn-lg">Khám phá sân ngay <i class="bi bi-arrow-right ms-1"></i></a>
                </div>
                <div class="col-lg-6" data-aos="fade-left">
                    <div class="position-relative p-3">
                        <img src="https://res.cloudinary.com/du02dvkx7/image/upload/v1783528000/san-bong-da-chat-luong-dat-tieu-chuan_kqbdez.jpg" alt="Tiện ích sân bóng" class="img-fluid rounded-4 shadow-lg hover-scale w-100" style="object-fit: cover; height: 450px;">
                        
                        <!-- Floating badge -->
                        <div class="position-absolute bottom-0 start-0 ms-0 ms-md-4 mb-4 p-3 bg-white rounded-3 shadow-sm d-flex align-items-center gap-3 floating-badge border border-success border-opacity-25">
                            <div class="bg-success text-white rounded-circle d-flex align-items-center justify-content-center shadow-sm" style="width: 48px; height: 48px;">
                                <i class="bi bi-star-fill fs-5"></i>
                            </div>
                            <div>
                                <h6 class="mb-0 fw-bold">4.9/5 Điểm</h6>
                                <small class="text-muted">Đánh giá từ khách hàng</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="py-5">
        <div class="container">
            <div class="d-flex justify-content-between align-items-end mb-4">
                <div>
                    <h2 class="section-title">Sân bóng nổi bật</h2>
                    <p class="text-muted mb-0">Danh sách các sân nổi bật.</p>
                </div>
                <a href="<%= ctx %>/search" class="btn btn-outline-success">Xem thêm <i class="bi bi-arrow-right"></i></a>
            </div>
            <div class="row g-4">
                <% if (topFields.isEmpty()) { %>
                    <div class="col-12 text-center text-muted py-5" data-aos="fade-up">
                        <i class="bi bi-calendar-x display-4"></i>
                        <p class="mt-3">Chưa có dữ liệu sân nổi bật.</p>
                    </div>
                <% } else { %>
                    <% int delay = 0; for (TopFieldSummary field : topFields) { delay += 100; %>
                    <div class="col-md-6 col-xl-4" data-aos="zoom-in" data-aos-delay="<%= delay %>">
                        <div class="card soft-card h-100 overflow-hidden position-relative">
                            <% 
                                String imgUrl = field.getImageUrl();
                                if (imgUrl == null || imgUrl.isBlank()) {
                                    imgUrl = request.getContextPath() + "/assets/images/icon/default_field.png";
                                } else if (!imgUrl.startsWith("http")) {
                                    imgUrl = request.getContextPath() + (imgUrl.startsWith("/") ? "" : "/") + imgUrl;
                                }
                            %>
                            <% if (field.isHot()) { %>
                            <div class="position-absolute top-0 end-0 m-3 z-3">
                                <span class="badge badge-hot"><i class="bi bi-fire me-1"></i>HOT</span>
                            </div>
                            <% } %>
                            <img class="field-img" src="<%= imgUrl %>" alt="Sân bóng" onerror="this.src='https://images.unsplash.com/photo-1529900681758-f4b84c32ddda?w=900&q=80'">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <h5 class="card-title mb-0"><%= field.getFieldName() %></h5>
                                    <%
                                        String badgeClass = "AVAILABLE".equalsIgnoreCase(field.getStatus())
                                            ? "badge-soft-success" : "badge-soft-danger";
                                        String badgeText = "AVAILABLE".equalsIgnoreCase(field.getStatus())
                                            ? "Còn trống" : "Không khả dụng";
                                    %>
                                    <span class="badge <%= badgeClass %>"><%= badgeText %></span>
                                </div>
                                <p class="text-muted small mb-2">
                                    <i class="bi bi-building me-1"></i><%= field.getComplexName() != null ? field.getComplexName() : "" %>
                                </p>
                                <p class="text-muted small mb-2">
                                    <i class="bi bi-geo-alt me-1"></i><%= field.getFullLocation() %>
                                </p>
                                <div class="d-flex gap-3 small mb-3">
                                    <span><i class="bi bi-trophy-fill text-warning"></i> <%= field.getBookingCount() %> lượt đặt</span>
                                    <% if (field.getFieldTypeName() != null && !field.getFieldTypeName().isBlank()) { %>
                                        <span><%= field.getFieldTypeName() %></span>
                                    <% } %>
                                </div>
                                <% if (field.getCurrentPrice() != null) { %>
                                <p class="text-success fw-bold mb-3" style="font-size: 1.1rem;">
                                    <i class="bi bi-cash me-1"></i> Giá lúc này: <%= String.format("%,d", field.getCurrentPrice().longValue()) %>đ/giờ
                                    <br><small class="text-muted fw-normal" style="font-size: 0.8rem;">* Giá có thể thay đổi theo khung giờ đặt</small>
                                </p>
                                <% } else { %>
                                <p class="text-success fw-bold mb-3">
                                    <i class="bi bi-cash me-1"></i> Chưa có giá
                                </p>
                                <% } %>
                                <div class="d-flex justify-content-end">
                                    <a href="<%= ctx %>/booking?complexId=<%= field.getComplexId() %>" class="btn btn-sf-primary">Đặt ngay</a>
                                </div>
                            </div>
                        </div>
                    </div>
                    <% } %>
                <% } %>
            </div>
        </div>
    </section>

    <%-- Business Rule BR-29: Khối ưu đãi/VIP chỉ hiển thị cho khách vãng lai và Customer. --%>
    <% if ("guest".equalsIgnoreCase(navRole) || "customer".equalsIgnoreCase(navRole)) { %>
    <section class="py-5 bg-sf-primary text-white" data-aos="fade-up">
        <div class="container text-center">
            <div class="mb-3">
                <i class="bi bi-gem display-4 text-warning"></i>
            </div>
            <% if (isVipActive) { %>
            <h2 class="fw-bold">Bạn đã là Hội viên VIP</h2>
            <p class="lead text-white-50">Cảm ơn bạn đã đồng hành. Bạn đang được hưởng đặc quyền giảm <%= vipDiscountStr %>% cho mọi lần đặt sân.</p>
            <button type="button" class="btn btn-warning btn-lg fw-bold shadow-sm px-4 text-dark" data-bs-toggle="modal" data-bs-target="#vipModal">
                Xem lại Đặc quyền
            </button>
            <% } else { %>
            <h2 class="fw-bold">Đăng ký Hội viên VIP</h2>
            <p class="lead text-white-50">Nhận ngay đặc quyền ưu đãi khủng, giảm <%= vipDiscountStr %>% cho tất cả các lần đặt sân.</p>
            <% if (sessionUser == null) { %>
            <a class="btn btn-light btn-lg fw-bold shadow-sm px-4" href="<%= ctx %>/register">Tham gia ngay</a>
            <% } else { %>
            <button type="button" class="btn btn-warning btn-lg fw-bold shadow-sm px-4 text-dark" data-bs-toggle="modal" data-bs-target="#vipModal">
                Xem Ưu đãi VIP
            </button>
            <% } %>
            <% } %>
        </div>
    </section>
    <% } %>
</main>

<!-- VIP Modal -->
<div class="modal fade" id="vipModal" tabindex="-1" aria-labelledby="vipModalLabel" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 rounded-4 shadow-lg overflow-hidden">
      <div class="modal-header bg-sf-primary text-white border-0">
        <h5 class="modal-title fw-bold" id="vipModalLabel"><i class="bi bi-gem me-2"></i>Đăng ký Hội viên VIP</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4 text-center">
        <div class="mb-4">
            <i class="bi bi-star-fill text-warning" style="font-size: 3.5rem; text-shadow: 0 4px 15px rgba(245, 158, 11, 0.4);"></i>
        </div>
        <h4 class="fw-bold mb-3">Đặc quyền Dành riêng cho Bạn</h4>
        <ul class="list-unstyled text-start mx-auto mb-4" style="max-width: 320px;">
            <li class="mb-3 d-flex align-items-center"><i class="bi bi-check-circle-fill text-success fs-5 me-3"></i><span><strong>Giảm trực tiếp <%= vipDiscountStr %>%</strong> cho mọi lần đặt sân</span></li>
            <li class="mb-3 d-flex align-items-center"><i class="bi bi-check-circle-fill text-success fs-5 me-3"></i><span>Ưu tiên hiển thị lịch trống, đặt sân nhanh chóng</span></li>
            <li class="mb-3 d-flex align-items-center"><i class="bi bi-check-circle-fill text-success fs-5 me-3"></i><span>Hỗ trợ chăm sóc khách hàng ưu tiên 24/7</span></li>
        </ul>
        
        <% if (isVipActive) { %>
        <div class="bg-light p-3 rounded-3 mb-4 border border-warning border-opacity-50">
            <h5 class="text-success mb-0"><i class="bi bi-check-circle-fill me-2"></i><strong>Đang kích hoạt</strong></h5>
        </div>
        <% if (showVipRenew) { %>
        <a href="<%= ctx %>/payment?action=method&type=membership" class="btn btn-outline-warning btn-lg w-100 fw-bold shadow-sm">Gia hạn thêm 30 ngày VIP</a>
        <% } else { %>
        <button type="button" class="btn btn-secondary btn-lg w-100 fw-bold shadow-sm" disabled>Đã Nâng Cấp VIP</button>
        <% } %>
        <% } else { %>
        <div class="bg-light p-3 rounded-3 mb-4 border border-warning border-opacity-50">
            <h5 class="text-sf-accent mb-0">Chỉ với: <strong><%= vipPriceStr %>đ/Tháng</strong></h5>
        </div>
        <a href="<%= ctx %>/payment?action=method&type=membership" class="btn btn-sf-accent btn-lg w-100 fw-bold shadow-sm">Nâng cấp VIP ngay</a>
        <% } %>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://unpkg.com/aos@2.3.1/dist/aos.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    AOS.init({
        once: true,
        offset: 50,
        duration: 800,
        easing: 'ease-in-out'
    });
</script>
</body>
</html>
