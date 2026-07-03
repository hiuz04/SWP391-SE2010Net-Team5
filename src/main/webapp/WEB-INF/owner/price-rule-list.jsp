<%@ page import="com.swp.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="com.swp.model.PriceRule" %>
<%@ page import="com.swp.model.Facility" %>
<%@ page import="com.swp.model.FieldType" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    User sessionUser = (User) request.getAttribute("sessionUser");
    if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
    String navRole = (String) request.getAttribute("navRole");
    if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) navRole = "guest";
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();
    
    List<PriceRule> priceRules = (List<PriceRule>) request.getAttribute("priceRules");
    List<Facility> facilities = (List<Facility>) request.getAttribute("facilities");
    List<FieldType> fieldTypes = (List<FieldType>) request.getAttribute("fieldTypes");
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/owner/styles.css" rel="stylesheet">
    <title>Quản lý Bảng Giá | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Bảng giá"></div>
<main class="py-5 main-wrapper">
    <div class="container">
        <div class="d-flex justify-content-between mb-4">
            <h1 class="section-title">Quản lý Bảng Giá</h1>
            <button class="btn btn-sf-primary" style="height: 40px; background: rgb(5, 150, 105);" data-bs-toggle="modal" data-bs-target="#priceRuleModal" onclick="openAddModal()">✛ Thêm luật giá mới</button>
        </div>
        
        <% if(request.getAttribute("errorMessage") != null) { %>
            <div class="alert alert-danger"><%= request.getAttribute("errorMessage") %></div>
        <% } %>

        <div class="card shadow-sm border-0">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Tên luật</th>
                                <th>Loại</th>
                                <th>Cơ sở</th>
                                <th>Mức giá</th>
                                <th>Khung giờ</th>
                                <th>Trạng thái</th>
                                <th>Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% if(priceRules != null && !priceRules.isEmpty()) { 
                                for(PriceRule rule : priceRules) { 
                            %>
                            <tr>
                                <td>#<%= rule.getPriceRuleId() %></td>
                                <td><%= rule.getRuleName() %></td>
                                <td><span class="badge bg-info text-dark"><%= rule.getRuleType() %></span></td>
                                <td>
                                    <% 
                                        String facName = "Tất cả cơ sở";
                                        if (rule.getFacilityId() != null) {
                                            for(Facility f : facilities) {
                                                if(f.getFacilityId() == rule.getFacilityId()) {
                                                    facName = f.getFacilityName();
                                                    break;
                                                }
                                            }
                                        }
                                    %>
                                    <%= facName %>
                                </td>
                                <td class="fw-bold text-success"><%= rule.getPrice() %> VNĐ</td>
                                <td>
                                    <% if (rule.getStartTime() != null && rule.getEndTime() != null) { %>
                                        <%= rule.getStartTime() %> - <%= rule.getEndTime() %>
                                    <% } else { %>
                                        Cả ngày
                                    <% } %>
                                </td>
                                <td>
                                    <% if("ACTIVE".equals(rule.getStatus())) { %>
                                        <span class="badge bg-success">Hoạt động</span>
                                    <% } else { %>
                                        <span class="badge bg-secondary">Tạm dừng</span>
                                    <% } %>
                                </td>
                                <td>
                                    <button class="btn btn-sm btn-outline-primary" 
                                        onclick='openEditModal(<%= rule.getPriceRuleId() %>, "<%= rule.getRuleName() %>", "<%= rule.getRuleType() %>", "<%= rule.getFacilityId() != null ? rule.getFacilityId() : "" %>", "<%= rule.getFieldTypeId() != null ? rule.getFieldTypeId() : "" %>", "<%= rule.getPrice() %>", "<%= rule.getStartTime() != null ? rule.getStartTime() : "" %>", "<%= rule.getEndTime() != null ? rule.getEndTime() : "" %>", "<%= rule.getDayOfWeek() != null ? rule.getDayOfWeek() : "" %>", "<%= rule.getSpecificDate() != null ? rule.getSpecificDate() : "" %>", "<%= rule.getPriority() %>", "<%= rule.getStatus() %>")'>
                                        <i class="bi bi-pencil"></i>
                                    </button>
                                    <form action="<%= ctx %>/owner/price-rules" method="POST" class="d-inline" onsubmit="return confirm('Bạn có chắc chắn muốn xóa luật giá này không?');">
                                        <input type="hidden" name="action" value="delete">
                                        <input type="hidden" name="id" value="<%= rule.getPriceRuleId() %>">
                                        <button type="submit" class="btn btn-sm btn-outline-danger"><i class="bi bi-trash"></i></button>
                                    </form>
                                </td>
                            </tr>
                            <% } } else { %>
                            <tr>
                                <td colspan="8" class="text-center text-muted py-4">Chưa có luật giá nào được cấu hình.</td>
                            </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</main>

<jsp:include page="priceRuleModal.jsp" />

<div id="footer" data-root="../../"></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    function openAddModal() {
        document.getElementById('priceRuleForm').reset();
        document.getElementById('modalAction').value = 'add';
        document.getElementById('priceRuleId').value = '';
        document.getElementById('priceRuleModalLabel').innerText = 'Thêm Luật Giá Mới';
        toggleDynamicFields();
    }

    function openEditModal(id, name, type, facility, fieldType, price, startTime, endTime, dayOfWeek, specificDate, priority, status) {
        document.getElementById('priceRuleForm').reset();
        document.getElementById('modalAction').value = 'edit';
        document.getElementById('priceRuleId').value = id;
        document.getElementById('priceRuleModalLabel').innerText = 'Cập Nhật Luật Giá';
        
        document.getElementById('ruleName').value = name;
        document.getElementById('ruleType').value = type;
        document.getElementById('facilityId').value = facility;
        document.getElementById('fieldTypeId').value = fieldType;
        document.getElementById('price').value = price;
        document.getElementById('startTime').value = startTime;
        document.getElementById('endTime').value = endTime;
        document.getElementById('dayOfWeek').value = dayOfWeek;
        document.getElementById('specificDate').value = specificDate;
        document.getElementById('priority').value = priority;
        document.getElementById('status').value = status;
        
        toggleDynamicFields();
        var modal = new bootstrap.Modal(document.getElementById('priceRuleModal'));
        modal.show();
    }

    function toggleDynamicFields() {
        var type = document.getElementById('ruleType').value;
        document.getElementById('dayOfWeekContainer').style.display = (type === 'WEEKDAY' || type === 'WEEKEND') ? 'block' : 'none';
        document.getElementById('specificDateContainer').style.display = (type === 'SPECIFIC_DATE') ? 'block' : 'none';
    }

    document.getElementById('ruleType').addEventListener('change', toggleDynamicFields);
</script>
</body>
</html>
