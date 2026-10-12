// Johuri.ai - Admin & Moderator Dispute Portal Logic (admin.js)

let currentDeals = [];
let currentFilter = 'ALL';
let activeModalDealId = null;
let activeModalAction = null; // 'FORCE_RELEASE' | 'REFUND'

function getAdminAuthHeaders() {
    const token = typeof getAuthToken === 'function' ? getAuthToken() : null;
    const headers = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

function verifyAdminSession() {
    const user = typeof getLoggedInUser === 'function' ? getLoggedInUser() : null;
    const role = user && user.role ? user.role.toUpperCase() : '';
    const banner = document.getElementById('adminAuthWarning');
    if (!user || (role !== 'ADMIN' && role !== 'ROLE_ADMIN')) {
        if (banner) {
            banner.classList.remove('hidden');
        }
    } else {
        if (banner) {
            banner.classList.add('hidden');
        }
    }
}

async function quickAdminLogin() {
    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: 'admin@johuri.ai', password: 'Admin@123' })
        });
        if (response.ok) {
            const data = await response.json();
            const authPayload = data.data;
            localStorage.setItem('johuri_user', JSON.stringify({
                id: authPayload.id,
                email: authPayload.email,
                name: authPayload.fullName || 'Johuri Admin',
                role: authPayload.role,
                jwtToken: authPayload.jwtToken
            }));
            window.location.reload();
        } else {
            alert('Default admin account not found. Please register or check server.');
        }
    } catch (e) {
        alert('Network error during quick login: ' + e.message);
    }
}

async function fetchOverviewMetrics() {
    try {
        const response = await fetch('/api/v1/admin/deals/overview', {
            headers: getAdminAuthHeaders()
        });
        if (!response.ok) throw new Error('Overview API error: ' + response.status);
        const result = await response.json();
        const data = result.data;

        document.getElementById('statTotalVault').textContent = `৳ ${(data.totalEscrowLockedBDT || 0).toLocaleString('en-US')}`;
        document.getElementById('statPendingDisputes').textContent = data.disputedDealsCount || 0;
        document.getElementById('statPlatformCommission').textContent = `৳ ${(Math.round(data.platformCommissionBDT || 0)).toLocaleString('en-US')}`;
        document.getElementById('statActiveCampaigns').textContent = data.activeCampaignsCount || 0;

        const disputeBadge = document.getElementById('disputeBadgeCount');
        if (disputeBadge) {
            disputeBadge.textContent = `${data.disputedDealsCount || 0} Open`;
            if (data.disputedDealsCount > 0) {
                disputeBadge.classList.add('bg-rose-500/20', 'text-rose-400', 'border-rose-500/40');
            }
        }
    } catch (e) {
        console.warn('Could not fetch admin overview metrics:', e);
    }
}

async function fetchDealsList(statusFilter = 'ALL') {
    currentFilter = statusFilter;
    const tbody = document.getElementById('adminDealsTableBody');
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-10 text-gray-400"><i data-lucide="loader-2" class="animate-spin inline mr-2"></i> Loading deals...</td></tr>`;
        lucide.createIcons();
    }

    try {
        let url = '/api/v1/admin/deals';
        if (statusFilter && statusFilter !== 'ALL') {
            url += `?status=${encodeURIComponent(statusFilter)}`;
        }

        const response = await fetch(url, { headers: getAdminAuthHeaders() });
        if (!response.ok) throw new Error('Deals API error: ' + response.status);
        const result = await response.json();
        currentDeals = result.data || [];

        renderDealsTable(currentDeals);
        renderDisputesSection(currentDeals.filter(d => d.status === 'DISPUTED'));
        renderDeliverablesSection(currentDeals.filter(d => d.status === 'DELIVERABLE_SUBMITTED'));
    } catch (e) {
        console.error('Error fetching admin deals:', e);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-10 text-rose-400">Failed to load deals. Ensure you are logged in as ROLE_ADMIN.</td></tr>`;
        }
    }
}

function renderDisputesSection(disputes) {
    const container = document.getElementById('disputesContainer');
    if (!container) return;

    if (!disputes || disputes.length === 0) {
        container.innerHTML = `
            <div class="col-span-full py-12 text-center text-gray-400 bg-navy-800/40 rounded-2xl border border-white/5">
                <i data-lucide="shield-check" class="w-10 h-10 mx-auto mb-2 text-pastel-mint opacity-80"></i>
                <p class="font-bold text-white text-base">No Active Disputes</p>
                <p class="text-xs text-gray-400 mt-1">All escrow transactions are currently running smoothly without brand/creator conflicts.</p>
            </div>
        `;
        lucide.createIcons();
        return;
    }

    container.innerHTML = disputes.map(d => {
        const budget = (d.budgetBDT || 0).toLocaleString('en-US');
        const reason = d.disputeReason || "Deliverable specifications contested by sponsor or delayed without notice.";
        const proof = d.proofLink ? `<a href="${d.proofLink}" target="_blank" class="text-pastel-mint underline hover:text-white inline-flex items-center gap-1 font-mono text-xs"><i data-lucide="play" size="12"></i> ${d.proofLink}</a>` : '<span class="text-gray-500 italic text-xs">No proof URL submitted</span>';

        return `
            <div class="glass-card rounded-2xl p-6 border-rose-500/30 bg-rose-950/10 relative overflow-hidden flex flex-col justify-between">
                <div class="flex justify-between items-start mb-4">
                    <div>
                        <div class="flex items-center gap-2">
                            <span class="text-xs font-mono font-bold text-pastel-indigo">${d.dealId}</span>
                            <span class="bg-rose-500/20 text-rose-400 border border-rose-500/30 px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">⚠️ DISPUTED</span>
                        </div>
                        <h4 class="text-lg font-bold text-white mt-1">${d.brandName} <span class="text-gray-400 font-normal">vs</span> ${d.creatorHandle}</h4>
                    </div>
                    <div class="text-right">
                        <span class="text-2xl font-bold font-mono text-pastel-amber">৳${budget}</span>
                        <span class="block text-[10px] text-gray-400">Locked in Vault</span>
                    </div>
                </div>

                <div class="bg-navy-900/60 p-3.5 rounded-xl border border-white/5 space-y-2 mb-4 text-xs">
                    <div>
                        <span class="text-gray-400 font-semibold block uppercase text-[10px]">Deliverable Scope:</span>
                        <span class="text-gray-200">${d.deliverableType || 'Custom Sponsorship'}</span>
                    </div>
                    <div>
                        <span class="text-gray-400 font-semibold block uppercase text-[10px]">Contested Reason:</span>
                        <p class="text-rose-300 italic">${reason}</p>
                    </div>
                    <div>
                        <span class="text-gray-400 font-semibold block uppercase text-[10px]">Submitted Proof:</span>
                        ${proof}
                    </div>
                </div>

                <div class="flex flex-col sm:flex-row gap-2 pt-2 border-t border-white/10">
                    <button onclick="openResolutionModal('${d.dealId}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', ${d.budgetBDT}, 'FORCE_RELEASE')" 
                            class="flex-1 bg-gradient-to-r from-emerald-500 to-pastel-mint text-navy-900 hover:opacity-90 font-bold py-2.5 px-3 rounded-xl text-xs transition flex items-center justify-center gap-1.5 shadow-sm">
                        <i data-lucide="check-circle" size="14"></i> Force Release to Creator
                    </button>
                    <button onclick="openResolutionModal('${d.dealId}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', ${d.budgetBDT}, 'REFUND')" 
                            class="flex-1 bg-rose-500 hover:bg-rose-600 text-white font-bold py-2.5 px-3 rounded-xl text-xs transition flex items-center justify-center gap-1.5 shadow-sm">
                        <i data-lucide="rotate-ccw" size="14"></i> Refund to Brand
                    </button>
                </div>
            </div>
        `;
    }).join('');

    lucide.createIcons();
}

function renderDeliverablesSection(deliverables) {
    const container = document.getElementById('deliverablesContainer');
    if (!container) return;

    if (!deliverables || deliverables.length === 0) {
        container.innerHTML = `
            <div class="col-span-full py-10 text-center text-gray-400 bg-navy-800/40 rounded-2xl border border-white/5">
                <i data-lucide="inbox" class="w-8 h-8 mx-auto mb-2 opacity-50"></i>
                <p class="text-sm font-semibold text-gray-300">No Pending Deliverables for Review</p>
                <p class="text-xs text-gray-500">Newly submitted proof URLs by creators will appear here for verification.</p>
            </div>
        `;
        lucide.createIcons();
        return;
    }

    container.innerHTML = deliverables.map(d => {
        const budget = (d.budgetBDT || 0).toLocaleString('en-US');
        const proof = d.proofLink ? `<a href="${d.proofLink}" target="_blank" class="text-pastel-mint underline hover:text-white inline-flex items-center gap-1 font-mono text-xs"><i data-lucide="external-link" size="12"></i> ${d.proofLink}</a>` : 'None';

        return `
            <div class="glass-card rounded-2xl p-5 border-pastel-amber/30 bg-navy-800/60 flex flex-col justify-between">
                <div>
                    <div class="flex justify-between items-start mb-2">
                        <span class="text-xs font-mono font-bold text-pastel-indigo">${d.dealId}</span>
                        <span class="bg-pastel-amber/20 text-pastel-amber border border-pastel-amber/30 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider">PROOF SUBMITTED</span>
                    </div>
                    <h4 class="font-bold text-white text-base mb-1">${d.creatorHandle} &bull; <span class="text-gray-300 font-normal">${d.brandName}</span></h4>
                    <p class="text-xs text-gray-400 mb-3">${d.deliverableType || 'Custom Deal'} &bull; ৳${budget}</p>

                    <div class="bg-navy-900/80 p-3 rounded-xl border border-white/5 text-xs mb-4">
                        <span class="text-gray-400 font-semibold block text-[10px] uppercase mb-1">Live YouTube Proof URL:</span>
                        ${proof}
                    </div>
                </div>

                <div class="flex gap-2">
                    <button onclick="openReviewModal('${d.dealId}', '${encodeURIComponent(d.proofLink || '')}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', 'APPROVE')" 
                            class="flex-1 bg-pastel-mint hover:bg-emerald-400 text-navy-900 font-bold py-2 rounded-xl text-xs transition flex items-center justify-center gap-1">
                        <i data-lucide="check" size="14"></i> Approve & Settle
                    </button>
                    <button onclick="openReviewModal('${d.dealId}', '${encodeURIComponent(d.proofLink || '')}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', 'REJECT')" 
                            class="flex-1 bg-navy-900 hover:bg-navy-700 text-rose-400 border border-rose-500/40 font-bold py-2 rounded-xl text-xs transition flex items-center justify-center gap-1">
                        <i data-lucide="x" size="14"></i> Reject & Flag
                    </button>
                </div>
            </div>
        `;
    }).join('');

    lucide.createIcons();
}

function renderDealsTable(deals) {
    const tbody = document.getElementById('adminDealsTableBody');
    if (!tbody) return;

    if (!deals || deals.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-10 text-gray-400"><i data-lucide="inbox" class="w-8 h-8 mx-auto mb-2 opacity-50"></i> No deals found for selected filter.</td></tr>`;
        lucide.createIcons();
        return;
    }

    tbody.innerHTML = deals.map(d => {
        const budget = (d.budgetBDT || 0).toLocaleString('en-US');
        const cleanHandle = (d.creatorHandle || '').replace('@', '');
        const dateStr = d.createdAt ? new Date(d.createdAt).toLocaleDateString() : 'Recent';
        
        let statusBadge = '';
        switch(d.status) {
            case 'DISPUTED':
                statusBadge = `<span class="bg-rose-500/20 text-rose-400 border border-rose-500/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">⚠️ DISPUTED</span>`;
                break;
            case 'ESCROW_LOCKED':
            case 'IN_ESCROW':
                statusBadge = `<span class="bg-pastel-indigo/20 text-pastel-indigo border border-pastel-indigo/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">🔒 IN ESCROW</span>`;
                break;
            case 'DELIVERABLE_SUBMITTED':
                statusBadge = `<span class="bg-pastel-amber/20 text-pastel-amber border border-pastel-amber/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">📤 PROOF SUBMITTED</span>`;
                break;
            case 'COMPLETED':
                statusBadge = `<span class="bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">✓ COMPLETED</span>`;
                break;
            case 'FORCE_RELEASED':
                statusBadge = `<span class="bg-purple-500/20 text-purple-300 border border-purple-500/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">⚖️ FORCE RELEASED</span>`;
                break;
            case 'REFUNDED':
                statusBadge = `<span class="bg-gray-600/30 text-gray-300 border border-gray-500/30 px-2.5 py-1 rounded-full text-xs font-bold inline-flex items-center gap-1">↩️ REFUNDED</span>`;
                break;
            default:
                statusBadge = `<span class="bg-gray-700 text-gray-300 px-2.5 py-1 rounded-full text-xs font-bold">${d.status}</span>`;
        }

        const proofCell = d.proofLink 
            ? `<a href="${d.proofLink}" target="_blank" class="text-pastel-mint hover:underline font-mono text-xs flex items-center gap-1"><i data-lucide="play" size="12"></i> Watch ↗</a>` 
            : `<span class="text-gray-500 text-xs">-</span>`;

        let actionButtons = '';
        if (d.status === 'DISPUTED' || d.status === 'ESCROW_LOCKED' || d.status === 'IN_ESCROW' || d.status === 'DELIVERABLE_SUBMITTED') {
            actionButtons = `
                <div class="flex items-center justify-end gap-1.5">
                    <button onclick="openResolutionModal('${d.dealId}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', ${d.budgetBDT}, 'FORCE_RELEASE')" 
                            title="Force Release to Creator" class="bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 px-2.5 py-1 rounded-lg text-xs font-bold transition">
                        Release
                    </button>
                    <button onclick="openResolutionModal('${d.dealId}', '${encodeURIComponent(d.brandName)}', '${encodeURIComponent(d.creatorHandle)}', ${d.budgetBDT}, 'REFUND')" 
                            title="Refund to Brand" class="bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/30 px-2.5 py-1 rounded-lg text-xs font-bold transition">
                        Refund
                    </button>
                </div>
            `;
        } else {
            actionButtons = `<span class="text-xs text-gray-500">Settled</span>`;
        }

        return `
            <tr class="border-b border-white/5 hover:bg-white/5 transition">
                <td class="py-3.5 px-4 font-mono font-bold text-xs text-white">${d.dealId}</td>
                <td class="py-3.5 px-4 font-semibold text-white">${d.brandName}</td>
                <td class="py-3.5 px-4">
                    <a href="creator-profile.html?handle=${encodeURIComponent(cleanHandle)}" target="_blank" class="text-pastel-mint hover:underline font-semibold flex items-center gap-1">
                        ${d.creatorHandle} <i data-lucide="external-link" size="11" class="opacity-50"></i>
                    </a>
                </td>
                <td class="py-3.5 px-4 font-mono font-bold text-pastel-mint">৳${budget}</td>
                <td class="py-3.5 px-4">${statusBadge}</td>
                <td class="py-3.5 px-4">${proofCell}</td>
                <td class="py-3.5 px-4 text-right">${actionButtons}</td>
            </tr>
        `;
    }).join('');

    lucide.createIcons();
}

function filterDealsByTab(status) {
    document.querySelectorAll('.filter-tab-btn').forEach(btn => {
        btn.classList.remove('bg-pastel-indigo', 'text-white', 'shadow-md');
        btn.classList.add('text-gray-400', 'bg-navy-800/80');
    });
    const activeBtn = document.getElementById(`tab-${status}`);
    if (activeBtn) {
        activeBtn.classList.remove('text-gray-400', 'bg-navy-800/80');
        activeBtn.classList.add('bg-pastel-indigo', 'text-white', 'shadow-md');
    }

    fetchDealsList(status);
}

function handleSearchTable(query) {
    const q = query.toLowerCase().trim();
    if (!q) {
        renderDealsTable(currentDeals);
        return;
    }

    const filtered = currentDeals.filter(d => 
        (d.dealId && d.dealId.toLowerCase().includes(q)) ||
        (d.brandName && d.brandName.toLowerCase().includes(q)) ||
        (d.creatorHandle && d.creatorHandle.toLowerCase().includes(q)) ||
        (d.status && d.status.toLowerCase().includes(q))
    );
    renderDealsTable(filtered);
}

// Modal Resolution Handlers
function openResolutionModal(dealId, encodedBrand, encodedCreator, amount, action) {
    activeModalDealId = dealId;
    activeModalAction = action;

    const brand = decodeURIComponent(encodedBrand);
    const creator = decodeURIComponent(encodedCreator);

    document.getElementById('modalDealId').textContent = dealId;
    document.getElementById('modalBrand').textContent = brand;
    document.getElementById('modalCreator').textContent = creator;
    document.getElementById('modalAmount').textContent = `৳ ${Number(amount).toLocaleString('en-US')}`;

    const titleEl = document.getElementById('modalResolutionTitle');
    const descEl = document.getElementById('modalResolutionDesc');
    const confirmBtn = document.getElementById('modalConfirmBtn');
    const noteInput = document.getElementById('adminResolutionNotes');
    noteInput.value = '';

    if (action === 'FORCE_RELEASE') {
        titleEl.textContent = 'Execute Force Release to Creator';
        descEl.textContent = `Arbitrarily disburse ৳${Number(amount).toLocaleString('en-US')} from escrow to ${creator}. This overrides brand dispute.`;
        confirmBtn.className = 'flex-1 bg-gradient-to-r from-emerald-500 to-pastel-mint text-navy-900 font-bold py-3 rounded-xl text-sm transition hover:opacity-90';
        confirmBtn.textContent = 'Confirm Force Release';
    } else {
        titleEl.textContent = 'Execute Refund to Brand';
        descEl.textContent = `Return ৳${Number(amount).toLocaleString('en-US')} from escrow back to sponsor ${brand}. This closes the campaign deal.`;
        confirmBtn.className = 'flex-1 bg-rose-500 hover:bg-rose-600 text-white font-bold py-3 rounded-xl text-sm transition';
        confirmBtn.textContent = 'Confirm Refund to Brand';
    }

    document.getElementById('resolutionModal').classList.remove('hidden');
    lucide.createIcons();
}

function closeResolutionModal() {
    document.getElementById('resolutionModal').classList.add('hidden');
    activeModalDealId = null;
    activeModalAction = null;
}

async function handleResolutionSubmit(event) {
    event.preventDefault();
    if (!activeModalDealId || !activeModalAction) return;

    const notes = document.getElementById('adminResolutionNotes').value.trim();
    if (!notes) {
        alert('Admin resolution notes are mandatory for financial overrides.');
        return;
    }

    const endpoint = activeModalAction === 'FORCE_RELEASE' 
        ? `/api/v1/admin/escrow/${activeModalDealId}/force-release`
        : `/api/v1/admin/escrow/${activeModalDealId}/refund`;

    const confirmBtn = document.getElementById('modalConfirmBtn');
    const originalText = confirmBtn.textContent;
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = `<i data-lucide="loader-2" class="animate-spin inline mr-1"></i> Processing...`;
    lucide.createIcons();

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: getAdminAuthHeaders(),
            body: JSON.stringify({ adminNotes: notes })
        });

        if (response.ok) {
            closeResolutionModal();
            alert(`✓ Transaction Override Successful for Deal #${activeModalDealId}`);
            await fetchOverviewMetrics();
            await fetchDealsList(currentFilter);
        } else {
            const err = await response.json();
            alert('Override failed: ' + (err.message || 'Check admin permissions'));
        }
    } catch (e) {
        alert('Network error: ' + e.message);
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.textContent = originalText;
    }
}

// Deliverable Review Modal Handlers
let activeReviewDealId = null;
let activeReviewAction = 'APPROVE';

function openReviewModal(dealId, encodedProof, encodedBrand, encodedCreator, action) {
    activeReviewDealId = dealId;
    activeReviewAction = action;

    const proof = decodeURIComponent(encodedProof);
    const brand = decodeURIComponent(encodedBrand);
    const creator = decodeURIComponent(encodedCreator);

    document.getElementById('reviewDealId').textContent = dealId;
    document.getElementById('reviewBrandCreator').textContent = `${brand} &bull; ${creator}`;
    
    const linkEl = document.getElementById('reviewProofUrl');
    if (proof) {
        linkEl.href = proof;
        linkEl.textContent = proof;
    } else {
        linkEl.href = '#';
        linkEl.textContent = 'No URL available';
    }

    document.getElementById('reviewNotes').value = '';
    const actionBadge = document.getElementById('reviewActionBadge');
    const submitBtn = document.getElementById('reviewSubmitBtn');

    if (action === 'APPROVE') {
        actionBadge.textContent = 'Approve & Release Funds';
        actionBadge.className = 'text-xs font-bold text-pastel-mint bg-pastel-mint/10 border border-pastel-mint/30 px-2.5 py-1 rounded-full';
        submitBtn.className = 'flex-1 bg-pastel-mint hover:bg-emerald-400 text-navy-900 font-bold py-3 rounded-xl text-sm transition';
        submitBtn.textContent = 'Confirm Approval';
    } else {
        actionBadge.textContent = 'Reject & Flag Dispute';
        actionBadge.className = 'text-xs font-bold text-rose-400 bg-rose-500/10 border border-rose-500/30 px-2.5 py-1 rounded-full';
        submitBtn.className = 'flex-1 bg-rose-500 hover:bg-rose-600 text-white font-bold py-3 rounded-xl text-sm transition';
        submitBtn.textContent = 'Confirm Rejection';
    }

    document.getElementById('deliverableModal').classList.remove('hidden');
    lucide.createIcons();
}

function closeReviewModal() {
    document.getElementById('deliverableModal').classList.add('hidden');
    activeReviewDealId = null;
}

async function handleReviewSubmit(event) {
    event.preventDefault();
    if (!activeReviewDealId) return;

    const notes = document.getElementById('reviewNotes').value.trim();
    if (!notes) {
        alert('Review quality notes are mandatory.');
        return;
    }

    try {
        const response = await fetch(`/api/v1/admin/deals/${activeReviewDealId}/review-deliverable`, {
            method: 'POST',
            headers: getAdminAuthHeaders(),
            body: JSON.stringify({
                action: activeReviewAction,
                adminNotes: notes
            })
        });

        if (response.ok) {
            closeReviewModal();
            alert(`✓ Deliverable review recorded for Deal #${activeReviewDealId}`);
            await fetchOverviewMetrics();
            await fetchDealsList(currentFilter);
        } else {
            const err = await response.json();
            alert('Review failed: ' + (err.message || 'Check admin permissions'));
        }
    } catch (e) {
        alert('Network error: ' + e.message);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    verifyAdminSession();
    fetchOverviewMetrics();
    fetchDealsList('ALL');
});

