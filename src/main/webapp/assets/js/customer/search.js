// Lưu đường context của trang
const ctx = window.APP_CTX;

function renderData(data) {
    const container = document.getElementById("list-container");
    if (!container) return;

    const html = data.map(item => `
        <div class="col-12 mb-3">
            <div class="card soft-card">
                <div class="row g-0 h-100">
                    <div class="col-md-4">
                        <img src="${item.thumbnailUrl || (ctx + '/assets/images/default-field.jpg')}"
                             class="img-fluid w-100 h-100 object-fit-cover"
                             alt="Sân bóng">
                    </div>
                    <div class="col-md-8">
                        <div class="card-body d-flex flex-column">
                            <div class="d-flex justify-content-between">
                                <h5 class="card-title">${item.complexName}</h5>
                            </div>
                            <p class="text-muted mb-3">
                                <i class="bi bi-geo-alt"></i>
                                ${item.address}, ${item.ward}, ${item.city}
                            </p>
                            <div class="mb-3 d-flex">
                                ${(item.fieldTypeList || []).map(field => `
                                    <span class="complex-item me-1">${field.typeName}</span>
                                `).join("")}
                            </div>
                            
                            ${item.currentPrice != null ? `
                                <p class="text-success fw-bold mb-3" style="font-size: 1.1rem;">
                                    <i class="bi bi-cash me-1"></i> Giá lúc này: ${new Intl.NumberFormat('vi-VN').format(item.currentPrice)}đ/giờ
                                    <br><small class="text-muted fw-normal" style="font-size: 0.8rem;">* Giá có thể thay đổi theo khung giờ đặt</small>
                                </p>
                            ` : `
                                <p class="text-success fw-bold mb-3">
                                    <i class="bi bi-cash me-1"></i> Chưa có giá
                                </p>
                            `}

                            <div class="mt-auto d-flex justify-content-end">
                                <a class="btn btn-outline-success me-2" href="${ctx}/field-details?id=${item.complexId}">
                                    Xem chi tiết
                                </a>
                                <a class="btn btn-sf-primary" href="${ctx}/booking?action=create&complexId=${item.complexId}">
                                    Đặt ngay
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `).join("");

    const fieldCountEl = document.getElementById("fieldCount");
    if (fieldCountEl) {
        fieldCountEl.textContent = `Tìm thấy ${data.length} sân phù hợp.`;
    }
    container.innerHTML = html;
}

function loadData() {
    fetch(`${ctx}/field-list`)
        .then(res => res.json())
        .then(renderData)
        .catch(err => console.error("Lỗi khi load dữ liệu mặc định:", err));
}

function searchData() {
    const params = new URLSearchParams();

    const complexNameEl = document.getElementById("complexName");
    const typeEl = document.getElementById("fieldType");
    const sortOrderEl = document.getElementById("sortOrder");

    const complexName = complexNameEl ? complexNameEl.value.trim() : "";
    const fieldTypeId = typeEl ? typeEl.value : "";
    const sortOrder = sortOrderEl ? sortOrderEl.value : "";

    if (complexName) params.append("complexName", complexName);
    if (fieldTypeId) params.append("fieldTypeId", fieldTypeId);
    if (sortOrder) params.append("sortOrder", sortOrder);

    fetch(`${ctx}/field-list?${params.toString()}`)
        .then(res => res.json())
        .then(renderData)
        .catch(err => console.error("Lỗi khi tìm kiếm dữ liệu:", err));
}

// Khởi tạo trang: tải danh sách filter một lần duy nhất, sau đó kiểm tra URL params
async function initPage() {
    // Đọc query parameter
    const urlParams = new URLSearchParams(window.location.search);
    const urlType = urlParams.get("type");
    const urlComplexName = urlParams.get("complexName");

    const typeEl = document.getElementById("fieldType");
    const complexNameEl = document.getElementById("complexName");

    let hasParams = false;

    if (urlType && typeEl) {
        typeEl.value = urlType;
        hasParams = true;
    }
    
    if (urlComplexName && complexNameEl) {
        complexNameEl.value = urlComplexName;
        hasParams = true;
    }

    // Load dữ liệu
    if (hasParams) {
        searchData();
    } else {
        loadData();
    }
}

// Chạy khởi tạo duy nhất 1 lần (ĐÃ BỎ các cuộc gọi hàm loadProvinces() và loadWards() riêng lẻ ở ngoài)
document.addEventListener("DOMContentLoaded", initPage);

let filterTimer;

function scheduleLoadData() {
    clearTimeout(filterTimer);

    filterTimer = setTimeout(() => {
        searchData();
    }, 500);
}

function clearAll() {
    document.querySelectorAll("#complexName, #fieldType")
        .forEach(el => {
            if (el.tagName === "INPUT") {
                el.value = "";
            } else if (el.tagName === "SELECT") {
                el.selectedIndex = 0;
            }
        });

    loadData();
}