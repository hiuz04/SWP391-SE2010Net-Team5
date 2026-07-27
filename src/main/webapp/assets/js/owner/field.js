/**
 * Module: Field Management
 * File: field.js
 * Description: Xử lý các chức năng CRUD và gọi API cho module cụm sân (Field).
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Tách biệt logic quản lý cụm sân từ file chung thành file độc lập để dễ dàng bảo trì.
 */
const ctx = window.APP_CTX || "";

// Danh sách status
const statusList = [
    {status: "AVAILABLE", display: "Có sẵn", badge: "badge-soft-success"},
    {status: "INACTIVE", display: "Ngừng hoạt động", badge: "badge-soft-danger"},
    {status: "MAINTENANCE", display: "Bảo trì", badge: "badge-soft-warning"},
    {status: "REMOVED", display: "Loại bỏ", badge: "badge-soft-danger"}
];

function getComplex() {
    const currentComplexId = Number(new URLSearchParams(window.location.search).get("complexId")) || 0;

    fetch(`${ctx}/api/complexes`)
        .then(res => res.json())
        .then(data => {

            const select = document.getElementById("complexSelect");

            select.innerHTML = "";

            data.forEach(complex => {

                const option = document.createElement("option");

                option.value = complex.complex.complexId;
                option.textContent = `🏟️ ${complex.complex.complexName}`;

                if (complex.complex.complexId === currentComplexId) {
                    option.selected = true;
                }

                select.appendChild(option);
            });

        })
        .catch(err => console.error(err));
}

function changeComplex(complexId) {
    location.href = `${ctx}/owner/field?complexId=${complexId}`;
}

// Lấy danh sách sân
function loadData() {
    const currentComplexId =
        new URLSearchParams(window.location.search).get("complexId");

    const keyword = document.getElementById("keyword").value.trim();
    const status = document.getElementById("status").value;
    const fieldType = document.getElementById("fieldType").value;

    const params = new URLSearchParams();
    params.append("complexId", currentComplexId);
    if (keyword) params.append("keyword", keyword);
    if (status)  params.append("status", status);
    if (fieldType) params.append("fieldTypeId", fieldType);

    fetch(`${ctx}/owner/api/fields?${params.toString()}`)
        .then(res => res.json())
        .then(data => {
            // Update count pill
            const fieldCount = document.getElementById("field-count");
            fieldCount.innerHTML = `
                <i class="bi bi-dribbble"></i>
                <strong>${data.length}</strong> sân
            `;

            const container = document.getElementById("field-data-container");

            if (data.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <i class="bi bi-dribbble"></i>
                        <p>Chưa có sân bóng nào trong cụm sân này.</p>
                    </div>
                `;
                return;
            }

            let html = `
                <table class="table">
                    <thead>
                    <tr>
                        <th style="width:22%">Tên sân</th>
                        <th style="width:13%">Loại sân</th>
                        <th style="width:30%">Mô tả</th>
                        <th class="text-center" style="width:14%">Trạng thái</th>
                        <th class="text-center" style="width:9%">Hot</th>
                        <th class="text-center" style="width:12%">Hành động</th>
                    </tr>
                    </thead>
                    <tbody>
            `;

            data.forEach(item => {
                const statusDisplay = statusList.find(s => s.status === item.status)?.display ?? 'Không xác định';
                const statusBadge   = statusList.find(s => s.status === item.status)?.badge  ?? 'badge-soft-secondary';

                html += `
                    <tr>
                        <td>
                            <div class="field-name-cell">
                                <div class="fw-semibold">${item.fieldName}</div>
                                <small>ID: #${item.fieldId}</small>
                            </div>
                        </td>

                        <td>
                            <span class="field-type-badge">${item.type}</span>
                        </td>

                        <td>
                            <div class="description">${item.description ?? '—'}</div>
                        </td>

                        <td class="text-center">
                            <div class="dropdown">
                                <button class="status-dropdown-btn ${statusBadge} dropdown-toggle"
                                        type="button"
                                        data-bs-toggle="dropdown"
                                        aria-expanded="false">
                                    <span class="status-dot" style="background:currentColor"></span>
                                    ${statusDisplay}
                                </button>
                                <ul class="dropdown-menu shadow-sm" style="border-radius:12px;min-width:160px;padding:6px 0">
                                    <li><a class="dropdown-item" href="#" onclick="updateFieldStatus(${item.fieldId}, 'AVAILABLE')">✅ Khả dụng</a></li>
                                    <li><a class="dropdown-item" href="#" onclick="updateFieldStatus(${item.fieldId}, 'INACTIVE')">🔴 Tạm ngưng</a></li>
                                    <li><a class="dropdown-item" href="#" onclick="updateFieldStatus(${item.fieldId}, 'MAINTENANCE')">🔵 Bảo trì</a></li>
                                </ul>
                            </div>
                        </td>

                        <td class="text-center">
                            <button
                                class="hot-toggle-btn ${item.isHot ? 'is-hot' : ''}"
                                title="${item.isHot ? 'Bỏ đánh dấu hot' : 'Đánh dấu hot'}"
                                onclick="toggleHotStatus(${item.fieldId}, ${item.isHot})">
                                <i class="bi bi-star-fill"></i>
                            </button>
                        </td>

                        <td class="text-center">
                            <div class="d-flex align-items-center justify-content-center gap-2">
                                <button class="action-btn action-btn-edit"
                                        title="Chỉnh sửa"
                                        onclick="openModalToEdit(${item.fieldId})">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button class="action-btn action-btn-delete"
                                        title="Xóa"
                                        onclick="deleteField(${item.fieldId})">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            });

            html += `</tbody></table>`;
            container.innerHTML = html;
        })
        .catch(err => console.error('Field load error:', err));
}

function loadFieldTypeDataForSearch() {
    return fetch(`${ctx}/owner/api/field-type`)
        .then(response => response.json())
        .then(data => {

            const select = document.getElementById("fieldType");

            // Giá trị đang được chọn (nếu có)
            const selectedValue = select.value;

            select.innerHTML = `
                <option value="">Tất cả loại sân</option>
            `;

            data.forEach(type => {

                const option = document.createElement("option");

                option.value = type.fieldTypeId;
                option.textContent = type.typeName;

                // Giữ lại option đã chọn
                if (String(type.fieldTypeId) === selectedValue) {
                    option.selected = true;
                }

                select.appendChild(option);
            });

        });
}

// Lấy danh sách loại sân
function loadFieldTypeData() {
    return fetch(`${ctx}/owner/api/field-type`)
        .then(response => response.json())
        .then(data => {
            const select = document.getElementById("typeF");

            select.innerHTML = `<option value="">-- Chọn loại sân --</option>`;

            data.forEach(type => {
                select.innerHTML += `
                    <option value="${type.fieldTypeId}">
                        ${type.typeName}
                    </option>
                `;
            });
        });
}

// Lấy dữ liệu Sân
function getFieldData(id) {
    fetch(`${ctx}/owner/field?action=get&id=` + id)
        .then(res => res.json())
        .then(data => {
            document.getElementById("fieldID").value =
                data.fieldId;

            document.getElementById("fieldName").value =
                data.fieldName;

            document.getElementById("desc").value =
                data.description ?? "";

            document.getElementById("typeF").value =
                String(data.fieldTypeId);
        })
}

// Mở Field Modal để Add Field
async function openModal() {
    const response =
        await fetch(`${ctx}/owner/field-form`);

    const html = await response.text();

    document.getElementById("modal").innerHTML = html;

    await loadFieldTypeData();
    document.getElementById("fieldTitle").innerHTML = "Thêm sân bóng mới"
    document.getElementById("submitBtn").innerHTML = "Thêm mới"

    const modalElement =
        document.getElementById("fieldFormModal");

    const modal =
        new bootstrap.Modal(modalElement);

    modal.show();
}

// Mở Field Modal để Edit Field
async function openModalToEdit(id) {
    const response =
        await fetch(`${ctx}/owner/field-form`);

    const html = await response.text();

    document.getElementById("modal").innerHTML = html;

    Promise.all([
        loadFieldTypeData(),
    ]).then(() => {
        getFieldData(id);
    });

    document.getElementById("fieldTitle").innerHTML = "Chỉnh sửa sân bóng"
    document.getElementById("submitBtn").innerHTML = "Lưu thay đổi"

    const modalElement =
        document.getElementById("fieldFormModal");

    const modal =
        new bootstrap.Modal(modalElement);

    modal.show();
}

// Xử lý form submit
function submitField() {
    const currentComplexId = Number(new URLSearchParams(window.location.search).get("complexId")) || 0;

    const id = document.getElementById("fieldID").value;

    let url = !id
        ? `${ctx}/owner/field?action=add`
        : `${ctx}/owner/field?action=edit`;

    const data = {
        fieldID: id,
        fieldName: document.getElementById("fieldName").value,
        description: document.getElementById("desc").value,
        fieldTypeID: document.getElementById("typeF").value,
        complexId: currentComplexId
    };

    let errors = [];

    if (!data.fieldName) errors.push("Vui lòng nhập Tên sân bóng!");
    if (data.fieldTypeID === "") errors.push("Vui lòng chọn Loại sân bóng!");

    if (errors.length > 0) {
        alert(errors.join("\n"));
        return;
    }

    fetch(url, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: new URLSearchParams(data)
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Server error");
            }
            return res.text();
        })
        .then(() => location.reload())
        .catch(err => {
            console.error(err);
            alert("Không thêm/sửa được, kiểm tra server!");
        });
}

function updateFieldStatus(fieldId, status) {
    // const currentComplexId = Number(new URLSearchParams(window.location.search).get("complexId")) || 0;

    const params = new URLSearchParams();
    params.append("fieldId", fieldId);
    params.append("status", status);

    fetch(`${ctx}/owner/field?action=status`, {
        method: "POST",
        body: params
    })
        .then(async res => {
            if (!res.ok) {
                throw new Error(await res.text());
            }
            return res.text();
        })
        .then(() => {
            location.reload();
        })
        .catch(err => {
            console.error(err);
            alert(err.message);
        });
}

// Xóa Field
function deleteField(id) {
    const confirmed = window.confirm("Xóa dữ liệu sẽ làm mất toàn bộ thông tin liên quan đến sân bóng và không thể khôi phục. Bạn có muốn tiếp tục?");

    if (!confirmed) return;

    fetch(`${ctx}/owner/field?action=delete&id=${id}`, {
        method: "POST"
    })
        .then(async res => {
            if (!res.ok) {
                const message = await res.text();
                throw new Error(message);
            }
            location.reload();
        })
        .catch(err => {
            console.error(err);
            alert(err.message);
        });
}

// Đảm bảo không còn để lại giá trị sau khi đóng modal
document.addEventListener("DOMContentLoaded", function () {
    const modalContainer = document.getElementById("fieldFormModal");
    if (modalContainer) {
        modalContainer.addEventListener("hidden.bs.modal", function () {
            document.getElementById("fieldID").value = ""

            document.getElementById("fieldName").value = ""

            document.getElementById("desc").value = ""

            document.getElementById("typeF").value = ""

            document.getElementById("fc").value = ""
        });
    }
});

// Toggle Sân Hot
function toggleHotStatus(fieldId, currentStatus) {
    const newStatus = !currentStatus;

    fetch(`${ctx}/owner/field/toggle-hot`, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: `fieldId=${fieldId}&isHot=${newStatus}`
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Cập nhật trạng thái thất bại");
            }
            return res.text();
        })
        .then(() => loadData()) // Reload data
        .catch(err => {
            console.error(err);
            alert("Có lỗi xảy ra khi cập nhật trạng thái HOT!");
        });
}

let filterTimer;

function scheduleLoadData() {
    clearTimeout(filterTimer);

    filterTimer = setTimeout(() => {
        loadData();
    }, 500);
}