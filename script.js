const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const loginBtn = document.getElementById('loginBtn');
const togglePass = document.getElementById('togglePass');
const loginForm = document.getElementById('loginForm');
const banner = document.getElementById('banner');

function updateButtonState() {
  const filled = usernameInput.value.trim().length > 0 && passwordInput.value.length > 0;
  loginBtn.disabled = !filled;
}

usernameInput.addEventListener('input', updateButtonState);
passwordInput.addEventListener('input', updateButtonState);

togglePass.addEventListener('click', () => {
  const isPassword = passwordInput.type === 'password';
  passwordInput.type = isPassword ? 'text' : 'password';
  togglePass.textContent = isPassword ? 'Hide' : 'Show';
});

function showBanner(message, type) {
  banner.textContent = message;
  banner.className = 'banner ' + (type === 'success' ? 'success' : '');
  banner.classList.remove('hidden');
}

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  loginBtn.disabled = true;
  loginBtn.textContent = 'Logging in...';

  try {
    const res = await fetch('/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: usernameInput.value.trim(),
        password: passwordInput.value
      })
    });
    const data = await res.json();
    showBanner(data.message || 'Login attempt recorded.', 'success');
  } catch (err) {
    showBanner('Could not reach the server. Is LoginServer.java running?', 'error');
  } finally {
    loginBtn.textContent = 'Log in';
    updateButtonState();
  }
});
