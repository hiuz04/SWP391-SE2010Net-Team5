// Lưu đường context của trang
const ctx = window.APP_CTX || "";

// Biến toàn cục để lưu đối tượng Chart.js.
// Mục đích: Nếu dashboard được load lại nhiều lần thì phải destroy chart cũ trước khi tạo chart mới.
let revenueChart = null;

function loadStatisticsData() {
    fetch(`${ctx}/owner/api/dashboard`)
        .then(res => res.json())
        .then(data => {
            console.log(">>> data ", data);

            // ===========================
            // Booking Statistics
            // ===========================
            document.getElementById("todayBooking").innerHTML = data.todayBooking;

            // const bookingDiff = document.getElementById("bookingDifference");
            // const bookingSign = data.bookingDifferent >= 0 ? "+" : "";

            // bookingDiff.innerHTML =
            //     `<i class="bi bi-arrow-${data.bookingDifferent >= 0 ? "up" : "down"}"></i>
            //      ${bookingSign}${data.bookingDifferent} booking so hôm qua`;
            // bookingDiff.className =
            //     data.bookingDifferent >= 0
            //         ? "text-success small"
            //         : "text-danger small";

            // ===========================
            // Revenue Statistics
            // ===========================
            document.getElementById("monthRevenue").innerHTML =
                data.monthRevenue.toLocaleString("vi-VN") + " ₫";

            // const revenueGrowth = document.getElementById("revenueGrowth");
            // const growthSign = data.revenueGrowthPercent >= 0 ? "+" : "";

            // revenueGrowth.innerHTML =
            //     `<i class="bi bi-arrow-${data.revenueGrowthPercent >= 0 ? "up" : "down"}"></i>
            //      ${growthSign}${data.revenueGrowthPercent.toFixed(1)}% tháng trước`;
            // revenueGrowth.className =
            //     data.revenueGrowthPercent >= 0
            //         ? "text-success small"
            //         : "text-danger small";

            // ===========================
            // Field Statistics
            // ===========================
            document.getElementById("activeFields").innerText = data.activeFields;
            document.getElementById("totalFields").innerText =
                `/${data.totalFields} sân`;

            // ===========================
            // Voucher Statistics
            // ===========================
            document.getElementById("activeVouchers").innerText = data.totalVouchers;

            // =====================================================
            // Chuẩn bị dữ liệu để vẽ biểu đồ doanh thu 7 ngày
            // =====================================================
            // Trục X: hiển thị theo định dạng DD/MM
            const labels = data.revenue7Days.map(item => {
                const date = new Date(item.date);
                const day = String(date.getDate()).padStart(2, "0");
                const month = String(date.getMonth() + 1).padStart(2, "0");

                return `${day}/${month}`;
            });

            // Trục Y
            const revenues = data.revenue7Days.map(item => item.revenue);

            // =====================================================
            // Nếu đã có biểu đồ trước đó thì xóa đi
            // =====================================================
            if (revenueChart !== null) {
                revenueChart.destroy();
            }

            // =====================================================
            // Tạo biểu đồ mới
            // =====================================================
            const ctxChart =
                document.getElementById("revenueChart").getContext("2d");

            revenueChart = new Chart(ctxChart, {
                // Kiểu biểu đồ
                type: "bar",
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: "Doanh thu",
                            data: revenues,
                            backgroundColor: "#198754",
                            borderRadius: 8,
                            maxBarThickness: 40
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            display: false
                        },
                        tooltip: {
                            callbacks: {
                                // Hiển thị tooltip theo định dạng tiền Việt Nam
                                label: function (context) {
                                    return context.raw.toLocaleString("vi-VN") + " ₫";
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function (value) {
                                    return value.toLocaleString("vi-VN");
                                }
                            }
                        }
                    }
                }
            });
        })
        .catch(err => {
            console.error("Dashboard load error:", err);
        });
}