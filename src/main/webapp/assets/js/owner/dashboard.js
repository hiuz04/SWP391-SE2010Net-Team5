// Lưu đường context của trang
const ctx = window.APP_CTX || "";

function loadData() {
    fetch(`${ctx}/api/dashboard`)
        .then(res => res.json())
        .then(data => {
            //Booking
            document.getElementById(
                "todayBooking"
            ).innerText = data.todayBooking;

            const bookingDiff = document.getElementById("bookingDifference");
            bookingDiff.innerText = `${data.bookingDifferent > 0 ? "+" : ""} ${data.bookingDifferent} booking`;

            if (data.bookingDifferent >= 0) {
                bookingDiff.className = "text-success small";
            } else {
                bookingDiff.className = "text-danger small";
            }

            // Revenue
            document.getElementById("monthRevenue").innerText =
                data.monthRevenue.toLocaleString("vi-VN") + " VNĐ";

            const revenueGrowth = document.getElementById("revenueGrowth");

            revenueGrowth.innerText =
                `${data.revenueGrowthPercent.toFixed(1)}%`;

            if (data.revenueGrowthPercent >= 0) {
                revenueGrowth.className = "text-success small";
            } else {
                revenueGrowth.className = "text-danger small";
            }

            // Field
            document.getElementById("activeFields").innerText = data.activeFields;
            document.getElementById("totalFields").innerText = `/${data.totalFields} sân`;

            // Chart
            const chart = document.getElementById("revenueChart");
            chart.innerHTML = "";

            const maxRevenue =
                Math.max(
                    ...data.revenue7Days
                        .map(x => x.revenue)
                );

            data.revenue7Days.forEach(item => {
                const percent =
                    item.revenue / maxRevenue * 100;
                chart.innerHTML +=
                    `
                        <div
                            class="bg-sf-primary
                                   rounded-top
                                   flex-fill"
                
                            style="
                                height:${percent}%;
                            ">
                        </div>
                    `;
            });
        });
}