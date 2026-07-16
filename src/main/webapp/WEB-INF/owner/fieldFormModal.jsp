<%@ page import="com.swp.model.FootballComplex" %>
<%@ page import="com.swp.model.FieldType" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    List<FootballComplex> complexes = (List<FootballComplex>) request.getAttribute("complexes");
    List<FieldType> fieldTypes = (List<FieldType>) request.getAttribute("fieldTypes");
%>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
</head>
<body>
<div class="modal fade" id="fieldModal">
    <div class="modal-dialog">
        <div class="modal-content">

            <form id="fieldForm" method="post"
                  action="<%= ctx %>/fields/add">

                <input type="hidden" name="fieldID" id="fieldID">

                <div class="modal-header">
                    <%-- Đổi thành Edit Field nếu là mode Edit --%>
                    <h5 class="modal-title" id="fieldTitle"></h5>
                    <button type="button" class="btn-close"
                            data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body">

                    <div class="mb-2">
                        <label for="fieldName" >Tên sân: </label>
                        <input type="text" name="fieldName" id="fieldName"
                               class="form-control" required>
                    </div>

                    <div class="mb-2">
                        <label for="desc" >Mô tả sân: </label>
                        <input type="text" name="description" id="desc"
                               class="form-control">
                    </div>

                    <div class="mb-2">
                        <label for="fc" >Loại sân:</label>
                        <select name="fieldTypeID" id="typeF" required>
                            <%
                                for(FieldType ft : fieldTypes) {
                            %>
                                    <option value="<%=ft.getFieldTypeId()%>"><%= ft.getTypeName() %></option>
                            <%
                                }
                            %>
                        </select>
                    </div>

                    <div class="mb-2">
                        <label for="fc" >Cơ sở sỡ hữu sân: </label>
                        <select name="complexId" id="fc" required>
                            <%
                                for(FootballComplex fc : complexes) {
                            %>
                                    <option value="<%=fc.getComplexId()%>"><%= fc.getComplexName() %></option>
                            <%
                                }
                            %>
                        </select>
                    </div>

                    <div class="mb-2">
                        <label for="fc" >Trạng thái sân</label>
                        <select name="status" id="status" required>
                            <option value="AVAILABLE"> Available </option>
                            <option value="OCCUPIED"> Occupied </option>
                            <option value="BOOKED"> Booked </option>
                            <option value="MAINTENANCE"> Maintenance </option>
                        </select>
                    </div>

                </div>

                <div class="modal-footer">
                    <button type="button" id="submitBtn" class="btn btn-primary" onclick="submitField()">
                    </button>
                </div>

            </form>

        </div>
    </div>

</div>
<script>
    function submitField() {
        const id = document.getElementById("fieldID").value;

        let url = !id
            ? "${pageContext.request.contextPath}/field/add"
            : "${pageContext.request.contextPath}/field/edit";

        const data = {
            fieldID: id,
            fieldName: document.getElementById("fieldName").value,
            description: document.getElementById("desc").value,
            fieldTypeID: document.getElementById("typeF").value,
            complexId: document.getElementById("fc").value,
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
</script>
</body>
</html>
