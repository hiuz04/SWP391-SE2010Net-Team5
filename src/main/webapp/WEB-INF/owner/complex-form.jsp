<!--
* Module: Complex Management
* File: complex-form.html
* Description: Trang nhập liệu để thêm mới hoặc cập nhật thông tin cơ sở.
*
* Author: Dương Hải Anh
* Version: 1.0
* Created Date: 01/06/2026
* Updated Date: 04/06/2026
* Update Description: Thêm giao diện cho biểu mẫu quản lý cơ sở.
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
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/complex.css" rel="stylesheet">
    <title></title>
</head>
<body>
<div class="container py-4">
    <div class="complex-form-wrapper">
        <div class="d-flex align-items-start me-2 mt-2">
            <button
                    type="button"
                    class="back-btn"
                    onclick="history.back()">
                <i class="bi bi-arrow-left"></i>
            </button>
        </div>
        <div class="card shadow-sm complex-form-card">
            <div class="card-body p-4">
                <h3 id="formTitle" class="mb-4 text-center"></h3>
                <form>
                    <input type="hidden" id="complexId">

                    <div class="row g-3">

                        <input type="hidden" id="complexId">

                        <div class="col-12">
                            <input type="file"
                                   id="images"
                                   name="images"
                                   multiple
                                   accept="image/*"
                                   hidden>

                            <label for="images" class="upload-btn">
                                <img src="<%= ctx %>/assets/images/icon/uploadIcon.png" height="30px">
                                Chọn ảnh
                            </label>

                            <div id="preview" class="mt-3"></div>
                        </div>

                        <div class="col-md-8">
                            <label for="complexName" class="form-label">Tên cơ sở <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="complexName" required>
                        </div>

                        <div class="col-md-4">
                            <label for="hotln" class="form-label">Hotline <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="hotln" required>
                        </div>

                        <div class="col-12">
                            <label for="desc" class="form-label">Mô tả</label>
                            <textarea class="form-control" rows="3" id="desc"></textarea>
                        </div>

                        <div class="col-12">
                            <label for="adrs" class="form-label">Địa chỉ <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="adrs" required>
                        </div>

                        <div class="col-md-4">
                            <label for="ward" class="form-label">Phường/Xã <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="ward" required>
                        </div>

                        <div class="col-md-4">
                            <label for="dist" class="form-label">Quận/Huyện <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="dist" required>
                        </div>

                        <div class="col-md-4">
                            <label for="city" class="form-label">Tỉnh/Thành phố <span
                                    class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="city" required>
                        </div>

                        <div class="col-md-6">
                            <label for="lat">Vĩ độ (Latitude)</label>
                            <input type="number"
                                   class="form-control"
                                   name="latitude"
                                   step="0.0000001"
                                   min="-90"
                                   max="90"
                                   id="lat">
                        </div>

                        <div class="col-md-6">
                            <label for="long">Kinh độ (Longitude)</label>
                            <input type="number"
                                   class="form-control"
                                   name="longitude"
                                   step="0.0000001"
                                   min="-180"
                                   max="180"
                                   id="long">
                        </div>

                        <div class="col-md-6">
                            <label for="opTime" class="form-label">Giờ mở cửa</label>
                            <input type="time" class="form-control" id="opTime">
                        </div>

                        <div class="col-md-6">
                            <label for="clsTime" class="form-label">Giờ đóng cửa</label>
                            <input type="time" class="form-control" id="clsTime">
                        </div>

                        <div class="col-12">
                            <label for="rule" class="form-label">Nội quy chung</label>
                            <textarea class="form-control" rows="4" id="rule"></textarea>
                        </div>

                        <div class="col-md-6">
                            <label for="status" class="form-label">Trạng thái</label>
                            <select class="form-select" id="status">
                                <option value="ACTIVE">Hoạt động</option>
                                <option value="INACTIVE">Ngưng hoạt động</option>
                                <option value="MAINTENANCE">Bảo trì</option>
                                <option value="CLOSED">Đóng cửa</option>
                            </select>
                        </div>

                        <div class="col-md-6 d-flex align-items-end">
                            <div class="form-check mb-2">
                                <input class="form-check-input"
                                       type="checkbox"
                                       id="feat">
                                <label class="form-check-label" for="feat">
                                    Cơ sở nổi bật
                                </label>
                            </div>
                        </div>

                    </div>

                    <div class="mt-4 text-end">
                        <button type="button"
                                class="btn btn-primary px-4"
                                onclick="submitForm()"
                                id="submitBtn">
                            Thêm Cụm sân
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/owner/complex.js"></script>
<script>dynamicLabel(); loadForm();</script>
</body>
</html>
