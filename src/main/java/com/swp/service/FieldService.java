package com.swp.service;

import com.swp.dao.BookingDAO;
import com.swp.dao.FieldDAO;
import com.swp.model.Field;

import java.util.List;
import java.util.Set;

public class FieldService {

    private static final FieldDAO fieldDao = new FieldDAO();
    private static final BookingDAO bookingDao = new BookingDAO();

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "AVAILABLE",
            "INACTIVE",
            "MAINTENANCE",
            "REMOVED"
    );

    public void insertField(Field field) {
        fieldDao.insertField(field);
    }

    public void updateField(Field field) {
        fieldDao.updateField(field);
    }

    public void deleteField(long id) {

        int bookingCount = bookingDao.getBookingCountWithFieldId(id);
        if(bookingCount > 0) {
            throw new IllegalStateException("Không thể xóa sân vì vẫn còn booking liên quan.");
        }

        fieldDao.deleteField(id);
    }

    public Field getFieldInfo(long id) {
        return fieldDao.getFieldByID(id);
    }

    public List<Field> getFieldOfThisComplex(long id) {
        return fieldDao.getFieldBelongToThisComplexId(id);
    }

    public List<Field> getAllField() {
        return fieldDao.getAllField();
    }

    public void changeStatus(long id, String status) {
        Field field = fieldDao.getFieldByID(id);

        if (field == null) {
            throw new IllegalArgumentException("Không tìm thấy cụm sân.");
        }

        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }

        fieldDao.updateStatus(id, status);
    }

    public List<Field> searchField(String fieldName, String status, Long typeId, long complexId) {
        return fieldDao.searchField(fieldName, status, typeId, complexId);
    }
}
