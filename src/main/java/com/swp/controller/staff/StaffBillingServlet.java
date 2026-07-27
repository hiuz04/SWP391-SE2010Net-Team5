package com.swp.controller.staff;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.swp.dao.StaffBillingDAO;
import com.swp.model.User;
import com.swp.model.dto.CheckoutResult;
import com.swp.model.dto.CheckoutView;
import com.swp.model.dto.InvoiceView;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Điều phối các trang và API checkout/invoice cho Staff hoặc Owner.
 * Servlet kiểm tra quyền thao tác theo vai trò, gọi StaffBillingDAO để tạo invoice và hỗ trợ xuất PDF hóa đơn.
 */
@WebServlet({
        "/staff/checkout",
        "/staff/invoice",
        "/staff/invoice/export",
        "/api/staff/checkout"
})
public class StaffBillingServlet extends HttpServlet {

    private static final int ROLE_OWNER = 2;
    private static final int ROLE_STAFF = 3;

    private final StaffBillingDAO billingDAO = new StaffBillingDAO();

    @Override
    /**
     * Điều hướng các request xem checkout, xem invoice và xuất PDF invoice.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        String path = getPath(req);
        if (path.startsWith("/staff/invoice/export")) {
            exportInvoice(req, resp, user);
            return;
        }
        if (path.startsWith("/staff/checkout")) {
            showCheckout(req, resp, user);
            return;
        }
        if (path.startsWith("/staff/invoice")) {
            showInvoice(req, resp, user);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    /**
     * API Staff/Owner xác nhận trả sân và gửi yêu cầu thanh toán checkout cho Customer.
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (!getPath(req).equals("/api/staff/checkout")) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJson(resp, false, "Không tìm thấy API.");
            return;
        }

        try {
            long bookingId = requirePositiveLong(req.getParameter("bookingId"), "Mã đặt sân không được bỏ trống.");
            String checkoutPaymentMethod = trim(req.getParameter("checkoutPaymentMethod"));
            CheckoutResult result = billingDAO.completeCheckout(
                    bookingId,
                    user.getUserId(),
                    user.getRoleId() == ROLE_STAFF,
                    checkoutPaymentMethod
            );
            writeCheckoutSuccess(resp, result, req.getContextPath() + "/staff/invoice?id=" + result.getBookingId());
        } catch (SecurityException e) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            writeJson(resp, false, e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, false, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Checkout failed", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, false, "Lỗi hệ thống khi xác nhận trả sân.");
        }
    }

    /**
     * Hiển thị preview checkout nếu booking hợp lệ và Staff có quyền thao tác tại complex đó.
     */
    private void showCheckout(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        Long bookingId = parsePageLong(req, req.getParameter("id"), "Mã đặt sân không được bỏ trống.");
        if (bookingId != null) {
            try {
                CheckoutView checkout = billingDAO.getCheckoutView(bookingId);
                if (checkout == null) {
                    req.setAttribute("error", "Không tìm thấy lịch đặt sân.");
                } else if (!"CHECKED_IN".equals(checkout.getStatus())) {
                    // Business Rule BR-15: Màn hình checkout chỉ mở cho booking đã CHECKED_IN.
                    req.setAttribute("error", "Chỉ lịch đã nhận sân mới được trả sân.");
                } else if (billingDAO.hasPaidInvoice(bookingId)) {
                    req.setAttribute("error", "Lịch đặt sân này đã có hóa đơn thanh toán.");
                } else if (user.getRoleId() == ROLE_STAFF
                        && !billingDAO.canStaffCheckoutComplex(user.getUserId(), checkout.getComplexId())) {
                    // Business Rule BR-12: Staff chỉ được checkout tại complex có ca đang hoạt động.
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("error", "Bạn không có ca làm việc đang hoạt động tại cơ sở này.");
                } else {
                    // TODO Business Rule BR-16: SRS yêu cầu chỉ checkout khi current time >= booking end time;
                    // màn hình hiện tại chưa chặn trường hợp checkout sớm.
                    req.setAttribute("checkout", checkout);
                }
            } catch (SQLException e) {
                getServletContext().log("Cannot load checkout", e);
                req.setAttribute("error", "Không thể tải thông tin trả sân.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/staff/checkout.jsp").forward(req, resp);
    }

    /**
     * Hiển thị invoice checkout cho Staff/Owner.
     * Staff chỉ được xem invoice thuộc complex mà họ có ca trong ngày.
     */
    private void showInvoice(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        Long bookingId = parsePageLong(req, req.getParameter("id"), "Mã đặt sân không được bỏ trống.");
        if (bookingId != null) {
            try {
                InvoiceView invoice = billingDAO.getInvoiceByBookingId(bookingId);
                if (invoice == null) {
                    req.setAttribute("error", "Không tìm thấy hóa đơn.");
                } else if (user.getRoleId() == ROLE_STAFF
                        && !billingDAO.canStaffViewComplexToday(user.getUserId(), invoice.getComplexId())) {
                    // Business Rule BR-12: Staff chỉ được xem hóa đơn thuộc complex mình có ca trong ngày.
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    req.setAttribute("error", "Bạn không có quyền xem hóa đơn này.");
                } else {
                    req.setAttribute("invoice", invoice);
                }
            } catch (SQLException e) {
                getServletContext().log("Cannot load invoice", e);
                req.setAttribute("error", "Không thể tải thông tin hóa đơn.");
            }
        }
        req.getRequestDispatcher("/WEB-INF/staff/invoice.jsp").forward(req, resp);
    }

    /**
     * Xuất invoice thành PDF sau khi kiểm tra quyền xem hóa đơn.
     */
    private void exportInvoice(HttpServletRequest req, HttpServletResponse resp, User user)
            throws IOException {
        try {
            InvoiceView invoice = loadInvoiceForExport(req);
            if (invoice == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy hóa đơn.");
                return;
            }
            // Business Rule BR-12: Xuất PDF invoice cũng phải kiểm tra quyền xem theo ca của Staff.
            if (user.getRoleId() == ROLE_STAFF
                    && !billingDAO.canStaffViewComplexToday(user.getUserId(), invoice.getComplexId())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền xuất hóa đơn này.");
                return;
            }

            String invoiceCode = invoice.getInvoiceCode() == null
                    ? String.valueOf(invoice.getInvoiceId())
                    : invoice.getInvoiceCode();
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"invoice-" + safeFilename(invoiceCode) + ".pdf\"");
            writeInvoicePdf(invoice, resp.getOutputStream());
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            getServletContext().log("Cannot export invoice", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tải hóa đơn.");
        } catch (DocumentException e) {
            getServletContext().log("Cannot render invoice PDF", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể tạo file PDF hóa đơn.");
        }
    }

    /**
     * Cho phép export theo invoiceId trực tiếp hoặc theo bookingId để dùng chung với link từ trang invoice.
     */
    private InvoiceView loadInvoiceForExport(HttpServletRequest req) throws SQLException {
        String invoiceId = trim(req.getParameter("invoiceId"));
        if (invoiceId != null && !invoiceId.isEmpty()) {
            return billingDAO.getInvoiceByInvoiceId(requirePositiveLong(invoiceId, "Mã hóa đơn không hợp lệ."));
        }
        return billingDAO.getInvoiceByBookingId(
                requirePositiveLong(req.getParameter("id"), "Mã đặt sân không được bỏ trống.")
        );
    }

    /**
     * Render hóa đơn PDF từ InvoiceView đã được load và kiểm quyền ở servlet.
     */
    private void writeInvoicePdf(InvoiceView invoice, OutputStream out)
            throws DocumentException, IOException {
        PdfFonts fonts = loadPdfFonts();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph title = new Paragraph("Sport Field Booking", fonts.title());
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        Paragraph subtitle = new Paragraph("Hóa đơn " + text(invoice.getInvoiceCode()), fonts.normal());
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(18);
        document.add(subtitle);

        PdfPTable meta = infoTable();
        addInfo(meta, "Ngày lập", dateTime(invoice.getIssuedAt()), fonts);
        addInfo(meta, "Trạng thái", text(invoice.getInvoiceStatus()), fonts);
        addInfo(meta, "Mã đặt sân", text(invoice.getBookingCode()), fonts);
        addInfo(meta, "Nhân viên", text(invoice.getStaffName()), fonts);
        document.add(meta);

        document.add(section("Thông tin khách hàng", fonts));
        PdfPTable customer = infoTable();
        addInfo(customer, "Tên khách hàng", text(invoice.getCustomerName()), fonts);
        addInfo(customer, "Số điện thoại", text(invoice.getCustomerPhone()), fonts);
        document.add(customer);

        document.add(section("Thông tin đặt sân", fonts));
        PdfPTable booking = infoTable();
        addInfo(booking, "Cơ sở", text(invoice.getComplexName()), fonts);
        addInfo(booking, "Địa chỉ", text(invoice.getComplexAddress()), fonts);
        addInfo(booking, "Sân", text(invoice.getFieldName()), fonts);
        addInfo(booking, "Thời gian", dateTime(invoice.getStartTime()) + " - " + dateTime(invoice.getEndTime()), fonts);
        document.add(booking);

        document.add(section("Tổng kết hóa đơn", fonts));
        PdfPTable summary = new PdfPTable(new float[]{3f, 2f});
        summary.setWidthPercentage(100);
        addHeader(summary, "Nội dung", fonts);
        addHeader(summary, "Số tiền", fonts);
        addMoney(summary, "Tổng tiền thuê sân", invoice.getFieldFee(), fonts);
        addMoney(summary, "Tiền cọc đã thanh toán", invoice.getDepositAmount(), fonts);
        addMoney(summary, "Số tiền còn lại", invoice.getPaidAmount(), fonts);
        addMoney(summary, "Tổng cộng", invoice.getFieldFee(), fonts);
        document.add(summary);

        document.close();
    }

    private PdfFonts loadPdfFonts() throws DocumentException, IOException {
        BaseFont base = BaseFont.createFont(resolveFontPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        return new PdfFonts(
                new Font(base, 18, Font.BOLD),
                new Font(base, 12, Font.BOLD),
                new Font(base, 10, Font.BOLD),
                new Font(base, 10, Font.NORMAL)
        );
    }

    private String resolveFontPath() {
        String windir = System.getenv("WINDIR");
        if (windir != null && !windir.isBlank()) {
            return windir + "\\Fonts\\arial.ttf";
        }
        return "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf";
    }

    private Paragraph section(String label, PdfFonts fonts) {
        Paragraph paragraph = new Paragraph(label, fonts.section());
        paragraph.setSpacingBefore(16);
        paragraph.setSpacingAfter(8);
        return paragraph;
    }

    private PdfPTable infoTable() throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.3f, 2.7f});
        table.setWidthPercentage(100);
        return table;
    }

    private void addInfo(PdfPTable table, String label, String value, PdfFonts fonts) {
        table.addCell(cell(label, fonts.label(), Rectangle.NO_BORDER, Element.ALIGN_LEFT));
        table.addCell(cell(value, fonts.normal(), Rectangle.NO_BORDER, Element.ALIGN_LEFT));
    }

    private void addHeader(PdfPTable table, String text, PdfFonts fonts) {
        PdfPCell cell = cell(text, fonts.label(), Rectangle.BOX, Element.ALIGN_LEFT);
        cell.setBackgroundColor(new java.awt.Color(240, 253, 244));
        table.addCell(cell);
    }

    private void addMoney(PdfPTable table, String label, BigDecimal amount, PdfFonts fonts) {
        table.addCell(cell(label, fonts.normal(), Rectangle.BOX, Element.ALIGN_LEFT));
        table.addCell(cell(money(amount), fonts.normal(), Rectangle.BOX, Element.ALIGN_RIGHT));
    }

    private PdfPCell cell(String text, Font font, int border, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text(text), font));
        cell.setBorder(border);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(7);
        return cell;
    }

    private Long parsePageLong(HttpServletRequest req, String value, String message) {
        try {
            return requirePositiveLong(value, message);
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            return null;
        }
    }

    private long requirePositiveLong(String value, String message) {
        String normalized = trim(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        try {
            long parsed = Long.parseLong(normalized);
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message, e);
        }
    }


    private String getPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        return uri.substring(contextPath.length());
    }

    private void writeCheckoutSuccess(HttpServletResponse resp, CheckoutResult result, String redirectUrl)
            throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":true"
                + ",\"bookingId\":" + result.getBookingId()
                + ",\"invoiceId\":" + result.getInvoiceId()
                + ",\"invoiceCode\":\"" + escapeJson(result.getInvoiceCode()) + "\""
                + ",\"message\":\"" + escapeJson(result.getMessage()) + "\""
                + ",\"redirectUrl\":\"" + escapeJson(redirectUrl) + "\""
                + "}");
        out.flush();
    }

    private void writeJson(HttpServletResponse resp, boolean success, String message) throws IOException {
        PrintWriter out = resp.getWriter();
        out.print("{\"success\":" + success + ",\"error\":\"" + escapeJson(message) + "\"}");
        out.flush();
    }

    private String safeFilename(String value) {
        String safe = value == null ? "invoice" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? "invoice" : safe;
    }

    private String money(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return String.format("%,d đ", amount.longValue());
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record PdfFonts(Font title, Font section, Font label, Font normal) {
    }
}
