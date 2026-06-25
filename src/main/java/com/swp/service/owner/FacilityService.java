package com.swp.service.owner;

import com.swp.dao.BookingDAO;
import com.swp.dao.FacilityDAO;
import com.swp.dao.FieldDAO;
import com.swp.model.Booking;
import com.swp.model.Facility;
import com.swp.model.FacilityImage;
import com.swp.service.CloudinaryService;

import java.io.IOException;
import java.util.List;

public class FacilityService {

    private static final FacilityDAO facilityDAO = new FacilityDAO();
    private static final BookingDAO bookingDao = new BookingDAO();
    private static final FieldDAO fieldDao = new FieldDAO();
    private static final CloudinaryService cloudinaryService = new CloudinaryService();

    public long addFacility(Facility facility) {
        return facilityDAO.addFacility(facility);
    }

    public List<Facility>  getListFacility() {
        return facilityDAO.getAllFacility();
    }

    public Facility getFacilityInfo(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid facility id.");
        }

       return facilityDAO.getFacilityDataByID(id);
    }

    public void updateFacility(Facility facility) {
        facilityDAO.editFacility(facility);
    }

    public void deleteFacility(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid facility id.");
        }

        // Thêm handle chặn nếu có booking
        int bookingCount = bookingDao.getBookingCountWithFacilityId(id);
        if(bookingCount > 0)
        {
            throw new IllegalStateException("Không thể xóa cơ sở vì vẫn còn booking liên quan.");
        }
        // Thêm handle yêu cầu xóa field trước
        int fieldCount = fieldDao.getFieldCountWithFacilityId(id);
        if((fieldCount > 0)) {
            throw new IllegalStateException("Yêu cầu chủ sân xóa toàn bộ các sân có liên quan tới cơ sở trước khi thử lại.");
        }

        deleteAllImageRelatedToFacilityOnCloudinary(id);
        facilityDAO.deleteAllImageRelatedToFacility(id);
        facilityDAO.deleteFacility(id);
    }

    public void addImg(FacilityImage image) {
        facilityDAO.addImage(image);
    }

    public void updateImg(long id, boolean isThumbnail) {
        facilityDAO.updateImage(id, isThumbnail);
    }

    public void deleteImg(long id) {
        facilityDAO.deleteImage(id);
    }

    public List<FacilityImage> getFacilityImg(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid facility id.");
        }

        return facilityDAO.getAllImage(id);
    }

    public FacilityImage getImgById(long id) {
        return facilityDAO.getImgById(id);
    }

    public FacilityImage getThumbnail(long id) {
        if(id <= 0 ) {
            throw new IllegalArgumentException("Invalid facility id.");
        }

        return facilityDAO.getThumbnail(id);
    }

    private void deleteAllImageRelatedToFacilityOnCloudinary(long id) {
        List<FacilityImage> images =
                facilityDAO.getAllImage(id);

        for (FacilityImage image : images) {
            try {
                cloudinaryService.delete(image.getPublicId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
