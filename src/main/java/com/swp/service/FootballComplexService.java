package com.swp.service;

import com.swp.dao.BookingDAO;
import com.swp.dao.FootballComplexDAO;
import com.swp.dao.FieldDAO;
import com.swp.model.FootballComplex;
import com.swp.model.FootballComplexImage;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class FootballComplexService {

    private static final FootballComplexDAO complexDao = new FootballComplexDAO();
    private static final BookingDAO bookingDao = new BookingDAO();
    private static final FieldDAO fieldDao = new FieldDAO();
    private static final CloudinaryService cloudinaryService = new CloudinaryService();

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "ACTIVE",
            "INACTIVE",
            "MAINTENANCE",
            "CLOSED"
    );

    public long addFootballComplex(FootballComplex fc) {
        return complexDao.addComplex(fc);
    }

    public List<FootballComplex>  getListFootballComplex() {
        return complexDao.getAllComplexExceptDeleteOne();
    }

    public FootballComplex getFootballComplexInfo(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid complex id.");
        }

       return complexDao.getFootballComplexDataByID(id);
    }

    public void updateFootballComplex(FootballComplex complex) {
        complexDao.editFootballComplex(complex);
    }

    public void deleteFootballComplex(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid complex id.");
        }

        // Thêm handle chặn nếu có booking
        int bookingCount = bookingDao.getBookingCountWithComplexId(id);
        if(bookingCount > 0)
        {
            throw new IllegalStateException("Không thể xóa cụm sân vì vẫn còn booking liên quan.");
        }
        // Thêm handle yêu cầu xóa field trước
        int fieldCount = fieldDao.getFieldCountWithComplexId(id);
        if((fieldCount > 0)) {
            throw new IllegalStateException("Yêu cầu chủ sân xóa toàn bộ các sân có liên quan tới cụm sân trước khi thử lại.");
        }

        deleteAllImageRelatedToFootballComplexOnCloudinary(id);
        complexDao.deleteAllImageRelatedToFootballComplex(id);
        complexDao.deleteFootballComplex(id);
    }

    public void addImg(FootballComplexImage image) {
        complexDao.addImage(image);
    }

    public void updateImg(long id, boolean isThumbnail) {
        complexDao.updateImage(id, isThumbnail);
    }

    public void deleteImg(long id) {
        complexDao.deleteImage(id);
    }

    public List<FootballComplexImage> getFootballComplexImg(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid complex id.");
        }

        return complexDao.getAllImage(id);
    }

    public FootballComplexImage getImgById(long id) {
        return complexDao.getImgById(id);
    }

    public FootballComplexImage getThumbnail(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid complex id.");
        }

        return complexDao.getThumbnail(id);
    }

    private void deleteAllImageRelatedToFootballComplexOnCloudinary(long id) {
        List<FootballComplexImage> images =
                complexDao.getAllImage(id);

        for (FootballComplexImage image : images) {
            try {
                cloudinaryService.delete(image.getPublicId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void changeStatus(long id, String status) {
        FootballComplex complex = complexDao.getFootballComplexDataByID(id);

        if (complex == null) {
            throw new IllegalArgumentException("Không tìm thấy cụm sân.");
        }

        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }

        complexDao.updateStatus(id, status);
    }

    public List<FootballComplex> searchComplex(String keyword, String status) {
        return complexDao.searchComplex(keyword, status);
    }

    public  boolean existByName(String complexName) {
        return complexDao.existByName(complexName);
    }

    public boolean existsByNameExceptId(String complexName, long complexId) {
        return complexDao.existsByNameExceptId(complexName, complexId);
    }
}
