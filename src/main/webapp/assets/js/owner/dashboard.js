// Lưu đường context của trang
const ctx = window.APP_CTX || "";

function loadStatisticsData() {
    fetch(`${ctx}/owner/api/dashboard`)
        .then(res => res.json())
        .then(data => {
            // Booking
            const bookingEl = document.getElementById("todayBooking");
            bookingEl.innerHTML = data.todayBooking;

            const bookingDiff = document.getElementById("bookingDifference");
            const diffSign = data.bookingDifferent >= 0 ? "+" : "";
            bookingDiff.innerHTML = `<i class="bi bi-arrow-${data.bookingDifferent >= 0 ? 'up' : 'down'}"></i> ${diffSign}${data.bookingDifferent} booking so hôm qua`;
            bookingDiff.className = data.bookingDifferent >= 0 ? "text-success small" : "text-danger small";

            // Revenue
            const revenueEl = document.getElementById("monthRevenue");
            revenueEl.innerHTML = data.monthRevenue.toLocaleString("vi-VN") + " ₫";

            const revenueGrowth = document.getElementById("revenueGrowth");
            const growthSign = data.revenueGrowthPercent >= 0 ? "+" : "";
            revenueGrowth.innerHTML = `<i class="bi bi-arrow-${data.revenueGrowthPercent >= 0 ? 'up' : 'down'}"></i> ${growthSign}${data.revenueGrowthPercent.toFixed(1)}% tháng trước`;
            revenueGrowth.className = data.revenueGrowthPercent >= 0 ? "text-success small" : "text-danger small";

            // Fields
            document.getElementById("activeFields").innerText = data.activeFields;
            document.getElementById("totalFields").innerText = `/${data.totalFields} sân`;

            // Chart
            const chart = document.getElementById("revenueChart");
            chart.innerHTML = "";

            const revenues = data.revenue7Days.map(x => x.revenue);
            const maxRevenue = Math.max(...revenues, 1);

            const dayNames = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

            data.revenue7Days.forEach((item, index) => {
                const pct = Math.max((item.revenue / maxRevenue) * 100, 2);
                const dateObj = new Date(item.date);
                const dayLabel = dayNames[dateObj.getDay()] || `N${index + 1}`;
                const formattedRevenue = item.revenue.toLocaleString("vi-VN") + "₫";

                const wrap = document.createElement("div");
                wrap.className = "chart-bar-wrap";
                wrap.title = `${item.date}: ${formattedRevenue}`;
                wrap.innerHTML = `
                    <span class="chart-bar-value">${item.revenue > 0 ? (item.revenue / 1000).toFixed(0) + 'k' : ''}</span>
                    <div class="chart-bar" style="height: ${pct}%;"></div>
                    <span class="chart-bar-label">${dayLabel}</span>
                `;
                chart.appendChild(wrap);
            });
        })
        .catch(err => console.error('Dashboard load error:', err));
}