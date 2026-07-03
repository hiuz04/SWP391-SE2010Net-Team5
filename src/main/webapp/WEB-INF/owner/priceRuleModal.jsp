<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.swp.model.Facility" %>
<%@ page import="com.swp.model.FieldType" %>
<%
    List<Facility> facilities = (List<Facility>) request.getAttribute("facilities");
    List<FieldType> fieldTypes = (List<FieldType>) request.getAttribute("fieldTypes");
    String ctx = request.getContextPath();
%>
<div class="modal fade" id="priceRuleModal" tabindex="-1" aria-labelledby="priceRuleModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form id="priceRuleForm" action="<%= ctx %>/owner/price-rules" method="POST">
                <div class="modal-header">
                    <h5 class="modal-title" id="priceRuleModalLabel">Thêm Luật Giá Mới</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" name="action" id="modalAction" value="add">
                    <input type="hidden" name="priceRuleId" id="priceRuleId" value="">
                    
                    <div class="row g-3">
                        <div class="col-md-12">
                            <label for="ruleName" class="form-label">Tên luật giá <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="ruleName" name="ruleName" required placeholder="Ví dụ: Giờ vàng cuối tuần">
                        </div>
                        
                        <div class="col-md-6">
                            <label for="ruleType" class="form-label">Loại quy tắc</label>
                            <select class="form-select" id="ruleType" name="ruleType" required>
                                <option value="WEEKDAY">Ngày thường (Thứ 2 - Thứ 6)</option>
                                <option value="WEEKEND">Cuối tuần (Thứ 7, CN)</option>
                                <option value="HOLIDAY">Ngày lễ</option>
                                <option value="PEAK">Giờ cao điểm</option>
                                <option value="OFF_PEAK">Giờ thấp điểm</option>
                                <option value="SPECIFIC_DATE">Ngày cụ thể</option>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label for="facilityId" class="form-label">Cơ sở áp dụng</label>
                            <select class="form-select" id="facilityId" name="facilityId">
                                <option value="">-- Tất cả cơ sở --</option>
                                <% if(facilities != null) { for(Facility fac : facilities) { %>
                                    <option value="<%= fac.getFacilityId() %>"><%= fac.getFacilityName() %></option>
                                <% } } %>
                            </select>
                        </div>
                        
                        <div class="col-md-6">
                            <label for="fieldTypeId" class="form-label">Loại sân áp dụng</label>
                            <select class="form-select" id="fieldTypeId" name="fieldTypeId">
                                <option value="">-- Tất cả loại sân --</option>
                                <% if(fieldTypes != null) { for(FieldType ft : fieldTypes) { %>
                                    <option value="<%= ft.getFieldTypeId() %>"><%= ft.getTypeName() %></option>
                                <% } } %>
                            </select>
                        </div>

                        <div class="col-md-6">
                            <label for="price" class="form-label">Mức giá (VNĐ) <span class="text-danger">*</span></label>
                            <input type="number" class="form-control" id="price" name="price" required min="0" step="1000">
                        </div>

                        <div class="col-md-6">
                            <label for="startTime" class="form-label">Từ giờ (Bỏ trống = Cả ngày)</label>
                            <input type="time" class="form-control" id="startTime" name="startTime">
                        </div>

                        <div class="col-md-6">
                            <label for="endTime" class="form-label">Đến giờ (Bỏ trống = Cả ngày)</label>
                            <input type="time" class="form-control" id="endTime" name="endTime">
                        </div>

                        <div class="col-md-6" id="dayOfWeekContainer" style="display: none;">
                            <label for="dayOfWeek" class="form-label">Thứ áp dụng</label>
                            <input type="text" class="form-control" id="dayOfWeek" name="dayOfWeek" placeholder="Ví dụ: T7, CN hoặc T2, T3">
                        </div>

                        <div class="col-md-6" id="specificDateContainer" style="display: none;">
                            <label for="specificDate" class="form-label">Ngày áp dụng</label>
                            <input type="date" class="form-control" id="specificDate" name="specificDate">
                        </div>
                        
                        <div class="col-md-6">
                            <label for="priority" class="form-label">Độ ưu tiên (Số càng cao, ưu tiên càng lớn)</label>
                            <input type="number" class="form-control" id="priority" name="priority" value="0" min="0">
                        </div>

                        <div class="col-md-6">
                            <label for="status" class="form-label">Trạng thái</label>
                            <select class="form-select" id="status" name="status">
                                <option value="ACTIVE">Hoạt động</option>
                                <option value="INACTIVE">Tạm dừng</option>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-sf-primary" style="background: rgb(5, 150, 105); color: white;">Lưu thay đổi</button>
                </div>
            </form>
        </div>
    </div>
</div>
