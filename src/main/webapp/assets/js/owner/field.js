/**
 * Module: Field Management
 * File: field.js
 * Description: Xử lý các chức năng CRUD và gọi API cho module cơ sở (Field).
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Tách biệt logic quản lý cơ sở từ file chung thành file độc lập để dễ dàng bảo trì.
 */

// Lưu đường context của trang
const ctx = "/SWP391-SE2010Net-Team5";

// Danh sách status
const statusList = [
    { status: "AVAILABLE", display: "Có sẵn", badge: "badge-soft-success" },
    { status: "OCCUPIED", display: "Đã được chọn", badge: "badge-soft-info" },
    { status: "BOOKED", display: "Đã đặt", badge: "badge-soft-primary" },
    { status: "INACTIVE", display: "Ngừng hoạt động", badge: "badge-soft-secondary" },
    { status: "MAINTENANCE", display: "Bảo trì", badge: "badge-soft-danger" }
];

// Lấy danh sách sân
function loadData(){
    fetch(`${ctx}/api/fields`)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("field-data-container");

            let html = "";

            if (data.length > 0){
                html += `
                <table class="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th style="width: 10%">Tên sân</th>
                        <th style="width: 7%">Loại sân</th>
                        <th style="width: 20%">Cơ sở</th>
                        <th style="width: 42%">Mô tả</th>
                        <th>Trạng thái</th>
                        <th class="text-center">Sân Hot</th>
                        <th class="text-center">Hành động</th>
                      </tr>
                    </thead>
                    
                    <tbody>
`
                data.forEach(item => {
                    const statusDisplay = statusList.find(s => s.status === item.status)?.display
                        ?? "Không xác định";
                    const statusBadgge = statusList.find(s => s.status === item.status)?.badge
                        ?? "badge-soft-secondary";
                    html += `
                      <tr>
                        <td>${item.fieldName}</td>
                        <td style="color: grey;">${item.type}</td>
                        <td>${item.facilityName} sân</td>
                        <td>${item.description}</td>
                        <td>
                            <button class="badge ${statusBadgge} border-0" onclick="">
                                ${statusDisplay}
                            </button>
                        </td>
                        <td class="text-center">
                            <button class="btn btn-sm ${item.hot ? 'text-warning' : 'text-secondary'}" onclick="toggleHotStatus(${item.fieldId}, ${item.hot})">
                                <i class="bi bi-star-fill fs-5"></i>
                            </button>
                        </td>
                        <td class="text-center">
                          <button class="btn btn-sm" onclick="openModalToEdit(${item.fieldId})"><img class="icon" alt="editIcon" src="${ctx}/assets/images/icon/editIcon.png"></button>
                          <button class="btn btn-sm" onclick="deleteField(${item.fieldId})"><img class="icon" alt="deleteIcon" src="${ctx}/assets/images/icon/deleteIcon.png"></button>
                        </td>
                      </tr>
                `
                })
            } else {
                html += `
                    <div>Không có dữ liệu</div>
                `
            }
            container.innerHTML = html;
        })

}

// Lấy danh sách loại sân
function loadFieldTypeData() {
    return fetch(`${ctx}/api/field-type`)
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

// Lấy danh sách cơ sở
function loadFacilityData() {
    return fetch(`${ctx}/api/facilities`)
        .then(response => response.json())
        .then(data => {
            const select = document.getElementById("fac");

            select.innerHTML = `<option value="">-- Chọn cơ sở --</option>`;

            data.forEach(fac => {
                select.innerHTML += `
                    <option value="${fac.facility.facilityId}">
                        ${fac.facility.facilityName}
                    </option>
                `;
            });
        });
}

// Lấy dữ liệu Sân
function getFieldData(id) {
    fetch(`${ctx}/field/edit?id=` + id)
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

            document.getElementById("fac").value =
                String(data.facilityId);
        })
}

// Mở Field Modal để Add Field
async function openModal() {
    const response =
        await fetch(`${ctx}/owner/field-form`);

    const html = await response.text();

    document.getElementById("modal").innerHTML = html;

    await loadFieldTypeData();
    await loadFacilityData();
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
        loadFacilityData()
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
    const id = document.getElementById("fieldID").value;

    let url = !id
        ? `${ctx}/field/add`
        : `${ctx}/field/edit`;

    const data = {
        fieldID: id,
        fieldName: document.getElementById("fieldName").value,
        description: document.getElementById("desc").value,
        fieldTypeID: document.getElementById("typeF").value,
        facilityId: document.getElementById("fac").value,
        status: document.getElementById("status").value
    };

    let errors = [];

    if (!data.fieldName) errors.push("Vui lòng nhập Tên sân bóng!");
    if (data.fieldTypeID == "") errors.push("Vui lòng chọn Loại sân bóng!");
    if (data.facilityId == "") errors.push("Vui lòng chọn Cơ sở sỡ hữu sân!");

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

// Xóa Field
function deleteField(id) {
    const confirmed = window.confirm("Xóa dữ liệu sẽ làm mất toàn bộ thông tin liên quan đến sân bóng và không thể khôi phục. Bạn có muốn tiếp tục?");

    if(!confirmed) return;

    fetch(`${ctx}/field/delete?id=${id}`, {
        method: "POST"
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Delete failed");
            }

            location.reload();
        })
        .catch(err => {
            console.error(err);
            alert("Xóa thất bại");
        });
}

// Đảm bảo không còn để lại giá trị sau khi đóng modal
document.addEventListener("DOMContentLoaded", function() {
    const modalContainer = document.getElementById("fieldFormModal");
    if (modalContainer) {
        modalContainer.addEventListener("hidden.bs.modal", function () {
            document.getElementById("fieldID").value = ""

            document.getElementById("fieldName").value = ""

            document.getElementById("desc").value = ""

            document.getElementById("typeF").value = ""

            document.getElementById("fac").value = ""
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