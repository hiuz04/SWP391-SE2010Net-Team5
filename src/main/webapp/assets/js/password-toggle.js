document.addEventListener('click', function (e) {
  const btn = e.target.closest('.password-toggle-btn');
  if (!btn) return;

  const wrap = btn.closest('.password-toggle-wrap');
  if (!wrap) return;

  const input = wrap.querySelector('input');
  if (!input) return;

  const isHidden = input.type === 'password';
  input.type = isHidden ? 'text' : 'password';

  const icon = btn.querySelector('i');
  if (icon) {
    icon.classList.toggle('bi-eye', !isHidden);
    icon.classList.toggle('bi-eye-slash', isHidden);
  }
  
  btn.setAttribute('aria-label', isHidden ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
  btn.setAttribute('aria-pressed', isHidden ? 'true' : 'false');
});
