<!--
* Module: Complex Management
* File: complex-form.jsp
* Description: Trang nhập liệu để thêm mới hoặc cập nhật thông tin cụm sân.
* Version: 2.0
-->
<%@ page import="com.swp.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/complex.css" rel="stylesheet">
    <title>Thêm / Sửa cụm sân | Sport Field Booking</title>
</head>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Cụm sân"></div>

<main class="owner-content">

        <div class="complex-form-page">
            <!-- Back button -->
            <button type="button" class="back-btn" onclick="history.back()">
                <i class="bi bi-arrow-left"></i>
            </button>

            <div class="complex-form-card">
                <input type="hidden" id="complexId">

                <!-- SECTION 1: Ảnh & Thông tin chính -->
                <div class="complex-form-section-header">
                    <div class="section-icon">
                        <i class="bi bi-image"></i>
                    </div>
                    <div>
                        <h6 id="formTitle">Thêm cụm sân mới</h6>
                        <small class="text-muted" style="font-size:.78rem">Điền đầy đủ thông tin bên dưới</small>
                    </div>
                </div>

                <div class="complex-form-body">
                    <div class="row g-3">

                        <!-- Image Upload -->
                        <div class="col-12">
                            <label class="form-label">
                                <i class="bi bi-images me-1 text-success"></i>Ảnh cụm sân
                            </label>
                            <input type="file" id="images" name="images"
                                   multiple accept="image/*" hidden>
                            <label for="images" class="upload-zone">
                                <i class="bi bi-cloud-arrow-up upload-icon"></i>
                                <p class="upload-text">Kéo thả hoặc click để chọn ảnh</p>
                                <p class="upload-hint">PNG, JPG, WEBP — Tối đa 5 ảnh</p>
                            </label>
                            <div id="preview" class="mt-2"></div>
                        </div>

                        <!-- Name & Hotline -->
                        <div class="col-md-8">
                            <label for="complexName" class="form-label">
                                Tên cụm sân <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="complexName"
                                   placeholder="VD: Sân bóng ABC" required>
                        </div>

                        <div class="col-md-4">
                            <label for="hotln" class="form-label">
                                Hotline <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="hotln"
                                   placeholder="0901 234 567" required>
                        </div>

                        <!-- Description -->
                        <div class="col-12">
                            <label for="desc" class="form-label">
                                <i class="bi bi-card-text me-1 text-success"></i>Mô tả
                            </label>
                            <textarea class="form-control" rows="3" id="desc"
                                      placeholder="Mô tả ngắn về cụm sân..."></textarea>
                        </div>
                    </div>

                    <div class="complex-form-divider"></div>

                    <!-- SECTION 2: Địa chỉ -->
                    <div class="d-flex align-items-center gap-2 mb-3">
                        <div class="section-icon">
                            <i class="bi bi-geo-alt-fill"></i>
                        </div>
                        <h6 class="mb-0 fw-bold">Địa chỉ</h6>
                    </div>

                    <div class="row g-3">
                        <div class="col-12">
                            <label for="adrs" class="form-label">
                                Địa chỉ chi tiết <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="adrs"
                                   placeholder="Số nhà, tên đường..." required>
                        </div>


                        <div class="col-md-6">
                            <label for="lat" class="form-label">Vĩ độ (Latitude)</label>
                            <input type="number" class="form-control" name="latitude"
                                   step="0.0000001" min="-90" max="90" id="lat"
                                   placeholder="10.776530">
                        </div>

                        <div class="col-md-6">
                            <label for="long" class="form-label">Kinh độ (Longitude)</label>
                            <input type="number" class="form-control" name="longitude"
                                   step="0.0000001" min="-180" max="180" id="long"
                                   placeholder="106.700981">
                        </div>
                    </div>

                    <div class="complex-form-divider"></div>

                    <!-- SECTION 3: Vận hành -->
                    <div class="d-flex align-items-center gap-2 mb-3">
                        <div class="section-icon">
                            <i class="bi bi-clock-fill"></i>
                        </div>
                        <h6 class="mb-0 fw-bold">Giờ hoạt động & Nội quy</h6>
                    </div>

                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="opTime" class="form-label">
                                <i class="bi bi-sunrise me-1 text-warning"></i>Giờ mở cửa <span class="text-danger">*</span>
                            </label>
                            <input type="time" class="form-control" id="opTime">
                        </div>

                        <div class="col-md-6">
                            <label for="clsTime" class="form-label">
                                <i class="bi bi-sunset me-1 text-danger"></i>Giờ đóng cửa <span class="text-danger">*</span>
                            </label>
                            <input type="time" class="form-control" id="clsTime">
                        </div>

                        <div class="col-12">
                            <label for="rule" class="form-label">
                                <i class="bi bi-clipboard-check me-1 text-info"></i>Nội quy chung
                            </label>
                            <textarea class="form-control" rows="4" id="rule"
                                      placeholder="Các quy định của cụm sân..."></textarea>
                        </div>
                    </div>
                </div>

                <!-- Sticky Footer -->
                <div class="complex-form-footer">
                    <button type="button" class="btn btn-light px-4"
                            onclick="history.back()">
                        <i class="bi bi-x-lg me-1"></i>Hủy
                    </button>
                    <button type="button" class="btn btn-success px-5"
                            onclick="submitForm()"
                            id="submitBtn">
                        <i class="bi bi-check-lg me-1"></i>Thêm cụm sân
                    </button>
                </div>
            </div>
        </div>
    </main>

<script>
    window.APP_CTX = '<%= ctx %>';
    display_name = '<%= displayName %>';
    current_role = '<%= navRole %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script src="<%= ctx %>/assets/js/owner/complex.js"></script>
<script>
    dynamicLabel();
    loadForm();
</script>
</body>
</html>
