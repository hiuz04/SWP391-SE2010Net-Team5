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
            if (!data.success) {
                voucherList.innerHTML = `<div class="col-12 text-center text-danger py-5">${data.message || "Không thể tải dữ liệu."}</div>`;
                return;
            }

            if (typeof data.point !== "undefined") {
                currentPoint = data.point;
                const pointEl = document.getElementById("available-point");
                if (pointEl) pointEl.textContent = Number(data.point).toLocaleString("vi-VN");
            }
            allVouchers = data.data;

            renderVoucherStock(allVouchers);
        })
        .catch(err => {
            console.error(err);
            voucherList.innerHTML = `<div class="col-12 text-center text-danger py-5">Đã có lỗi xảy ra.</div>`;
        });
}

function renderVoucherStock(vouchers) {
    const voucherList = document.getElementById("voucher-list");

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
            if (!res.ok) throw new Error("Network error " + res.status);
            return res.json();
        })
        .then(data => {
            if (!data.success) {
                listEl.innerHTML = `<div class="col-12 text-center text-danger py-5">${escapeHtml(data.message || "Không thể tải dữ liệu.")}</div>`;
                return;
            }
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

    if (!vouchers || vouchers.length === 0) {
        listEl.innerHTML = `<div class="col-12 text-center text-muted py-5">Không có voucher nào trong mục này.</div>`;
        return;
    }

    listEl.innerHTML = vouchers.map(v => {
        const s = statusLabel[v.effectiveStatus] || {text: v.effectiveStatus, badge: "bg-light text-dark"};
        const dimClass = v.effectiveStatus !== "AVAILABLE" ? "opacity-50" : "";
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
    if (voucherFilterEl) {
        voucherFilterEl.addEventListener("click", function (e) {
            const btn = e.target.closest("button[data-status]");
            if (!btn) return;

            this.querySelectorAll(".nav-link").forEach(el => el.classList.remove("active"));
            btn.classList.add("active");

            loadMyVoucher(btn.dataset.status);
        });
    }

    const voucherTypeFilterEl = document.getElementById("voucherTypeFilter");
    if (voucherTypeFilterEl) {
        voucherTypeFilterEl.addEventListener("click", function (e) {
            const btn = e.target.closest("button[data-type]");
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
                    if (data.success) {
                        alert(data.message || "Đổi voucher thành công.");
                        location.reload();
                    } else {
                        alert(data.message || "Không thể đổi voucher.");
                    }
                })
                .catch(err => {
                    console.error(err);
                    alert("Đã có lỗi xảy ra, vui lòng thử lại sau.");
                })
                .finally(() => {
                    btn.disabled = false;
                });
        });
    }

    const searchInputEl = document.getElementById("voucherSearchInput");
    if (searchInputEl) {
        let debounceTimer;
        searchInputEl.addEventListener("input", function () {
            clearTimeout(debounceTimer);
            const keyword = this.value.trim().toLowerCase();

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
