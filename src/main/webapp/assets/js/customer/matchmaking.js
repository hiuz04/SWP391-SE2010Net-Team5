// ============================================================================
// Logic xử lý Bảng tin Giao hữu (Tìm đối / Tìm đồng đội) - Community Matchmaking
// Sử dụng chung cho cả Trang Danh sách (matchmaking-list.jsp) và Trang Chi tiết (matchmaking-details.jsp)
// ============================================================================

const ctx = typeof window.APP_CTX !== 'undefined' ? window.APP_CTX : (window.location.pathname.substring(0, window.location.pathname.indexOf('/', 1)) || '');

// Danh sách các trạng thái kỹ năng và badge tương ứng
const skillLevels = {
    "BEGINNER": { display: "Mới chơi (Beginner)", badge: "badge-skill-beginner" },
    "INTERMEDIATE": { display: "Trung bình (Intermediate)", badge: "badge-skill-intermediate" },
    "ADVANCED": { display: "Khá / Giỏi (Advanced)", badge: "badge-skill-advanced" }
};

// Biến lưu trữ danh sách cơ sở/địa điểm
let complexesList = [];
// Biến đánh dấu trạng thái hiển thị: false = Tất cả tin, true = Tin của tôi (chỉ áp dụng ở trang Danh sách)
let myPostsMode = false;

// Cờ xác định xem chúng ta đang ở trang Danh sách hay trang Chi tiết
const isListPage = document.getElementById("posts-container") !== null;
const isDetailPage = document.getElementById("editPostForm") !== null || window.location.pathname.includes('matchmaking-details');

// ============================================================================
// [1] HÀM KHỞI TẠO & LOAD DỮ LIỆU CƠ BẢN
// ============================================================================

// Tải danh sách địa điểm (cơ sở) từ API để nạp vào bộ lọc và Form tạo mới / chỉnh sửa
async function loadComplexes() {
    try {
        const response = await fetch(`${ctx}/api/complexes`);
        if (!response.ok) throw new Error("Không tải được danh sách sân.");
        
        const data = await response.json();
        complexesList = data.map(item => item.complex);
        
        // Nạp vào dropdown Bộ lọc (nếu có ở trang Danh sách)
        const filterSelect = document.getElementById("complex");
        if (filterSelect) {
            let filterHtml = `<option value="">Tất cả địa điểm</option>`;
            complexesList.forEach(fac => {
                filterHtml += `<option value="${fac.complexId}">${fac.complexName} (${fac.city})</option>`;
            });
            filterSelect.innerHTML = filterHtml;
        }

        // Nạp vào dropdown Form Tạo Mới (nếu có ở trang Danh sách)
        const formSelect = document.getElementById("newComplex");
        if (formSelect) {
            let formHtml = `<option value="">Chọn địa điểm mong muốn</option>`;
            complexesList.forEach(fac => {
                formHtml += `<option value="${fac.complexId}">${fac.complexName} (${fac.city})</option>`;
            });
            formSelect.innerHTML = formHtml;
        }

        // Nạp vào dropdown Form Chỉnh Sửa (nếu có ở trang Chi tiết)
        const editSelect = document.getElementById("editFacility");
        if (editSelect) {
            const selectedVal = editSelect.getAttribute("data-selected");
            let editHtml = `<option value="">Chọn địa điểm mong muốn</option>`;
            complexesList.forEach(fac => {
                const isSelected = (fac.complexId == selectedVal) ? 'selected' : '';
                editHtml += `<option value="${fac.complexId}" ${isSelected}>${fac.complexName} (${fac.city})</option>`;
            });
            editSelect.innerHTML = editHtml;
        }

    } catch (error) {
        console.error("Lỗi khi load danh sách cụm sân:", error);
    }
}

// Cập nhật lại thời gian tối thiểu (min) cho ô chọn thời gian (không cho chọn giờ trong quá khứ)
function updateMinDateTime(inputId) {
    const timeInput = document.getElementById(inputId);
    if (timeInput) {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        timeInput.min = `${year}-${month}-${day}T${hours}:${minutes}`;
    }
}

// Khởi chạy các hàm cơ bản khi tài liệu (DOM) đã sẵn sàng
document.addEventListener("DOMContentLoaded", async () => {
    // 1. Tải danh sách địa điểm chung cho cả 2 trang
    await loadComplexes();
    
    // 2. Nếu đang ở trang Danh sách (matchmaking-list.jsp), tự động gọi hàm tìm kiếm ban đầu
    if (isListPage) {
        await searchPosts();
        
        updateMinDateTime("newExpectedTime");
        const createPostModal = document.getElementById("createPostModal");
        if (createPostModal) {
            createPostModal.addEventListener("show.bs.modal", () => updateMinDateTime("newExpectedTime"));
        }
    }
    
    // 3. Nếu đang ở trang Chi tiết (matchmaking-details.jsp), set thời gian tối thiểu cho form edit
    if (isDetailPage) {
        updateMinDateTime("editExpectedTime");
        const editPostModal = document.getElementById("editPostModal");
        if (editPostModal) {
            editPostModal.addEventListener("show.bs.modal", () => updateMinDateTime("editExpectedTime"));
        }
    }
});

// ============================================================================
// [2] CÁC CHỨC NĂNG CỦA TRANG DANH SÁCH (LIST)
// ============================================================================

// Chuyển đổi giữa chế độ xem "Tất cả tin" và "Tin của tôi"
function togglePostsMode(isMyPosts) {
    myPostsMode = isMyPosts;
    const btnAll = document.getElementById("btnAllPosts");
    const btnMy = document.getElementById("btnMyPosts");
    if (btnAll && btnMy) {
        if (isMyPosts) {
            btnAll.classList.remove("active");
            btnMy.classList.add("active");
        } else {
            btnAll.classList.add("active");
            btnMy.classList.remove("active");
        }
    }
    searchPosts(); // Cập nhật lại danh sách sau khi đổi tab
}

// Gọi API lấy danh sách bài đăng và render HTML
async function searchPosts() {
    if (!isListPage) return; // Bảo vệ an toàn: Nếu không phải trang danh sách thì bỏ qua

    const postType = document.getElementById("postType") ? document.getElementById("postType").value : "ALL";
    const skillLevel = document.getElementById("skillLevel") ? document.getElementById("skillLevel").value : "ALL";
    const complexId = document.getElementById("complex") ? document.getElementById("complex").value : "";
    
    const params = new URLSearchParams();
    if (postType && postType !== "ALL") params.append("postType", postType);
    if (skillLevel && skillLevel !== "ALL") params.append("skillLevel", skillLevel);
    if (complexId) params.append("complexId", complexId);
    
    if (myPostsMode) {
        params.append("myPostsOnly", "true");
    }
    
    const postsContainer = document.getElementById("posts-container");
    const postCountText = document.getElementById("postCount");
    
    try {
        const response = await fetch(`${ctx}/api/matchmaking?${params.toString()}`);
        if (!response.ok) throw new Error("Lỗi khi lấy dữ liệu bài viết.");
        
        const posts = await response.json();
        
        if (postCountText) postCountText.textContent = `Tìm thấy ${posts.length} bài đăng phù hợp.`;
        
        if (posts.length === 0) {
            postsContainer.innerHTML = `
                <div class="col-12 text-center py-5">
                    <i class="bi bi-chat-left-dots text-muted display-4"></i>
                    <h5 class="mt-3 text-muted">Chưa có bài viết nào phù hợp bộ lọc của bạn</h5>
                    <p class="text-muted small">Hãy thử điều chỉnh lại bộ lọc hoặc đăng tin mới!</p>
                </div>
            `;
            return;
        }
        
        postsContainer.innerHTML = posts.map(item => {
            const post = item.post;
            const authorName = item.authorName || "Ẩn danh";
            const complexName = item.complexName || "Tự chọn địa điểm / Sân khách";
            
            const isOpponent = post.postType === "FIND_OPPONENT";
            let typeBadgeClass = isOpponent ? "badge-soft-success" : "badge-soft-info";
            let typeText = isOpponent ? "🤝 Tìm đối" : "⚽ Tìm đồng đội";
            
            const isClosed = post.status === "CLOSED";
            if (isClosed) {
                typeBadgeClass = "badge-soft-danger";
                typeText = "🔒 Đã đóng";
            }
            
            let skillBadgeClass = "bg-light text-secondary border";
            let skillText = "Mới chơi";
            if (post.skillLevel === "INTERMEDIATE") {
                skillBadgeClass = "badge-soft-warning";
                skillText = "Trung bình";
            } else if (post.skillLevel === "ADVANCED") {
                skillBadgeClass = "badge-soft-danger";
                skillText = "Khá / Giỏi";
            }
            
            let expectedTimeStr = post.expectedTime ? post.expectedTime : "Chưa xếp lịch";
            const isMyPost = window.IS_LOGGED_IN && post.authorId == window.CURRENT_USER_ID;
            
            let actionButton = "";
            let myPostBadge = "";
            const detailUrl = `${ctx}/matchmaking-details?id=${post.postId}`;
            
            if (isMyPost) {
                myPostBadge = `<span class="badge bg-secondary ms-2"><i class="bi bi-person-fill"></i> Của tôi</span>`;
                if (!isClosed) {
                    actionButton = `
                        <div class="d-flex gap-2 w-100 mt-auto">
                            <a href="${detailUrl}" class="btn btn-outline-success btn-sm flex-grow-1 text-nowrap">
                                <i class="bi bi-info-circle me-1"></i> Chi tiết
                            </a>
                            <button class="btn btn-outline-primary btn-sm flex-grow-1 text-nowrap" onclick="openResponsesListModal(${post.postId})">
                                <i class="bi bi-chat-text-fill me-1"></i> Phản hồi (${item.responseCount})
                            </button>
                            <button class="btn btn-outline-danger btn-sm" onclick="closeMatchmakingPost(${post.postId})" title="Đóng bài đăng">
                                <i class="bi bi-x-circle-fill"></i>
                            </button>
                        </div>
                    `;
                } else {
                    actionButton = `
                        <div class="d-flex gap-2 w-100 mt-auto">
                            <a href="${detailUrl}" class="btn btn-outline-success btn-sm flex-grow-1 text-nowrap">
                                <i class="bi bi-info-circle me-1"></i> Chi tiết
                            </a>
                            <button class="btn btn-outline-primary btn-sm flex-grow-1 text-nowrap" onclick="openResponsesListModal(${post.postId})">
                                <i class="bi bi-chat-text-fill me-1"></i> Phản hồi (${item.responseCount})
                            </button>
                            <button class="btn btn-outline-danger btn-sm" onclick="deleteMatchmakingPost(${post.postId})" title="Xóa bài đăng">
                                <i class="bi bi-trash-fill"></i>
                            </button>
                        </div>
                    `;
                }
            } else {
                if (isClosed) {
                    actionButton = `
                        <div class="d-flex gap-2 w-100 mt-auto">
                            <a href="${detailUrl}" class="btn btn-outline-success btn-sm flex-grow-1">
                                <i class="bi bi-info-circle me-1"></i> Chi tiết
                            </a>
                            <button class="btn btn-secondary btn-sm flex-grow-1" disabled>
                                <i class="bi bi-lock-fill me-1"></i> Đã đóng
                            </button>
                        </div>
                    `;
                } else {
                    const respondBtn = window.IS_LOGGED_IN 
                        ? `<button class="btn btn-sf-primary btn-sm flex-grow-1" onclick="openRespondModal(${post.postId}, '${post.title.replace(/'/g, "\\'").replace(/"/g, "&quot;")}')">
                               <i class="bi bi-reply-fill me-1"></i> Phản hồi ngay
                           </button>`
                        : `<a href="${ctx}/login" class="btn btn-sf-primary btn-sm flex-grow-1">
                               <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập
                           </a>`;
                    actionButton = `
                        <div class="d-flex gap-2 w-100 mt-auto">
                            <a href="${detailUrl}" class="btn btn-outline-success btn-sm flex-grow-1">
                                <i class="bi bi-info-circle me-1"></i> Chi tiết
                            </a>
                            ${respondBtn}
                        </div>
                    `;
                }
            }
            
            return `
                <div class="col-md-6 col-xl-4 mb-4">
                    <div class="card soft-card p-4 h-100 d-flex flex-column justify-content-between">
                        <div>
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <div>
                                    <span class="badge ${typeBadgeClass}">${typeText}</span>
                                    ${myPostBadge}
                                </div>
                                <span class="badge ${skillBadgeClass} fw-normal" style="font-size: 0.75rem;">
                                    ${skillText}
                                </span>
                            </div>
                            
                            <h5 class="fw-bold mb-2 text-dark text-truncate-2" title="${post.title}" style="min-height: 48px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.4;">
                                ${post.title}
                            </h5>
                            
                            <div class="mb-3 text-muted" style="font-size: 0.85rem;">
                                <div class="mb-1 text-truncate" title="${complexName}">
                                    <i class="bi bi-geo-alt me-2 text-success"></i>${complexName}
                                </div>
                                <div class="mb-1">
                                    <i class="bi bi-clock me-2 text-success"></i>${expectedTimeStr}
                                </div>
                                <div class="mb-1">
                                    <i class="bi bi-person me-2 text-success"></i>Đăng bởi: <strong>${authorName}</strong>
                                </div>
                            </div>
                            
                            ${post.description ? `
                                <p class="text-secondary mb-3 text-truncate-3" style="font-size: 0.85rem; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; min-height: 57px; line-height: 1.5;">
                                    ${post.description}
                                </p>
                            ` : `
                                <p class="text-muted mb-3 italic" style="font-size: 0.85rem; min-height: 57px; font-style: italic;">
                                    Không có mô tả chi tiết.
                                </p>
                            `}
                        </div>
                        
                        <div>
                            <div class="border-top pt-2 mb-3 text-muted text-truncate" style="font-size: 0.8rem; border-top: 1px dashed #dee2e6 !important; white-space: nowrap;" title="${post.contactName} - ${post.contactPhone}">
                                <i class="bi bi-telephone-fill me-1 text-success"></i> <strong>${post.contactName}</strong> - <span class="text-dark">${post.contactPhone}</span>
                            </div>
                            ${actionButton}
                        </div>
                    </div>
                </div>
            `;
        }).join("");
        
    } catch (error) {
        console.error("Lỗi khi load danh sách tin matchmaking:", error);
        postsContainer.innerHTML = `
            <div class="col-12 text-center py-5 text-danger">
                <i class="bi bi-exclamation-triangle display-4"></i>
                <h5 class="mt-3">Không tải được dữ liệu bảng tin</h5>
                <p>${error.message}</p>
            </div>
        `;
    }
}

// Validate số điện thoại (chỉ nhận số, bắt đầu bằng 0, độ dài 10)
function validatePhoneNumber(phone) {
    const phonePattern = /^0\d{9}$/;
    return phonePattern.test(phone.trim());
}

// Xử lý nộp form tạo bài đăng mới (Tìm đối / đồng đội)
async function submitNewPost(event) {
    event.preventDefault();
    const form = document.getElementById("createPostForm");
    
    // 1. Kiểm tra Validate thời gian dự kiến (không được trong quá khứ)
    const expectedTimeInput = document.getElementById("newExpectedTime");
    if (expectedTimeInput && expectedTimeInput.value) {
        const selectedTime = new Date(expectedTimeInput.value);
        if (selectedTime < new Date()) {
            showToast("Thời gian dự kiến không được chọn trước ngày và giờ hiện tại.", "danger");
            return;
        }
    }
    
    // 2. Kiểm tra Validate số điện thoại liên hệ
    const contactPhoneInput = document.getElementById("newContactPhone");
    if (contactPhoneInput && !validatePhoneNumber(contactPhoneInput.value)) {
        showToast("Số điện thoại không đúng định dạng (phải bao gồm 10 chữ số và bắt đầu bằng số 0).", "danger");
        return;
    }
    
    // 3. Gửi Request POST dữ liệu lên server
    const formData = new FormData(form);
    const urlEncoded = new URLSearchParams();
    for (const pair of formData.entries()) {
        urlEncoded.append(pair[0], pair[1]);
    }
    
    try {
        const response = await fetch(`${ctx}/api/matchmaking`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: urlEncoded.toString()
        });
        
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Gửi bài thất bại");
        
        showToast("Đăng tin tuyển đối/đồng đội thành công!", "success");
        
        // Đóng modal và reset form
        const modal = bootstrap.Modal.getInstance(document.getElementById("createPostModal"));
        if (modal) modal.hide();
        form.reset();
        
        // Cập nhật lại danh sách nếu đang ở trang Danh sách
        if (isListPage) {
            searchPosts();
        }
    } catch (error) {
        showToast("Lỗi: " + error.message, "danger");
    }
}

// ============================================================================
// [3] CÁC CHỨC NĂNG DÙNG CHUNG CỦA BÀI VIẾT (GỬI LỜI NHẮN, ĐÓNG, XÓA, XEM LỜI NHẮN)
// ============================================================================

// Xử lý Gửi lời nhắn / Phản hồi vào một bài đăng cụ thể
async function submitResponse(event) {
    event.preventDefault();
    const form = document.getElementById("respondForm");
    const formData = new FormData(form);
    
    const urlEncoded = new URLSearchParams();
    for (const pair of formData.entries()) {
        urlEncoded.append(pair[0], pair[1]);
    }
    
    try {
        const response = await fetch(`${ctx}/api/matchmaking`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: urlEncoded.toString()
        });
        
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Gửi phản hồi thất bại");
        
        // Ẩn modal hiện tại
        const modal = bootstrap.Modal.getInstance(document.getElementById("respondModal"));
        if (modal) modal.hide();
        form.reset();
        
        // Nếu ở trang danh sách thì hiển thị toast thông thường, nếu ở chi tiết thì hiển thị xong reload lại trang
        if (isDetailPage) {
            showToastAfterReload("Gửi phản hồi thành công! Người đăng tin sẽ nhận được lời nhắn của bạn.", "success");
            window.location.href = window.location.pathname + window.location.search + (window.location.search.includes('?') ? '&' : '?') + '_t=' + new Date().getTime();
        } else {
            showToast("Lưu phản hồi thành công! Người đăng tin sẽ nhận được lời nhắn của bạn.", "success");
        }
    } catch (error) {
        showToast("Lỗi: " + error.message, "danger");
    }
}

// Mở modal phản hồi (Chỉ áp dụng ở trang danh sách, vì trang chi tiết form đã hiển thị tĩnh)
async function openRespondModal(postId, postTitle) {
    if (!window.IS_LOGGED_IN) {
        window.location.href = `${ctx}/login`;
        return;
    }
    
    // Nạp dữ liệu vào form
    document.getElementById("respondPostId").value = postId;
    document.getElementById("respondPostTitle").textContent = `Phản hồi cho tin: "${postTitle}"`;
    
    const messageTextarea = document.getElementById("respondMessage");
    if (messageTextarea) messageTextarea.value = "";
    
    const submitBtn = document.getElementById("respondSubmitBtn");
    const modalTitle = document.getElementById("respondModalLabel");
    if (submitBtn) {
        submitBtn.textContent = "Đang kiểm tra...";
        submitBtn.disabled = true;
    }
    
    // Hiện modal
    const modal = new bootstrap.Modal(document.getElementById("respondModal"));
    modal.show();
    
    // Tự động fetch xem người dùng này đã từng gửi lời nhắn cho bài đăng này chưa
    try {
        const response = await fetch(`${ctx}/api/matchmaking?action=get_my_response&postId=${postId}`);
        if (response.ok) {
            const data = await response.json();
            if (data.exists) { // Nếu đã từng phản hồi thì nạp vào TextArea để cập nhật
                if (messageTextarea) messageTextarea.value = data.message;
                if (modalTitle) modalTitle.textContent = "Chỉnh sửa phản hồi / Lời nhắn";
                if (submitBtn) submitBtn.textContent = "Cập nhật lời nhắn";
            } else {
                if (modalTitle) modalTitle.textContent = "Gửi phản hồi / Lời nhắn";
                if (submitBtn) submitBtn.textContent = "Gửi lời nhắn";
            }
        }
    } catch (error) {
        console.error("Lỗi khi tải phản hồi cũ:", error);
    } finally {
        if (submitBtn) submitBtn.disabled = false;
    }
}

// Mở danh sách các phản hồi nhận được (dành riêng cho chủ bài đăng)
async function openResponsesListModal(postId) {
    const listContainer = document.getElementById("responses-list-container");
    listContainer.innerHTML = `<div class="text-center py-3"><div class="spinner-border text-primary" role="status"></div><p class="mt-2 text-muted">Đang tải phản hồi...</p></div>`;
    
    const modal = new bootstrap.Modal(document.getElementById("viewResponsesModal"));
    modal.show();
    
    try {
        const response = await fetch(`${ctx}/api/matchmaking?action=get_responses&postId=${postId}`);
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error || "Không tải được danh sách phản hồi");
        }
        
        const data = await response.json();
        
        if (data.length === 0) {
            listContainer.innerHTML = `
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-chat-dots display-6"></i>
                    <p class="mt-2 mb-0">Chưa có đội bóng nào gửi phản hồi cho bài đăng này.</p>
                </div>
            `;
            return;
        }
        
        // Build giao diện danh sách phản hồi
        listContainer.innerHTML = data.map(item => {
            const resp = item.response;
            const responderName = item.responderName || "Ẩn danh";
            return `
                <div class="list-group-item list-group-item-action flex-column align-items-start border-0 border-bottom py-3">
                    <div class="d-flex w-100 justify-content-between">
                        <h6 class="mb-1 text-success fw-bold"><i class="bi bi-person-circle me-1"></i>${responderName}</h6>
                        <small class="text-muted">${resp.createdAt || ''}</small>
                    </div>
                    <div class="small text-muted mb-2"><i class="bi bi-telephone-fill me-1"></i>Số điện thoại: <strong class="text-dark">${item.responderPhone || 'Không có'}</strong></div>
                    <p class="mb-0 text-dark bg-light p-2 rounded small">${resp.message}</p>
                </div>
            `;
        }).join("");
        
    } catch (error) {
        listContainer.innerHTML = `
            <div class="alert alert-danger my-2" role="alert">
                <i class="bi bi-exclamation-triangle-fill me-1"></i> ${error.message}
            </div>
        `;
    }
}

// Xử lý: Đóng bài viết tìm đối (Ngừng nhận thêm kèo)
async function closeMatchmakingPost(postId) {
    showConfirm("Bạn có chắc muốn đóng bài đăng này không? Khi đóng tin, những người dùng khác sẽ không thể gửi phản hồi nữa.", async () => {
        try {
            const formData = new URLSearchParams();
            formData.append("action", "close_post");
            formData.append("postId", postId);

            const response = await fetch(`${ctx}/api/matchmaking`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            });
            
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "Không đóng được bài viết");
            
            // Xử lý UI linh hoạt
            if (isDetailPage) {
                showToastAfterReload("Đã đóng bài viết thành công!", "success");
                window.location.href = `${ctx}/matchmaking-details?id=${postId}&_t=${new Date().getTime()}`;
            } else {
                showToast("Đã đóng bài viết thành công!", "success");
                searchPosts(); // Render lại danh sách
            }
        } catch (error) {
            showToast("Lỗi: " + error.message, "danger");
        }
    });
}

// Xử lý: Xóa hoàn toàn bài viết tìm đối đã đóng
async function deleteMatchmakingPost(postId) {
    showConfirm("Bạn có chắc muốn xóa bài đăng này không? Hành động này sẽ xóa vĩnh viễn tin tuyển đối cùng toàn bộ các phản hồi nhận được.", async () => {
        try {
            const formData = new URLSearchParams();
            formData.append("action", "delete_post");
            formData.append("postId", postId);

            const response = await fetch(`${ctx}/api/matchmaking`, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            });

            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "Không xóa được bài viết");

            // Xử lý UI linh hoạt
            if (isDetailPage) {
                showToastAfterReload("Đã xóa bài viết thành công!", "success");
                window.location.href = `${ctx}/matchmaking?_t=${new Date().getTime()}`; // Quay lại trang danh sách vì bài đã bị xóa
            } else {
                showToast("Đã xóa bài viết thành công!", "success");
                searchPosts(); // Render lại danh sách
            }
        } catch (error) {
            showToast("Lỗi: " + error.message, "danger");
        }
    });
}

// ============================================================================
// [4] CÁC CHỨC NĂNG DÀNH RIÊNG CHO TRANG CHI TIẾT (DETAILS)
// ============================================================================

// Xử lý Cập nhật bài đăng hiện có (Ở trang Chi tiết)
async function submitUpdatePost(event) {
    event.preventDefault();
    const form = document.getElementById("editPostForm");
    
    // 1. Validate thời gian
    const expectedTimeInput = document.getElementById("editExpectedTime");
    if (expectedTimeInput && expectedTimeInput.value) {
        const selectedTime = new Date(expectedTimeInput.value);
        if (selectedTime < new Date()) {
            showToast("Thời gian dự kiến không được chọn trước ngày và giờ hiện tại.", "danger");
            return;
        }
    }
    
    // 2. Validate số điện thoại
    const contactPhoneInput = document.getElementById("editContactPhone");
    if (contactPhoneInput && !validatePhoneNumber(contactPhoneInput.value)) {
        showToast("Số điện thoại không đúng định dạng (phải bao gồm 10 chữ số và bắt đầu bằng số 0).", "danger");
        return;
    }
    
    // 3. Gọi API cập nhật
    const formData = new FormData(form);
    const urlEncoded = new URLSearchParams();
    for (const pair of formData.entries()) {
        urlEncoded.append(pair[0], pair[1]);
    }
    
    try {
        const response = await fetch(`${ctx}/api/matchmaking`, {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: urlEncoded.toString()
        });
        
        const result = await response.json();
        if (!response.ok) throw new Error(result.error || "Cập nhật bài đăng thất bại");
        
        // Thành công -> Reload trang chi tiết
        showToastAfterReload("Chỉnh sửa bài đăng tìm đối/đồng đội thành công!", "success");
        window.location.href = window.location.pathname + window.location.search + (window.location.search.includes('?') ? '&' : '?') + '_t=' + new Date().getTime();
    } catch (error) {
        showToast("Lỗi: " + error.message, "danger");
    }
}
