// Lưu đường context của trang
const ctx = window.APP_CTX || "";

let currentPoint = 0;

let allVouchers = [];

const statusLabel = {
    AVAILABLE: {text: "Có thể sử dụng", badge: "bg-success"},
    RESERVED: {text: "Đang được giữ", badge: "bg-warning text-dark"},
    USED: {text: "Đã sử dụng", badge: "bg-secondary"},
    EXPIRED: {text: "Hết hạn", badge: "bg-danger"}
};

function escapeHtml(str) {
    const div = document.createElement("div");
    div.textContent = str ?? "";
    return div.innerHTML;
}

function formatDate(isoStr) {
    // API trả LocalDateTime dạng ISO, thiếu ngày thì hiển thị rỗng.
    if (!isoStr) return "";
    // isoStr dạng "2026-12-31T00:00:00" (LocalDateTime.toString())
    const [datePart] = isoStr.split("T");
    const [y, m, d] = datePart.split("-");
    return `${d}/${m}/${y}`;
}

// Load kho voucher
function loadVoucherStock(type = "ALL_TYPE") {
    const voucherList = document.getElementById("voucher-list");
    voucherList.innerHTML = `<div class="col-12 text-center text-muted py-5">Đang tải...</div>`;

    fetch(`${ctx}/vouchers-api?to=center&type=${encodeURIComponent(type)}`)
        .then(res => res.json())
        .then(data => {
            // API trả success=false khi Customer chưa đủ quyền hoặc server validate fail.
            if (!data.success) {
                voucherList.innerHTML = `<div class="col-12 text-center text-danger py-5">${data.message || "Không thể tải dữ liệu."}</div>`;
                return;
            }

            // Điểm thưởng được cập nhật từ DB mỗi lần load kho voucher.
            if (typeof data.point !== "undefined") {
                currentPoint = data.point;
                const pointEl = document.getElementById("available-point");
                // Trang không có pointEl thì bỏ qua, dùng chung JS cho nhiều view.
                if (pointEl) pointEl.textContent = Number(data.point).toLocaleString("vi-VN");
            }
            allVouchers = data.data;

            renderVoucherStock(allVouchers);
        })
        .catch(err => {
            // Lỗi network/server render error state thay vì để danh sách trống.
            console.error(err);
            voucherList.innerHTML = `<div class="col-12 text-center text-danger py-5">Đã có lỗi xảy ra.</div>`;
        });
}

function renderVoucherStock(vouchers) {
    const voucherList = document.getElementById("voucher-list");

    // Không có voucher phù hợp filter/search thì hiển thị empty state.
    if (!vouchers || vouchers.length === 0) {
        voucherList.innerHTML = `<div class="col-12 text-center text-muted py-5">Không tìm thấy voucher nào.</div>`;
        return;
    }

    voucherList.innerHTML = vouchers.map(voucher => {
        const discountText = voucher.discountType === "PERCENT"
            ? `-${voucher.discountValue}%`
            : `-${voucher.discountValue.toLocaleString("vi-VN")}đ`;

        const pointText = voucher.exchangePoints > 0
            ? `⭐ ${voucher.exchangePoints.toLocaleString("vi-VN")} điểm`
            : "Miễn phí";

        const description = voucher.discountType === "PERCENT"
            ? `Giảm ${voucher.discountValue}% cho đơn đặt sân.`
            : `Giảm ${voucher.discountValue.toLocaleString("vi-VN")}đ cho đơn đặt sân.`;

        const remain = voucher.quantity - voucher.used;
        const endDate = formatDate(voucher.endDate);
        // Không đủ điểm thì disable nút đổi ngay trên client; server vẫn kiểm lại.
        const disabled = voucher.exchangePoints > currentPoint;

        return `
            <div class="col-lg-4 col-md-6 mb-4">
                <div class="voucher-card">
                    <div class="voucher-banner">
                        <div class="voucher-value">${discountText}</div>
                        <div class="voucher-cost">${pointText}</div>
                    </div>
                    <div class="voucher-body">
                        <h5>${escapeHtml(voucher.name)}</h5>
                        <p class="text-muted small">${description}</p>
                        <ul class="small text-muted ps-3">
                            <li>Đơn tối thiểu ${voucher.minOrder.toLocaleString("vi-VN")}đ</li>
                            <li>HSD: ${endDate}</li>
                        </ul>
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <span class="badge bg-primary">${escapeHtml(voucher.code)}</span>
                            <small class="text-success">Còn ${remain} voucher</small>
                        </div>
                        <button
                            class="btn btn-success exchange-btn"
                            data-id="${voucher.id}"
                            data-name="${escapeHtml(voucher.name)}"
                            data-point="${voucher.exchangePoints}"
                            data-bs-toggle="modal"
                            data-bs-target="#exchangeModal"
                            ${disabled ? "disabled" : ""}
                        >
                            ${disabled ? "Không đủ điểm" : "Đổi ngay"}
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join("");
}

function loadMyVoucher(status = "ALL") {
    const listEl = document.getElementById("myVoucherList");
    listEl.innerHTML = `<div class="col-12 text-center text-muted py-5">Đang tải...</div>`;

    fetch(`${ctx}/vouchers-api?to=owned&status=${encodeURIComponent(status)}`)
        .then(res => {
            // API owned trả lỗi HTTP khi session hết hạn hoặc lỗi server.
            if (!res.ok) throw new Error("Network error " + res.status);
            return res.json();
        })
        .then(data => {
            // success=false hiển thị message từ backend.
            if (!data.success) {
                listEl.innerHTML = `<div class="col-12 text-center text-danger py-5">${escapeHtml(data.message || "Không thể tải dữ liệu.")}</div>`;
                return;
            }
            // Cập nhật điểm thưởng trên trang Voucher của tôi.
            if (typeof data.point !== "undefined") {
                currentPoint = data.point;
                const pointDisplay = document.getElementById("userPointDisplay");
                if (pointDisplay) {
                    pointDisplay.textContent = Number(data.point).toLocaleString("vi-VN");
                }
            }
            renderMyVoucher(data.data);
        })
        .catch(err => {
            // Lỗi fetch được hiển thị bằng message chung cho Customer.
            console.error(err);
            listEl.innerHTML = `<div class="col-12 text-center text-danger py-5">Đã có lỗi xảy ra, vui lòng thử lại.</div>`;
        });
}

function buildDescription(v) {
    const discount = v.discountType === "PERCENT"
        ? `Giảm ${v.discountValue}%`
        : `Giảm ${v.discountValue.toLocaleString()}đ`;
    const condition = v.minOrder ? ` cho đơn từ ${v.minOrder.toLocaleString()}đ` : "";
    return discount + condition;
}

function renderMyVoucher(vouchers) {
    const listEl = document.getElementById("myVoucherList");
    const countEl = document.getElementById("voucherCount");

    countEl.textContent = vouchers.length;

    // Không có voucher ở filter hiện tại thì hiển thị empty state.
    if (!vouchers || vouchers.length === 0) {
        listEl.innerHTML = `<div class="col-12 text-center text-muted py-5">Không có voucher nào trong mục này.</div>`;
        return;
    }

    listEl.innerHTML = vouchers.map(v => {
        const s = statusLabel[v.effectiveStatus] || {text: v.effectiveStatus, badge: "bg-light text-dark"};
        const dimClass = v.effectiveStatus !== "AVAILABLE" ? "opacity-50" : "";
        // Voucher RESERVED hiển thị booking đang giữ để Customer hiểu vì sao chưa dùng được.
        const reservedText = v.effectiveStatus === "RESERVED" && v.reservedBookingCode
            ? `<p class="small text-warning mb-0">Đang giữ cho booking: ${escapeHtml(v.reservedBookingCode)}</p>`
            : "";

        return `
            <div class="col-md-4">
                <div class="card h-100 ${dimClass}">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <h5 class="card-title mb-0">${escapeHtml(v.voucherName)}</h5>
                            <span class="badge ${s.badge}">${s.text}</span>
                        </div>
                        <p class="card-text small mb-1"><strong>${escapeHtml(buildDescription(v))}</strong></p>
                        <p class="card-text text-muted small">Mã: ${escapeHtml(v.voucherCode)}</p>
                        <p class="small text-muted mb-0">HSD: ${formatDate(v.expiredAt)}</p>
                        ${reservedText}
                        ${v.usedAt ? `<p class="small text-muted mb-0">Đã dùng lúc: ${formatDate(v.usedAt)}</p>` : ""}
                    </div>
                </div>
            </div>
        `;
    }).join("");
}

document.addEventListener("DOMContentLoaded", function () {
    const voucherFilterEl = document.getElementById("voucherFilter");
    // Bộ lọc trang Voucher của tôi theo effective_status.
    if (voucherFilterEl) {
        voucherFilterEl.addEventListener("click", function (e) {
            const btn = e.target.closest("button[data-status]");
            // Click ngoài button filter thì bỏ qua.
            if (!btn) return;

            this.querySelectorAll(".nav-link").forEach(el => el.classList.remove("active"));
            btn.classList.add("active");

            loadMyVoucher(btn.dataset.status);
        });
    }

    const voucherTypeFilterEl = document.getElementById("voucherTypeFilter");
    // Bộ lọc kho voucher theo ALL/MEMBER cho Customer VIP.
    if (voucherTypeFilterEl) {
        voucherTypeFilterEl.addEventListener("click", function (e) {
            const btn = e.target.closest("button[data-type]");
            // Click ngoài button type filter thì bỏ qua.
            if (!btn) return;

            this.querySelectorAll(".filter-btn").forEach(el => {
                el.classList.remove("active", "btn-success", "btn-warning");
                el.classList.add("btn-outline-success");
            });
            btn.classList.remove("btn-outline-success");
            btn.classList.add("active", btn.dataset.type === "MEMBER" ? "btn-warning" : "btn-success");

            if (searchInputEl) searchInputEl.value = ""; // reset search khi đổi tab
            loadVoucherStock(btn.dataset.type);
        });
    }

    document.addEventListener("click", function (e) {
        // Chỉ xử lý khi Customer bấm nút đổi voucher.
        if (!e.target.classList.contains("exchange-btn")) return;
        const btn = e.target;
        const voucherId = btn.dataset.id;
        const voucherName = btn.dataset.name;
        const exchangePoint = Number(btn.dataset.point);

        document.getElementById("voucherName").textContent = `Bạn có chắc chắn muốn đổi "${voucherName}" không?`;
        document.getElementById("exchangePoint").textContent = exchangePoint.toLocaleString("vi-VN");
        document.getElementById("remainPoint").textContent = (currentPoint - exchangePoint).toLocaleString("vi-VN");
        document.getElementById("confirmExchangeBtn").dataset.id = voucherId;
    });

    const confirmBtn = document.getElementById("confirmExchangeBtn");
    // Modal xác nhận chỉ tồn tại ở trang Voucher Center.
    if (confirmBtn) {
        confirmBtn.addEventListener("click", function () {
            const voucherId = this.dataset.id; // lấy từ dataset đã set ở bước (1)
            const btn = this;

            btn.disabled = true;

            const params = new URLSearchParams();
            params.append("action", "redeem");
            params.append("voucherId", voucherId);

            fetch(`${ctx}/vouchers`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                body: params.toString()
            })
                .then(res => res.json())
                .then(data => {
                    // Thành công reload để cập nhật điểm và số lượng còn lại.
                    if (data.success) {
                        alert(data.message || "Đổi voucher thành công.");
                        location.reload();
                    } else {
                        // Thất bại nghiệp vụ hiển thị message từ backend.
                        alert(data.message || "Không thể đổi voucher.");
                    }
                })
                .catch(err => {
                    // Lỗi network/server trả alert chung.
                    console.error(err);
                    alert("Đã có lỗi xảy ra, vui lòng thử lại sau.");
                })
                .finally(() => {
                    // Luôn bật lại nút sau khi request kết thúc.
                    btn.disabled = false;
                });
        });
    }

    const searchInputEl = document.getElementById("voucherSearchInput");
    // Search chỉ tồn tại ở trang Voucher Center.
    if (searchInputEl) {
        let debounceTimer;
        searchInputEl.addEventListener("input", function () {
            clearTimeout(debounceTimer);
            const keyword = this.value.trim().toLowerCase();

            // Debounce để không render lại danh sách sau mỗi phím quá dày.
            debounceTimer = setTimeout(() => {
                const filtered = allVouchers.filter(v =>
                    v.name.toLowerCase().includes(keyword) ||
                    v.code.toLowerCase().includes(keyword)
                );
                renderVoucherStock(filtered);
            }, 250);
        });
    }
});
