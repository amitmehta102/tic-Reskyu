/* ============================================================
   RESKYU — NGO Dashboard Logic
   Handles: auth guard, tab nav, browse surplus, claim modal,
            collections feed, milestones, impact stats, profile
   ============================================================ */
(function () {
  'use strict';

  const auth = window.RESKYU_AUTH;
  const db = window.RESKYU_DB;

  const loadingEl = document.getElementById('dash-loading');
  const mainEl = document.getElementById('dash-main');

  if (!auth || !db) { window.location.href = 'pitch.html'; return; }

  let currentUser = null;
  let allCollections = [];
  let activeSurplus = [];
  let selectedListing = null;

  // ── Auth Guard ────────────────────────────────────────────
  auth.onAuthStateChanged(function (user) {
    if (!user) { window.location.href = 'pitch.html'; return; }
    currentUser = user;
    loadProfile(user);
    loadSurplus();
    loadCollections(user.uid);
  });

  // ── Tab Navigation (syncs mobile bottom bar too) ─────────────
  window.switchTab = function (tabId) {
    document.querySelectorAll('.role-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('.mobile-tab-btn').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('.role-panel').forEach(p => {
      const id = 'tab-' + tabId;
      if (p.id === id) { p.classList.add('active'); p.removeAttribute('hidden'); }
      else { p.classList.remove('active'); p.setAttribute('hidden', ''); }
    });
  };
  document.querySelectorAll('.role-tab, .mobile-tab-btn').forEach(t => {
    t.addEventListener('click', () => switchTab(t.dataset.tab));
  });

  // ── Load Profile ──────────────────────────────────────────
  function loadProfile(user) {
    db.collection('users').doc(user.uid).get().then(doc => {
      const data = doc.exists ? doc.data() : {};
      const name = data.name || user.displayName || 'NGO';
      const email = user.email;
      const joined = data.createdAt
        ? data.createdAt.toDate().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })
        : 'Recently';

      const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
      document.getElementById('dash-avatar').textContent = initials;
      document.getElementById('dash-name').textContent = name;
      document.getElementById('dash-greeting').textContent = greeting();

      document.getElementById('edit-name').value = name;
      document.getElementById('edit-email').value = email;
      document.getElementById('edit-address').value = data.address || '';
      document.getElementById('edit-phone').value = data.phone || '';
      document.getElementById('edit-reg').value = data.regId || '';
      document.getElementById('edit-joined').value = joined;

      loadingEl.style.display = 'none';
      mainEl.removeAttribute('hidden');

      // Trigger onboarding for new users (Feature 7)
      if (window.Onboarding) {
        Onboarding.checkAndShow(user, db);
      }
    });
  }

  // ── Load Active Surplus from Firestore ────────────────────
  function loadSurplus() {
    db.collection('listings')
      .where('status', '==', 'active')
      .orderBy('createdAt', 'desc')
      .onSnapshot(snapshot => {
        activeSurplus = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderSurplus(activeSurplus);
        // Count unique partner restaurants
        const partners = new Set(activeSurplus.map(l => l.restaurantId));
        document.getElementById('stat-partners').textContent = partners.size;
      }, err => {
        console.warn('[Surplus]', err.message);
        // Demo cards are already shown via HTML
      });
  }

  // ── Render Surplus Cards ──────────────────────────────────
  function renderSurplus(items) {
    const container = document.getElementById('live-surplus-grid');
    const noSurplus = document.getElementById('no-surplus');
    if (!items.length) {
      noSurplus.removeAttribute('hidden'); container.innerHTML = ''; return;
    }
    noSurplus.setAttribute('hidden', '');

    const filter = document.getElementById('category-filter').value;
    const query = (document.getElementById('surplus-search')?.value || '').trim().toLowerCase();

    let filtered = filter === 'all' ? items : items.filter(i => i.category === filter);

    if (query) {
      filtered = filtered.filter(i =>
        (i.foodName || '').toLowerCase().includes(query) ||
        (i.restaurantName || '').toLowerCase().includes(query)
      );
    }

    if (!filtered.length) {
      noSurplus.removeAttribute('hidden'); container.innerHTML = '';
      noSurplus.querySelector('p').textContent = query
        ? `No results for "${query}". Try a different search.`
        : 'No live surplus available right now. Check back closer to meal times!';
      return;
    }
    noSurplus.setAttribute('hidden', '');

    container.innerHTML = filtered.map(l => `
      <div class="surplus-card">
        <div class="surplus-card-badge">🟢 Active</div>
        <div class="surplus-card-category">${categoryEmoji(l.category)} ${escHtml(l.category)}</div>
        <h3 class="surplus-card-name">${escHtml(l.foodName)}</h3>
        <div class="surplus-card-restaurant">🏪 ${escHtml(l.restaurantName || 'Restaurant')}</div>
        <div class="surplus-card-qty">${escHtml(String(l.quantity))} portions available</div>
        <div class="surplus-card-prices">
          <span class="surplus-original">₹${l.originalPrice}</span>
          <span class="surplus-discounted">₹${l.discountedPrice}</span>
          <span class="surplus-save-pct">${l.discountPct}% off</span>
        </div>
        <div class="surplus-card-pickup">⏰ Pickup: ${escHtml(l.pickupStart)} – ${escHtml(l.pickupEnd)}</div>
        <button class="dash-btn-primary surplus-claim-btn"
          onclick="openClaimModal('${l.id}','${escHtml(l.foodName)}',${l.quantity})">
          Claim for NGO →
        </button>
      </div>
    `).join('');
  }

  // Category filter + search both trigger re-render
  document.getElementById('category-filter').addEventListener('change', function () {
    renderSurplus(activeSurplus);
  });
  const searchInput = document.getElementById('surplus-search');
  if (searchInput) {
    searchInput.addEventListener('input', function () {
      renderSurplus(activeSurplus);
    });
  }

  // ── Claim Modal ───────────────────────────────────────────
  const claimModal = document.getElementById('claim-modal');
  const claimBackdrop = document.getElementById('claim-backdrop');
  const claimClose = document.getElementById('claim-close');

  window.openClaimModal = function (listingId, foodName, maxQty) {
    selectedListing = { listingId, foodName, maxQty };
    document.getElementById('claim-item-name').textContent = '📦 ' + foodName;
    document.getElementById('claim-qty').max = maxQty;
    document.getElementById('claim-qty').value = '';
    document.getElementById('claim-note').value = '';
    document.getElementById('claim-msg').textContent = '';
    claimModal.removeAttribute('hidden');
    requestAnimationFrame(() => claimModal.classList.add('open'));
  };

  function closeClaimModal() {
    claimModal.classList.remove('open');
    setTimeout(() => claimModal.setAttribute('hidden', ''), 280);
  }

  claimClose.addEventListener('click', closeClaimModal);
  claimBackdrop.addEventListener('click', closeClaimModal);

  // Also handle demo card buttons
  document.querySelectorAll('.demo-card .surplus-claim-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      openClaimModal(btn.dataset.id, btn.closest('.surplus-card').querySelector('.surplus-card-name').textContent, 10);
    });
  });

  // ── Confirm Claim ─────────────────────────────────────────
  document.getElementById('confirm-claim-btn').addEventListener('click', function () {
    if (!currentUser || !selectedListing) return;
    const qty = parseInt(document.getElementById('claim-qty').value);
    const note = document.getElementById('claim-note').value.trim();
    const msg = document.getElementById('claim-msg');

    if (!qty || qty < 1) {
      msg.textContent = '⚠ Please enter a valid quantity.';
      msg.style.color = '#F5A623'; return;
    }

    this.textContent = '⏳ Claiming…'; this.disabled = true;

    const claimsRef = db.collection('claims');
    const ngoName = document.getElementById('dash-name').textContent;

    claimsRef.add({
      ngoId: currentUser.uid,
      ngoName: ngoName,
      listingId: selectedListing.listingId,
      foodName: selectedListing.foodName,
      quantity: qty,
      note: note,
      status: 'scheduled',
      claimedAt: firebase.firestore.FieldValue.serverTimestamp()
    }).then(() => {
      // Mark listing as claimed if fully taken
      if (qty >= selectedListing.maxQty) {
        db.collection('listings').doc(selectedListing.listingId).update({ status: 'claimed' })
          .catch(() => { });
      }
      if (window.Toast) Toast.success('Claimed! Check My Collections for pickup details.');
      msg.style.color = '#7BE08A';
      msg.textContent = '✅ Claimed! Check "My Collections" for pickup details.';
      setTimeout(closeClaimModal, 1800);
    }).catch(err => {
      if (window.Toast) Toast.error('Could not claim: ' + err.message);
      msg.style.color = '#F5A623';
      msg.textContent = '⚠ ' + err.message;
    }).finally(() => {
      document.getElementById('confirm-claim-btn').textContent = '✅ Confirm Claim';
      document.getElementById('confirm-claim-btn').disabled = false;
    });
  });

  // ── Load Collections ──────────────────────────────────────
  function loadCollections(uid) {
    db.collection('claims')
      .where('ngoId', '==', uid)
      .orderBy('claimedAt', 'desc')
      .onSnapshot(snapshot => {
        allCollections = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderCollections('all');
        updateStats();
        updateImpact();
        updateMilestones();
      }, err => {
        console.warn('[Collections]', err.message);
      });
  }

  // ── Render Collections ────────────────────────────────────
  function renderCollections(filter) {
    const container = document.getElementById('collections-container');
    const empty = document.getElementById('collections-empty');
    const filtered = filter === 'all'
      ? allCollections
      : allCollections.filter(c => (c.status || 'scheduled') === filter);

    if (!filtered.length) {
      empty.style.display = ''; container.innerHTML = '';
      container.appendChild(empty); return;
    }
    empty.style.display = 'none';

    container.innerHTML = filtered.map(c => `
      <div class="listing-card">
        <div>
          <div class="listing-card-name">${escHtml(c.foodName)}</div>
          <div class="listing-card-meta">
            <span>📦 ${c.quantity} portions</span>
            <span>📅 ${c.claimedAt ? c.claimedAt.toDate().toLocaleDateString('en-IN') : 'Pending'}</span>
            ${c.note ? `<span>📝 ${escHtml(c.note)}</span>` : ''}
          </div>
        </div>
        <div class="listing-card-actions">
          <span class="status-badge status-${c.status === 'collected' ? 'claimed' : c.status === 'missed' ? 'expired' : 'active'}">
            ${collStatusLabel(c.status)}
          </span>
          ${c.status === 'scheduled'
        ? `<button class="listing-delete-btn" onclick="markCollected('${c.id}')">Mark Collected ✅</button>`
        : ''}
        </div>
      </div>
    `).join('');
  }

  window.markCollected = function (id) {
    db.collection('claims').doc(id).update({ status: 'collected' });
  };

  // Collection filter chips
  document.querySelectorAll('.filter-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      renderCollections(chip.dataset.filter);
    });
  });

  // ── Update Stats ──────────────────────────────────────────
  function updateStats() {
    const totalMeals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    const co2 = (totalMeals * 2.5).toFixed(1);
    document.getElementById('stat-claimed').textContent = totalMeals;
    document.getElementById('stat-people').textContent = totalMeals;
    document.getElementById('stat-co2').textContent = co2 + ' kg';
  }

  // ── Impact Tab ────────────────────────────────────────────
  function updateImpact() {
    const totalMeals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    const co2 = (totalMeals * 2.5).toFixed(1);
    animateNum('ana-meals', totalMeals);
    animateNum('ana-people', totalMeals);
    document.getElementById('ana-co2').textContent = co2 + ' kg';

    // Chart.js — 7-day collections trend
    renderNgoChart();
  }

  let ngoChartInstance = null;
  function renderNgoChart() {
    const canvas = document.getElementById('ngo-meals-chart');
    if (!canvas || typeof Chart === 'undefined') return;

    const days = 7;
    const labels = [];
    const data = new Array(days).fill(0);
    const now = new Date();

    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      labels.push(d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric' }));
    }

    if (allCollections.length) {
      allCollections.forEach(c => {
        if (!c.claimedAt) return;
        const date = c.claimedAt.toDate ? c.claimedAt.toDate() : new Date(c.claimedAt);
        const daysAgo = Math.floor((now - date) / 86400000);
        if (daysAgo < days) data[days - 1 - daysAgo] += (c.quantity || 0);
      });
    } else {
      // Demo data
      [0, 2, 5, 3, 8, 5, 10].forEach((v, i) => { data[i] = v; });
    }

    const ctx = canvas.getContext('2d');
    const greenGrad = ctx.createLinearGradient(0, 0, 0, 220);
    greenGrad.addColorStop(0, 'rgba(123,224,138,.3)');
    greenGrad.addColorStop(1, 'rgba(123,224,138,.02)');

    if (ngoChartInstance) ngoChartInstance.destroy();
    ngoChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Meals Claimed',
          data,
          backgroundColor: greenGrad,
          borderColor: '#7BE08A',
          borderWidth: 2.5,
          pointBackgroundColor: '#7BE08A',
          pointRadius: 4,
          pointHoverRadius: 7,
          fill: true,
          tension: 0.4,
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1A1535',
            titleColor: '#FEFAF2',
            bodyColor: '#7BE08A',
            borderColor: 'rgba(123,224,138,.3)',
            borderWidth: 1,
            padding: 12,
            callbacks: {
              label: ctx => ' ' + ctx.parsed.y + ' meals claimed'
            }
          }
        },
        scales: {
          x: {
            grid: { color: 'rgba(254,250,242,.05)' },
            ticks: { color: 'rgba(254,250,242,.45)', font: { size: 11 } }
          },
          y: {
            grid: { color: 'rgba(254,250,242,.05)' },
            ticks: { color: 'rgba(254,250,242,.45)', font: { size: 11 }, stepSize: 1 },
            beginAtZero: true
          }
        }
      }
    });
  }

  // ── Milestones ────────────────────────────────────────────
  function updateMilestones() {
    const totalMeals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    document.querySelectorAll('.milestone').forEach(m => {
      const target = parseInt(m.dataset.target);
      m.querySelector('.milestone-progress').textContent = totalMeals + ' / ' + target;
      if (totalMeals >= target) {
        m.classList.remove('locked'); m.classList.add('unlocked');
      }
    });
  }

  // ── Certificate Print (PDF-quality) ──────────────────────────────
  document.getElementById('download-cert-btn').addEventListener('click', function () {
    const meals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    const co2 = (meals * 2.5).toFixed(1);
    const orgName = document.getElementById('dash-name').textContent || 'Your Organisation';
    const dateStr = new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });
    const certId = 'RESKYU-' + Date.now().toString(36).toUpperCase();

    const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"/>
<title>RESKYU Impact Certificate</title>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;800;900&family=DM+Sans:wght@400;500&display=swap" rel="stylesheet"/>
<style>
@page{size:A4 landscape;margin:0}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'DM Sans',sans-serif;background:#0E0A1F;color:#FEFAF2;width:297mm;height:210mm;display:flex;align-items:center;justify-content:center;overflow:hidden}
.cert{width:270mm;height:190mm;background:linear-gradient(135deg,#0E0A1F 0%,#1A1535 50%,#0E0A1F 100%);border:2px solid rgba(245,166,35,.5);border-radius:16px;position:relative;overflow:hidden;display:flex;flex-direction:column;padding:32px 44px}
.cert::before{content:'';position:absolute;inset:10px;border:1px solid rgba(245,166,35,.15);border-radius:10px;pointer-events:none}
.cert::after{content:'';position:absolute;inset:0;background:radial-gradient(ellipse 60% 50% at 10% 10%,rgba(245,166,35,.08) 0%,transparent 50%),radial-gradient(ellipse 50% 50% at 90% 90%,rgba(139,92,246,.07) 0%,transparent 50%);pointer-events:none}
.hdr{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}
.logo{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:1.5rem;letter-spacing:.1em}
.logo em{color:#F5A623;font-style:normal}
.badge{background:rgba(245,166,35,.12);border:1px solid rgba(245,166,35,.35);border-radius:50px;padding:4px 16px;font-family:'Space Grotesk',sans-serif;font-weight:700;font-size:.65rem;letter-spacing:.12em;text-transform:uppercase;color:#F5A623}
.ttl{text-align:center;margin-bottom:20px}
.ttl p{font-size:.75rem;letter-spacing:.18em;text-transform:uppercase;color:rgba(254,250,242,.45);margin-bottom:4px}
.ttl h1{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:2.2rem;letter-spacing:-.03em;text-transform:uppercase}
.org{text-align:center;margin-bottom:24px}
.org p{font-size:.75rem;color:rgba(254,250,242,.45);margin-bottom:4px}
.org h2{font-family:'Space Grotesk',sans-serif;font-weight:800;font-size:1.7rem;color:#F5A623}
.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px}
.sbox{background:rgba(254,250,242,.04);border:1px solid rgba(254,250,242,.1);border-radius:12px;padding:12px;text-align:center}
.sbox .ico{font-size:1.4rem;margin-bottom:4px}
.sbox .val{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:1.4rem;color:#F5A623;margin-bottom:2px}
.sbox .lbl{font-size:.65rem;color:rgba(254,250,242,.45);line-height:1.4}
.ftr{display:flex;align-items:flex-end;justify-content:space-between;margin-top:auto;padding-top:14px;border-top:1px solid rgba(254,250,242,.08)}
.meta{font-size:.65rem;color:rgba(254,250,242,.35);line-height:1.9}
.meta strong{color:rgba(254,250,242,.55)}
.seal{width:68px;height:68px;border-radius:50%;background:rgba(245,166,35,.12);border:2px solid rgba(245,166,35,.4);display:flex;flex-direction:column;align-items:center;justify-content:center;font-size:.5rem;font-weight:700;letter-spacing:.05em;text-transform:uppercase;color:#F5A623;text-align:center;gap:2px}
.seal .si{font-size:1.3rem}
@media print{body{background:#0E0A1F!important}}
</style></head>
<body><div class="cert">
  <div class="hdr"><div class="logo">RES<em>KYU</em></div><div class="badge">Official Impact Certificate</div></div>
  <div class="ttl"><p>This certifies that</p><h1>Social Impact Achievement</h1></div>
  <div class="org"><p>Awarded to</p><h2>${orgName}</h2></div>
  <div class="stats">
    <div class="sbox"><div class="ico">🍲</div><div class="val">${meals}</div><div class="lbl">Meals Rescued<br>from Food Waste</div></div>
    <div class="sbox"><div class="ico">👥</div><div class="val">${meals}</div><div class="lbl">People Fed<br>with Surplus Food</div></div>
    <div class="sbox"><div class="ico">♻️</div><div class="val">${co2} kg</div><div class="lbl">CO₂ Emissions<br>Prevented</div></div>
  </div>
  <div class="ftr">
    <div class="meta"><strong>Certificate ID:</strong> ${certId}<br><strong>Issued on:</strong> ${dateStr}<br><strong>Verified by:</strong> RESKYU Platform — reskyu-app.netlify.app</div>
    <div class="seal"><span class="si">🏆</span>RESKYU<br>VERIFIED</div>
  </div>
</div>
<script>window.addEventListener('load',function(){setTimeout(function(){window.print();},600);})<\/script>
</body></html>`;

    const win = window.open('', '_blank', 'width=1100,height=820');
    if (!win) { alert('Please allow pop-ups to generate your certificate.'); return; }
    win.document.write(html);
    win.document.close();
  });


  // ── Profile Save ───────────────────────────────────────────
  document.getElementById('profile-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const user = auth.currentUser; if (!user) return;
    const name = document.getElementById('edit-name').value.trim();
    const address = document.getElementById('edit-address').value.trim();
    const phone = document.getElementById('edit-phone').value.trim();
    const regId = document.getElementById('edit-reg').value.trim();
    const btn = document.getElementById('save-profile-btn');
    btn.textContent = 'Saving…'; btn.disabled = true;
    Promise.all([
      user.updateProfile({ displayName: name }),
      db.collection('users').doc(user.uid).update({ name, address, phone, regId })
    ]).then(() => {
      document.getElementById('profile-msg').style.color = '#7BE08A';
      document.getElementById('profile-msg').textContent = '✓ Profile saved!';
      document.getElementById('dash-name').textContent = name;
      document.getElementById('dash-avatar').textContent =
        name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
    }).catch(err => {
      document.getElementById('profile-msg').style.color = '#F5A623';
      document.getElementById('profile-msg').textContent = '⚠ ' + err.message;
    }).finally(() => { btn.textContent = 'Save Changes'; btn.disabled = false; });
  });

  // ── Reset Password ─────────────────────────────────────────
  document.getElementById('reset-pw-btn').addEventListener('click', function () {
    const user = auth.currentUser; if (!user) return;
    this.textContent = 'Sending…'; this.disabled = true;
    auth.sendPasswordResetEmail(user.email).then(() => {
      document.getElementById('reset-msg').style.color = '#7BE08A';
      document.getElementById('reset-msg').textContent = '✓ Reset link sent!';
    }).finally(() => { this.textContent = 'Send Reset Email'; this.disabled = false; });
  });

  // ── Logout ─────────────────────────────────────────────────
  document.getElementById('dash-logout-btn').addEventListener('click', function () {
    auth.signOut().then(() => window.location.href = 'pitch.html');
  });

  // ── Helpers ────────────────────────────────────────────────
  function greeting() {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning,' : h < 17 ? 'Good afternoon,' : 'Good evening,';
  }
  function escHtml(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function categoryEmoji(c) {
    const m = { meals: '🍛', snacks: '🥪', breads: '🫓', desserts: '🍮', beverages: '🧃', veg: '🥗', mystery: '🎁' };
    return m[c] || '🍽️';
  }
  function collStatusLabel(s) {
    return s === 'collected' ? '✅ Collected' : s === 'missed' ? '❌ Missed' : '🕐 Scheduled';
  }
  function animateNum(id, target) {
    const el = document.getElementById(id); if (!el) return;
    let cur = 0; const step = Math.max(1, Math.ceil(target / 30));
    const t = setInterval(() => {
      cur = Math.min(cur + step, target);
      el.textContent = cur;
      if (cur >= target) clearInterval(t);
    }, 40);
  }

})();
