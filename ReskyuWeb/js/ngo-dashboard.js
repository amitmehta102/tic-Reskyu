/* ============================================================
   RESKYU — NGO Dashboard Logic
   Handles: auth guard, tab nav, browse surplus, claim modal,
            collections feed, milestones, impact stats, profile
   ============================================================ */
(function () {
  'use strict';

  const auth = window.RESKYU_AUTH;
  const db   = window.RESKYU_DB;

  const loadingEl = document.getElementById('dash-loading');
  const mainEl    = document.getElementById('dash-main');

  if (!auth || !db) { window.location.href = 'pitch.html'; return; }

  let currentUser    = null;
  let allCollections = [];
  let activeSurplus  = [];
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
      const name   = data.name  || user.displayName || 'NGO';
      const email  = user.email;
      const joined = data.createdAt
        ? data.createdAt.toDate().toLocaleDateString('en-IN', { year:'numeric', month:'long', day:'numeric' })
        : 'Recently';

      const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0,2);
      document.getElementById('dash-avatar').textContent   = initials;
      document.getElementById('dash-name').textContent     = name;
      document.getElementById('dash-greeting').textContent = greeting();

      document.getElementById('edit-name').value   = name;
      document.getElementById('edit-email').value  = email;
      document.getElementById('edit-address').value = data.address || '';
      document.getElementById('edit-phone').value  = data.phone   || '';
      document.getElementById('edit-reg').value    = data.regId   || '';
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
    const filtered = filter === 'all' ? items : items.filter(i => i.category === filter);

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

  // Category filter
  document.getElementById('category-filter').addEventListener('change', function () {
    renderSurplus(activeSurplus);
  });

  // ── Claim Modal ───────────────────────────────────────────
  const claimModal    = document.getElementById('claim-modal');
  const claimBackdrop = document.getElementById('claim-backdrop');
  const claimClose    = document.getElementById('claim-close');

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
    const qty  = parseInt(document.getElementById('claim-qty').value);
    const note = document.getElementById('claim-note').value.trim();
    const msg  = document.getElementById('claim-msg');

    if (!qty || qty < 1) {
      msg.textContent = '⚠ Please enter a valid quantity.';
      msg.style.color = '#F5A623'; return;
    }

    this.textContent = '⏳ Claiming…'; this.disabled = true;

    const claimsRef = db.collection('claims');
    const ngoName   = document.getElementById('dash-name').textContent;

    claimsRef.add({
      ngoId:      currentUser.uid,
      ngoName:    ngoName,
      listingId:  selectedListing.listingId,
      foodName:   selectedListing.foodName,
      quantity:   qty,
      note:       note,
      status:     'scheduled',
      claimedAt:  firebase.firestore.FieldValue.serverTimestamp()
    }).then(() => {
      // Mark listing as claimed if fully taken
      if (qty >= selectedListing.maxQty) {
        db.collection('listings').doc(selectedListing.listingId).update({ status: 'claimed' })
          .catch(() => {});
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
    const empty     = document.getElementById('collections-empty');
    const filtered  = filter === 'all'
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
    document.getElementById('stat-people').textContent  = totalMeals;
    document.getElementById('stat-co2').textContent     = co2 + ' kg';
  }

  // ── Impact Tab ────────────────────────────────────────────
  function updateImpact() {
    const totalMeals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    const co2 = (totalMeals * 2.5).toFixed(1);
    animateNum('ana-meals', totalMeals);
    animateNum('ana-people', totalMeals);
    document.getElementById('ana-co2').textContent = co2 + ' kg';
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

  // ── Certificate Download ───────────────────────────────────
  document.getElementById('download-cert-btn').addEventListener('click', function () {
    const meals = allCollections.reduce((s, c) => s + (c.quantity || 0), 0);
    const text = [
      'RESKYU IMPACT CERTIFICATE',
      '=========================',
      'Organisation: ' + document.getElementById('dash-name').textContent,
      'Date: ' + new Date().toLocaleDateString('en-IN'),
      '',
      'This is to certify that the above organisation has:',
      '',
      '✅ Rescued ' + meals + ' meals from food waste',
      '👥 Fed approximately ' + meals + ' individuals',
      '♻️ Prevented ' + (meals * 2.5).toFixed(1) + ' kg of CO₂ emissions',
      '',
      'Generated by RESKYU — reskyu-app.netlify.app'
    ].join('\n');
    const blob = new Blob([text], { type: 'text/plain' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'RESKYU_Impact_Certificate.txt';
    a.click();
  });

  // ── Profile Save ───────────────────────────────────────────
  document.getElementById('profile-form').addEventListener('submit', function (e) {
    e.preventDefault();
    const user = auth.currentUser; if (!user) return;
    const name    = document.getElementById('edit-name').value.trim();
    const address = document.getElementById('edit-address').value.trim();
    const phone   = document.getElementById('edit-phone').value.trim();
    const regId   = document.getElementById('edit-reg').value.trim();
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
  function categoryEmoji(c) {
    const m = { meals:'🍛', snacks:'🥪', breads:'🫓', desserts:'🍮', beverages:'🧃', veg:'🥗', mystery:'🎁' };
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
