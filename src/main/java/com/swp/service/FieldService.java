package com.swp.service;

import com.swp.dao.BookingDAO;
import com.swp.dao.FieldDAO;
import com.swp.model.Field;

import java.util.List;

public class FieldService {

    private static final FieldDAO fieldDao = new FieldDAO();
    private static final BookingDAO bookingDao = new BookingDAO();

    public void addField(Field field) {
        fieldDao.addField(field);
    }

    public void updateField(Field field) {
        fieldDao.editField(field);
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

}
