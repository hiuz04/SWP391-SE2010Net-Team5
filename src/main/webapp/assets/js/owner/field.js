const ctx = "/SWP391_war_exploded";

// Lấy danh sách sân bóng
function loadFieldData() {

    fetch(`${ctx}/api/field-list`)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("data-container");

            let html = "";

            data.forEach(item => {
                html += `
                    <div>
                    <div>
                        <div class="card soft-card p-4">
                            <h3>${item.facility.facilityName}</h3>
                            <p class="text-muted">${item.facility.address}</p>
                            
                            <div class="fields">    
                                    
                `;

                item.fields.forEach(field => {
                        html += `
                             <div class="field-card">
                                <h5>${field.fieldName}</h5>
                                <p>${field.description ?? ""}</p>
                                <a onclick="openModalToEdit(${field.fieldId})">Edit</a>
                                <a onclick="deleteField(${field.fieldId})" >Delete</a>
                            </div>
                        `;
                    }
                );

                html += `
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        })
}
loadFieldData();

// Lấy danh sách loại sân
function loadFieldTypeData() {
    fetch(`${ctx}/api/field-type`)
        .then(response => response.json())
        .then(data => {
            const select = document.getElementById("typeF");
            select.innerHTML = "";

            data.forEach(type => {
                select.innerHTML += `
                    <option value="${type.fieldTypeId}">
                        ${type.typeName}
                    </option>
                `;
            })
        })
}

// Lấy danh sách cơ sở
function loadFacilityData() {
    fetch(`${ctx}/api/facilities`)
        .then(response => response.json())
        .then(data => {
            const select = document.getElementById("fac");
            select.innerHTML = "";

            data.forEach(fac => {
                select.innerHTML += `
                    <option value="${fac.facilityId}">
                        ${fac.facilityName}
                    </option>
                `;
            })
        })
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
                data.fieldTypeId;

            document.getElementById("fac").value =
                data.facilityId;
        })
}

// Mở Form Modal để Add Field
async function openModal() {
    const response =
        await fetch(`${ctx}/UI/owner/field-form.html`);

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

// Mở Form Modal để Edit Field
async function openModalToEdit(id) {
    const response =
        await fetch(`${ctx}/UI/owner/field-form.html`);

    const html = await response.text();

    document.getElementById("modal").innerHTML = html;

    await loadFieldTypeData();
    await loadFacilityData();
    await getFieldData(id);
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

// Xóa field
function deleteField(id) {
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