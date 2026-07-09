/**
 * File: app.js
 * Description: Xử lý render các thành phần giao diện dùng chung (Navbar và Footer).
 *
 * Author: Dương Hải Anh
 * Version: 1.0
 * Created date: 01/06/2026
 */
(function () {
  const pages = {
    customer: [
      ['Trang chủ','index'], ['Tìm sân','search'], ['Tìm đối','matchmaking'], ['Lịch sử đặt sân','booking?action=history'], ['Hồ sơ','profile']
    ],
    staff: [
      ['Dashboard','staff/dashboard'], ['Lịch trong ngày','staff/schedule'], ['Check-in','staff/checkin']
    ],
    owner: [

      ['Dashboard','owner'], ['Cơ sở','owner/facility'], ['Sân bóng','owner/field'], ['Quản lý ca trực','owner/work-shift'], ['Bảng giá','owner/price-rules']

    ],
    admin: [
      ['Dashboard','admin/dashboard'], ['Người dùng','admin/users'], ['Cài đặt','admin/settings']
    ]
  };

  function link(root, href) {
    if (href.startsWith('http')) return href;
    return (root || '') + href;
  }

  function renderNavbar() {
    const target = document.getElementById('navbar');
    if (!target) return;
    const root = target.dataset.root || '';
    const role = target.dataset.role || 'guest';
    const name = target.dataset.name || 'Người dùng';
    const active = target.dataset.active || '';
    console.log(">>> target: ", target );
    console.log(">>> active: ", active);
    const roleLinks = pages[role] || pages.customer;
    console.log(">>> roleLinks: ", roleLinks);
    const auth = role === 'guest'
      ? `<a class="btn btn-outline-success" href="${link(root, 'login')}">Đăng nhập</a><a class="btn btn-sf-primary" href="${link(root, 'register')}">Đăng ký</a>`
      : `<div class="dropdown me-2">
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
               <li><a class="dropdown-item text-center text-primary" href="${link(root, 'notifications')}">Xem tất cả</a></li>
            </ul>
         </div>
         <div class="dropdown">
            <button class="btn btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown"><i class="bi bi-person-circle me-1"></i>${name}</button>
            <ul class="dropdown-menu dropdown-menu-end shadow">
              <li><h6 class="dropdown-header">Tài khoản</h6></li>
              <li><a class="dropdown-item" href="${link(root, 'profile')}">Thông tin cá nhân</a></li>
              <li><a class="dropdown-item" href="${link(root, role !== 'guest' ? (pages[role] && pages[role][0] ? pages[role][0][1] : '#') : '#')}">Khu vực ${role}</a></li>
              <li><hr class="dropdown-divider"></li>
              <li><a class="dropdown-item text-danger" href="${link(root, 'logout')}"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
            </ul>
         </div>`;

    target.innerHTML = `
      <nav class="navbar navbar-expand-lg bg-white border-bottom sticky-top shadow-sm">
        <div class="container">
          <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="${link(root, 'index')}">
            <span class="logo-box">⚽</span><span>Sport Field Booking</span>
          </a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"><span class="navbar-toggler-icon"></span></button>
          <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav ms-auto me-lg-3 mb-2 mb-lg-0">
              ${roleLinks.map(([label, href]) => `
                <li class="nav-item">
                    <a class="nav-link ${active === label ? 'active fw-semibold text-success' : ''}" href="${link(root, href)}">${label}</a>
                </li>`)
              .join('')}
            </ul>
            <div class="d-flex gap-2 align-items-center">${auth}</div>
          </div>
        </div>
      </nav>`;
  }

  function renderFooter() {
    const target = document.getElementById('footer');
    if (!target) return;
    const root = target.dataset.root || '';
    target.innerHTML = `
      <footer class="footer pt-5 pb-4 mt-5">
        <div class="container">
          <div class="row g-4">
            <div class="col-lg-3">
              <div class="d-flex align-items-center gap-2 mb-3"><span class="logo-box">⚽</span><h5 class="mb-0 text-white">Sport Field Booking</h5></div>
              <p>Nền tảng kết nối đam mê, mang đến trải nghiệm đặt sân bóng nhanh chóng, tiện lợi và chuyên nghiệp nhất.</p>
            </div>
            <div class="col-6 col-lg-2">
              <h6 class="text-white">Khám phá</h6>
              <a class="d-block mb-2" href="${link(root, 'explore/about.jsp')}">Giới thiệu</a>
              <a class="d-block mb-2" href="${link(root, 'explore/security-policy.jsp')}">Chính sách bảo mật</a>
              <a class="d-block mb-2" href="${link(root, 'explore/terms-of-use.jsp')}">Điều khoản sử dụng</a>
              <a class="d-block mb-2" href="${link(root, 'explore/user-guide.jsp')}">Hướng dẫn sử dụng</a>
              <a class="d-block mb-2" href="${link(root, 'explore/privacy-policy.jsp')}">Chính sách quyền riêng tư</a>
              <a class="d-block mb-2" href="${link(root, 'explore/contact.jsp')}">Liên Hệ</a>
            </div>
            <div class="col-6 col-lg-2"><h6 class="text-white">Khách hàng</h6><a class="d-block mb-2" href="${link(root, 'index.jsp')}">Trang chủ</a><a class="d-block mb-2" href="${link(root, 'register.jsp')}">Đăng ký</a></div>
            <div class="col-6 col-lg-2"><h6 class="text-white">Tài khoản</h6><a class="d-block mb-2" href="${link(root, 'login.jsp')}">Đăng nhập</a></div>
            <div class="col-lg-3"><h6 class="text-white">Liên hệ</h6><p class="mb-1"><i class="bi bi-geo-alt me-2"></i>Hoà Lạc, Việt Nam</p><p class="mb-1"><i class="bi bi-envelope me-2"></i>tranbaolong.280904@gmail.com</p><p class="mb-1"><i class="bi bi-telephone me-2"></i>0385028924</p></div>
          </div>
          <hr class="border-secondary my-4"><p class="small mb-0">© 2026 Sport Field Booking. Static Bootstrap UI prototype.</p>
        </div>
      </footer>`;
  }

  function initDemoActions() {
    document.querySelectorAll('[data-demo-alert]').forEach(btn => {
      btn.addEventListener('click', () => alert(btn.getAttribute('data-demo-alert')));
    });
    document.querySelectorAll('[data-fill-date="today"]').forEach(el => {
      const d = new Date();
      el.value = d.toISOString().slice(0, 10);
    });
  }

  window.markAllAsRead = function(e) {
      if(e) e.stopPropagation();
      const target = document.getElementById('navbar');
      const root = target ? (target.dataset.root || '') : '';
      fetch(link(root, 'api/notifications'), {
          method: 'POST',
          headers: {'Content-Type': 'application/x-www-form-urlencoded'},
          body: 'action=mark_all_read'
      }).then(res => res.json()).then(data => {
          if(data.success) {
              updateNotificationCount();
          }
      });
  };

  window.markAsRead = function(id) {
      const target = document.getElementById('navbar');
      const root = target ? (target.dataset.root || '') : '';
      fetch(link(root, 'api/notifications'), {
          method: 'POST',
          headers: {'Content-Type': 'application/x-www-form-urlencoded'},
          body: 'action=mark_read&id=' + id
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

  window.handleNotificationClick = function(event, id, href) {
      if (event) event.preventDefault();
      const target = document.getElementById('navbar');
      const root = target ? (target.dataset.root || '') : '';
      fetch(link(root, 'api/notifications'), {
          method: 'POST',
          headers: {'Content-Type': 'application/x-www-form-urlencoded'},
          body: 'action=mark_read&id=' + encodeURIComponent(id)
      }).then(() => {
          if (href && href !== '#') {
              window.location.href = href;
          } else {
              updateNotificationCount();
          }
      }).catch(() => {
          if (href && href !== '#') {
              window.location.href = href;
          }
      });
  };

  window.updateNotificationCount = function() {
    const target = document.getElementById('navbar');
    if (!target) return;
    const root = target.dataset.root || '';
    const role = target.dataset.role || 'guest';
    
    if (role === 'guest') return;

    fetch(link(root, 'api/notifications'))
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
        if (list) {
            if (!data.notifications || data.notifications.length === 0) {
                list.innerHTML = '<li><span class="dropdown-item text-center text-muted py-3">Không có thông báo mới</span></li>';
            } else {
                let html = '';
                data.notifications.forEach(n => {
                    const bg = n.isRead ? '' : 'bg-light';
                    const href = notificationHref(root, n);
                    html += `<li>
                        <a class="dropdown-item border-bottom py-2 ${bg}" href="${href}" onclick="handleNotificationClick(event, ${n.notificationId}, '${href}')">
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
  };

  document.addEventListener('DOMContentLoaded', function () {
    renderNavbar();
    renderFooter();
    initDemoActions();
    updateNotificationCount();
  });
})();
