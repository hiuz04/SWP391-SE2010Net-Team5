(function () {
  document.querySelectorAll('.password-toggle-wrap').forEach(function (wrap) {
    const input = wrap.querySelector('input');
    const btn = wrap.querySelector('.password-toggle-btn');
    if (!input || !btn) return;

    const icon = btn.querySelector('i');
    btn.addEventListener('click', function () {
      const isHidden = input.type === 'password';
      input.type = isHidden ? 'text' : 'password';
      if (icon) {
        icon.classList.toggle('bi-eye', !isHidden);
        icon.classList.toggle('bi-eye-slash', isHidden);
      }
      btn.setAttribute('aria-label', isHidden ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      btn.setAttribute('aria-pressed', isHidden ? 'true' : 'false');
    });
  });
})();
