<%@ page import="com.swp.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) {
        navRole = "guest";
    }
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();
    boolean isLoggedIn = sessionUser != null;
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/customer/matchmaking.css" rel="stylesheet">
    <title>Bảng tin giao hữu & Tìm đồng đội | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Tìm đối"></div>

<main class="py-5">
    <div class="container">
        <div class="d-flex justify-content-between align-items-end mb-4">
            <div>
                <h1 class="section-title">Tìm đối giao hữu</h1>
                <p class="text-muted mb-0">Kết nối các đội bóng phong trào.</p>
            </div>
            <% if (isLoggedIn) { %>
            <button class="btn btn-sf-primary" data-bs-toggle="modal" data-bs-target="#createPostModal">
                Tạo kèo mới
            </button>
            <% } else { %>
            <a class="btn btn-sf-primary" href="<%= ctx %>/login">
                Tạo kèo mới
            </a>
            <% } %>
        </div>

        <div class="row g-4">
            <!-- Sidebar bộ lọc -->
            <aside class="col-lg-3">
                <div class="card soft-card p-3 sidebar-card">
                    <h5>Bộ lọc tin tuyển</h5>
                    
                    <label class="form-label mt-3" for="postType">Loại tin đăng</label>
                    <select class="form-select" id="postType" onchange="searchPosts()">
                        <option value="ALL">Tất cả</option>
                        <option value="FIND_OPPONENT">Tìm đối thủ giao hữu</option>
                        <option value="FIND_TEAMMATE">Tìm đồng đội mới</option>
                    </select>

                    <label class="form-label mt-3" for="skillLevel">Trình độ</label>
                    <select class="form-select" id="skillLevel" onchange="searchPosts()">
                        <option value="ALL">Tất cả trình độ</option>
                        <option value="BEGINNER">Mới chơi (Beginner)</option>
                        <option value="INTERMEDIATE">Trung bình (Intermediate)</option>
                        <option value="ADVANCED">Khá / Giỏi (Advanced)</option>
                    </select>

                    <label class="form-label mt-3" for="facility">Địa điểm (Cơ sở)</label>
                    <select class="form-select" id="facility" onchange="searchPosts()">
                        <option value="">Tất cả địa điểm</option>
                    </select>

                    <% if (!isLoggedIn) { %>
                    <div class="alert alert-warning mt-4 py-2" role="alert" style="font-size: 0.9rem;">
                        <i class="bi bi-info-circle me-1"></i> Đăng nhập để đăng bài viết hoặc phản hồi.
                    </div>
                    <% } %>
                </div>
            </aside>

            <!-- Giao diện bài viết -->
            <section class="col-lg-9">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <p class="text-muted mb-0" id="postCount">Đang tải danh sách bài đăng...</p>
                    </div>
                    <% if (isLoggedIn) { %>
                    <div class="btn-group" role="group" aria-label="Lọc tin đăng">
                        <button type="button" class="btn btn-outline-success active btn-sm" id="btnAllPosts" onclick="togglePostsMode(false)">Tất cả tin</button>
                        <button type="button" class="btn btn-outline-success btn-sm" id="btnMyPosts" onclick="togglePostsMode(true)">Tin của tôi</button>
                    </div>
                    <% } %>
                </div>

                <div class="row g-4" id="posts-container">
                    <!-- Cards will be rendered here dynamically -->
                </div>
            </section>
        </div>
    </div>
</main>

<!-- Modal tạo bài đăng -->
<% if (isLoggedIn) { %>
<div class="modal fade" id="createPostModal" tabindex="-1" aria-labelledby="createPostModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="createPostModalLabel">Đăng tin tuyển đối / đồng đội</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form id="createPostForm" onsubmit="submitNewPost(event)">
                <input type="hidden" name="action" value="create_post">
                <div class="modal-body">
                    <div class="mb-3">
                        <label for="newPostType" class="form-label">Phân loại <span class="text-danger">*</span></label>
                        <select class="form-select" id="newPostType" name="postType" required>
                            <option value="FIND_OPPONENT">Tìm đối thủ giao hữu</option>
                            <option value="FIND_TEAMMATE">Tìm đồng đội mới</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label for="newTitle" class="form-label">Tiêu đề <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" id="newTitle" name="title" placeholder="Ví dụ: Tìm đối mềm sân Cầu Giấy tối nay" required>
                    </div>
                    <div class="mb-3">
                        <label for="newDescription" class="form-label">Mô tả chi tiết</label>
                        <textarea class="form-control" id="newDescription" name="description" rows="3" placeholder="Nhập khung giờ cụ thể, loại sân, cá cược nước nôi, màu áo..."></textarea>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="newSkillLevel" class="form-label">Trình độ yêu cầu</label>
                            <select class="form-select" id="newSkillLevel" name="skillLevel">
                                <option value="BEGINNER">Mới chơi (Beginner)</option>
                                <option value="INTERMEDIATE" selected>Trung bình (Intermediate)</option>
                                <option value="ADVANCED">Khá / Giỏi (Advanced)</option>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label for="newExpectedTime" class="form-label">Thời gian dự kiến</label>
                            <input type="datetime-local" class="form-control" id="newExpectedTime" name="expectedTime">
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="newFacility" class="form-label">Địa điểm (Cơ sở)</label>
                        <select class="form-select" id="newFacility" name="facilityId">
                            <option value="">Chọn địa điểm mong muốn</option>
                        </select>
                    </div>
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label for="newContactName" class="form-label">Tên liên hệ <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="newContactName" name="contactName" value="<%= displayName %>" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label for="newContactPhone" class="form-label">Số điện thoại <span class="text-danger">*</span></label>
                            <input type="tel" class="form-control" id="newContactPhone" name="contactPhone" required>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-sf-primary">Đăng tin</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal gửi phản hồi -->
<div class="modal fade" id="respondModal" tabindex="-1" aria-labelledby="respondModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="respondModalLabel">Gửi phản hồi / Lời nhắn</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form id="respondForm" onsubmit="submitResponse(event)">
                <input type="hidden" name="action" value="respond_post">
                <input type="hidden" id="respondPostId" name="postId">
                <div class="modal-body">
                    <p id="respondPostTitle" class="fw-semibold text-success"></p>
                    <div class="mb-3">
                        <label for="respondMessage" class="form-label">Lời nhắn / Lời chào <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="respondMessage" name="message" rows="4" placeholder="Ví dụ: Team mình trình độ tương đương, có thể đá 19h sân Mỹ Đình nhé..." required></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-sf-primary">Gửi lời nhắn</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal xem danh sách phản hồi (Dành cho chủ bài viết) -->
<div class="modal fade" id="viewResponsesModal" tabindex="-1" aria-labelledby="viewResponsesModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="viewResponsesModalLabel">Danh sách phản hồi từ đội bạn</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body" style="max-height: 400px; overflow-y: auto;">
                <div class="list-group" id="responses-list-container">
                    <!-- Phản hồi sẽ hiển thị ở đây -->
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>
<% } %>

<div id="footer" data-root="../../"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
    window.IS_LOGGED_IN = <%= isLoggedIn %>;
    window.CURRENT_USER_ID = <%= isLoggedIn ? sessionUser.getUserId() : "null" %>;
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/customer/matchmaking.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>
