const ctx = "/SWP391_war_exploded";

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
        latitude: document.getElementById("lat").value,
        longitude: document.getElementById("long").value,
        hotline: document.getElementById("hotln").value,
        openingTime: document.getElementById("opTime").value,
        closingTime: document.getElementById("clsTime").value,
        generalRules: document.getElementById("rule").value,
        status: document.getElementById("status").value,
        featured: document.getElementById("feat").checked
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
        .then(() => {
            location.href = `${ctx}/UI/owner/facilities-n-fields.html`;
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
        id ? "Chỉnh sửa cơ sở" : "Thêm cơ sở mới";
}
dynamicLabel();

// Lấy data để edit
function loadData() {
    const params = new URLSearchParams(window.location.search);
    const id = params.get("id");
    if(!id) return;
    fetch(`${ctx}/facility/edit?id=${id}`)
        .then(res => res.json())
        .then(data =>
            {
                document.getElementById("facilityID").value = data.facilityId;
                document.getElementById("facName").value = data.facilityName;
                document.getElementById("desc").value = data.description;
                document.getElementById("adrs").value = data.address;
                document.getElementById("ward").value = data.ward;
                document.getElementById("dist").value = data.district;
                document.getElementById("city").value = data.city;
                document.getElementById("lat").value = data.latitude ?? "";
                document.getElementById("long").value = data.longitude ?? "";
                document.getElementById("hotln").value = data.hotline;
                document.getElementById("opTime").value = data.openingTime?.slice(0,5);
                document.getElementById("clsTime").value = data.closingTime?.slice(0,5);
                document.getElementById("rule").value = data.generalRules;
                document.getElementById("status").value = data.status;
                document.getElementById("feat").checked = !!data.featured;
            }
        )
}
loadData();