// Sử dụng context path động từ JSP, nếu không có mới fallback về mặc định
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

async function loadProvinces() {
    try {
        const response = await fetch(`${ctx}/cities`);
        const cities = await response.json();
        const provinceSelect = document.getElementById("province");
        if (!provinceSelect) return;

        // Xây dựng chuỗi HTML và gán DOM một lần duy nhất
        const optionsHtml = cities.map(city => `
            <option value="${city}">${city}</option>
        `).join("");

        provinceSelect.innerHTML = `<option value="">Tất cả tỉnh thành</option>` + optionsHtml;
    } catch (err) {
        console.error("Lỗi khi load danh sách tỉnh/thành:", err);
    }
}

async function loadWards() {
    try {
        const response = await fetch(`${ctx}/wards`);
        const wards = await response.json();
        const wardSelect = document.getElementById("ward");
        if (!wardSelect) return;

        // Xây dựng chuỗi HTML và gán DOM một lần duy nhất
        const optionsHtml = wards.map(ward => `
            <option value="${ward}">${ward}</option>
        `).join("");

        wardSelect.innerHTML = `<option value="">Tất cả xã phường</option>` + optionsHtml;
    } catch (err) {
        console.error("Lỗi khi load danh sách phường/xã:", err);
    }
}

function searchData() {
    const params = new URLSearchParams();

    const provinceEl = document.getElementById("province");
    const wardEl = document.getElementById("ward");
    const typeEl = document.getElementById("type");
    const sortOrderEl = document.getElementById("sortOrder");

    const province = provinceEl ? provinceEl.value : "";
    const ward = wardEl ? wardEl.value : "";
    const fieldTypeId = typeEl ? typeEl.value : "";
    const sortOrder = sortOrderEl ? sortOrderEl.value : "";

    if (province) params.append("province", province);
    if (ward) params.append("ward", ward);
    if (fieldTypeId) params.append("fieldTypeId", fieldTypeId);
    if (sortOrder) params.append("sortOrder", sortOrder);

    fetch(`${ctx}/field-list?${params.toString()}`)
        .then(res => res.json())
        .then(renderData)
        .catch(err => console.error("Lỗi khi tìm kiếm dữ liệu:", err));
}

// Khởi tạo trang: tải danh sách filter một lần duy nhất, sau đó kiểm tra URL params
async function initPage() {
    // Đợi tải xong cả 2 select box
    await Promise.all([
        loadProvinces(),
        loadWards()
    ]);

    // Kiểm tra query parameter trên URL (từ trang chủ chuyển sang)
    const urlParams = new URLSearchParams(window.location.search);
    const urlProvince = urlParams.get("province");
    const urlWard = urlParams.get("ward");
    const urlType = urlParams.get("type");

    let hasParams = false;

    const provinceEl = document.getElementById("province");
    const wardEl = document.getElementById("ward");
    const typeEl = document.getElementById("type");

    if (urlProvince && provinceEl) {
        provinceEl.value = urlProvince;
        hasParams = true;
    }
    if (urlWard && wardEl) {
        wardEl.value = urlWard;
        hasParams = true;
    }
    if (urlType && typeEl) {
        typeEl.value = urlType;
        hasParams = true;
    }

    if (hasParams) {
        // Có ít nhất 1 param từ trang chủ -> trigger tìm kiếm theo bộ lọc
        searchData();
    } else {
        // Load mặc định
        loadData();
    }
}

// Chạy khởi tạo duy nhất 1 lần (ĐÃ BỎ các cuộc gọi hàm loadProvinces() và loadWards() riêng lẻ ở ngoài)
document.addEventListener("DOMContentLoaded", initPage);