// Global Authentication State & Session Sync for Johuri.ai

function getLoggedInUser() {
    try {
        const userStr = localStorage.getItem('johuri_user');
        return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
        return null;
    }
}

function getAuthToken() {
    const user = getLoggedInUser();
    return user ? (user.jwtToken || null) : null;
}

function getAuthHeaders(extraHeaders = {}) {
    const headers = { 'Content-Type': 'application/json', ...extraHeaders };
    const token = getAuthToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

function logout() {
    localStorage.removeItem('johuri_user');
    window.location.href = 'auth.html';
}

// Automatically attach Bearer token to all internal API calls
const originalFetch = window.fetch;
window.fetch = async function (resource, config = {}) {
    let url = typeof resource === 'string' ? resource : (resource.url || '');
    if (url.startsWith('/api/v1/') || url.includes('/api/v1/')) {
        config = config || {};
        config.headers = config.headers || {};
        const token = getAuthToken();
        if (token) {
            if (config.headers instanceof Headers) {
                if (!config.headers.has('Authorization')) {
                    config.headers.set('Authorization', `Bearer ${token}`);
                }
            } else if (Array.isArray(config.headers)) {
                config.headers.push(['Authorization', `Bearer ${token}`]);
            } else {
                if (!config.headers['Authorization']) {
                    config.headers['Authorization'] = `Bearer ${token}`;
                }
            }
        }
    }
    return originalFetch(resource, config);
};

document.addEventListener('DOMContentLoaded', () => {
    const user = getLoggedInUser();
    const navRightSide = document.querySelector('nav .z-20') || document.querySelector('nav .flex.items-center.gap-4') || document.querySelector('nav');
    
    if (navRightSide) {
        if (user) {
            const role = (user.role || '').toUpperCase();
            let dashboardLink = 'creator-dashboard.html';
            let roleBadge = 'Creator';
            if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
                dashboardLink = 'admin-dashboard.html';
                roleBadge = 'Admin';
            } else if (role === 'BRAND') {
                dashboardLink = 'brand-dashboard.html';
                roleBadge = 'Brand';
            }
            
            // Remove existing login button if it exists
            const loginBtns = Array.from(navRightSide.querySelectorAll('button, a')).filter(b => 
                b.textContent && (b.textContent.includes('Login') || b.textContent.includes('Sign In'))
            );
            loginBtns.forEach(b => b.remove());
            
            // Add user dropdown/avatar if not already there
            if (!document.getElementById('userProfileDropdown')) {
                const profileDiv = document.createElement('div');
                profileDiv.id = 'userProfileDropdown';
                profileDiv.className = 'relative group ml-4 cursor-pointer z-50';
                
                const displayName = user.name || user.fullName || user.email || 'User';
                const initials = displayName.substring(0, 2).toUpperCase();
                
                let mediaKitLinkHtml = '';
                if (role === 'CREATOR') {
                    const handle = (user.creatorHandle || user.youtubeHandle || user.name || '').replace('@', '').trim();
                    if (handle) {
                        mediaKitLinkHtml = `
                            <a href="creator-profile.html?handle=${encodeURIComponent(handle)}" class="block px-3 py-2 text-xs font-semibold text-pastel-mint hover:text-white hover:bg-white/5 rounded-xl transition">
                                <i data-lucide="eye" class="inline mr-2" size="14"></i> View Public Media Kit
                            </a>
                        `;
                    }
                } else if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
                    mediaKitLinkHtml = `
                        <a href="admin-dashboard.html" class="block px-3 py-2 text-xs font-semibold text-rose-400 hover:text-white hover:bg-white/5 rounded-xl transition">
                            <i data-lucide="shield-alert" class="inline mr-2" size="14"></i> Dispute Portal
                        </a>
                    `;
                }

                profileDiv.innerHTML = `
                    <div class="flex items-center gap-2.5 bg-navy-800 border border-pastel-indigo/30 hover:border-pastel-mint px-3.5 py-1.5 rounded-full transition shadow-md">
                        <div class="w-7 h-7 rounded-full bg-gradient-to-tr from-pastel-indigo to-pastel-mint flex items-center justify-center text-navy-900 font-extrabold text-xs">
                            ${initials}
                        </div>
                        <div class="flex flex-col text-left leading-tight hidden sm:flex">
                            <span class="text-xs font-bold text-white">${displayName}</span>
                            <span class="text-[10px] text-pastel-mint uppercase font-semibold">${roleBadge}</span>
                        </div>
                    </div>
                    <div class="absolute right-0 mt-2 w-48 bg-navy-800 border border-white/10 rounded-2xl shadow-2xl opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 overflow-hidden">
                        <div class="p-2 space-y-1">
                            <a href="${dashboardLink}" class="block px-3 py-2 text-xs font-semibold text-gray-300 hover:text-white hover:bg-white/5 rounded-xl transition">
                                <i data-lucide="layout-dashboard" class="inline mr-2" size="14"></i> My Dashboard
                            </a>
                            ${mediaKitLinkHtml}
                            <button onclick="logout()" class="w-full text-left px-3 py-2 text-xs font-semibold text-red-400 hover:text-red-300 hover:bg-white/5 rounded-xl transition border-t border-white/5">
                                <i data-lucide="log-out" class="inline mr-2" size="14"></i> Log Out
                            </button>
                        </div>
                    </div>
                `;
                navRightSide.appendChild(profileDiv);
                if (window.lucide) window.lucide.createIcons();
            }
        } else {
            const isDashboard = window.location.pathname.includes('dashboard');
            if (isDashboard) {
                window.location.href = 'auth.html';
            } else {
                const loginBtns = Array.from(navRightSide.querySelectorAll('button, a')).filter(b => 
                    b.textContent && (b.textContent.includes('Login') || b.textContent.includes('Sign In'))
                );
                loginBtns.forEach(btn => {
                    btn.onclick = () => window.location.href = 'auth.html';
                });
            }
        }
    }
});

function isAuthenticated() {
    const user = getLoggedInUser();
    const token = getAuthToken();
    if (!user || !token) return false;
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return false;
        const payload = JSON.parse(atob(parts[1]));
        if (payload.exp && Date.now() >= payload.exp * 1000) return false;
        return true;
    } catch (e) {
        return false;
    }
}

function showAuthRequiredModal(message = "Sign in or create an account to unlock AI Pitch generation and deal escrow locking.") {
    let modal = document.getElementById('authRequiredModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'authRequiredModal';
        modal.className = 'fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-900/80 backdrop-blur-md transition-opacity duration-300';
        modal.innerHTML = `
            <div class="glass-card bg-navy-900/95 border border-pastel-indigo/40 rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl relative text-center">
                <div class="w-16 h-16 rounded-2xl bg-gradient-to-tr from-pastel-indigo/30 via-pastel-mint/30 to-pastel-amber/30 border border-pastel-mint/40 flex items-center justify-center mx-auto mb-5 text-pastel-mint shadow-[0_0_25px_rgba(110,231,183,0.3)]">
                    <i data-lucide="lock" size="32"></i>
                </div>
                <h3 class="text-2xl font-black text-white mb-2">Member Feature Locked</h3>
                <p id="authRequiredModalMsg" class="text-xs sm:text-sm text-gray-400 mb-6 leading-relaxed">${message}</p>
                <div class="space-y-3">
                    <a id="authModalLoginBtn" href="auth.html" class="w-full bg-gradient-to-r from-pastel-indigo via-pastel-mint to-pastel-amber text-navy-900 font-extrabold py-3.5 px-4 rounded-xl text-sm flex items-center justify-center gap-2 hover:shadow-[0_0_20px_rgba(110,231,183,0.4)] transition transform hover:-translate-y-0.5">
                        <i data-lucide="log-in" size="16"></i>
                        <span>Sign In / Create Account</span>
                    </a>
                    <button type="button" onclick="closeAuthRequiredModal()" class="w-full bg-white/5 hover:bg-white/10 text-gray-300 font-semibold py-3 px-4 rounded-xl text-xs transition">
                        Explore Public Views
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        if (window.lucide) window.lucide.createIcons();
    } else {
        const msgEl = document.getElementById('authRequiredModalMsg');
        if (msgEl) msgEl.textContent = message;
        modal.classList.remove('hidden');
    }

    const loginBtn = document.getElementById('authModalLoginBtn');
    if (loginBtn) {
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
        loginBtn.href = `auth.html?returnUrl=${returnUrl}`;
    }
}

function closeAuthRequiredModal() {
    const modal = document.getElementById('authRequiredModal');
    if (modal) {
        modal.classList.add('hidden');
    }
}

function requireAuth(actionCallback, message) {
    if (!isAuthenticated()) {
        showAuthRequiredModal(message);
        return false;
    }
    if (typeof actionCallback === 'function') {
        actionCallback();
    }
    return true;
}

