<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.MatchmakingPostDTO" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
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

    MatchmakingPostDTO postDTO = (MatchmakingPostDTO) request.getAttribute("postDTO");
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    String timeStr = "Chưa xếp lịch";
    if (postDTO != null && postDTO.getPost().getExpectedTime() != null) {
        timeStr = postDTO.getPost().getExpectedTime().format(formatter);
    }
    
    boolean isClosed = postDTO != null && "CLOSED".equalsIgnoreCase(postDTO.getPost().getStatus());
    boolean isMyPost = isLoggedIn && postDTO != null && postDTO.getPost().getAuthorId() == sessionUser.getUserId();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <title>Chi tiết tìm đối | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Tìm đối"></div>

<main class="py-5">
    <div class="container">
        <!-- Nút quay lại -->
        <div class="mb-4">
            <a href="<%= ctx %>/matchmaking" class="text-success text-decoration-none">
                <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách tìm đối
            </a>
        </div>

        <% if (postDTO == null) { %>
            <div class="alert alert-danger py-4 text-center" role="alert">
                <i class="bi bi-exclamation-triangle display-4 d-block mb-3"></i>
                <h5>Không tìm thấy thông tin bài đăng!</h5>
                <p class="mb-0 text-muted">Bài đăng này có thể đã bị xóa hoặc không tồn tại.</p>
            </div>
        <% } else { %>
            <div class="row g-4">
                <div class="col-lg-8">
                    <div class="card soft-card p-4">
                        <h1 class="section-title mb-2 text-dark"><%= postDTO.getPost().getTitle() %></h1>
                        <p class="text-muted mb-4">
                            <i class="bi bi-geo-alt-fill me-1 text-success"></i><%= postDTO.getFacilityName() != null ? postDTO.getFacilityName() : "Tự chọn địa điểm / Sân khách" %> 
                            &middot; 
                            <i class="bi bi-clock-fill me-1 text-success"></i><%= timeStr %>
                        </p>
                        
                        <div class="row g-3 my-3">
                            <div class="col-md-4">
                                <div class="stat-card p-3">
                                    <strong><%= "FIND_OPPONENT".equals(postDTO.getPost().getPostType()) ? "Tìm đối giao hữu" : "Tìm đồng đội mới" %></strong>
                                    <div class="small text-muted">Mục đích</div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="stat-card p-3">
                                    <%
                                        String skillDisplay = "Mới chơi (Beginner)";
                                        if ("INTERMEDIATE".equals(postDTO.getPost().getSkillLevel())) {
                                            skillDisplay = "Trung bình (Intermediate)";
                                        } else if ("ADVANCED".equals(postDTO.getPost().getSkillLevel())) {
                                            skillDisplay = "Khá / Giỏi (Advanced)";
                                        }
                                    %>
                                    <strong><%= skillDisplay %></strong>
                                    <div class="small text-muted">Trình độ</div>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <div class="stat-card p-3">
                                    <strong><%= isClosed ? "Đã đóng" : "Đang mở nhận kèo" %></strong>
                                    <div class="small text-muted">Trạng thái</div>
                                </div>
                            </div>
                        </div>
                        
                        <h5 class="mt-4 mb-3 text-dark fw-bold">Mô tả chi tiết</h5>
                        <p class="text-secondary" style="line-height: 1.6; white-space: pre-line;">
                            <%= postDTO.getPost().getDescription() != null && !postDTO.getPost().getDescription().trim().isEmpty() 
                                ? postDTO.getPost().getDescription() 
                                : "Không có mô tả chi tiết." %>
                        </p>
                    </div>
                </div>
                
                <aside class="col-lg-4">
                    <div class="card soft-card p-4 sidebar-card shadow-sm border-0">
                        <h5 class="fw-bold mb-3 text-dark">Thông tin liên hệ</h5>
                        <div class="mb-3">
                            <p class="mb-2"><i class="bi bi-person me-2 text-success"></i>Người đăng: <strong><%= postDTO.getAuthorName() != null ? postDTO.getAuthorName() : "Ẩn danh" %></strong></p>
                            <p class="mb-2"><i class="bi bi-telephone me-2 text-success"></i>Điện thoại: <strong><%= postDTO.getPost().getContactPhone() %></strong></p>
                            <p class="mb-0"><i class="bi bi-person-badge me-2 text-success"></i>Tên liên hệ: <strong><%= postDTO.getPost().getContactName() %></strong></p>
                        </div>
                        
                        <div class="mt-4">
                            <% if (isClosed) { %>
                                <button class="btn btn-secondary w-100 py-2" disabled>
                                    <i class="bi bi-lock-fill me-1"></i> Kèo đã đóng tuyển
                                </button>
                            <% } else if (isMyPost) { %>
                                <button class="btn btn-outline-danger w-100 py-2" onclick="closeMatchmakingPost(<%= postDTO.getPost().getPostId() %>)">
                                    <i class="bi bi-x-circle-fill me-1"></i> Đóng bài đăng
                                </button>
                            <% } else if (!isLoggedIn) { %>
                                <a href="<%= ctx %>/login" class="btn btn-sf-primary w-100 py-2">
                                    Đăng nhập để tham gia
                                </a>
                            <% } else { %>
                                <button class="btn btn-sf-primary w-100 py-2" data-bs-toggle="modal" data-bs-target="#respondModal">
                                    <i class="bi bi-reply-fill me-1"></i> Tham gia kèo
                                </button>
                            <% } %>
                        </div>
                    </div>
                </aside>
            </div>
            
            <!-- Modal phản hồi -->
            <% if (isLoggedIn && !isClosed && !isMyPost) { %>
                <div class="modal fade" id="respondModal" tabindex="-1" aria-labelledby="respondModalLabel" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="respondModalLabel">Gửi phản hồi / Lời nhắn</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <form id="respondForm" onsubmit="submitResponse(event)">
                                <input type="hidden" name="action" value="respond_post">
                                <input type="hidden" name="postId" value="<%= postDTO.getPost().getPostId() %>">
                                <div class="modal-body">
                                    <p class="fw-semibold text-success">Phản hồi cho tin: "<%= postDTO.getPost().getTitle() %>"</p>
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
                
                <script>
                    async function submitResponse(event) {
                        event.preventDefault();
                        const form = document.getElementById("respondForm");
                        const formData = new FormData(form);
                        
                        const urlEncoded = new URLSearchParams();
                        for (const pair of formData.entries()) {
                            urlEncoded.append(pair[0], pair[1]);
                        }
                        
                        try {
                            const response = await fetch(`${window.APP_CTX}/api/matchmaking`, {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/x-www-form-urlencoded"
                                },
                                body: urlEncoded.toString()
                            });
                            
                            const result = await response.json();
                            if (!response.ok) throw new Error(result.error || "Gửi phản hồi thất bại");
                            
                            alert("Gửi phản hồi thành công! Người đăng tin sẽ nhận được lời nhắn của bạn.");
                            
                            // Đóng modal
                            const modalEl = document.getElementById("respondModal");
                            const modal = bootstrap.Modal.getInstance(modalEl);
                            if (modal) modal.hide();
                            
                            form.reset();
                        } catch (error) {
                            alert("Lỗi: " + error.message);
                        }
                    }
                </script>
            <% } %>
            
            <script>
                window.APP_CTX = '<%= ctx %>';
                <% if (isMyPost && !isClosed) { %>
                async function closeMatchmakingPost(postId) {
                    if (!confirm("Bạn có chắc muốn đóng bài đăng này không? Khi đóng tin, những người dùng khác sẽ không thể gửi phản hồi nữa.")) {
                        return;
                    }
                    try {
                        const response = await fetch(`${window.APP_CTX}/api/matchmaking?action=close_post&postId=${postId}`, {
                            method: "POST"
                        });
                        const result = await response.json();
                        if (!response.ok) throw new Error(result.error || "Không đóng được bài viết");
                        alert("Đã đóng bài viết thành công!");
                        window.location.reload();
                    } catch (error) {
                        alert("Lỗi: " + error.message);
                    }
                }
                <% } %>
            </script>
        <% } %>
    </div>
</main>

<div id="footer" data-root="<%= ctx %>/"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
</body>
</html>
