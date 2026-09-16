document.addEventListener('DOMContentLoaded', () => {
    if (localStorage.getItem('adminToken')) {
        window.location.href = 'admin_dashboard.html';
        return;
    }
    document.getElementById('admin-login-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = e.target.querySelector('[type="submit"]');
        const errEl = document.getElementById('login-error');
        errEl.classList.remove('show');
        setLoading(btn, true);

        try {
            const data = await apiFetch('/api/admin/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    email: document.getElementById('admin-email').value.trim(),
                    password: document.getElementById('admin-password').value
                })
            });
            localStorage.setItem('adminToken', data.token);
            localStorage.setItem('adminName', data.name);
            localStorage.setItem('adminEmail', data.email);
            window.location.href = 'admin_dashboard.html';
        } catch (err) {
            errEl.textContent = 'Invalid email or password. Please try again.';
            errEl.classList.add('show');
            setLoading(btn, false);
        }
    });
});
