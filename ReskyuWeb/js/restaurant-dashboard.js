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

  // ── Tab Navigation ────────────────────────────────────────
  window.switchTab = function (tabId) {
    document.querySelectorAll('.role-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('.role-panel').forEach(p => {
      const id = 'tab-' + tabId;
      if (p.id === id) { p.classList.add('active'); p.removeAttribute('hidden'); }
      else             { p.classList.remove('active'); p.setAttribute('hidden', ''); }
    });
  };
  document.querySelectorAll('.role-tab').forEach(t => {
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
      listingMsg.style.color = '#7BE08A';
      listingMsg.textContent = '✅ Listing published! It\'s now visible to students and NGOs.';
      listingForm.reset();
      pricePreview.setAttribute('hidden', '');
      setTimeout(() => switchTab('listings'), 1800);
    }).catch(err => {
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
  }

  function setEsg(barId, pct) {
    const rounded = Math.round(pct);
    setTimeout(() => {
      document.getElementById(barId).style.width = rounded + '%';
      document.getElementById(barId + '-val').textContent = rounded + '%';
    }, 300);
  }

  // ── ESG Report Download ────────────────────────────────────
  document.getElementById('generate-report-btn').addEventListener('click', function () {
    const claimed = allListings.filter(l => l.status === 'claimed').length;
    const text = [
      'RESKYU ESG IMPACT REPORT',
      '========================',
      'Restaurant: ' + (document.getElementById('dash-name').textContent),
      'Date: ' + new Date().toLocaleDateString('en-IN'),
      '',
      'SUMMARY',
      '-------',
      'Total Listings: ' + allListings.length,
      'Meals Rescued: ' + claimed,
      'CO2 Saved: ' + (claimed * 2.5).toFixed(1) + ' kg',
      'Revenue from Surplus: ₹' + allListings.filter(l=>l.status==='claimed').reduce((s,l)=>s+(l.discountedPrice||0),0),
      '',
      'Generated by RESKYU — reskyu-app.netlify.app'
    ].join('\n');

    const blob = new Blob([text], { type: 'text/plain' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'RESKYU_ESG_Report.txt';
    a.click();
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
