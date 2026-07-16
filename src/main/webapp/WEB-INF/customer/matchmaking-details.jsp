<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.dto.MatchmakingPostDTO" %>
<%@ page import="com.swp.model.dto.MatchmakingPostResponseDTO" %>
<%@ page import="com.swp.model.MatchmakingPostResponse" %>
<%@ page import="java.util.List" %>
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
                            <i class="bi bi-geo-alt-fill me-1 text-success"></i><%= postDTO.getComplexName() != null ? postDTO.getComplexName() : "Tự chọn địa điểm / Sân khách" %>
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

                    <% if (isMyPost) { %>
                        <%
                            List<MatchmakingPostResponseDTO> responses = (List<MatchmakingPostResponseDTO>) request.getAttribute("responses");
                        %>
                        <div class="card soft-card p-4 mt-4">
                            <h4 class="fw-bold text-dark mb-4"><i class="bi bi-chat-text-fill text-success me-2"></i>Danh sách phản hồi từ đội bạn (<%= responses != null ? responses.size() : 0 %>)</h4>
                            <% if (responses == null || responses.isEmpty()) { %>
                                <div class="text-center py-4 text-muted">
                                    <i class="bi bi-chat-dots display-6"></i>
                                    <p class="mt-2 mb-0">Chưa có đội bóng hay người chơi nào gửi phản hồi cho bài đăng này.</p>
                                </div>
                            <% } else { %>
                                <div class="list-group">
                                    <% for (MatchmakingPostResponseDTO item : responses) { %>
                                        <% 
                                            MatchmakingPostResponse respObj = item.getResponse(); 
                                            String createdStr = respObj.getCreatedAt() != null ? respObj.getCreatedAt().format(formatter) : "";
                                        %>
                                        <div class="list-group-item list-group-item-action flex-column align-items-start border-0 border-bottom py-3 px-0">
                                            <div class="d-flex w-100 justify-content-between">
                                                <h6 class="mb-1 text-success fw-bold"><i class="bi bi-person-circle me-1"></i><%= item.getResponderName() != null ? item.getResponderName() : "Ẩn danh" %></h6>
                                                <small class="text-muted"><%= createdStr %></small>
                                            </div>
                                            <div class="small text-muted mb-2"><i class="bi bi-telephone-fill me-1"></i>Số điện thoại: <strong class="text-dark"><%= item.getResponderPhone() != null ? item.getResponderPhone() : "Không có" %></strong></div>
                                            <p class="mb-0 text-dark bg-light p-2 rounded small"><%= respObj.getMessage() %></p>
                                        </div>
                                    <% } %>
                                </div>
                            <% } %>
                        </div>
                    <% } %>
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
                                <% if (isMyPost) { %>
                                    <button class="btn btn-danger w-100 py-2" onclick="deleteMatchmakingPost(<%= postDTO.getPost().getPostId() %>)">
                                        <i class="bi bi-trash-fill me-1"></i> Xóa bài đăng
                                    </button>
                                <% } else { %>
                                    <button class="btn btn-secondary w-100 py-2" disabled>
                                        <i class="bi bi-lock-fill me-1"></i> Kèo đã đóng tuyển
                                    </button>
                                <% } %>
                            <% } else if (isMyPost) { %>
                                <button class="btn btn-sf-primary w-100 py-2 mb-2" data-bs-toggle="modal" data-bs-target="#editPostModal">
                                    <i class="bi bi-pencil-square me-1"></i> Chỉnh sửa bài đăng
                                </button>
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
                            const response = await fetch(window.APP_CTX + '/api/matchmaking', {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/x-www-form-urlencoded"
                                },
                                body: urlEncoded.toString()
                            });
                            
                            const result = await response.json();
                            if (!response.ok) throw new Error(result.error || "Gửi phản hồi thất bại");
                            
                            showToast("Gửi phản hồi thành công! Người đăng tin sẽ nhận được lời nhắn của bạn.", "success");
                            
                            // Đóng modal
                            const modalEl = document.getElementById("respondModal");
                            const modal = bootstrap.Modal.getInstance(modalEl);
                            if (modal) modal.hide();
                            
                            form.reset();
                        } catch (error) {
                            showToast("Lỗi: " + error.message, "danger");
                        }
                    }
                </script>
            <% } %>

            <% if (isMyPost) { %>
                <!-- Modal chỉnh sửa bài đăng -->
                <div class="modal fade" id="editPostModal" tabindex="-1" aria-labelledby="editPostModalLabel" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title" id="editPostModalLabel">Chỉnh sửa bài đăng tìm đối / đồng đội</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <form id="editPostForm" onsubmit="submitUpdatePost(event)">
                                <input type="hidden" name="action" value="update_post">
                                <input type="hidden" name="postId" value="<%= postDTO.getPost().getPostId() %>">
                                <div class="modal-body">
                                    <div class="mb-3">
                                        <label for="editPostType" class="form-label">Phân loại <span class="text-danger">*</span></label>
                                        <select class="form-select" id="editPostType" name="postType" required>
                                            <option value="FIND_OPPONENT" <%= "FIND_OPPONENT".equals(postDTO.getPost().getPostType()) ? "selected" : "" %>>Tìm đối thủ giao hữu</option>
                                            <option value="FIND_TEAMMATE" <%= "FIND_TEAMMATE".equals(postDTO.getPost().getPostType()) ? "selected" : "" %>>Tìm đồng đội mới</option>
                                        </select>
                                    </div>
                                    <div class="mb-3">
                                        <label for="editTitle" class="form-label">Tiêu đề <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="editTitle" name="title" value="<%= postDTO.getPost().getTitle().replace("\"", "&quot;") %>" placeholder="Ví dụ: Tìm đối mềm sân Cầu Giấy tối nay" maxlength="100" required>
                                    </div>
                                    <div class="mb-3">
                                        <label for="editDescription" class="form-label">Mô tả chi tiết</label>
                                        <textarea class="form-control" id="editDescription" name="description" rows="3" placeholder="Nhập khung giờ cụ thể, loại sân, cá cược nước nôi, màu áo..." maxlength="500"><%= postDTO.getPost().getDescription() != null ? postDTO.getPost().getDescription() : "" %></textarea>
                                    </div>
                                    <div class="row mb-3">
                                        <div class="col-md-6">
                                            <label for="editSkillLevel" class="form-label">Trình độ yêu cầu</label>
                                            <select class="form-select" id="editSkillLevel" name="skillLevel">
                                                <option value="BEGINNER" <%= "BEGINNER".equals(postDTO.getPost().getSkillLevel()) ? "selected" : "" %>>Mới chơi (Beginner)</option>
                                                <option value="INTERMEDIATE" <%= "INTERMEDIATE".equals(postDTO.getPost().getSkillLevel()) ? "selected" : "" %>>Trung bình (Intermediate)</option>
                                                <option value="ADVANCED" <%= "ADVANCED".equals(postDTO.getPost().getSkillLevel()) ? "selected" : "" %>>Khá / Giỏi (Advanced)</option>
                                            </select>
                                        </div>
                                        <div class="col-md-6">
                                            <label for="editExpectedTime" class="form-label">Thời gian dự kiến</label>
                                            <%
                                                String expectedTimeVal = "";
                                                if (postDTO.getPost().getExpectedTime() != null) {
                                                    expectedTimeVal = postDTO.getPost().getExpectedTime().toString();
                                                    if (expectedTimeVal.length() > 16) {
                                                        expectedTimeVal = expectedTimeVal.substring(0, 16);
                                                    }
                                                }
                                            %>
                                            <input type="datetime-local" class="form-control" id="editExpectedTime" name="expectedTime" value="<%= expectedTimeVal %>">
                                        </div>
                                    </div>
                                    <div class="mb-3">
                                        <label for="editFacility" class="form-label">Địa điểm (Cơ sở)</label>
                                        <select class="form-select" id="editFacility" name="complexId" data-selected="<%= postDTO.getPost().getComplexId() != null ? postDTO.getPost().getComplexId() : "" %>">
                                            <option value="">Chọn địa điểm mong muốn</option>
                                        </select>
                                    </div>
                                    <div class="row">
                                        <div class="col-md-6 mb-3">
                                            <label for="editContactName" class="form-label">Tên liên hệ <span class="text-danger">*</span></label>
                                            <input type="text" class="form-control" id="editContactName" name="contactName" value="<%= postDTO.getPost().getContactName().replace("\"", "&quot;") %>" maxlength="50" required>
                                        </div>
                                        <div class="col-md-6 mb-3">
                                            <label for="editContactPhone" class="form-label">Số điện thoại <span class="text-danger">*</span></label>
                                            <input type="tel" class="form-control" id="editContactPhone" name="contactPhone" value="<%= postDTO.getPost().getContactPhone() %>" maxlength="10" required>
                                        </div>
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                                    <button type="submit" class="btn btn-sf-primary">Lưu thay đổi</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <script>
                    async function loadEditFacilities() {
                        try {
                            const response = await fetch(window.APP_CTX + '/api/complexes');
                            if (response.ok) {
                                const data = await response.json();
                                const select = document.getElementById("editFacility");
                                if (select) {
                                    const selectedVal = select.getAttribute("data-selected");
                                    let html = '<option value="">Chọn địa điểm mong muốn</option>';
                                    data.forEach(item => {
                                        const fac = item.complex;
                                        const isSelected = fac.complexId == selectedVal ? 'selected' : '';
                                        html += '<option value="' + fac.complexId + '" ' + isSelected + '>' + fac.complexName + ' (' + fac.city + ')</option>';
                                    });
                                    select.innerHTML = html;
                                }
                            }
                        } catch (e) {
                            console.error("Lỗi khi load danh sách sân:", e);
                        }
                    }

                    async function submitUpdatePost(event) {
                        event.preventDefault();
                        const form = document.getElementById("editPostForm");
                        
                        // Validate expected time
                        const expectedTimeInput = document.getElementById("editExpectedTime");
                        if (expectedTimeInput && expectedTimeInput.value) {
                            const selectedTime = new Date(expectedTimeInput.value);
                            const currentTime = new Date();
                            if (selectedTime < currentTime) {
                                showToast("Thời gian dự kiến không được chọn trước ngày và giờ hiện tại.", "danger");
                                return;
                            }
                        }
                        
                        // Validate phone number
                        const contactPhoneInput = document.getElementById("editContactPhone");
                        if (contactPhoneInput) {
                            const phone = contactPhoneInput.value.trim();
                            const phonePattern = /^0\d{9}$/;
                            if (!phonePattern.test(phone)) {
                                showToast("Số điện thoại không đúng định dạng (phải bao gồm 10 chữ số và bắt đầu bằng số 0).", "danger");
                                return;
                            }
                        }
                        
                        const formData = new FormData(form);
                        const urlEncoded = new URLSearchParams();
                        for (const pair of formData.entries()) {
                            urlEncoded.append(pair[0], pair[1]);
                        }
                        
                        try {
                            const response = await fetch(window.APP_CTX + '/api/matchmaking', {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/x-www-form-urlencoded"
                                },
                                body: urlEncoded.toString()
                            });
                            
                            const result = await response.json();
                            if (!response.ok) throw new Error(result.error || "Cập nhật bài đăng thất bại");
                            
                            showToastAfterReload("Chỉnh sửa bài đăng tìm đối/đồng đội thành công!", "success");
                            window.location.reload();
                        } catch (error) {
                            showToast("Lỗi: " + error.message, "danger");
                        }
                    }

                    document.addEventListener("DOMContentLoaded", async () => {
                        await loadEditFacilities();
                        
                        const expectedTimeInput = document.getElementById("editExpectedTime");
                        if (expectedTimeInput) {
                            const updateMinDateTime = () => {
                                const now = new Date();
                                const year = now.getFullYear();
                                const month = String(now.getMonth() + 1).padStart(2, '0');
                                const day = String(now.getDate()).padStart(2, '0');
                                const hours = String(now.getHours()).padStart(2, '0');
                                const minutes = String(now.getMinutes()).padStart(2, '0');
                                expectedTimeInput.min = year + '-' + month + '-' + day + 'T' + hours + ':' + minutes;
                            };
                            updateMinDateTime();
                            
                            const editPostModal = document.getElementById("editPostModal");
                            if (editPostModal) {
                                editPostModal.addEventListener("show.bs.modal", updateMinDateTime);
                            }
                        }
                    });
                </script>
            <% } %>

            <script>
                window.APP_CTX = '<%= ctx %>';
                <% if (isMyPost) { %>
                async function closeMatchmakingPost(postId) {
                    showConfirm("Bạn có chắc muốn đóng bài đăng này không? Khi đóng tin, những người dùng khác sẽ không thể gửi phản hồi nữa.", async () => {
                        try {
                            const response = await fetch(window.APP_CTX + '/api/matchmaking?action=close_post&postId=' + postId, {
                                method: "POST"
                            });
                            const result = await response.json();
                            if (!response.ok) throw new Error(result.error || "Không đóng được bài viết");
                            showToastAfterReload("Đã đóng bài viết thành công!", "success");
                            window.location.reload();
                        } catch (error) {
                            showToast("Lỗi: " + error.message, "danger");
                        }
                    });
                }

                async function deleteMatchmakingPost(postId) {
                    showConfirm("Bạn có chắc muốn xóa bài đăng này không? Hành động này sẽ xóa vĩnh viễn tin tuyển đối cùng toàn bộ các phản hồi nhận được.", async () => {
                        try {
                            const formData = new URLSearchParams();
                            formData.append("action", "delete_post");
                            formData.append("postId", postId);

                            const response = await fetch(window.APP_CTX + '/api/matchmaking', {
                                method: "POST",
                                headers: {
                                    "Content-Type": "application/x-www-form-urlencoded"
                                },
                                body: formData.toString()
                            });

                            const result = await response.json();
                            if (!response.ok) throw new Error(result.error || "Không xóa được bài viết");

                            showToastAfterReload("Đã xóa bài viết thành công!", "success");
                            window.location.href = window.APP_CTX + '/matchmaking';
                        } catch (error) {
                            showToast("Lỗi: " + error.message, "danger");
                        }
                    });
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
