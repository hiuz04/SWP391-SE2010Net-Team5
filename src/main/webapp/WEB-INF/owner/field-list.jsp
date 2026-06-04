<%@ page import="com.swp.model.Field" %>
<%@ page import="com.swp.model.Facility" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    Map<Facility, List<Field>> fieldFacility = (Map<Facility, List<Field>>) request.getAttribute("fieldFacility");
%>
<%@ include file="/WEB-INF/owner/fieldFormModal.jsp" %>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/field.css" rel="stylesheet">
    <title>Field And Facility Management</title>
</head>
<body>
<div class="container">
    <h1>Đây là danh sách sân</h1>
    <button onclick="" >Thêm cơ sở mới ➕</button>
    <button onclick="openAddFieldModal()" >Thêm sân bóng mới ➕</button>
    <%
        if (fieldFacility != null && !fieldFacility.isEmpty()) {
            for (Map.Entry<Facility, List<Field>> entry : fieldFacility.entrySet()) {
                Facility facility = entry.getKey();
                List<Field> fields = entry.getValue();
    %>

    <div class="facility-box">
        <!-- Tên cơ sở -->
        <h3><%= facility.getFacilityName() %></h3>

        <!-- Danh sách sân -->
        <%
            if (fields != null && !fields.isEmpty()) {
                for (Field f : fields) {
        %>
                    <div class="field-item">
                        • <b><%= f.getFieldName() %>
                        </b>
                        - <%= f.getStatus() %>
                    </div>
                    <a onclick="openEditFieldModal(<%=f.getFieldId()%>)"> Edit sân </a>
                    <a onclick="deleteField(<%=f.getFieldId()%>)"> Xóa sân </a>
        <%
                }
            } else {
        %>
            <div class="field-item">Không có sân</div>
        <%  }  %>

    </div>

    <%
        }
    } else {
    %>

    <p>Không có dữ liệu</p>

    <%
        }
    %>

</div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function openAddFieldModal() {
        const e = document.getElementById("fieldModal");

        const modal = new bootstrap.Modal(
            e
        );
        modal.show();
        document.getElementById("fieldTitle").innerHTML = "Thêm sân bóng mới";
        document.getElementById("submitBtn").innerHTML = "Thêm mới";

        // Reset tránh hiện lại data cũ
        document.getElementById("fieldForm").reset();
        document.getElementById("fieldID").value = "";
    }
    function openEditFieldModal(id) {
        fetch('${pageContext.request.contextPath}/field/edit?id=' + id)
            .then(response => response.text())
            .then(text => {
                const p = text.split("|");

                document.getElementById("fieldID").value = p[0];
                document.getElementById("fieldName").value = p[1];
                document.getElementById("desc").value = p[2];
                document.getElementById("typeF").value = p[3];
                document.getElementById("fac").value = p[4];
                document.getElementById("status").value = p[5];

                document.getElementById("fieldTitle").innerHTML = "Chỉnh sửa sân bóng";
                document.getElementById("submitBtn").innerHTML = "Lưu thay đổi";

                const e = document.getElementById("fieldModal")
                const modal = new bootstrap.Modal(
                    e
                );
                modal.show();
            })
    }
    function deleteField(id) {
        const url = "${pageContext.request.contextPath}/field/delete?id=" + id;

        fetch(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
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
                alert("Không xóa được, kiểm tra server!");
            });
    }
</script>
</body>
</html>
