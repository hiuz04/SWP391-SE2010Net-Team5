(function () {
  const form = document.getElementById('registerForm');
  if (!form) return;

  const emailPattern = /^[a-zA-Z0-9.]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  const phonePattern = /^0[35789]\d{8}$/;
  const fullNamePattern = /^[\p{L}][\p{L}\s'.]{0,98}[\p{L}.]$|^[\p{L}]{2,}$/u;

  const fields = {
    fullName: {
      validate: (v) => {
        // Business Rule BR-26: SRS yêu cầu họ tên 2-50 ký tự, chỉ chữ cái và khoảng trắng.
        // Frontend hiện tại vẫn kiểm tra 2-100 ký tự và pattern tên rộng hơn.
        if (!v) return 'Họ tên không được để trống.';
        if (v.length < 2 || v.length > 100) return 'Họ tên phải từ 2 đến 100 ký tự.';
        if (!fullNamePattern.test(v)) return 'Họ tên chỉ được chứa chữ cái và khoảng trắng.';
        return '';
      }
    },
    phone: {
      validate: (v) => {
        // Business Rule BR-28: SRS yêu cầu số bắt đầu bằng 0 và dài 10-11 chữ số.
        // Frontend đăng ký hiện tại vẫn dùng regex số di động VN 10 chữ số.
        if (!v) return 'Số điện thoại không được để trống.';
        if (!phonePattern.test(v)) return 'Số điện thoại không đúng định dạng (10 số, mạng VN).';
        return '';
      }
    },
    email: {
      validate: (v) => {
        // Business Rule BR-27: SRS yêu cầu email đúng định dạng quốc tế và tối đa 50 ký tự.
        // Frontend hiện tại vẫn kiểm tra tối đa 100 ký tự với regex email cơ bản.
        if (!v) return 'Email không được để trống.';
        if (v.length > 100) return 'Email không được vượt quá 100 ký tự.';
        if (!emailPattern.test(v)) return 'Email không đúng định dạng.';
        return '';
      }
    },
    password: {
      validate: (v) => {
        if (!v) return 'Mật khẩu không được để trống.';
        if (v.length < 6) return 'Mật khẩu phải có ít nhất 6 ký tự.';
        if (v.length > 64) return 'Mật khẩu không được vượt quá 64 ký tự.';
        if (!/[A-Za-z\u00C0-\u1EF9]/.test(v) || !/\d/.test(v)) {
          return 'Mật khẩu phải có ít nhất 1 chữ cái và 1 chữ số.';
        }
        return '';
      }
    },
    confirmPassword: {
      validate: (v, formEl) => {
        const password = formEl.password.value;
        if (!v) return 'Vui lòng xác nhận mật khẩu.';
        if (v !== password) return 'Mật khẩu xác nhận không khớp.';
        return '';
      }
    }
  };

  function getFieldContainer(input) {
    return input.closest('.col-md-6, .col-12');
  }

  function getFeedback(input) {
    const container = getFieldContainer(input);
    return container ? container.querySelector('.invalid-feedback') : null;
  }

  function showError(input, message) {
    input.classList.add('is-invalid');
    input.classList.remove('is-valid');
    const feedback = getFeedback(input);
    if (feedback) feedback.textContent = message;
  }

  function clearError(input) {
    input.classList.remove('is-invalid');
    const feedback = getFeedback(input);
    if (feedback && input.dataset.serverError !== 'true') {
      feedback.textContent = feedback.getAttribute('data-default') || '';
    }
  }

  function validateField(name) {
    const config = fields[name];
    const input = form[name];
    if (!config || !input) return true;

    const message = config.validate(input.value.trim(), form);
    if (message) {
      showError(input, message);
      return false;
    }
    input.classList.remove('is-invalid');
    input.classList.add('is-valid');
    return true;
  }

  Object.keys(fields).forEach((name) => {
    const input = form[name];
    if (!input) return;
    input.addEventListener('blur', () => validateField(name));
    input.addEventListener('input', () => {
      if (input.classList.contains('is-invalid')) validateField(name);
    });
  });

  form.addEventListener('submit', (e) => {
    let valid = true;
    Object.keys(fields).forEach((name) => {
      if (!validateField(name)) valid = false;
    });
    if (!valid) e.preventDefault();
  });

  document.querySelectorAll('.is-invalid').forEach((input) => {
    input.dataset.serverError = 'true';
  });
})();
