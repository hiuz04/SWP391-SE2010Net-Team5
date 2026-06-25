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

let selectedImg = [];
let deletedImg = [];
const imgInput = document.getElementById("images");
const preview = document.getElementById("preview");

// Lấy data của facility để edit
async function loadFacilityData() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if (!id) return;
    await fetch(`${ctx}/owner/facility?action=get&id=${id}`)
        .then(res => res.json())
        .then(data => {
            console.log(">>> data",data);
                document.getElementById("facilityID").value = data.facility.facilityId;
                document.getElementById("facName").value = data.facility.facilityName;
                document.getElementById("desc").value = data.facility.description;
                document.getElementById("adrs").value = data.facility.address;
                document.getElementById("ward").value = data.facility.ward;
                document.getElementById("dist").value = data.facility.district;
                document.getElementById("city").value = data.facility.city;
                document.getElementById("hotln").value = data.facility.hotline;
                document.getElementById("opTime").value = data.facility.openingTime?.slice(0, 5);
                document.getElementById("clsTime").value = data.facility.closingTime?.slice(0, 5);
                document.getElementById("rule").value = data.facility.generalRules;
                document.getElementById("status").value = data.facility.status;
                document.getElementById("feat").checked = !!data.facility.featured;

                data.img.forEach((img) => {
                    selectedImg.push({
                        imageId: img.imageId,
                        imageUrl: img.imageUrl,
                        thumbnail: img.thumbnail,
                        isOld: true
                    })
                })
            }
        )
}

async function loadForm() {
    await loadFacilityData();
    renderPreview();

    // Click ra ngoài thì đóng menu
    document.addEventListener("click", ()=>{
        document.querySelectorAll(".menu-popup").forEach(menu=>{
            menu.classList.remove("show");
        });
    });

    imgInput.addEventListener("change", (e) => {
        const newFiles = Array.from(e.target.files);
        newFiles.forEach(file => {
            selectedImg.push({
                file: file,
                thumbnail: selectedImg.length === 0,
                isOld: false,
            });
        });
        renderPreview();
        e.target.value = "";
    });
}

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
function submitForm() {
    const id = document.getElementById("facilityID").value;

    let url = !id
        ? `${ctx}/owner/facility?action=add`
        : `${ctx}/owner/facility?action=edit`;

    const data = {
        facilityID: id,
        facilityName: document.getElementById("facName").value,
        description: document.getElementById("desc").value,
        address: document.getElementById("adrs").value,
        ward: document.getElementById("ward").value,
        district: document.getElementById("dist").value,
        city: document.getElementById("city").value,
        latitude: document.getElementById("lat").value,
        longitude: document.getElementById("long").value,
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

    const formData = new FormData();
    Object.entries(data).forEach(([key, value]) => {
        formData.append(key, value);
    });

    // Upload ảnh mới
    selectedImg
        .filter(img => !img.isOld)
        .forEach(img => {
            formData.append("images", img.file);
            formData.append("thumbnail", img.thumbnail);
        });

    // Update ảnh cũ
    selectedImg
        .filter(img => img.isOld)
        .forEach(img => {
            formData.append("imagesOld", img.imageId);
            formData.append("thumbnailOld", img.thumbnail);
        });

    // Xóa ảnh cũ
    if (deletedImg.length > 0) {
        deletedImg.forEach(id => {
            formData.append("deletedImg", id);
        });
    }

    fetch(url, {
        method: "POST",
        body: formData
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Server error");
            }
            return res.text();
        })
        .then(() => {
            location.href = `${ctx}/owner/facility`;
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

    fetch(`${ctx}/owner/facility?action=delete&id=${id}`, {
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

function renderPreview() {
    let html = "";
    selectedImg.forEach((image, index) => {
        html += `
            <div class="image-item" id="item-${index}">`

        if(image.isOld){
            let srcUrl = image.imageUrl.startsWith('http') ? image.imageUrl : `${ctx}/${image.imageUrl.replace(/^\//, '')}`;
            html += `<img 
                src="${srcUrl}" 
                alt="img-${index}" 
                id="image-${index}"
                height="330"
                width="550"
                style="object-fit: cover"
            >`
        } else {
            html += `<img
                src="${URL.createObjectURL(image.file)}"
                alt="img-${index}"
                id="image-${index}"
                height="330"
                width="550"
                style="object-fit: cover"
            >`
        }

        html += `<button
                    type="button"
                    class="menu-btn hidden"
                    id="img-menu-${index}"
                    onclick="toggleMenu(event, ${index})"
                >
                    <svg viewBox="0 0 24 24" width="20" height="20">
                        <path d="M12 8a2 2 0 100-4 2 2 0 000 4zm0 6a2 2 0 100-4 2 2 0 000 4zm0 6a2 2 0 100-4 2 2 0 000 4z"/>
                    </svg>
                </button>

                <div class="menu-popup" id="menu-${index}">
                    <button type="button" class="mt-0" onclick="setThumbnail(${index})" ${image.thumbnail ? "disabled" : ""}>
                        <img alt="thumbnailIcon" src="${ctx}/assets/images/icon/thumbnailIcon.png" height="25" width="25">
                        Đặt làm Thumbnail
                    </button>
                    <button type="button" class="mt-2" onclick="removeImage(${index})">
                        <img alt="deleteIcon" src="${ctx}/assets/images/icon/deleteIcon.png" height="25" width="25">
                        Xóa ảnh
                    </button>
                </div>
        `;

        if(image.thumbnail) {
            html += `<div class="thumbnail-box">
                        <img alt='thumbnail' src='${ctx}/assets/images/icon/thumbnailIcon.png' height="25" width="25">
                    </div>`;
        }

        html +=
            `</div>`;
    });
    preview.innerHTML = html;

    selectedImg.forEach((image,index)=>{
        const item = document.getElementById(`item-${index}`);
        const menuItem = document.getElementById(`img-menu-${index}`);
        item.addEventListener("mouseenter",()=>{
            menuItem.classList.remove("hidden");
        });
        item.addEventListener("mouseleave",()=>{
            menuItem.classList.add("hidden");
        });
    });
}

function setThumbnail(index) {
    selectedImg.forEach((img) => {
        img.thumbnail = false;
    })
    selectedImg[index].thumbnail = true;
    renderPreview();
}

function removeImage(index) {
    const wasThumbnail = selectedImg[index].thumbnail;
    const isOld = selectedImg[index].isOld;
    if(isOld) {
        deletedImg.push(selectedImg[index].imageId)
    }
    selectedImg.splice(index, 1);
    if (wasThumbnail && selectedImg.length > 0) {
        selectedImg[0].thumbnail = true;
    }
    renderPreview();
}

function toggleMenu(event, index){
    event.stopPropagation();

    // Đóng tất cả menu khác
    document.querySelectorAll(".menu-popup").forEach(menu=>{
        menu.classList.remove("show");
    });
    document.getElementById(`menu-${index}`)
        .classList.toggle("show");
}