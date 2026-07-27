<%@ page import="com.swp.model.User" %>
    <!--
* Module: Owner Dashboard
* File: dashboard.jsp
* Description: Hiển thị các chỉ số vận hành chính (lượt đặt sân trong ngày, doanh thu, số sân đang hoạt động, đánh giá),
*              biểu đồ doanh thu tuần.
*
* Author: Dương Hải Anh
* Version: 2.0
-->
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
        <%@ page import="com.swp.model.User" %>
            <%! private String esc(String value) { if (value==null) return "" ; return value.replace("&", "&amp;"
                ).replace("<", "&lt;" ) .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
                }
                %>
                <% User sessionUser=(User) request.getAttribute("sessionUser"); if (sessionUser==null)
                    sessionUser=(User) session.getAttribute("user"); String navRole=(String)
                    request.getAttribute("navRole"); if (navRole==null) navRole=sessionUser==null ? "guest" : (String)
                    session.getAttribute("navRole"); if (navRole==null) navRole="guest" ; String displayName=sessionUser
                    !=null ? sessionUser.getFullName() : "" ; String ctx=request.getContextPath(); %>
                    <!DOCTYPE html>
                    <html lang="vi">

                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <meta name="description"
                            content="Owner Dashboard - Theo dõi hiệu quả kinh doanh cơ sở sân bóng">
                        <link rel="preconnect" href="https://fonts.googleapis.com">
                        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                        <link
                            href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap"
                            rel="stylesheet">
                        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
                            rel="stylesheet">
                        <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css"
                            rel="stylesheet">
                        <link rel="stylesheet"
                            href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.7.2/css/all.min.css">
                        <link href="<%= ctx %>/assets/css/styles.css" rel="stylesheet">
                        <link href="<%= ctx %>/assets/css/owner/dashboard.css" rel="stylesheet">
                        <title>Owner Dashboard | Sport Field Booking</title>
                    </head>

                    <body>
                        <div id="navbar" data-root="<%= ctx %>/" data-role="<%= navRole %>"
                            data-name="<%= displayName %>" data-active="Dashboard"></div>

                        <main class="owner-content">

                            <!-- Page Header -->
                            <div class="page-header">
                                <div class="page-header-left">
                                    <h1><i class="bi bi-speedometer2 me-2"></i>Owner Dashboard</h1>
                                    <p>Theo dõi các chỉ số và hiệu quả kinh doanh của cơ sở sân bóng.</p>
                                </div>
                            </div>

                            <!-- Stat Cards -->
                            <div class="row g-4 mb-4">
                                <div class="col-xl-3 col-md-6">
                                    <div class="dashboard-card">
                                        <div class="icon bg-primary-subtle">
                                            <i class="bi bi-calendar-check-fill"></i>
                                        </div>
                                        <span>Đặt sân hôm nay</span>
                                        <h2 id="todayBooking">
                                            <span class="skeleton d-inline-block"
                                                style="width:60px;height:36px;"></span>
                                        </h2>
                                        <!-- <small id="bookingDifference" class="text-success">
                                            +0 booking
                                        </small> -->
                                    </div>
                                </div>

                                <div class="col-xl-3 col-md-6">
                                    <div class="dashboard-card">
                                        <div class="icon bg-success-subtle">
                                            <i class="bi bi-currency-dollar"></i>
                                        </div>
                                        <span>Doanh thu tháng</span>
                                        <h2 id="monthRevenue" style="font-size:1.5rem">
                                            <span class="skeleton d-inline-block"
                                                style="width:100px;height:36px;"></span>
                                        </h2>
                                        <!-- <small id="revenueGrowth" class="text-success">
                                            +0%
                                        </small> -->
                                    </div>
                                </div>

                                <div class="col-xl-3 col-md-6">
                                    <div class="dashboard-card">
                                        <div class="icon bg-warning-subtle">
                                            <i class="fa-solid fa-futbol"></i>
                                        </div>
                                        <span>Sân đang hoạt động</span>
                                        <h2 id="activeFields">
                                            <span class="skeleton d-inline-block"
                                                style="width:50px;height:36px;"></span>
                                        </h2>
                                        <small id="totalFields" class="text-muted">
                                            /0 sân
                                        </small>
                                    </div>
                                </div>

                                <div class="col-xl-3 col-md-6">
                                    <div class="dashboard-card">
                                        <div class="icon bg-danger-subtle">
                                            <i class="bi bi-ticket-perforated-fill"></i>
                                        </div>
                                        <span>Voucher</span>
                                        <h2 id="activeVouchers">
                                            <span class="skeleton d-inline-block"
                                                style="width:50px;height:36px;"></span>
                                        </h2>
                                        <small class="badge-soft-success"
                                            style="border-radius:20px;padding:3px 9px;font-size:.75rem">
                                            Đang hoạt động
                                        </small>
                                    </div>
                                </div>
                            </div>

                            <!-- Revenue Chart -->
                            <div class="chart-wrapper">
                                <div class="chart-header">
                                    <div>
                                        <h5>
                                            <i class="bi bi-bar-chart-fill me-2 text-success"></i>
                                            Doanh thu 7 ngày gần nhất
                                        </h5>
                                        <p class="mb-0 text-muted" style="font-size:.8rem">
                                            Cập nhật theo thời gian thực
                                        </p>
                                    </div>
                                </div>

                                <div style="height:320px">
                                    <canvas id="revenueChart"></canvas>
                                </div>
                            </div>
                        </main>
                        <div id="footer" data-root="../../"></div>

                        <script>
                            window.APP_CTX = '<%= ctx %>';
                        </script>
                        <script
                            src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
                        <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                        <script src="<%= ctx %>/assets/js/app.js"></script>
                        <script src="<%= ctx %>/assets/js/owner/dashboard.js"></script>
                        <script>
                            loadStatisticsData();
                        </script>
                    </body>

                    </html>