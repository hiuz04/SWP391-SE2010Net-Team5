// Lưu đường context của trang
const ctx = window.APP_CTX || "";
const displayName = display_name;
const currentRole = current_role;

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

const menuItems = [
    {
        title: "Dashboard",
        path: "owner/dashboard",
        icon: "bi bi-grid-1x2-fill"
    },
    {
        title: "Cụm sân",
        path: "owner/complex",
        icon: "bi bi-buildings-fill"
    },
    {
        title: "Sân bóng",
        path: "owner/field",
        icon: "bi bi-dribbble"
    },
    {
        title: "Quản lý ca trực",
        path: "owner/work-shift",
        icon: "bi bi-person-workspace"
    },
    {
        title: "Bảng giá",
        path: "owner/price-rules",
        icon: "bi bi-cash-stack"
    },
    {
        title: "Voucher",
        path: "owner/vouchers",
        icon: "bi bi-ticket-perforated-fill"
    }
];

function displayTopLeftMenu() {
    const container = document.getElementById("owner-sidebar");
    const current = window.location.pathname.replace(ctx + "/", "");

    container.innerHTML = `
        <div class="sidebar-logo">
            <span class="logo-box">⚽</span>

            <div>
                <a href="${link(ctx, '/index')}"><h5>Sport Field Booking</h5></a>
                <small>Owner Panel</small>
            </div>
        </div>

        <div class="owner-menu">
            <ul class="owner-menu-list">
                ${menuItems.map(item => `
                    <li>
                        <a href="${ctx}/${item.path}"
                           class="owner-menu-item ${current === item.path ? "active" : ""}">
                            <i class="${item.icon}"></i>
                            <span>${item.title}</span>
                        </a>
                    </li>
                `).join("")}
            </ul>
        </div>

        <div class="sidebar-user mt-auto">
            <div class="avatar">
                <i class="bi bi-person-fill"></i>
            </div>

            <div>
                <div class="fw-semibold">Owner</div>
                <small class="text-white">Football Booking</small>
            </div>
        </div>
    `;
}

function link(root, href) {
    if (href.startsWith('http')) return href;
    return (root || '') + href;
}

function renderTopbar(
    {
      title = "Dashboard",
      subtitle = ""
    } = {}
) {
    const target = document.getElementById('topbar');
    if (!target) return;
    const root = ctx;
    const name = displayName;
    target.innerHTML = `
        <div>
            <h2 class="mb-0">${title}</h2>
            <small class="text-muted">
                ${subtitle}
            </small>
        </div>
        <div class="d-flex gap-2">
            <div class="dropdown me-2">
                <button class="btn btn-light position-relative dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false" title="Thông báo">
                   <i class="bi bi-bell"></i>
                   <span id="notifBadge" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="display:none; font-size: 0.6rem;">0</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow" id="notifDropdown" style="width: 300px; max-height: 400px; overflow-y: auto;">
                   <li><h6 class="dropdown-header d-flex justify-content-between align-items-center">
                       <span>Thông báo</span>
                       <button class="btn btn-sm text-primary p-0" onclick="markAllAsRead(event)" style="font-size: 0.8rem;">Đánh dấu đã đọc</button>
                   </h6></li>
                   <div id="notifList">
                       <li><span class="dropdown-item text-center text-muted py-3">Đang tải...</span></li>
                   </div>
                   <li><hr class="dropdown-divider"></li>
                   <li><a class="dropdown-item text-center text-primary" href="${link(root, '/notifications')}">Xem tất cả</a></li>
                </ul>
            </div>
            <div class="dropdown">
                <button class="btn btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown"><i class="bi bi-person-circle me-1"></i>${name}</button>
                <ul class="dropdown-menu dropdown-menu-end shadow">
                  <li><h6 class="dropdown-header">Tài khoản</h6></li>
                  <li><a class="dropdown-item text-danger" href="${link(root, '/logout')}"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
                </ul>
            </div>
        </div>
      `
}

window.markAsRead = function(id) {
    const root = ctx;
    fetch(link(root, '/api/notifications'), {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'action=mark_read&id=' + id
    }).then(res => res.json()).then(data => {
        if(data.success) {
            updateNotificationCount();
        }
    });
};

window.markAllAsRead = function(e) {
    if(e) e.stopPropagation();
    const root = ctx;
    fetch(link(root, '/api/notifications'), {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'action=mark_all_read'
    }).then(res => res.json()).then(data => {
        if(data.success) {
            updateNotificationCount();
        }
    });
};

function escapeHtml(value) {
    if (value == null) return '';
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function notificationHref(root, notification) {
    const type = notification.notificationType || notification.notification_type;
    const ref = notification.referenceId || notification.reference_id;
    if ((type === 'CHECKOUT_PAYMENT' || type === 'CHECKOUT_PAYMENT_SUCCESS') && ref) {
        return link(root, 'customer/checkout-invoice?id=' + encodeURIComponent(ref));
    }
    if ((type === 'BOOKING' || type === 'REMINDER') && ref) {
        return link(root, 'booking?action=detail&id=' + encodeURIComponent(ref));
    }
    return '#';
}

window.handleNotificationClick = function(event, id, href, title, message) {
    if (event) event.preventDefault();
    const root = ctx;
    fetch(link(root, '/api/notifications'), {
        method: 'POST',
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        body: 'action=mark_read&id=' + encodeURIComponent(id)
    }).then(() => {
        if (href && href !== '#') {
            window.location.href = href;
        } else {
            updateNotificationCount();
            if (title && message) showNotificationDetail(title, message);
        }
    }).catch(() => {
        if (href && href !== '#') {
            window.location.href = href;
        } else {
            if (title && message) showNotificationDetail(title, message);
        }
    });
};

function updateNotificationCount() {
    const target = document.getElementById('topbar');
    if (!target) return;
    const root = ctx;

    fetch(link(root, '/api/notifications'))
        .then(res => {
            if (!res.ok) throw new Error('Not logged in');
            return res.json();
        })
        .then(data => {
            const badge = document.getElementById('notifBadge');
            if (badge) {
                if (data.unreadCount > 0) {
                    badge.textContent = data.unreadCount > 99 ? '99+' : data.unreadCount;
                    badge.style.display = 'inline-block';
                } else {
                    badge.style.display = 'none';
                }
            }

            const list = document.getElementById('notifList');
            if (list && data.notifications) {
                if (data.notifications.length === 0) {
                    list.innerHTML = '<li><span class="dropdown-item text-center text-muted py-3">Không có thông báo mới</span></li>';
                } else {
                    let html = '';
                    data.notifications.forEach(n => {
                        const bg = n.isRead ? '' : 'bg-light';
                        const href = notificationHref(root, n);
                        const titleEsc = escapeHtml(n.title).replace(/'/g, "\\'");
                        const msgEsc = escapeHtml(n.message).replace(/'/g, "\\'").replace(/\n/g, "\\n");
                        html += `<li>
                        <a class="dropdown-item border-bottom py-2 ${bg}" href="${href}" onclick="handleNotificationClick(event, ${n.notificationId || n.notification_id}, '${href}', '${titleEsc}', '${msgEsc}')">
                            <div class="fw-bold" style="font-size:0.85rem">${escapeHtml(n.title)}</div>
                            <div class="text-wrap text-muted" style="font-size:0.8rem; line-height: 1.2;">${escapeHtml(n.message)}</div>
                        </a>
                    </li>`;
                    });
                    list.innerHTML = html;
                }
            }
        })
        .catch(e => console.log('Could not fetch notifications', e));
}

displayTopLeftMenu();
updateNotificationCount();