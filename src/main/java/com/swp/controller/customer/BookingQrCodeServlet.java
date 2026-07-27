package com.swp.controller.customer;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.swp.dao.BookingDAO;
import com.swp.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;

@WebServlet(name = "BookingQrCodeServlet", urlPatterns = {"/booking/qr"})
public class BookingQrCodeServlet extends HttpServlet {

    private static final String CUSTOMER_ROLE_NAME = "CUSTOMER";
    private static final int QR_SIZE = 280;

    private final BookingDAO bookingDAO = new BookingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        User currentUser = getSessionUser(request);
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để xem QR booking.");
            return;
        }

        if (!CUSTOMER_ROLE_NAME.equalsIgnoreCase(currentUser.getRoleName())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Customer được xem QR booking.");
            return;
        }

        Long bookingId = parseBookingId(request.getParameter("id"));
        if (bookingId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "bookingId không hợp lệ.");
            return;
        }

        try {
            String bookingCode = bookingDAO.getBookingCodeByIdAndCustomerId(bookingId, currentUser.getUserId());
            if (bookingCode == null || bookingCode.isBlank()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy booking.");
                return;
            }

            BitMatrix qrMatrix = createQrMatrix(bookingCode);

            response.setContentType("image/png");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setHeader("X-Content-Type-Options", "nosniff");

            MatrixToImageWriter.writeToStream(qrMatrix, "PNG", response.getOutputStream());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Có lỗi khi tải QR booking.");
        } catch (WriterException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tạo QR booking.");
        }
    }

    private User getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            return null;
        }

        return (User) session.getAttribute("user");
    }

    private Long parseBookingId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BitMatrix createQrMatrix(String bookingCode) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);

        return new MultiFormatWriter().encode(
                bookingCode,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                hints
        );
    }
}
