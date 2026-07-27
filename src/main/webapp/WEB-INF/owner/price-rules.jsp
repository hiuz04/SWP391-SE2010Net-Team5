<%@ page import="com.swp.model.User" %>
<%@ page import="com.swp.model.FootballComplex" %>
<%@ page import="com.swp.model.PriceRule" %>
<%@ page import="com.swp.model.Field" %>
<%@ page import="com.swp.model.FieldType" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  User sessionUser = (User) request.getAttribute("sessionUser");
  if (sessionUser == null) sessionUser = (User) session.getAttribute("user");
  String navRole = (String) request.getAttribute("navRole");
  if (navRole == null) navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
  if (navRole == null) navRole = "guest";
  String displayName = sessionUser != null ? sessionUser.getFullName() : "";
  String ctx = request.getContextPath();
  
  List<FootballComplex> complexes = (List<FootballComplex>) request.getAttribute("complexes");
  List<PriceRule> priceRules = (List<PriceRule>) request.getAttribute("priceRules");
  Long selectedComplexId = (Long) request.getAttribute("selectedComplexId");
  List<FieldType> fieldTypes = (List<FieldType>) request.getAttribute("fieldTypes");
  List<Field> fields = (List<Field>) request.getAttribute("fields");
  
  String successMsg = (String) session.getAttribute("successMsg");
  String errorMsg = (String) session.getAttribute("errorMsg");
  session.removeAttribute("successMsg");
  session.removeAttribute("errorMsg");
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
  <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
  <title>Bảng giá | Sport Field Booking</title>
</head>
<body>
<div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>" data-name="<%= displayName %>" data-active="Bảng giá"></div>

<main class="owner-content">

  <div class="container">
    <div class="page-header">
        <div class="page-header-left">
            <h1><i class="bi bi-tags-fill me-2"></i>Quản lý Bảng giá</h1>
            <p>Thiết lập luật giá linh hoạt theo loại sân, khung giờ và ngày đặc biệt.</p>
        </div>

        <% if (complexes != null && !complexes.isEmpty()) { %>
        <form action="<%= ctx %>/owner/price-rules" method="get" class="d-flex gap-2 align-items-center">
            <label for="complexId" class="form-label mb-0 fw-semibold text-nowrap text-white">Cụm sân:</label>
            <select class="form-select form-select-sm" name="complexId" id="complexId" style="width:240px; border-radius:9px; font-size:.875rem;" onchange="this.form.submit()">
              <% for (FootballComplex fc : complexes) { %>
                <option value="<%= fc.getComplexId() %>" <%= (selectedComplexId != null && selectedComplexId.equals(fc.getComplexId())) ? "selected" : "" %>>
                  <%= fc.getComplexName() %>
                </option>
              <% } %>
            </select>
        </form>
        <% } %>
    </div>

    <% if (successMsg != null) { %>
        <div class="alert alert-success alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-check-circle-fill me-2"></i> <%= successMsg %>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    <% } %>
    <% if (errorMsg != null) { %>
        <div class="alert alert-danger alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2"></i> <%= errorMsg %>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    <% } %>

    <div class="card border-0 shadow-sm">
      <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
        <h5 class="card-title fw-bold mb-0 text-sf-primary">Danh sách Bảng giá</h5>
        <button class="btn btn-sf-primary btn-sm px-3" onclick="openAddModal()">
          <i class="bi bi-plus-lg me-1"></i> Thêm mới
        </button>
      </div>
      <div class="card-body p-0">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th class="ps-4">Tên luật</th>
                <th>Phạm vi (Loại/Sân)</th>
                <th>Thời gian áp dụng</th>
                <th>Khung giờ</th>
                <th>Giá tiền</th>
                <th>Độ ưu tiên</th>
                <th class="text-end pe-4">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <% if (priceRules != null && !priceRules.isEmpty()) {
                   for (PriceRule pr : priceRules) {
              %>
              <tr>
                <td class="ps-4 fw-semibold text-dark"><%= pr.getRuleName() %>
                   <br><span class="badge bg-secondary"><%= pr.getRuleType() %></span>
                </td>
                <td>
                    <% if (pr.getFieldId() != null) { %>
                        <span class="badge bg-info text-dark">Sân ID: <%= pr.getFieldId() %></span>
                    <% } else if (pr.getFieldTypeId() != null) { %>
                        <span class="badge bg-primary">Loại sân: <%= pr.getFieldTypeId() %></span>
                    <% } else { %>
                        <span class="badge bg-success">Tất cả sân</span>
                    <% } %>
                </td>
                <td>
                    <% if (pr.getSpecificDate() != null) { %>
                        Ngày: <strong class="text-danger"><%= pr.getSpecificDate() %></strong>
                    <% } else if (pr.getDayOfWeek() != null && !pr.getDayOfWeek().isEmpty()) { %>
                        Thứ: <strong><%= pr.getDayOfWeek() %></strong>
                    <% } else { %>
                        <strong>Tất cả ngày</strong>
                    <% } %>
                </td>
                <td>
                    <% if (pr.getStartTime() != null && pr.getEndTime() != null) { %>
                        <%= pr.getStartTime() %> - <%= pr.getEndTime() %>
                    <% } else { %>
                        Cả ngày
                    <% } %>
                </td>
                <td class="fw-bold text-success">
                    <%= pr.getPrice() %> đ
                </td>
                <td>
                    <span class="badge bg-warning text-dark">Mức <%= pr.getPriority() %></span>
                </td>
                <td class="text-end pe-4">
                  <button class="btn btn-sm btn-outline-secondary me-1" onclick="openEditModal('<%= pr.getPriceRuleId() %>', '<%= pr.getRuleName() %>', '<%= pr.getRuleType() %>', '<%= pr.getFieldTypeId() == null ? "" : pr.getFieldTypeId() %>', '<%= pr.getFieldId() == null ? "" : pr.getFieldId() %>', '<%= pr.getDayOfWeek() == null ? "" : pr.getDayOfWeek() %>', '<%= pr.getSpecificDate() == null ? "" : pr.getSpecificDate() %>', '<%= pr.getStartTime() == null ? "" : pr.getStartTime() %>', '<%= pr.getEndTime() == null ? "" : pr.getEndTime() %>', '<%= pr.getPrice() %>', '<%= pr.getPriority() %>')">
                    <i class="bi bi-pencil"></i>
                  </button>
                  <button type="button" class="btn btn-sm btn-outline-danger" onclick="confirmDelete('<%= pr.getPriceRuleId() %>')">
                    <i class="bi bi-trash"></i>
                  </button>
                </td>
              </tr>
              <% } } else { %>
              <tr>
                <td colspan="7" class="text-center py-4 text-muted">Chưa có bảng giá nào được thiết lập cho cụm sân này.</td>
              </tr>
              <% } %>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</main>

<!-- Modal Thêm/Sửa Bảng Giá -->
<div class="modal fade" id="priceRuleModal" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title fw-bold" id="priceRuleModalTitle">Thêm Bảng giá mới</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <form id="priceRuleForm" method="post" action="<%= ctx %>/owner/price-rules">
          <input type="hidden" name="action" id="formAction" value="add">
          <input type="hidden" name="complexId" value="<%= selectedComplexId %>">
          <input type="hidden" name="priceRuleId" id="priceRuleId" value="">

          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label">Tên luật giá <span class="text-danger">*</span></label>
              <input type="text" class="form-control" name="ruleName" id="ruleName" required placeholder="Vd: Giờ vàng buổi tối">
            </div>
            <div class="col-md-6">
              <label class="form-label">Loại luật (Rule Type)</label>
              <select class="form-select" name="ruleType" id="ruleType">
                  <option value="BASE">Cơ bản (BASE)</option>
                  <option value="PEAK">Giờ cao điểm (PEAK)</option>
                  <option value="HOLIDAY">Ngày lễ (HOLIDAY)</option>
              </select>
            </div>

            <div class="col-md-6">
              <label class="form-label">Loại sân (Để trống nếu áp dụng tất cả)</label>
              <select class="form-select" name="fieldTypeId" id="fieldTypeId">
                  <option value="">-- Tất cả loại sân --</option>
                  <% if (fieldTypes != null) {
                       for (FieldType ft : fieldTypes) { %>
                           <option value="<%= ft.getFieldTypeId() %>"><%= ft.getTypeName() %> (Sân <%= ft.getNumberOfPlayers() %> người)</option>
                  <% } } %>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label">Sân cụ thể (Để trống nếu áp dụng tất cả)</label>
              <select class="form-select" name="fieldId" id="fieldId">
                  <option value="">-- Tất cả sân --</option>
                  <% if (fields != null) {
                       for (Field f : fields) { %>
                           <option value="<%= f.getFieldId() %>"><%= f.getFieldName() %></option>
                  <% } } %>
              </select>
            </div>

            <div class="col-md-6">
              <label class="form-label">Thứ trong tuần (VD: Monday, Tuesday)</label>
              <select class="form-select" name="dayOfWeek" id="dayOfWeek">
                  <option value="">-- Tất cả --</option>
                  <option value="Monday">Thứ Hai</option>
                  <option value="Tuesday">Thứ Ba</option>
                  <option value="Wednesday">Thứ Tư</option>
                  <option value="Thursday">Thứ Năm</option>
                  <option value="Friday">Thứ Sáu</option>
                  <option value="Saturday">Thứ Bảy</option>
                  <option value="Sunday">Chủ Nhật</option>
              </select>
            </div>
            <div class="col-md-6">
              <label class="form-label">Ngày đặc biệt (Lễ/Tết)</label>
              <input type="date" class="form-control" name="specificDate" id="specificDate">
            </div>

            <div class="col-md-6">
              <label class="form-label">Giờ bắt đầu</label>
              <input type="time" class="form-control" name="startTime" id="startTime">
            </div>
            <div class="col-md-6">
              <label class="form-label">Giờ kết thúc</label>
              <input type="time" class="form-control" name="endTime" id="endTime">
            </div>

            <div class="col-md-6">
              <label class="form-label">Giá tiền (VNĐ) <span class="text-danger">*</span></label>
              <input type="number" step="0.01" class="form-control" name="price" id="price" required>
            </div>
            <div class="col-md-6">
              <label class="form-label">Độ ưu tiên (Số càng lớn ưu tiên càng cao)</label>
              <input type="number" class="form-control" name="priority" id="priority" value="0">
            </div>
          </div>
        </form>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
        <button type="submit" form="priceRuleForm" class="btn btn-sf-primary">Lưu bảng giá</button>
      </div>
    </div>
  </div>

<!-- Modal Xác nhận xóa -->
<div class="modal fade" id="deleteConfirmModal" tabindex="-1">
  <div class="modal-dialog modal-sm modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <div class="modal-body text-center p-4">
        <i class="bi bi-exclamation-circle text-danger mb-3" style="font-size: 3rem;"></i>
        <h5 class="fw-bold mb-3">Xác nhận xóa</h5>
        <p class="text-muted mb-4">Bạn có chắc chắn muốn xóa luật giá này không? Hành động này không thể hoàn tác.</p>
        <form method="post" action="<%= ctx %>/owner/price-rules" id="deleteForm">
            <input type="hidden" name="action" value="delete">
            <input type="hidden" name="complexId" value="<%= selectedComplexId %>">
            <input type="hidden" name="priceRuleId" id="deletePriceRuleId" value="">
            <div class="d-flex justify-content-center gap-2">
                <button type="button" class="btn btn-light px-4" data-bs-dismiss="modal">Hủy</button>
                <button type="submit" class="btn btn-danger px-4">Đồng ý Xóa</button>
            </div>
        </form>
      </div>
    </div>
  </div>
</div>

<div id="footer" data-root="<%= ctx %>/"></div>

<script>
    window.APP_CTX = '<%= ctx %>';
    display_name = '<%= displayName %>';
    current_role = '<%= navRole %>';
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/app.js"></script>
<script>
    function openAddModal() {
        document.getElementById('priceRuleModalTitle').innerText = 'Thêm Bảng giá mới';
        document.getElementById('formAction').value = 'add';
        document.getElementById('priceRuleForm').reset();
        document.getElementById('priceRuleId').value = '';
        var myModal = new bootstrap.Modal(document.getElementById('priceRuleModal'));
        myModal.show();
    }

    function openEditModal(id, ruleName, ruleType, fieldTypeId, fieldId, dayOfWeek, specificDate, startTime, endTime, price, priority) {
        document.getElementById('priceRuleModalTitle').innerText = 'Chỉnh sửa Bảng giá';
        document.getElementById('formAction').value = 'edit';
        document.getElementById('priceRuleId').value = id;
        document.getElementById('ruleName').value = ruleName;
        document.getElementById('ruleType').value = ruleType;
        document.getElementById('fieldTypeId').value = fieldTypeId;
        document.getElementById('fieldId').value = fieldId;
        document.getElementById('dayOfWeek').value = dayOfWeek;
        document.getElementById('specificDate').value = specificDate;
        
        if (startTime && startTime.length > 5) startTime = startTime.substring(0,5);
        if (endTime && endTime.length > 5) endTime = endTime.substring(0,5);
        document.getElementById('startTime').value = startTime;
        document.getElementById('endTime').value = endTime;
        
        document.getElementById('price').value = price;
        document.getElementById('priority').value = priority;
        
        var myModal = new bootstrap.Modal(document.getElementById('priceRuleModal'));
        myModal.show();
    }

    function confirmDelete(id) {
        document.getElementById('deletePriceRuleId').value = id;
        var myModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
        myModal.show();
    }
</script>
</body>
</html>
