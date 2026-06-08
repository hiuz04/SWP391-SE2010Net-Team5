// Lưu đường context của trang
const ctx = "/SWP391-SE2010Net-Team5";

function renderData(data) {
    const container = document.getElementById("list-container");
    const html = data.map(item => `
        <div class="col-12 mb-3">
            <div class="card soft-card">
                <div class="row g-0" style="max-height: 180px">

                    <div class="col-md-4">
                        <img
                            src="https://res.cloudinary.com/du02dvkx7/image/upload/v1780892069/court_a6at9n.webp"
                            class="img-fluid w-100 object-fit-cover"
                            alt="Sân bóng">
                    </div>

                    <div class="col-md-8">
                        <div class="card-body d-flex flex-column">

                            <div class="d-flex justify-content-between">
                                <h5 class="card-title">
                                    ${item.facilityName}
                                </h5>
                            </div>

                            <p class="text-muted mb-3">
                                <i class="bi bi-geo-alt"></i>
                                ${item.address},
                                ${item.ward},
                                ${item.city}
                            </p>

                            <div class="mb-3 d-flex">
                                ${(item.fieldTypeList || [])
                                .map(field => `
                                        <span class="facility-item me-1">
                                            ${field.typeName}
                                        </span>
                                    `)
                                .join("")}
                            </div>

                            <div class="mt-auto d-flex justify-content-end">
                                <a class="btn btn-sf-primary"
                                   href="${ctx}/field-details?id=${item.facilityId}">
                                    Đặt ngay
                                </a>
                            </div>

                        </div>
                    </div>

                </div>
            </div>
        </div>
    `).join("");

    document.getElementById("fieldCount").textContent =
        `Tìm thấy ${data.length} sân phù hợp.`;

    container.innerHTML = html;
}

function loadData() {
    fetch(`${ctx}/field-list`)
        .then(res => res.json())
        .then(renderData);
}
loadData()

async function loadProvinces() {
    const response = await fetch(`${ctx}/cities`);
    const cities = await response.json();
    const provinceSelect = document.getElementById("province");

    provinceSelect.innerHTML =
        `<option value="">Chọn tỉnh/thành</option>`;

    cities.forEach(city => {
        provinceSelect.innerHTML += `
            <option value="${city}">
                ${city}
            </option>
        `;
    });
}
loadProvinces();

async function loadWards() {
    const response = await fetch(`${ctx}/wards`);
    const wards = await response.json();
    const wardSelect = document.getElementById("ward");

    wardSelect.innerHTML =
        `<option value="">Chọn phường/xã</option>`;

    wards.forEach(ward => {
        wardSelect.innerHTML += `
            <option value="${ward}">
                ${ward}
            </option>
        `;
    });
}
loadWards();

function searchData() {

    const params = new URLSearchParams();

    const province = document.getElementById("province").value;
    const ward = document.getElementById("ward").value;
    const fieldTypeId = document.getElementById("type").value;

    if (province) {
        params.append("province", province);
    }

    if (ward) {
        params.append("ward", ward);
    }

    if (fieldTypeId) {
        params.append("fieldTypeId", fieldTypeId);
    }

    fetch(`${ctx}/field-list?${params.toString()}`)
        .then(res => res.json())
        .then(renderData);
}