// Lưu đường context của trang
const ctx = window.APP_CTX || "";

const id = new URLSearchParams(window.location.search).get("id");

// Load dữ liệu chi tiết của cụm sân
function loadData(id) {
    fetch(`${ctx}/field?id=${id}`)
        .then(res => res.json())
        .then(data => {
            console.log(">>> role",currentRole);
            console.log(">>> data",data);
            document.getElementById("field-name").innerHTML = data.complexName;
            document.getElementById("address").innerHTML = `<i class="bi bi-geo-alt me-1"></i>` + data.complexAddress;
            
            const currentPriceElem = document.getElementById("currentPrice");
            if (currentPriceElem) {
                if (data.currentPrice != null) {
                    currentPriceElem.innerHTML = `<i class="bi bi-cash me-1"></i> Giá lúc này: ${new Intl.NumberFormat('vi-VN').format(data.currentPrice)} đ/giờ<br><small class="text-muted fw-normal" style="font-size: 0.9rem;">* Giá có thể thay đổi tùy thuộc vào khung giờ bạn chọn</small>`;
                } else {
                    currentPriceElem.innerHTML = `<i class="bi bi-cash me-1"></i> Chưa có giá`;
                }
            }
            
            document.getElementById("description").innerHTML = data.description;
            document.getElementById("workingTime").innerHTML = `${data.openingTime} - ${data.closingTime}`;
            document.querySelectorAll(".hotline").forEach(el => {
                el.innerHTML = data.hotline;
            });
            document.getElementById("fieldCount").innerHTML = data.fields.length;
            const bookingUrl = document.getElementById("bookingUrl");
            if (bookingUrl) {
                bookingUrl.href = `${ctx}/booking?action=create&complexId=${data.complexId}`;
            }

            const fields = document.getElementById("fields");
            fields.innerHTML = "";
            const fieldTypeMap = {};

            data.fieldTypeList.forEach(ft => {
                fieldTypeMap[ft.fieldTypeId] = ft.typeName;
            });

            fields.innerHTML = data.fields.map(item => `
                <div>
                    <span class="complex-item">${item.fieldName}</span>
                </div>
            `).join("");

            const priceRuleList = document.getElementById("price-rule-list");
            if (priceRuleList) {
                if (!data.priceRules || data.priceRules.length === 0) {
                    priceRuleList.innerHTML = `<div class="text-muted">Chưa có thông tin bảng giá.</div>`;
                } else {
                    let html = `<div class="table-responsive"><table class="table table-bordered">
                        <thead class="table-light">
                            <tr>
                                <th>Loại giá</th>
                                <th>Thời gian áp dụng</th>
                                <th>Khung giờ</th>
                                <th>Giá tiền</th>
                            </tr>
                        </thead>
                        <tbody>`;
                    
                    data.priceRules.forEach(rule => {
                        let dayDisplay = rule.dayOfWeek;
                        if (dayDisplay === 'All') dayDisplay = 'Tất cả các ngày';
                        else if (dayDisplay === 'Weekday') dayDisplay = 'Thứ 2 - Thứ 6';
                        else if (dayDisplay === 'Weekend') dayDisplay = 'Thứ 7, Chủ nhật';
                        else if (dayDisplay === 'SpecificDate') dayDisplay = `Ngày: ${rule.specificDate}`;
                        
                        let timeDisplay = 'Cả ngày';
                        if (rule.startTime && rule.endTime) {
                            timeDisplay = `${rule.startTime} - ${rule.endTime}`;
                        }
                        
                        html += `<tr>
                            <td><span class="badge bg-secondary">${rule.ruleName}</span></td>
                            <td>${dayDisplay}</td>
                            <td>${timeDisplay}</td>
                            <td class="text-success fw-bold">${new Intl.NumberFormat('vi-VN').format(rule.price)} đ</td>
                        </tr>`;
                    });
                    
                    html += `</tbody></table></div>`;
                    priceRuleList.innerHTML = html;
                }
            }

            const feedbackContainer = document.getElementById("feedbackContainer");

            if (!data.feedbacks || data.feedbacks.length === 0) {
                feedbackContainer.innerHTML = `
                    <div class="text-center text-muted py-3">
                        Chưa có đánh giá nào.
                    </div>
                `;
                return;
            }

            feedbackContainer.innerHTML = data.feedbacks.map(feedback => {
                const stars = Array.from({ length: 5 }, (_, i) =>
                    `<i class="bi ${i < feedback.rating
                        ? 'bi-star-fill text-warning'
                        : 'bi-star text-secondary'}"></i>`
                ).join("");

                const ownerReply = currentRole.toLowerCase() !== "owner"
                    ? `
                        ${feedback.ownerReply
                            ? `
                            <div class="owner-reply mt-3">
                                <div class="fw-semibold text-success">
                                    <i class="bi bi-reply-fill"></i>
                                    Phản hồi từ chủ sân
                                </div>
                                <div>${feedback.ownerReply}</div>
                            </div>
                            ` : ""
                        }
                    ` : "";

                const replySection = currentRole.toLowerCase() === "owner"
                    ? `
                            ${!feedback.ownerReply ? `
                                <button class="btn btn-sm btn-success mt-2" style="margin-right: 0; margin-left: auto; display: block"
                                        onclick="showReplyForm(${feedback.feedbackId})">
                                    <i class="bi bi-reply-fill"></i> Reply
                                </button>
                    
                                <div id="reply-form-${feedback.feedbackId}"
                                     class="reply-form mt-3"
                                     style="display: none;">
                                    <textarea
                                        id="reply-content-${feedback.feedbackId}"
                                        class="form-control"
                                        rows="3"
                                        maxlength="1000"
                                        placeholder="Nhập phản hồi..."
                                    ></textarea>
                    
                                    <div class="mt-2 text-end">
                                        <button class="btn btn-secondary btn-sm"
                                                onclick="hideReplyForm(${feedback.feedbackId})">
                                            Cancel
                                        </button>
                    
                                        <button class="btn btn-success btn-sm"
                                                onclick="submitReply(${feedback.feedbackId})">
                                            Send
                                        </button>
                                    </div>
                                </div>
                            ` : `
                                <div class="owner-reply mt-3">
                                    <div class="fw-semibold text-success mb-1">
                                        <i class="bi bi-reply-fill"></i> Owner Reply
                                    </div>
                                    <div>${feedback.ownerReply}</div>
                                </div>
                            `}
                        ` : "";

                return `
                        <div class="feedback-item mb-4">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="fw-bold">${feedback.userName}</div>
                                    ${stars}
                                </div>
                
                                <small class="text-muted">
                                    ${new Date(feedback.createdAt).toLocaleDateString("vi-VN")}
                                </small>
                            </div>
                
                            <div class="text-muted mt-2">
                                <i class="bi bi-geo-alt-fill"></i>
                                ${feedback.fieldName}
                            </div>
                
                            <p class="mt-3 mb-0">${feedback.feedbackDesc}</p>
                
                            ${ownerReply}
                            
                            ${replySection}
                        </div>
                    `;
            }).join("");
        })
}
loadData(id);

function showReplyForm(feedbackId) {
    document.getElementById(`reply-form-${feedbackId}`).style.display = "block";
}

function hideReplyForm(feedbackId) {
    document.getElementById(`reply-form-${feedbackId}`).style.display = "none";
}

function submitReply(feedbackId) {
    const content = document
        .getElementById(`reply-content-${feedbackId}`)
        .value
        .trim();

    if (!content) {
        alert("Vui lòng nhập nội dung phản hồi.");
        return;
    }

    fetch(`${ctx}/feedback-owner`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams({
            action: "reply",
            feedbackId: feedbackId,
            message: content
        })
    })
    .then(res => {
        if (!res.ok) {
            throw new Error("Reply failed");
        }
        return res.text();
    })
    .then(() => {
        alert("Phản hồi thành công.");
        location.reload();
    })
    .catch(err => {
        console.error(err);
        alert("Có lỗi xảy ra khi gửi phản hồi.");
    });
}