<!--
* Module: Field Management
* File: field-form.jsp
* Description: Trang nhập liệu để thêm mới hoặc cập nhật thông tin cơ sở.
*
* Author: Duong Hai Anh
* Version: 1.0
* Created Date: 04/06/2026
-->
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<div class="modal fade field-modal" id="fieldFormModal">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">

      <form>
        <input type="hidden" name="fieldID" id="fieldID">

        <div class="modal-header custom-header">
          <h5 class="modal-title" id="fieldTitle"></h5>
          <button type="button" class="btn-close btn-close-white"
                  data-bs-dismiss="modal"></button>
        </div>

        <div class="modal-body">

          <div class="mb-3">
            <label for="fieldName" class="form-label">Tên sân <span class="text-danger">*</span></label>
            <input type="text" name="fieldName" id="fieldName"
                   class="form-control custom-input" required>
          </div>

          <div class="mb-3">
            <label for="desc" class="form-label">Mô tả sân</label>
            <input type="text" name="description" id="desc"
                   class="form-control custom-input">
          </div>

          <div class="mb-3">
            <label for="typeF" class="form-label">Loại sân <span class="text-danger">*</span></label>
            <select name="fieldTypeID" id="typeF"
                    class="form-select custom-input" required>
            </select>
          </div>

          <div class="mb-3">
            <label for="fc" class="form-label">Cơ sở sở hữu sân <span class="text-danger">*</span></label>
            <select name="complexId" id="fc"
                    class="form-select custom-input" required>
            </select>
          </div>

          <div class="mb-3">
            <label for="status" class="form-label">Trạng thái sân</label>
            <select name="status" id="status"
                    class="form-select custom-input" required>
              <option value="AVAILABLE">Có sẵn</option>
              <option value="OCCUPIED">Đã được chọn</option>
              <option value="BOOKED">Đã đặt</option>
              <option value="INACTIVE">Ngừng hoạt động</option>
              <option value="MAINTENANCE">Bảo trì</option>
            </select>
          </div>

        </div>

        <div class="modal-footer">
          <button type="button" id="submitBtn"
                  class="btn custom-btn"
                  onclick="submitField()">
          </button>
        </div>

      </form>

    </div>
  </div>
</div>