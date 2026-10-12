/**
 * Johuri.ai Strict Frontend Route Guard (auth-guard.js)
 * Executes immediately in the document <head> to prevent flicker or unauthorized access
 */
(function() {
    function getStoredUser() {
        try {
            const raw = localStorage.getItem('johuri_user') || sessionStorage.getItem('johuri_user');
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function isTokenValid(token) {
        if (!token || typeof token !== 'string') return false;
        const parts = token.split('.');
        if (parts.length !== 3) return false;
        try {
            const payload = JSON.parse(atob(parts[1]));
            if (payload.exp && (Date.now() >= payload.exp * 1000)) {
                return false; // Expired
            }
            return true;
        } catch (e) {
            return false;
        }
    }

    const currentPath = window.location.pathname.toLowerCase();
    const user = getStoredUser();
    const token = user ? (user.jwtToken || null) : null;
    const hasValidAuth = user && isTokenValid(token);

    // Determine target role requirements based on URL
    let requiredRole = null;
    let dashboardName = '';

    if (currentPath.includes('admin-dashboard')) {
        requiredRole = ['ADMIN', 'ROLE_ADMIN'];
        dashboardName = 'Admin';
    } else if (currentPath.includes('brand-dashboard')) {
        requiredRole = ['BRAND', 'ROLE_BRAND'];
        dashboardName = 'Brand';
    } else if (currentPath.includes('creator-dashboard')) {
        // Permit both Creator and Admin to access creator dashboard
        requiredRole = ['CREATOR', 'ROLE_CREATOR', 'ADMIN', 'ROLE_ADMIN'];
        dashboardName = 'Creator';
    }

    // If on a protected route:
    if (requiredRole) {
        const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
        
        if (!hasValidAuth) {
            // Unauthenticated: clear any corrupt storage and redirect immediately
            try {
                localStorage.removeItem('johuri_user');
                sessionStorage.removeItem('johuri_user');
            } catch (e) {}
            window.location.replace(`auth.html?returnUrl=${returnUrl}&error=auth_required`);
            return;
        }

        const userRole = (user.role || '').toUpperCase();
        const roleMatches = requiredRole.some(r => r === userRole);

        if (!roleMatches) {
            // Role mismatch redirect to their actual dashboard
            console.warn(`Role mismatch: User has role ${userRole}, but route requires ${requiredRole.join('/')}`);
            if (userRole === 'ADMIN' || userRole === 'ROLE_ADMIN') {
                window.location.replace('admin-dashboard.html');
            } else if (userRole === 'BRAND' || userRole === 'ROLE_BRAND') {
                window.location.replace('brand-dashboard.html');
            } else {
                window.location.replace('creator-dashboard.html');
            }
        }
    }
})();

