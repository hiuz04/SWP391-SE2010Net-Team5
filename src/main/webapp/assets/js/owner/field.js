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