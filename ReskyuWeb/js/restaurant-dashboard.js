/* ============================================================
   RESKYU — Restaurant Dashboard Logic
   Handles: auth guard, tab nav, listing form, listings feed,
            price calculator, analytics, profile, ESG score
   ============================================================ */
(function () {
  'use strict';

  const auth = window.RESKYU_AUTH;
  const db   = window.RESKYU_DB;

  const loadingEl = document.getElementById('dash-loading');
  const mainEl    = document.getElementById('dash-main');

  if (!auth || !db) { window.location.href = 'pitch.html'; return; }

  let currentUser = null;
  let allListings  = [];

  // ── Auth Guard ────────────────────────────────────────────
  auth.onAuthStateChanged(function (user) {
    if (!user) { window.location.href = 'pitch.html'; return; }
    currentUser = user;
    loadProfile(user);
    loadListings(user.uid);
  });

  // ── Tab Navigation (syncs mobile bottom bar too) ─────────────
  window.switchTab = function (tabId) {
    // Desktop top tabs
    document.querySelectorAll('.role-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    // Mobile bottom bar
    document.querySelectorAll('.mobile-tab-btn').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    // Panels
    document.querySelectorAll('.role-panel').forEach(p => {
      const id = 'tab-' + tabId;
      if (p.id === id) { p.classList.add('active'); p.removeAttribute('hidden'); }
      else             { p.classList.remove('active'); p.setAttribute('hidden', ''); }
    });
  };
  document.querySelectorAll('.role-tab, .mobile-tab-btn').forEach(t => {
    t.addEventListener('click', () => switchTab(t.dataset.tab));
  });

  // ── Load Profile ──────────────────────────────────────────
  function loadProfile(user) {
    db.collection('users').doc(user.uid).get().then(doc => {
      const data   = doc.exists ? doc.data() : {};
      const name   = data.name  || user.displayName || 'Restaurant';
      const email  = user.email;
      const joined = data.createdAt
        ? data.createdAt.toDate().toLocaleDateString('en-IN', { year:'numeric', month:'long', day:'numeric' })
        : 'Recently';

      const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0,2);
      document.getElementById('dash-avatar').textContent   = initials;
      document.getElementById('dash-name').textContent     = name;
      document.getElementById('dash-greeting').textContent = greeting();

      // Profile form
      document.getElementById('edit-name').value   = name;
      document.getElementById('edit-email').value  = email;
      document.getElementById('edit-address').value = data.address || '';
      document.getElementById('edit-phone').value  = data.phone   || '';
      document.getElementById('edit-joined').value = joined;

      loadingEl.style.display = 'none';
      mainEl.removeAttribute('hidden');

      // Trigger onboarding for new users
      if (window.Onboarding) {
        Onboarding.checkAndShow(user, db);
      }
    });
  }

  // ── Load Listings ─────────────────────────────────────────
  function loadListings(uid) {
    db.collection('listings')
      .where('restaurantId', '==', uid)
      .orderBy('createdAt', 'desc')
      .onSnapshot(snapshot => {
        allListings = snapshot.docs.map(d => ({ id: d.id, ...d.data() }));
        renderListings('all');
        updateStats();
        updateAnalytics();
      }, err => {
        // Firestore index not built yet — show empty gracefully
        console.warn('[Listings]', err.message);
        renderListings('all');
      });
  }

  // ── Render Listings ───────────────────────────────────────
  function renderListings(filter) {
    const container = document.getElementById('listings-container');
    const empty     = document.getElementById('listings-empty');

    const filtered = filter === 'all' ? allListings
      : allListings.filter(l => (l.status || 'active') === filter);

    if (filtered.length === 0) {
      empty.style.display = ''; container.innerHTML = '';
      container.appendChild(empty); return;
    }
    empty.style.display = 'none';

    container.innerHTML = filtered.map(l => `
      <div class="listing-card" id="card-${l.id}">
        <div>
          <div class="listing-card-name">${escHtml(l.foodName)}</div>
          <div class="listing-card-meta">
            <span>📦 ${escHtml(String(l.quantity))} portions</span>
            <span>💰 ₹${l.discountedPrice} <del style="opacity:.4;font-size:.75em">₹${l.originalPrice}</del></span>
            <span>⏰ ${escHtml(l.pickupStart)} – ${escHtml(l.pickupEnd)}</span>
            <span>${categoryEmoji(l.category)} ${escHtml(l.category)}</span>
          </div>
        </div>
        <div class="listing-card-actions">
          <span class="status-badge status-${l.status || 'active'}">${statusLabel(l.status)}</span>
          ${(l.status || 'active') === 'active'
            ? `<button class="listing-delete-btn" onclick="deleteListing('${l.id}')">Delete</button>`
            : ''}
        </div>
      </div>
    `).join('');
  }

  // ── Filter Chips ──────────────────────────────────────────
  document.querySelectorAll('.filter-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      renderListings(chip.dataset.filter);
    });
  });

  // ── Delete Listing ────────────────────────────────────────
  window.deleteListing = function (id) {
    if (!confirm('Remove this listing?')) return;
    db.collection('listings').doc(id).update({ status: 'expired' })
      .catch(err => console.error(err));
  };

  // ── Price Calculator ────────────────────────────────────── 
  const origInput    = document.getElementById('food-original-price');
  const discSelect   = document.getElementById('food-discount');
  const discInput    = document.getElementById('food-discounted-price');
  const pricePreview = document.getElementById('price-preview');

  function calcPrice() {
    const orig = parseFloat(origInput.value);
    const disc = parseInt(discSelect.value);
    if (!orig || !disc) { pricePreview.setAttribute('hidden',''); return; }
    const discounted = Math.round(orig * (1 - disc / 100));
    discInput.value = discounted;
    document.getElementById('preview-original').textContent   = '₹' + orig;
    document.getElementById('preview-discounted').textContent = '₹' + discounted;
    document.getElementById('preview-savings').textContent    = '₹' + (orig - discounted) + ' (' + disc + '%)';
    pricePreview.removeAttribute('hidden');
  }
  origInput.addEventListener('input', calcPrice);
  discSelect.addEventListener('change', calcPrice);

  // ── Listing Form Submit ────────────────────────────────────
  const listingForm = document.getElementById('listing-form');
  const listingMsg  = document.getElementById('listing-msg');

  listingForm.addEventListener('submit', function (e) {
    e.preventDefault();
    if (!currentUser) return;

    const name     = document.getElementById('food-name').value.trim();
    const category = document.getElementById('food-category').value;
    const qty      = parseInt(document.getElementById('food-qty').value);
    const origP    = parseFloat(document.getElementById('food-original-price').value);
    const disc     = parseInt(document.getElementById('food-discount').value);
    const discP    = parseFloat(document.getElementById('food-discounted-price').value);
    const start    = document.getElementById('pickup-start').value;
    const end      = document.getElementById('pickup-end').value;
    const desc     = document.getElementById('food-desc').value.trim();

    if (!name || !category || !qty || !origP || !disc || !start || !end) {
      listingMsg.textContent = '⚠ Please fill all required fields.';
      listingMsg.style.color = '#F5A623'; return;
    }

    const btn = document.getElementById('list-submit-btn');
    btn.textContent = '⏳ Publishing…'; btn.disabled = true;

    db.collection('users').doc(currentUser.uid).get().then(doc => {
      const restaurantName = doc.exists ? (doc.data().name || 'Restaurant') : 'Restaurant';

      return db.collection('listings').add({
        restaurantId:   currentUser.uid,
        restaurantName: restaurantName,
        foodName:       name,
        category:       category,
        quantity:       qty,
        originalPrice:  origP,
        discountedPrice: discP,
        discountPct:    disc,
        pickupStart:    start,
        pickupEnd:      end,
        description:    desc,
        status:         'active',
        createdAt:      firebase.firestore.FieldValue.serverTimestamp()
      });
    }).then(() => {
      if (window.Toast) Toast.success('Listing published! Now visible to students & NGOs.');
      listingMsg.style.color = '#7BE08A';
      listingMsg.textContent = '✅ Listing published!';
      listingForm.reset();
      pricePreview.setAttribute('hidden', '');
      setTimeout(() => switchTab('listings'), 1800);
    }).catch(err => {
      if (window.Toast) Toast.error('Could not publish: ' + err.message);
      listingMsg.style.color = '#F5A623';
      listingMsg.textContent = '⚠ Error: ' + err.message;
    }).finally(() => {
      btn.textContent = '🚀 Publish Listing'; btn.disabled = false;
    });
  });

  // ── Update Stats Strip ─────────────────────────────────────
  function updateStats() {
    const total   = allListings.length;
    const claimed = allListings.filter(l => l.status === 'claimed').length;
    const co2     = (claimed * 2.5).toFixed(1);
    const revenue = allListings
      .filter(l => l.status === 'claimed')
      .reduce((s, l) => s + (l.discountedPrice || 0), 0);

    document.getElementById('stat-listed').textContent  = total;
    document.getElementById('stat-claimed').textContent = claimed;
    document.getElementById('stat-co2').textContent     = co2 + ' kg';
    document.getElementById('stat-revenue').textContent = '₹' + revenue;
  }

  // ── Analytics Tab ──────────────────────────────────────────
  function updateAnalytics() {
    const claimed = allListings.filter(l => l.status === 'claimed').length;
    const co2     = (claimed * 2.5).toFixed(1);
    const revenue = allListings.filter(l => l.status === 'claimed')
      .reduce((s, l) => s + (l.discountedPrice || 0), 0);

    animateNum('ana-total-rescued', claimed);
    document.getElementById('ana-co2').textContent     = co2 + ' kg';
    document.getElementById('ana-revenue').textContent = '₹' + revenue;

    // ESG score (0-100 based on listings)
    const envScore = Math.min(100, claimed * 5);
    const socScore = Math.min(100, claimed * 4);
    const conScore = Math.min(100, allListings.length * 8);
    setEsg('esg-env', envScore);
    setEsg('esg-soc', socScore);
    setEsg('esg-con', conScore);

    // Chart.js — 7-day meals rescued trend
    renderMealsChart();
  }

  let mealsChartInstance = null;
  function renderMealsChart() {
    const canvas = document.getElementById('meals-chart');
    if (!canvas || typeof Chart === 'undefined') return;

    // Build last-7-days buckets
    const days = 7;
    const labels = [];
    const data   = new Array(days).fill(0);
    const now    = new Date();
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date(now);
      d.setDate(d.getDate() - i);
      labels.push(d.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric' }));
    }

    // Count claimed listings per day (use demo data if none)
    const claimedListings = allListings.filter(l => l.status === 'claimed' && l.createdAt);
    if (claimedListings.length) {
      claimedListings.forEach(l => {
        const date = l.createdAt.toDate ? l.createdAt.toDate() : new Date(l.createdAt);
        const daysAgo = Math.floor((now - date) / 86400000);
        if (daysAgo < days) data[days - 1 - daysAgo]++;
      });
    } else {
      // Demo sparkline data
      [0, 1, 3, 2, 4, 2, 5].forEach((v, i) => { data[i] = v; });
    }

    const ctx = canvas.getContext('2d');
    const orangeGrad = ctx.createLinearGradient(0, 0, 0, 220);
    orangeGrad.addColorStop(0, 'rgba(245,166,35,.35)');
    orangeGrad.addColorStop(1, 'rgba(245,166,35,.02)');

    if (mealsChartInstance) mealsChartInstance.destroy();
    mealsChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Meals Rescued',
          data,
          backgroundColor: orangeGrad,
          borderColor: '#F5A623',
          borderWidth: 2,
          borderRadius: 8,
          borderSkipped: false,
        }]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1A1535',
            titleColor: '#FEFAF2',
            bodyColor: '#F5A623',
            borderColor: 'rgba(245,166,35,.3)',
            borderWidth: 1,
            padding: 12,
            callbacks: {
              label: ctx => ' ' + ctx.parsed.y + ' meals rescued'
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

  function setEsg(barId, pct) {
    const rounded = Math.round(pct);
    setTimeout(() => {
      document.getElementById(barId).style.width = rounded + '%';
      document.getElementById(barId + '-val').textContent = rounded + '%';
    }, 300);
  }

  // ── ESG Report Print (PDF-quality) ────────────────────────────────
  document.getElementById('generate-report-btn').addEventListener('click', function () {
    const claimed   = allListings.filter(l => l.status === 'claimed').length;
    const revenue   = allListings.filter(l => l.status === 'claimed').reduce((s,l) => s+(l.discountedPrice||0), 0);
    const co2       = (claimed * 2.5).toFixed(1);
    const restName  = document.getElementById('dash-name').textContent || 'Your Restaurant';
    const dateStr   = new Date().toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });
    const reportId  = 'ESG-' + Date.now().toString(36).toUpperCase();
    const envScore  = Math.min(100, claimed * 5);
    const socScore  = Math.min(100, claimed * 4);
    const conScore  = Math.min(100, allListings.length * 8);
    const overall   = Math.round((envScore + socScore + conScore) / 3);

    const bar = (pct) => {
      const filled = Math.round(pct / 5);
      return '█'.repeat(filled) + '░'.repeat(20 - filled) + ' ' + Math.round(pct) + '%';
    };

    const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"/>
<title>RESKYU ESG Report — ${restName}</title>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;700;800;900&family=DM+Sans:wght@400;500;600&display=swap" rel="stylesheet"/>
<style>
@page{size:A4 portrait;margin:0}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'DM Sans',sans-serif;background:#0E0A1F;color:#FEFAF2;width:210mm;min-height:297mm;padding:0}
.page{width:210mm;min-height:297mm;display:flex;flex-direction:column;padding:36px 44px;position:relative}
.page::after{content:'';position:absolute;inset:0;background:radial-gradient(ellipse 70% 40% at 10% 10%,rgba(245,166,35,.07) 0%,transparent 50%),radial-gradient(ellipse 50% 50% at 90% 90%,rgba(139,92,246,.06) 0%,transparent 50%);pointer-events:none;z-index:0}
.inner{position:relative;z-index:1;flex:1;display:flex;flex-direction:column}
.hdr{display:flex;align-items:center;justify-content:space-between;margin-bottom:28px;padding-bottom:20px;border-bottom:1px solid rgba(245,166,35,.3)}
.logo{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:1.4rem;letter-spacing:.1em}
.logo em{color:#F5A623;font-style:normal}
.rtype{font-family:'Space Grotesk',sans-serif;font-size:.65rem;font-weight:700;letter-spacing:.12em;text-transform:uppercase;color:#F5A623;background:rgba(245,166,35,.12);border:1px solid rgba(245,166,35,.3);border-radius:50px;padding:4px 14px}
.rest-section{margin-bottom:24px}
.rest-section .eyb{font-size:.65rem;font-weight:700;letter-spacing:.14em;text-transform:uppercase;color:rgba(254,250,242,.4);margin-bottom:6px}
.rest-section h1{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:1.6rem;color:#FEFAF2;letter-spacing:-.02em}
.overall-box{background:rgba(245,166,35,.1);border:1px solid rgba(245,166,35,.3);border-radius:14px;padding:18px 22px;margin-bottom:24px;display:flex;align-items:center;gap:20px}
.overall-score{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:3rem;color:#F5A623;line-height:1}
.overall-label .title{font-family:'Space Grotesk',sans-serif;font-weight:800;font-size:1rem;margin-bottom:4px}
.overall-label .sub{font-size:.78rem;color:rgba(254,250,242,.55)}
.section-title{font-family:'Space Grotesk',sans-serif;font-weight:800;font-size:.8rem;letter-spacing:.12em;text-transform:uppercase;color:rgba(254,250,242,.5);margin-bottom:12px}
.esg-rows{display:flex;flex-direction:column;gap:10px;margin-bottom:22px}
.esg-row{display:grid;grid-template-columns:120px 1fr 60px;gap:12px;align-items:center}
.esg-cat{font-size:.8rem;font-weight:600;color:rgba(254,250,242,.75)}
.esg-track{height:8px;background:rgba(254,250,242,.08);border-radius:4px;overflow:hidden}
.esg-fill{height:100%;border-radius:4px;background:linear-gradient(90deg,#F5A623,#FFD060)}
.esg-pct{font-family:'Space Grotesk',sans-serif;font-weight:700;font-size:.82rem;color:#F5A623;text-align:right}
.stat-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;margin-bottom:22px}
.sbox{background:rgba(254,250,242,.04);border:1px solid rgba(254,250,242,.09);border-radius:12px;padding:14px}
.sbox .ico{font-size:1.2rem;margin-bottom:6px}
.sbox .val{font-family:'Space Grotesk',sans-serif;font-weight:900;font-size:1.3rem;color:#F5A623;margin-bottom:2px}
.sbox .lbl{font-size:.7rem;color:rgba(254,250,242,.45)}
.ftr{display:flex;align-items:center;justify-content:space-between;margin-top:auto;padding-top:16px;border-top:1px solid rgba(254,250,242,.08)}
.meta{font-size:.62rem;color:rgba(254,250,242,.3);line-height:1.9}
.meta strong{color:rgba(254,250,242,.5)}
.seal{padding:6px 14px;border:1px solid rgba(245,166,35,.35);border-radius:50px;font-family:'Space Grotesk',sans-serif;font-size:.6rem;font-weight:700;letter-spacing:.1em;text-transform:uppercase;color:#F5A623}
@media print{body{background:#0E0A1F!important}}
</style></head>
<body><div class="page"><div class="inner">
  <div class="hdr"><div class="logo">RES<em>KYU</em></div><div class="rtype">ESG Impact Report</div></div>
  <div class="rest-section"><div class="eyb">Report prepared for</div><h1>${restName}</h1></div>
  <div class="overall-box">
    <div class="overall-score">${overall}</div>
    <div class="overall-label"><div class="title">Overall ESG Score</div><div class="sub">Combined Environmental, Social & Governance rating based on RESKYU activity</div></div>
  </div>
  <div class="section-title">ESG Breakdown</div>
  <div class="esg-rows">
    <div class="esg-row"><div class="esg-cat">🌱 Environmental</div><div class="esg-track"><div class="esg-fill" style="width:${envScore}%"></div></div><div class="esg-pct">${Math.round(envScore)}%</div></div>
    <div class="esg-row"><div class="esg-cat">🤝 Social Impact</div><div class="esg-track"><div class="esg-fill" style="width:${socScore}%"></div></div><div class="esg-pct">${Math.round(socScore)}%</div></div>
    <div class="esg-row"><div class="esg-cat">📊 Consistency</div><div class="esg-track"><div class="esg-fill" style="width:${conScore}%"></div></div><div class="esg-pct">${Math.round(conScore)}%</div></div>
  </div>
  <div class="section-title">Platform Summary</div>
  <div class="stat-grid">
    <div class="sbox"><div class="ico">📋</div><div class="val">${allListings.length}</div><div class="lbl">Total Listings</div></div>
    <div class="sbox"><div class="ico">🍲</div><div class="val">${claimed}</div><div class="lbl">Meals Rescued</div></div>
    <div class="sbox"><div class="ico">♻️</div><div class="val">${co2} kg</div><div class="lbl">CO₂ Prevented</div></div>
    <div class="sbox"><div class="ico">💰</div><div class="val">₹${revenue}</div><div class="lbl">Revenue from Surplus</div></div>
  </div>
  <div class="ftr">
    <div class="meta"><strong>Report ID:</strong> ${reportId}<br><strong>Generated:</strong> ${dateStr}<br><strong>Platform:</strong> RESKYU — reskyu-app.netlify.app</div>
    <div class="seal">ESG VERIFIED</div>
  </div>
</div></div>
<script>window.addEventListener('load',function(){setTimeout(function(){window.print();},600);})<\/script>
</body></html>`;

    const win = window.open('', '_blank', 'width=900,height=1100');
    if (!win) { alert('Please allow pop-ups to generate your report.'); return; }
    win.document.write(html);
    win.document.close();
  });


  // ── Profile Save ───────────────────────────────────────────
  document.getElementById('profile-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const user = auth.currentUser; if (!user) return;
    const name    = document.getElementById('edit-name').value.trim();
    const address = document.getElementById('edit-address').value.trim();
    const phone   = document.getElementById('edit-phone').value.trim();
    const btn = document.getElementById('save-profile-btn');
    btn.textContent = 'Saving…'; btn.disabled = true;
    Promise.all([
      user.updateProfile({ displayName: name }),
      db.collection('users').doc(user.uid).update({ name, address, phone })
    ]).then(() => {
      document.getElementById('profile-msg').style.color = '#7BE08A';
      document.getElementById('profile-msg').textContent = '✓ Profile saved!';
      document.getElementById('dash-name').textContent = name;
      document.getElementById('dash-avatar').textContent =
        name.split(' ').map(w=>w[0]).join('').toUpperCase().slice(0,2);
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
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
  function statusLabel(s) {
    return s === 'claimed' ? '✅ Claimed' : s === 'expired' ? '🔴 Expired' : '🟢 Active';
  }
  function categoryEmoji(c) {
    const m = { meals:'🍛', snacks:'🥪', breads:'🫓', desserts:'🍮', beverages:'🧃', veg:'🥗', mystery:'🎁' };
    return m[c] || '🍽️';
  }
  function animateNum(id, target) {
    const el = document.getElementById(id); if (!el) return;
    let cur = 0;
    const step = Math.ceil(target / 30);
    const t = setInterval(() => {
      cur = Math.min(cur + step, target);
      el.textContent = cur;
      if (cur >= target) clearInterval(t);
    }, 40);
  }

})();
