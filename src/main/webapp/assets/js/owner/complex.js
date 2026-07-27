/**
 * Module: Complex Management
 * File: complex.js
 * Description: Xử lý các chức năng CRUD và gọi API cho module cụm sân (Complex).
 *
 * Author: Dương Hải Anh
 * Version: 1.1
 * Created date: 01/06/2026
 * Updated date: 04/06/2026
 * Update Notes: Tách biệt logic quản lý cơ sở từ file chung thành file độc lập để dễ dàng bảo trì.
 */
const ctx = window.APP_CTX || "";

// Danh sách status
const statusList = [
    {status: "ACTIVE", display: "Hoạt động", badge: "badge-soft-success"},
    {status: "INACTIVE", display: "Ngừng hoạt động", badge: "badge-soft-danger"},
    {status: "PENDING", display: "Đang thiết lập", badge: "badge-soft-secondary"},
    {status: "MAINTENANCE", display: "Bảo trì", badge: "badge-soft-warning"}
]

let selectedImg = [];
let deletedImg = [];
const imgInput = document.getElementById("images");
const preview = document.getElementById("preview");

let provincesList = [];

async function loadHanoiWards() {
    const wardSelect = document.getElementById("ward");
    if (!wardSelect) return;

    wardSelect.disabled = true;
    wardSelect.innerHTML = '<option value="">Đang tải Phường/Xã...</option>';

    try {
        const res = await fetch("https://provinces.open-api.vn/api/v2/p/01?depth=2");
        if (!res.ok) throw new Error("Lỗi kết nối API");

        const data = await res.json();

        wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';

        data.wards.forEach(ward => {
            const opt = document.createElement("option");
            opt.value = ward.name;
            opt.textContent = ward.name;
            wardSelect.appendChild(opt);
        });

        wardSelect.disabled = false;
    } catch (err) {
        console.error(err);
        wardSelect.innerHTML = '<option value="">-- Chọn Phường/Xã --</option>';
        wardSelect.disabled = false;
    }
}

// Lấy data của complex để edit
async function loadComplexData() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if (!id) return;
    await fetch(`${ctx}/owner/complex?action=get&id=${id}`)
        .then(res => res.json())
        .then(async data => {
                document.getElementById("complexId").value = data.complex.complexId;
                document.getElementById("complexName").value = data.complex.complexName;
                document.getElementById("desc").value = data.complex.description;
                document.getElementById("adrs").value = data.complex.address;

                const wardSelect = document.getElementById("ward");

                await loadHanoiWards();

                if (data.complex.ward && wardSelect) {
                    const matchedWard = Array.from(wardSelect.options)
                        .find(opt => opt.value === data.complex.ward);

                    if (matchedWard) {
                        wardSelect.value = data.complex.ward;
                    } else {
                        const opt = document.createElement("option");
                        opt.value = data.complex.ward;
                        opt.textContent = data.complex.ward;
                        opt.selected = true;
                        wardSelect.appendChild(opt);
                    }
                }

                document.getElementById("hotln").value = data.complex.hotline;
                document.getElementById("opTime").value = data.complex.openingTime?.slice(0, 5);
                document.getElementById("clsTime").value = data.complex.closingTime?.slice(0, 5);
                document.getElementById("rule").value = data.complex.generalRules;

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

    await loadHanoiWards();

    await loadComplexData();

    renderPreview();

    document.addEventListener("click", () => {
        document.querySelectorAll(".menu-popup").forEach(menu => {
            menu.classList.remove("show");
        });
    });

    if (imgInput) {
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
}

// Lấy danh sách thông tin của complex
function loadData() {
    const keyword = document.getElementById("keyword").value.trim();
    const status  = document.getElementById("status").value;
    const params  = new URLSearchParams();
    if (keyword) params.append("keyword", keyword);
    if (status)  params.append("status", status);

    fetch(`${ctx}/api/complexes?${params}`)
        .then(res => res.json())
        .then(data => {
            // Update count
            const countEl = document.getElementById("complex-count");
            if (countEl) {
                countEl.innerHTML = `<i class="bi bi-buildings"></i> <strong>${data.length}</strong> cơ sở`;
            }

            const container = document.getElementById("complex-data-container");

            if (data.length === 0) {
                container.innerHTML = `
                    <div class="empty-state">
                        <i class="bi bi-buildings"></i>
                        <p>Chưa có cơ sở nào. Hãy thêm cơ sở đầu tiên!</p>
                    </div>
                `;
                return;
            }

            let html = `
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th style="width:50px">#</th>
                            <th>Tên cơ sở</th>
                            <th>Địa chỉ</th>
                            <th style="width:90px">Số sân</th>
                            <th style="width:140px">Giờ hoạt động</th>
                            <th style="width:130px">Hotline</th>
                            <th style="width:140px">Trạng thái</th>
                            <th style="width:120px" class="text-center">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            data.forEach((item, index) => {
                const statusDisplay = statusList.find(s => s.status === item.complex.status)?.display ?? 'Không xác định';
                const statusClass   = item.complex.status ? `status-${item.complex.status.toLowerCase()}` : 'status-inactive';

                const address = [item.complex.address, item.complex.ward, item.complex.city]
                    .filter(Boolean).join(', ');

                const timeDisplay = item.complex.openingTime
                    ? `${item.complex.openingTime.slice(0,5)} – ${item.complex.closingTime?.slice(0,5)}`
                    : '<span class="text-muted">—</span>';

                const hotlineDisplay = item.complex.hotline
                    ? `<a href="tel:${item.complex.hotline}" class="text-decoration-none">${item.complex.hotline}</a>`
                    : '<span class="text-muted">—</span>';

                html += `
                    <tr>
                        <td class="text-center text-muted fw-semibold">${index + 1}</td>
                        <td>
                            <span class="fw-semibold text-dark">
                                <i class="bi bi-building me-1 text-success"></i>${item.complex.complexName}
                            </span>
                        </td>
                        <td>
                            <span class="complex-address-text">
                                <i class="bi bi-geo-alt-fill text-danger me-1"></i>${address}
                            </span>
                        </td>
                        <td class="text-center">
                            <span class="badge bg-success-subtle text-success fw-semibold">
                                <i class="fa-solid fa-futbol"></i>  ${item.fields.length}
                            </span>
                        </td>
                        <td>
                            <span class="text-muted small"><i class="bi bi-clock me-1"></i>${timeDisplay}</span>
                        </td>
                        <td class="small">${hotlineDisplay}</td>
                        <td>
                            <div class="dropdown">
                                <button class="complex-status-badge ${statusClass} dropdown-toggle"
                                        type="button" data-bs-toggle="dropdown">
                                    ${statusDisplay}
                                </button>
                                <ul class="dropdown-menu shadow-sm" style="border-radius:12px;min-width:180px;padding:6px 0">
                                    <li><a class="dropdown-item" href="#" onclick="updateComplexStatus(${item.complex.complexId}, 'ACTIVE')"><i class="bi bi-check-circle text-success me-2"></i>Hoạt động</a></li>
                                    <li><a class="dropdown-item" href="#" onclick="updateComplexStatus(${item.complex.complexId}, 'INACTIVE')"><i class="bi bi-pause-circle text-secondary me-2"></i>Tạm ngưng</a></li>
                                    <li><a class="dropdown-item" href="#" onclick="updateComplexStatus(${item.complex.complexId}, 'MAINTENANCE')"><i class="bi bi-tools text-warning me-2"></i>Bảo trì</a></li>
                                </ul>
                            </div>
                        </td>
                        <td class="text-center">
                            <div class="d-flex gap-1 justify-content-center">
                                <button class="btn btn-sm btn-outline-primary"
                                        title="Chỉnh sửa"
                                        onclick="navigateComplexFormWithID(${item.complex.complexId})">
                                    <i class="bi bi-pencil"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger"
                                        title="Xóa"
                                        onclick="deleteComplex(${item.complex.complexId})">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
            });

            html += `
                    </tbody>
                </table>
            </div>`;

            // Wrap in data-container for consistent styling with field-list
            const wrapper = document.createElement('div');
            wrapper.id = 'complex-table-wrapper';
            wrapper.innerHTML = html;
            container.innerHTML = '';
            container.appendChild(wrapper);
        })
        .catch(err => console.error('Complex load error:', err));
}

// Xử lý form submit
function submitForm() {
    document.getElementById("submitBtn").disabled = true;
    const id = document.getElementById("complexId").value;

    let url = !id
        ? `${ctx}/owner/complex?action=add`
        : `${ctx}/owner/complex?action=edit`;

    const data = {
        complexId: id,
        complexName: document.getElementById("complexName").value,
        description: document.getElementById("desc").value,
        address: document.getElementById("adrs").value,
        ward: document.getElementById("ward").value,
        latitude: document.getElementById("lat").value,
        longitude: document.getElementById("long").value,
        hotline: document.getElementById("hotln").value,
        openingTime: document.getElementById("opTime").value,
        closingTime: document.getElementById("clsTime").value,
        generalRules: document.getElementById("rule").value
    };

    let errors = [];

    if (!data.complexName) errors.push("Vui lòng nhập Tên cơ sở!");
    if (!data.address) errors.push("Vui lòng nhập Địa chỉ!");
    if (!data.ward) errors.push("Vui lòng chọn Phường/Xã!");
    if (!data.openingTime) errors.push("Vui lòng đặt thời gian Mở cửa!");
    if (!data.closingTime) errors.push("Vui lòng đặt thời gian Đóng cửa!");
    if(data.openingTime > data.closingTime) errors.push("Vui lòng đặt thời gian Mở cửa lớn hơn thời gian Đóng cửa!")

    if (errors.length > 0) {
        alert(errors.join("\n"));
        document.getElementById("submitBtn").disabled = false;
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
            location.href = `${ctx}/owner/complex`;
        })
        .catch(err => {
            console.error(err);
            alert("Không thêm/sửa được, kiểm tra server!");
        });
    document.getElementById("submitBtn").disabled = false;
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

// Chuyển sang trang Complex Form
function navigateComplexForm() {
    window.location.href = `${ctx}/owner/complex-form`
}

// Với ID
function navigateComplexFormWithID(id) {
    window.location.href = `${ctx}/owner/complex-form?id=${id}`
}

// Xóa Complex
function deleteComplex(id) {
    const confirmed = window.confirm("Xóa dữ liệu sẽ làm mất toàn bộ thông tin liên quan đến cơ sở " +
        "và toàn bộ sân bóng thuộc quyền sỡ hữu của cơ sở. Dữ liệu bị xóa " +
        "sẽ không thể khôi phục. Bạn có muốn tiếp tục?");

    if (!confirmed) return;

    fetch(`${ctx}/owner/complex?action=delete&id=${id}`, {
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

        if (image.isOld) {
            let srcUrl = image.imageUrl.startsWith('http') ? image.imageUrl : `${ctx}/${image.imageUrl.replace(/^\//, '')}`;
            html += `<img
                class="pic"
                src="${srcUrl}"
                alt="img-${index}"
                id="image-${index}"
            >`
        } else {
            html += `<img
                class="pic"
                src="${URL.createObjectURL(image.file)}"
                alt="img-${index}"
                id="image-${index}"
            >`
        }

        html += `<button
                    type="button"
                    class="menu-btn hidden"
                    id="img-menu-${index}"
                    onclick="toggleMenu(event, ${index})"
                >
                    <svg viewBox="0 0 24 24" width="18" height="18">
                        <path d="M12 8a2 2 0 100-4 2 2 0 000 4zm0 6a2 2 0 100-4 2 2 0 000 4zm0 6a2 2 0 100-4 2 2 0 000 4z"/>
                    </svg>
                </button>

                <div class="menu-popup" id="menu-${index}">
                    <button type="button" class="mt-0" onclick="setThumbnail(${index})" ${image.thumbnail ? "disabled" : ""}>
                        <img alt="thumbnailIcon" src="${ctx}/assets/images/icon/thumbnailIcon.png" height="22" width="22">
                        Đặt làm Thumbnail
                    </button>
                    <button type="button" class="mt-2" onclick="removeImage(${index})">
                        <img alt="deleteIcon" src="${ctx}/assets/images/icon/deleteIcon.png" height="22" width="22">
                        Xóa ảnh
                    </button>
                </div>
        `;

        if (image.thumbnail) {
            html += `<div class="thumbnail-box">
                        <img alt='thumbnail' src='${ctx}/assets/images/icon/thumbnailIcon.png' height="20" width="20">
                    </div>`;
        }

        html += `</div>`;
    });
    preview.innerHTML = html;

    selectedImg.forEach((image, index) => {
        const item     = document.getElementById(`item-${index}`);
        const menuItem = document.getElementById(`img-menu-${index}`);
        item.addEventListener("mouseenter", () => menuItem.classList.remove("hidden"));
        item.addEventListener("mouseleave", () => menuItem.classList.add("hidden"));
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

function updateComplexStatus(complexId, status) {
    const params = new URLSearchParams();
    params.append("complexId", complexId);
    params.append("status", status);

    fetch(`${ctx}/owner/complex?action=status`, {
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

let filterTimer;

function scheduleLoadData() {
    clearTimeout(filterTimer);

    filterTimer = setTimeout(() => {
        loadData();
    }, 500);
}