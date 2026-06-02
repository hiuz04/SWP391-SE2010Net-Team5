
(function () {
  const pages = {
    customer: [
      ['Trang chủ','index.jsp'], ['Tìm sân','#'], ['Tìm đối','#'], ['Lịch sử đặt sân','#'], ['Hồ sơ','#']
    ],
    staff: [
      ['Dashboard','UI/staff/dashboard.html'], ['Lịch trong ngày','UI/staff/schedule.html'], ['Check-in','UI/staff/checkin.html'], ['Checkout','UI/staff/checkout.html']
    ],
    owner: [
      ['Dashboard','UI/owner/dashboard.html'], ['Cơ sở','#'], ['Sân bóng','#'], ['Bảng giá','#'], ['Dịch vụ','#']
    ],
    admin: [
      ['Dashboard','UI/admin/dashboard.html'], ['Người dùng','#'], ['Duyệt chủ sân','#'], ['Cài đặt','#']
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
    const roleLinks = pages[role] || pages.customer;
    const auth = role === 'guest'
      ? `<a class="btn btn-outline-success" href="${link(root, 'login')}">Đăng nhập</a><a class="btn btn-sf-primary" href="${link(root, 'register')}">Đăng ký</a>`
      : `<a class="btn btn-light position-relative" href="${link(root, '#')}" title="Thông báo"><i class="bi bi-bell"></i><span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle"></span></a>
         <div class="dropdown">
           <button class="btn btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown"><i class="bi bi-person-circle me-1"></i>${name}</button>
           <ul class="dropdown-menu dropdown-menu-end shadow">
             <li><h6 class="dropdown-header">Tài khoản</h6></li>
             <li><a class="dropdown-item" href="${link(root, '#')}">Thông tin cá nhân</a></li>
             <li><a class="dropdown-item" href="${link(root, role !== 'guest' ? (pages[role] && pages[role][0] ? pages[role][0][1] : '#') : '#')}">Khu vực ${role}</a></li>
             <li><hr class="dropdown-divider"></li>
             <li><a class="dropdown-item text-danger" href="${link(root, 'logout')}"><i class="bi bi-box-arrow-right me-2"></i>Đăng xuất</a></li>
           </ul>
         </div>`;

    target.innerHTML = `
      <nav class="navbar navbar-expand-lg bg-white border-bottom sticky-top shadow-sm">
        <div class="container">
          <a class="navbar-brand d-flex align-items-center gap-2 fw-bold" href="${link(root, 'index.jsp')}">
            <span class="logo-box">⚽</span><span>Sport Field Booking</span>
          </a>
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav"><span class="navbar-toggler-icon"></span></button>
          <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav ms-auto me-lg-3 mb-2 mb-lg-0">
              ${roleLinks.map(([label, href]) => `<li class="nav-item"><a class="nav-link ${active === label ? 'active fw-semibold text-success' : ''}" href="${link(root, href)}">${label}</a></li>`).join('')}
              <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">Vai trò</a>
                <ul class="dropdown-menu shadow">
                  <li><a class="dropdown-item" href="${link(root, 'index.jsp')}">Customer</a></li>
                </ul>
              </li>
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
            <div class="col-lg-4">
              <div class="d-flex align-items-center gap-2 mb-3"><span class="logo-box">⚽</span><h5 class="mb-0 text-white">Sport Field Booking</h5></div>
              <p>Phiên bản chuyển đổi sang HTML, CSS, Bootstrap. Dữ liệu hiện là mock data để phục vụ demo giao diện.</p>
            </div>
            <div class="col-6 col-lg-2"><h6 class="text-white">Khách hàng</h6><a class="d-block mb-2" href="${link(root, 'index.jsp')}">Trang chủ</a><a class="d-block mb-2" href="${link(root, 'register')}">Đăng ký</a></div>
            <div class="col-6 col-lg-2"><h6 class="text-white">Tài khoản</h6><a class="d-block mb-2" href="${link(root, 'login')}">Đăng nhập</a></div>
            <div class="col-lg-4"><h6 class="text-white">Liên hệ</h6><p class="mb-1"><i class="bi bi-geo-alt me-2"></i>TP.HCM, Việt Nam</p><p class="mb-1"><i class="bi bi-envelope me-2"></i>support@sportfield.local</p></div>
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

  document.addEventListener('DOMContentLoaded', function () {
    renderNavbar();
    renderFooter();
    initDemoActions();
  });
})();
