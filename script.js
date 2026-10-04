const fullNameInput = document.getElementById('fullName');
const emailInput = document.getElementById('email');
const phoneInput = document.getElementById('phone');
const messageInput = document.getElementById('message');
const loginBtn = document.getElementById('loginBtn');
const loginForm = document.getElementById('loginForm');
const banner = document.getElementById('banner');
const loadingOverlay = document.getElementById('loadingOverlay');

const fieldErrors = {
  fullName: document.getElementById('fullNameError'),
  email: document.getElementById('emailError'),
  phone: document.getElementById('phoneError'),
  message: document.getElementById('messageError')
};

function setFieldError(fieldName, message) {
  const field = document.getElementById(fieldName);
  const errorBox = fieldErrors[fieldName];

  if (field) field.classList.toggle('input-error', Boolean(message));
  if (errorBox) errorBox.textContent = message || '';
}

function clearAllErrors() {
  Object.keys(fieldErrors).forEach((fieldName) => setFieldError(fieldName, ''));
}

function validateForm() {
  let valid = true;
  const fullName = fullNameInput.value.trim();
  const email = emailInput.value.trim();
  const phone = phoneInput.value.trim();
  const message = messageInput.value.trim();

  if (!fullName) {
    setFieldError('fullName', 'Full name is required.');
    valid = false;
  } else if (fullName.length < 2) {
    setFieldError('fullName', 'Please enter a valid name.');
    valid = false;
  } else {
    setFieldError('fullName', '');
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!email) {
    setFieldError('email', 'Email is required.');
    valid = false;
  } else if (!emailPattern.test(email)) {
    setFieldError('email', 'Please enter a valid email address.');
    valid = false;
  } else {
    setFieldError('email', '');
  }

  if (!phone) {
    setFieldError('phone', 'Phone number is required.');
    valid = false;
  } else if (!/^\+?[0-9\s-]{10,15}$/.test(phone)) {
    setFieldError('phone', 'Please enter a valid phone number.');
    valid = false;
  } else {
    setFieldError('phone', '');
  }

  if (!message) {
    setFieldError('message', 'Please write your message.');
    valid = false;
  } else if (message.length < 10) {
    setFieldError('message', 'Message should be at least 10 characters long.');
    valid = false;
  } else {
    setFieldError('message', '');
  }

  return valid;
}

function updateButtonState() {
  const hasRequiredFields = [fullNameInput.value.trim(), emailInput.value.trim(), phoneInput.value.trim(), messageInput.value.trim()].every((value) => value.length > 0);
  loginBtn.disabled = !hasRequiredFields;
}

[fullNameInput, emailInput, phoneInput, messageInput].forEach((input) => {
  input.addEventListener('input', () => {
    clearAllErrors();
    updateButtonState();
  });
});

function showBanner(message, type) {
  banner.textContent = message;
  banner.className = 'banner ' + (type === 'success' ? 'success' : '');
  banner.classList.remove('hidden');
}

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  clearAllErrors();

  if (!validateForm()) {
    showBanner('Please fix the highlighted fields and try again.', 'error');
    return;
  }

  loginBtn.disabled = true;
  loginBtn.textContent = 'Sending...';
  loadingOverlay.classList.remove('hidden');

  try {
    const response = await fetch('/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        fullName: fullNameInput.value.trim(),
        email: emailInput.value.trim(),
        phone: phoneInput.value.trim(),
        message: messageInput.value.trim()
      })
    });

    const data = await response.json();
    const isSuccess = response.ok && data.status === 'ok';
    showBanner(data.message || 'Your enquiry was submitted.', isSuccess ? 'success' : 'error');

    if (isSuccess) {
      loginForm.reset();
      updateButtonState();
    }
  } catch (error) {
    showBanner('Could not reach the server. Please check if the Java backend is running.', 'error');
  } finally {
    loadingOverlay.classList.add('hidden');
    loginBtn.textContent = 'Submit enquiry';
    updateButtonState();
  }
});
