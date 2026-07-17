<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.swp.model.User" %>
<%
    User sessionUser = (User) session.getAttribute("user");
    String navRole = sessionUser == null ? "guest" : (String) session.getAttribute("navRole");
    if (navRole == null) {
        navRole = "guest";
    }
    String displayName = sessionUser != null ? sessionUser.getFullName() : "";
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
    <link href="<%= ctx %>/assets/css/customer/feedback.css" rel="stylesheet">

    <title>Đánh giá trải nghiệm | Sport Field Booking</title>
</head>
<body>
<div class="container py-5">
    <div class="row justify-content-center">
        <div class="col-lg-8">

            <div class="card shadow-sm">
                <div class="card-header bg-success text-white">
                    <h4 class="mb-0">
                        <i class="bi bi-star-fill me-2"></i>
                        Đánh giá trải nghiệm
                    </h4>
                </div>

                <div class="card-body">
                    <form>
                        <input type="hidden" id="bookingId" value="${bookingId}">
                        <input type="hidden" id="feedbackId" value="${feedbackId}">

                        <!-- Đánh giá -->
                        <div class="mb-4">
                            <label class="form-label fw-semibold">
                                Mức độ hài lòng <span class="text-danger">*</span>
                            </label>

                            <div class="rating">
                                <% for (int i = 5; i >= 1; i--) { %>
                                <input type="radio"
                                       id="star<%= i %>"
                                       name="rating"
                                       value="<%= i %>"
                                >

                                <label for="star<%= i %>">
                                    <i class="bi bi-star-fill"></i>
                                </label>
                                <% } %>
                            </div>
                        </div>

                        <!-- Mô tả -->
                        <div class="mb-4">
                            <label class="form-label fw-semibold">
                                Chia sẻ trải nghiệm
                            </label>

                            <textarea
                                    class="form-control"
                                    name="description"
                                    rows="5"
                                    maxlength="1000"
                                    placeholder="Hãy chia sẻ trải nghiệm của bạn tại sân bóng..."
                            ></textarea>

                            <div class="form-text">
                                Tối đa 1000 ký tự.
                            </div>
                        </div>

                        <hr>

                        <div class="d-flex justify-content-end gap-2">
                            <button type="button"
                                    class="btn btn-outline-secondary"
                                    onclick="history.back()"
                            >
                                Hủy
                            </button>

                            <button type="submit"
                                    class="btn btn-success"
                                    id="submitBtn"
                                    onclick="submitForm()"
                            >
                                <i class="bi bi-send-fill me-1"></i>
                                Gửi đánh giá
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    window.APP_CTX = '<%= ctx %>';
    const currentRole = "<%= navRole %>";
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= ctx %>/assets/js/customer/feedback.js"></script>
<script>loadFeedback();</script>
</body>
</html>