/**
 * Johuri.ai - Unified Mobile Navigation & Glassmorphism Drawer Manager
 * Provides responsive hamburger toggle, slide-in glassmorphism drawer,
 * active link indication, and session-aware controls across all pages.
 */

(function () {
    'use strict';

    // 1. Inject or ensure drawer HTML exists
    function ensureDrawerMarkup() {
        if (document.getElementById('mobileDrawerPanel')) return;

        const drawerContainer = document.createElement('div');
        drawerContainer.id = 'mobileNavWrapper';
        drawerContainer.innerHTML = `
            <!-- Backdrop Overlay -->
            <div id="mobileDrawerBackdrop" class="mobile-drawer-backdrop" aria-hidden="true"></div>

            <!-- Slide-in Glassmorphism Drawer -->
            <nav id="mobileDrawerPanel" class="mobile-drawer-panel" aria-label="Mobile Navigation">
                
                <!-- Drawer Header -->
                <div class="mobile-drawer-header">
                    <a href="index.html" class="flex items-center gap-2.5 group">
                        <svg viewBox="0 0 48 48" class="w-8 h-8 group-hover:scale-105 transition transform" fill="none" stroke-linecap="round" stroke-linejoin="round">
                            <defs>
                                <linearGradient id="brandGradDrawer" x1="0%" y1="0%" x2="100%" y2="100%">
                                    <stop offset="0%" stop-color="#818CF8" />
                                    <stop offset="100%" stop-color="#6EE7B7" />
                                </linearGradient>
                            </defs>
                            <g transform="translate(24, 24) rotate(-15) translate(-24, -24)">
                                <rect x="6" y="14" width="36" height="20" rx="3" fill="#0F172A" stroke="#6EE7B7" stroke-width="1.5" opacity="0.8"/>
                                <circle cx="24" cy="24" r="4" stroke="#6EE7B7" stroke-width="1.5"/>
                            </g>
                            <g transform="translate(24, 24) rotate(0) translate(-24, -24)">
                                <rect x="6" y="14" width="36" height="20" rx="3" fill="#0F172A" stroke="#818CF8" stroke-width="1.5" opacity="0.9"/>
                                <circle cx="24" cy="24" r="4" stroke="#818CF8" stroke-width="1.5"/>
                            </g>
                            <g transform="translate(24, 24) rotate(15) translate(-24, -24)">
                                <rect x="6" y="14" width="36" height="20" rx="3" fill="#0F172A" stroke="#FCD34D" stroke-width="1.5" opacity="0.9"/>
                                <circle cx="24" cy="24" r="4" stroke="#FCD34D" stroke-width="1.5"/>
                            </g>
                            <g transform="translate(6, 6) scale(1.5)">
                                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" fill="url(#brandGradDrawer)" stroke="#0F172A" stroke-width="1"/>
                            </g>
                        </svg>
                        <div class="flex items-center gap-1.5">
                            <span class="font-bangla font-bold text-xl gradient-text leading-none">জহুরী</span>
                            <span class="font-sans text-base font-bold text-white leading-none">Johuri.ai</span>
                        </div>
                    </a>
                    <button id="mobileDrawerClose" aria-label="Close Mobile Navigation" class="mobile-drawer-close-btn">
                        <i data-lucide="x" size="20"></i>
                    </button>
                </div>

                <!-- Navigation Links List -->
                <div class="mobile-drawer-links">
                    <div class="mobile-drawer-link-section-title">Ecosystem & Portals</div>
                    
                    <a href="creators.html" class="mobile-nav-link" data-path="creators.html">
                        <i data-lucide="compass" size="18" class="text-pastel-mint"></i>
                        <span>Marketplace</span>
                        <span class="mobile-nav-badge bg-pastel-mint/15 text-pastel-mint border border-pastel-mint/30">Browse</span>
                    </a>

                    <a href="brand-dashboard.html" class="mobile-nav-link" data-path="brand-dashboard.html">
                        <i data-lucide="briefcase" size="18" class="text-pastel-indigo"></i>
                        <span>Brand Dashboard</span>
                        <span class="mobile-nav-badge bg-pastel-indigo/15 text-pastel-indigo border border-pastel-indigo/30">Escrow</span>
                    </a>

                    <a href="creator-dashboard.html" class="mobile-nav-link" data-path="creator-dashboard.html">
                        <i data-lucide="video" size="18" class="text-pastel-amber"></i>
                        <span>Creator Studio</span>
                        <span class="mobile-nav-badge bg-pastel-amber/15 text-pastel-amber border border-pastel-amber/30">Earnings</span>
                    </a>

                    <a href="admin-dashboard.html" class="mobile-nav-link" data-path="admin-dashboard.html">
                        <i data-lucide="shield-alert" size="18" class="text-rose-400"></i>
                        <span>Admin & Governance</span>
                        <span class="mobile-nav-badge bg-rose-500/15 text-rose-300 border border-rose-500/30">Disputes</span>
                    </a>

                    <div class="mobile-drawer-link-section-title mt-2">Media & Platform</div>

                    <a href="creator-profile.html?handle=ashiquirrahmanoli" id="drawerMediaKitLink" class="mobile-nav-link" data-path="creator-profile.html">
                        <i data-lucide="sparkles" size="18" class="text-cyan-400"></i>
                        <span>Public Media Kit</span>
                        <span class="mobile-nav-badge bg-cyan-500/15 text-cyan-300 border border-cyan-500/30">AI Rate</span>
                    </a>

                    <a href="index.html" class="mobile-nav-link" data-path="index.html">
                        <i data-lucide="home" size="18" class="text-gray-400"></i>
                        <span>Home Overview</span>
                    </a>
                </div>

                <!-- Footer / Session Card -->
                <div id="drawerSessionFooter" class="mobile-drawer-footer">
                    <!-- Injected dynamically -->
                </div>
            </nav>
        `;

        document.body.appendChild(drawerContainer);
    }

    // 2. Ensure Hamburger Toggle button exists in active <nav>
    function ensureToggleButton() {
        if (document.getElementById('mobileNavToggle')) return;

        const nav = document.querySelector('nav');
        if (!nav) return;

        const toggleBtn = document.createElement('button');
        toggleBtn.id = 'mobileNavToggle';
        toggleBtn.type = 'button';
        toggleBtn.className = 'mobile-nav-toggle-btn';
        toggleBtn.setAttribute('aria-label', 'Open Mobile Navigation');
        toggleBtn.innerHTML = `<i data-lucide="menu" size="22"></i>`;

        // Find the inner container of nav
        const container = nav.querySelector('.max-w-7xl') || nav.querySelector('.max-w-6xl') || nav.querySelector('.flex') || nav;
        
        // Append to the right side
        container.appendChild(toggleBtn);
    }

    // 3. Highlight Current Active Page in Drawer
    function highlightActivePage() {
        const currentPath = window.location.pathname.toLowerCase();
        const navLinks = document.querySelectorAll('.mobile-drawer-panel .mobile-nav-link');
        
        navLinks.forEach(link => {
            const dataPath = (link.getAttribute('data-path') || '').toLowerCase();
            const href = (link.getAttribute('href') || '').toLowerCase();

            if (dataPath && currentPath.endsWith(dataPath)) {
                link.classList.add('active-page');
            } else if (dataPath === 'index.html' && (currentPath === '/' || currentPath.endsWith('/') || currentPath === '')) {
                link.classList.add('active-page');
            } else {
                link.classList.remove('active-page');
            }
        });
    }

    // 4. Update User Session Section inside Drawer
    function updateSessionSection() {
        const footer = document.getElementById('drawerSessionFooter');
        if (!footer) return;

        let user = null;
        try {
            const userStr = localStorage.getItem('johuri_user');
            if (userStr) user = JSON.parse(userStr);
        } catch (e) {
            user = null;
        }

        if (user && (user.email || user.name || user.fullName)) {
            const displayName = user.name || user.fullName || user.email || 'User';
            const role = (user.role || '').toUpperCase();
            const initials = displayName.substring(0, 2).toUpperCase();

            let roleBadge = 'Creator';
            let roleColor = 'text-pastel-indigo bg-pastel-indigo/15 border-pastel-indigo/30';
            if (role === 'ADMIN' || role === 'ROLE_ADMIN') {
                roleBadge = 'Admin';
                roleColor = 'text-rose-400 bg-rose-500/15 border-rose-500/30';
            } else if (role === 'BRAND') {
                roleBadge = 'Brand';
                roleColor = 'text-pastel-mint bg-pastel-mint/15 border-pastel-mint/30';
            }

            // Update Media Kit link if creator handle is present
            const handle = (user.creatorHandle || '').replace('@', '').trim();
            const mediaKitLink = document.getElementById('drawerMediaKitLink');
            if (mediaKitLink && handle) {
                mediaKitLink.href = `creator-profile.html?handle=${encodeURIComponent(handle)}`;
            }

            footer.innerHTML = `
                <div class="drawer-user-card">
                    <div class="drawer-user-avatar">${initials}</div>
                    <div class="min-w-0 flex-1">
                        <div class="text-xs font-bold text-white truncate">${displayName}</div>
                        <div class="text-[10px] text-gray-400 truncate">${user.email || ''}</div>
                    </div>
                    <span class="text-[10px] font-bold px-2 py-0.5 rounded-full border ${roleColor}">${roleBadge}</span>
                </div>
                <button type="button" id="drawerLogoutBtn" class="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-xl text-xs font-bold text-rose-300 hover:text-white bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 transition">
                    <i data-lucide="log-out" size="14"></i> Sign Out
                </button>
            `;

            const logoutBtn = document.getElementById('drawerLogoutBtn');
            if (logoutBtn) {
                logoutBtn.addEventListener('click', () => {
                    localStorage.removeItem('johuri_user');
                    window.location.href = 'auth.html';
                });
            }
        } else {
            footer.innerHTML = `
                <a href="auth.html" class="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl text-sm font-bold text-navy-900 bg-gradient-to-r from-pastel-indigo via-pastel-mint to-pastel-amber hover:shadow-[0_0_20px_rgba(110,231,183,0.4)] transition">
                    <i data-lucide="log-in" size="16"></i> Sign In / Create Account
                </a>
                <p class="text-[10px] text-gray-500 text-center mt-2 flex items-center justify-center gap-1">
                    <i data-lucide="shield-check" size="12" class="text-pastel-mint"></i>
                    bKash Escrow & 256-bit SSL Protected
                </p>
            `;
        }
    }

    // 5. Open & Close Drawer Controller
    function openDrawer() {
        const panel = document.getElementById('mobileDrawerPanel');
        const backdrop = document.getElementById('mobileDrawerBackdrop');
        if (panel && backdrop) {
            panel.classList.add('active');
            backdrop.classList.add('active');
            document.body.style.overflow = 'hidden';
        }
    }

    function closeDrawer() {
        const panel = document.getElementById('mobileDrawerPanel');
        const backdrop = document.getElementById('mobileDrawerBackdrop');
        if (panel && backdrop) {
            panel.classList.remove('active');
            backdrop.classList.remove('active');
            document.body.style.overflow = '';
        }
    }

    function toggleDrawer() {
        const panel = document.getElementById('mobileDrawerPanel');
        if (panel && panel.classList.contains('active')) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    // 6. Bind Event Listeners
    function bindEventListeners() {
        const toggleBtn = document.getElementById('mobileNavToggle');
        if (toggleBtn) {
            toggleBtn.removeEventListener('click', toggleDrawer);
            toggleBtn.addEventListener('click', toggleDrawer);
        }

        const closeBtn = document.getElementById('mobileDrawerClose');
        if (closeBtn) {
            closeBtn.removeEventListener('click', closeDrawer);
            closeBtn.addEventListener('click', closeDrawer);
        }

        const backdrop = document.getElementById('mobileDrawerBackdrop');
        if (backdrop) {
            backdrop.removeEventListener('click', closeDrawer);
            backdrop.addEventListener('click', closeDrawer);
        }

        // Close on escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeDrawer();
        });

        // Close on drawer link click
        const drawerLinks = document.querySelectorAll('.mobile-drawer-panel a');
        drawerLinks.forEach(link => {
            link.addEventListener('click', () => {
                closeDrawer();
            });
        });
    }

    // 7. Initialize Mobile Navigation
    function initMobileNav() {
        ensureDrawerMarkup();
        ensureToggleButton();
        highlightActivePage();
        updateSessionSection();
        bindEventListeners();

        if (window.lucide && typeof window.lucide.createIcons === 'function') {
            window.lucide.createIcons();
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMobileNav);
    } else {
        initMobileNav();
    }

    // Expose helpers globally if needed
    window.JohuriMobileNav = {
        open: openDrawer,
        close: closeDrawer,
        toggle: toggleDrawer,
        refresh: initMobileNav
    };

})();

