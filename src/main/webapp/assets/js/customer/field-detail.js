// Lưu đường context của trang
const ctx = window.APP_CTX || "";

const id = new URLSearchParams(window.location.search).get("id");

// Load dữ liệu chi tiết của cụm sân
function loadData(id) {
    fetch(`${ctx}/field?id=${id}`)
        .then(res => res.json())
        .then(data => {
            document.getElementById("field-name").innerHTML = data.complexName;
            document.getElementById("address").innerHTML = `<i class="bi bi-geo-alt me-1"></i>` + data.complexAddress;
            document.getElementById("description").innerHTML = data.description;
            document.getElementById("workingTime").innerHTML = `${data.openingTime} - ${data.closingTime}`;
            document.querySelectorAll(".hotline").forEach(el => {
                el.innerHTML = data.hotline;
            });
            document.getElementById("fieldCount").innerHTML = data.fields.length;
            const bookingUrl = document.getElementById("bookingUrl");
            if (bookingUrl) {
                bookingUrl.href = `${ctx}/booking?action=create&complexId=${data.complexId}`;
            }

            const fields = document.getElementById("fields");
            fields.innerHTML = "";
            const fieldTypeMap = {};

            data.fieldTypeList.forEach(ft => {
                fieldTypeMap[ft.fieldTypeId] = ft.typeName;
            });

            fields.innerHTML = data.fields.map(item => `
                <div>
                    <span class="complex-item">${item.fieldName}</span>
                </div>
            `).join("");
        })

    fetch(`${ctx}/feedback?complexId=${id}`)
        .then(res => res.json())
        .then(data => {
            console.log(data)
        })
}
loadData(id);
