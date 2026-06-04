/**
 * Module: Facility Management
 * File: facility.js
 * Description: Xử lý các chức năng CRUD và gọi API cho module cơ sở (Facility).
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
    {status: "ACTIVE", display: "Hoạt động", badge: "badge-soft-success"},
    {status: "INACTIVE", display: "Ngừng hoạt động", badge: "badge-soft-secondary"},
    {status: "MAINTENANCE", display: "Bảo trì", badge: "badge-soft-warning"},
    {status: "CLOSED", display: "Đóng cửa", badge: "badge-soft-danger"}
]

// Lấy data của facility để edit
function loadSelectionFacilityData() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if (!id) return;
    fetch(`${ctx}/facility/edit?id=${id}`)
        .then(res => res.json())
        .then(data => {
                document.getElementById("facilityID").value = data.facilityId;
                document.getElementById("facName").value = data.facilityName;
                document.getElementById("desc").value = data.description;
                document.getElementById("adrs").value = data.address;
                document.getElementById("ward").value = data.ward;
                document.getElementById("dist").value = data.district;
                document.getElementById("city").value = data.city;
                document.getElementById("hotln").value = data.hotline;
                document.getElementById("opTime").value = data.openingTime?.slice(0, 5);
                document.getElementById("clsTime").value = data.closingTime?.slice(0, 5);
                document.getElementById("rule").value = data.generalRules;
                document.getElementById("status").value = data.status;
                document.getElementById("feat").checked = !!data.featured;
            }
        )
}

loadSelectionFacilityData();

// Lấy danh sách thông tin của facility
function loadData() {
    fetch(`${ctx}/api/facilities`)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById("facility-data-container");

            let html = "";
            if (data.length > 0) {
                html += `
                <table class="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th style="width: 20%">Tên cơ sở</th>
                        <th style="width: 36%">Địa chỉ</th>
                        <th style="width: 12%">Số sân</th>
                        <th style="width: 23%">Trạng thái</th>
                        <th class="text-center">Hành động</th>
                      </tr>
                    </thead>
                    
                    <tbody>
`
                data.forEach(item => {
                    const statusDisplay = statusList.find(s => s.status === item.facility.status)?.display
                        ?? "Không xác định";
                    const statusBadgge = statusList.find(s => s.status === item.facility.status)?.badge
                        ?? "badge-soft-secondary";
                    html += `
                      <tr>
                        <td>${item.facility.facilityName}</td>
                        <td style="color: grey;"><img class="icon" alt="editIcon" src="${ctx}/assets/images/icon/locationIcon.png"> ${item.facility.address}, ${item.facility.ward}, ${item.facility.district}, ${item.facility.city}</td>
                        <td>${item.fields.length} sân</td>
                        <td>
                            <button class="badge ${statusBadgge} border-0" onclick="">
                                ${statusDisplay}
                            </button>
                        </td>
                        <td class="text-center">
                          <button class="btn btn-sm" onclick="navigateFacilityFormWithID(${item.facility.facilityId})"><img class="icon" alt="editIcon" src="${ctx}/assets/images/icon/editIcon.png"></button>
                          <button class="btn btn-sm" onclick="deleteFacility(${item.facility.facilityId})"><img class="icon" alt="deleteIcon" src="${ctx}/assets/images/icon/deleteIcon.png"></button>
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

// Xử lý form submit
function submitField() {
    const id = document.getElementById("facilityID").value;

    let url = !id
        ? `${ctx}/facility/add`
        : `${ctx}/facility/edit`;

    const data = {
        facilityID: id,
        facilityName: document.getElementById("facName").value,
        description: document.getElementById("desc").value,
        address: document.getElementById("adrs").value,
        ward: document.getElementById("ward").value,
        district: document.getElementById("dist").value,
        city: document.getElementById("city").value,
        hotline: document.getElementById("hotln").value,
        openingTime: document.getElementById("opTime").value,
        closingTime: document.getElementById("clsTime").value,
        generalRules: document.getElementById("rule").value,
        status: document.getElementById("status").value,
        featured: document.getElementById("feat").checked
    };

    let errors = [];

    if (!data.facilityName) errors.push("Vui lòng nhập Tên cơ sở!");
    if (!data.address) errors.push("Vui lòng nhập Địa chỉ!");
    if (!data.ward) errors.push("Vui lòng nhập Phường/Xã!");
    if (!data.district) errors.push("Vui lòng nhập Quận/Huyện!");
    if (!data.city) errors.push("Vui lòng nhập Tỉnh/Thành phố!");

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
        .then(() => {
            location.href = `${ctx}/owner/facility-list`;
        })
        .catch(err => {
            console.error(err);
            alert("Không thêm/sửa được, kiểm tra server!");
        });
}

// Xử lý title thay đổi phụ thuộc vào thao tác
function dynamicLabel() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    document.getElementById("formTitle").textContent =
        id ? "Chỉnh sửa thông tin cơ sở" : "Thêm cơ sở mới";
    document.getElementById("submitBtn").textContent =
        id ? "Lưu thay đổi" : "Thêm mới";
    document.title =
        id ? "Chỉnh sửa cơ sở | Sport Field Booking" : "Thêm cơ sở mới | Sport Field Booking";
}

// Chuyển sang trang Facility Form
function navigateFacilityForm() {
    window.location.href = `${ctx}/owner/facility-form`
}

// Với ID
function navigateFacilityFormWithID(id) {
    window.location.href = `${ctx}/owner/facility-form?id=${id}`
}

// Xóa Facility
function deleteFacility(id) {
    const confirmed = window.confirm("Xóa dữ liệu sẽ làm mất toàn bộ thông tin liên quan đến cơ sở " +
        "và toàn bộ sân bóng thuộc quyền sỡ hữu của cơ sở. Dữ liệu bị xóa " +
        "sẽ không thể khôi phục. Bạn có muốn tiếp tục?");

    if (!confirmed) return;

    fetch(`${ctx}/facility/delete?id=${id}`, {
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