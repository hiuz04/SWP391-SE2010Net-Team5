package com.swp.controller.owner;

import com.swp.dao.VoucherDAO;
import com.swp.model.User;
import com.swp.model.Voucher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@WebServlet("/owner/vouchers")
public class VoucherManagementServlet extends HttpServlet {

    private static final String MANAGEMENT_PATH = "/owner/vouchers";
    private static final String LIST_VIEW = "/WEB-INF/owner/vouchers/list.jsp";
    private static final String FORM_VIEW = "/WEB-INF/owner/vouchers/form.jsp";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String TYPE_PERCENT = "PERCENT";
    private static final String TYPE_FIXED = "FIXED";

    private VoucherDAO voucherDAO;

    @Override
    public void init() throws ServletException {
        voucherDAO = new VoucherDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = trim(request.getParameter("action"));
        try {
            if ("create".equals(action)) {
                showForm(request, response, new Voucher(), "create");
            } else if ("edit".equals(action)) {
                int id = parsePositiveInt(request.getParameter("id"), "Mã giảm giá không hợp lệ.");
                Voucher voucher = voucherDAO.findById(id);
                if (voucher == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy mã giảm giá.");
                    return;
                }
                showForm(request, response, voucher, "edit");
            } else {
                List<Voucher> vouchers = voucherDAO.getAllVouchers();
                request.setAttribute("vouchers", vouchers);
                request.getRequestDispatcher(LIST_VIEW).forward(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Không thể xử lý mã giảm giá.", e);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String action = trim(request.getParameter("action"));
        try {
            if ("create".equals(action)) {
                Voucher voucher = parseVoucher(request, false);
                voucher.setUsed(0);
                ensureUniqueCode(voucher.getCode(), 0);
                voucherDAO.createVoucher(voucher);
                session.setAttribute("successMessage", "Tạo mã giảm giá thành công.");
                response.sendRedirect(request.getContextPath() + MANAGEMENT_PATH);
            } else if ("edit".equals(action)) {
                Voucher voucher = parseVoucher(request, true);
                Voucher existingVoucher = voucherDAO.findById(voucher.getId());
                if (existingVoucher == null) {
                    throw new IllegalArgumentException("Không tìm thấy mã giảm giá.");
                }
                voucher.setUsed(existingVoucher.getUsed());
                validateEditableQuantity(voucher, existingVoucher.getUsed());
                ensureUniqueCode(voucher.getCode(), voucher.getId());
                voucherDAO.updateVoucher(voucher);
                session.setAttribute("successMessage", "Cập nhật mã giảm giá thành công.");
                response.sendRedirect(request.getContextPath() + MANAGEMENT_PATH);
            } else if ("toggle-status".equals(action)) {
                int id = parsePositiveInt(request.getParameter("id"), "Mã giảm giá không hợp lệ.");
                Voucher voucher = voucherDAO.findById(id);
                if (voucher == null) {
                    throw new IllegalArgumentException("Không tìm thấy mã giảm giá.");
                }
                String nextStatus = STATUS_ACTIVE.equalsIgnoreCase(voucher.getStatus())
                        ? STATUS_DISABLED
                        : STATUS_ACTIVE;
                voucherDAO.updateStatus(id, nextStatus);
                session.setAttribute("successMessage", "Đã cập nhật trạng thái mã giảm giá.");
                response.sendRedirect(request.getContextPath() + MANAGEMENT_PATH);
            } else {
                response.sendRedirect(request.getContextPath() + MANAGEMENT_PATH);
            }
        } catch (IllegalArgumentException e) {
            if ("toggle-status".equals(action)) {
                session.setAttribute("errorMessage", e.getMessage());
                response.sendRedirect(request.getContextPath() + MANAGEMENT_PATH);
                return;
            }
            Voucher voucher = safeParseVoucherForReturn(request);
            if ("edit".equals(action)) {
                try {
                    restoreExistingUsage(voucher);
                } catch (SQLException sqlException) {
                    throw new ServletException("Không thể tải số lượt đã dùng của mã giảm giá.", sqlException);
                }
            }
            request.setAttribute("error", e.getMessage());
            showForm(request, response, voucher, "edit".equals(action) ? "edit" : "create");
        } catch (SQLException e) {
            throw new ServletException("Không thể lưu mã giảm giá.", e);
        }
    }


    private void showForm(
            HttpServletRequest request,
            HttpServletResponse response,
            Voucher voucher,
            String mode
    ) throws ServletException, IOException {
        request.setAttribute("voucher", voucher);
        request.setAttribute("mode", mode);
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    private Voucher parseVoucher(HttpServletRequest request, boolean requireId) {
        Voucher voucher = new Voucher();
        if (requireId) {
            voucher.setId(parsePositiveInt(request.getParameter("id"), "Mã giảm giá không hợp lệ."));
        }

        voucher.setCode(requireText(request.getParameter("code"), "Mã giảm giá không được để trống.")
                .toUpperCase(Locale.ROOT));
        voucher.setName(requireText(request.getParameter("name"), "Tên mã giảm giá không được để trống."));
        voucher.setDiscountType(normalize(request.getParameter("discountType")));
        voucher.setDiscountValue(parseMoney(request.getParameter("discountValue"), "Giá trị giảm giá không hợp lệ."));
        voucher.setMinOrder(parseMoney(defaultIfBlank(request.getParameter("minOrder"), "0"), "Giá trị đơn tối thiểu không hợp lệ."));
        voucher.setQuantity(parsePositiveInt(request.getParameter("quantity"), "Số lượng mã giảm giá phải lớn hơn 0."));
        voucher.setUsed(0);
        voucher.setStartDate(parseDateTime(request.getParameter("startDate"), "Ngày bắt đầu không hợp lệ."));
        voucher.setEndDate(parseDateTime(request.getParameter("endDate"), "Ngày kết thúc không hợp lệ."));
        voucher.setStatus(normalize(request.getParameter("status")));

        validateVoucher(voucher);
        return voucher;
    }

    private Voucher safeParseVoucherForReturn(HttpServletRequest request) {
        Voucher voucher = new Voucher();
        try {
            String id = trim(request.getParameter("id"));
            if (id != null && !id.isEmpty()) {
                voucher.setId(Integer.parseInt(id));
            }
        } catch (NumberFormatException ignored) {
        }
        voucher.setCode(trim(request.getParameter("code")));
        voucher.setName(trim(request.getParameter("name")));
        voucher.setDiscountType(trim(request.getParameter("discountType")));
        voucher.setDiscountValue(safeMoney(request.getParameter("discountValue")));
        voucher.setMinOrder(safeMoney(request.getParameter("minOrder")));
        voucher.setQuantity(safeInt(request.getParameter("quantity")));
        voucher.setUsed(0);
        voucher.setStartDate(safeDateTime(request.getParameter("startDate")));
        voucher.setEndDate(safeDateTime(request.getParameter("endDate")));
        voucher.setStatus(trim(request.getParameter("status")));
        return voucher;
    }

    private void validateVoucher(Voucher voucher) {
        if (!TYPE_PERCENT.equals(voucher.getDiscountType()) && !TYPE_FIXED.equals(voucher.getDiscountType())) {
            throw new IllegalArgumentException("Loại giảm giá chỉ được là phần trăm hoặc số tiền cố định.");
        }
        if (voucher.getDiscountValue().signum() <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
        }
        if (TYPE_PERCENT.equals(voucher.getDiscountType())
                && voucher.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Mã giảm theo phần trăm không được lớn hơn 100.");
        }
        if (voucher.getMinOrder().signum() < 0) {
            throw new IllegalArgumentException("Đơn tối thiểu không được âm.");
        }
        if (voucher.getStartDate().isAfter(voucher.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        if (!STATUS_ACTIVE.equals(voucher.getStatus()) && !STATUS_DISABLED.equals(voucher.getStatus())) {
            throw new IllegalArgumentException("Trạng thái chỉ được là đang hoạt động hoặc tạm tắt.");
        }
    }

    private void validateEditableQuantity(Voucher voucher, int currentUsed) {
        if (voucher.getQuantity() < currentUsed) {
            throw new IllegalArgumentException("Số lượng mã giảm giá không được nhỏ hơn số lượt đã dùng hiện tại.");
        }
    }

    private void restoreExistingUsage(Voucher voucher) throws SQLException {
        if (voucher.getId() <= 0) {
            return;
        }

        Voucher existingVoucher = voucherDAO.findById(voucher.getId());
        if (existingVoucher != null) {
            voucher.setUsed(existingVoucher.getUsed());
        }
    }

    private void ensureUniqueCode(String code, int currentId) throws SQLException {
        Voucher existing = voucherDAO.findByCode(code);
        if (existing != null && existing.getId() != currentId) {
            throw new IllegalArgumentException("Mã giảm giá đã tồn tại.");
        }
    }

    private int parsePositiveInt(String rawValue, String message) {
        int value = parseNonNegativeInt(rawValue, message);
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private int parseNonNegativeInt(String rawValue, String message) {
        String value = requireText(rawValue, message);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private BigDecimal parseMoney(String rawValue, String message) {
        String value = requireText(rawValue, message);
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private LocalDateTime parseDateTime(String rawValue, String message) {
        String value = requireText(rawValue, message);
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(message, e);
        }
    }

    private String requireText(String rawValue, String message) {
        String value = trim(rawValue);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String defaultIfBlank(String rawValue, String defaultValue) {
        String value = trim(rawValue);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    private String normalize(String value) {
        String trimmed = trim(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal safeMoney(String rawValue) {
        try {
            String value = trim(rawValue);
            return value == null || value.isEmpty() ? BigDecimal.ZERO : new BigDecimal(value);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private int safeInt(String rawValue) {
        try {
            String value = trim(rawValue);
            return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private LocalDateTime safeDateTime(String rawValue) {
        try {
            String value = trim(rawValue);
            return value == null || value.isEmpty() ? null : LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
