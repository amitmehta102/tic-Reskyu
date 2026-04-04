/* ============================================================
   RESKYU — Dashboard Logic
   Handles: auth guard, profile load/edit, reset password,
            quick actions by role, stat display
   ============================================================ */

(function () {
  'use strict';

  const auth = window.RESKYU_AUTH;
  const db   = window.RESKYU_DB;

  const loadingEl = document.getElementById('dash-loading');
  const mainEl    = document.getElementById('dash-main');

  if (!auth || !db) {
    console.warn('[Dashboard] Firebase not ready');
    window.location.href = 'pitch.html';
    return;
  }

  // ── Auth Guard + Role Router ──────────────────────────────
  auth.onAuthStateChanged(function (user) {
    if (!user) {
      window.location.href = 'pitch.html';
      return;
    }
    // Redirect to role-specific dashboard
    db.collection('users').doc(user.uid).get().then(function (doc) {
      const role = doc.exists ? (doc.data().role || 'consumer') : 'consumer';
      if (role === 'restaurant') {
        window.location.href = 'restaurant-dashboard.html'; return;
      }
      if (role === 'ngo') {
        window.location.href = 'ngo-dashboard.html'; return;
      }
      // consumer — load generic dashboard
      loadProfile(user);
    });
  });

  // ── Load + Render Profile ─────────────────────────────────
  function loadProfile(user) {
    db.collection('users').doc(user.uid).get().then(function (doc) {

      const data = doc.exists ? doc.data() : {};
      const name    = data.name  || user.displayName || 'RESKYU User';
      const email   = data.email || user.email;
      const role    = data.role  || 'consumer';
      const joined  = data.createdAt
        ? data.createdAt.toDate().toLocaleDateString('en-IN', { year:'numeric', month:'long', day:'numeric' })
        : 'Recently';

      // Header
      const initials = name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
      document.getElementById('dash-avatar').textContent   = initials;
      document.getElementById('dash-name').textContent     = name;
      document.getElementById('dash-greeting').textContent = greeting();

      // Role badge
      const roleBadge = document.getElementById('dash-role-badge');
      roleBadge.textContent = roleLabel(role);

      // Populate form
      document.getElementById('edit-name').value   = name;
      document.getElementById('edit-email').value  = email;
      document.getElementById('edit-role').value   = role;
      document.getElementById('edit-joined').value = joined;

      // Quick actions by role
      renderActions(role);

      // Show main, hide loader
      loadingEl.style.display = 'none';
      mainEl.removeAttribute('hidden');

    }).catch(function (err) {
      console.error('[Dashboard]', err);
      loadingEl.style.display = 'none';
      mainEl.removeAttribute('hidden');
    });
  }

  // ── Greeting based on time ────────────────────────────────
  function greeting() {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning,';
    if (h < 17) return 'Good afternoon,';
    return 'Good evening,';
  }

  // ── Role Label ────────────────────────────────────────────
  function roleLabel(role) {
    const map = {
      restaurant: '🍽️ Restaurant / Merchant',
      consumer:   '🎓 Student / Consumer',
      ngo:        '🤝 NGO / Community Org',
    };
    return map[role] || '👤 Member';
  }

  // ── Quick Actions by Role ─────────────────────────────────
  function renderActions(role) {
    const container = document.getElementById('dash-actions');
    if (!container) return;

    const actionSets = {
      restaurant: [
        { icon: '📦', label: 'List Surplus Food',    href: '#' },
        { icon: '📊', label: 'View ESG Dashboard',   href: '#' },
        { icon: '🔔', label: 'Manage Notifications', href: '#' },
      ],
      consumer: [
        { icon: '🗺️', label: 'Find Food Near Me',   href: '#' },
        { icon: '🛒', label: 'My Recent Orders',      href: '#' },
        { icon: '❤️', label: 'Saved Restaurants',    href: '#' },
      ],
      ngo: [
        { icon: '📋', label: 'Claim Surplus',         href: '#' },
        { icon: '📈', label: 'Impact Reports',         href: '#' },
        { icon: '🤝', label: 'Partner Restaurants',   href: '#' },
      ],
    };

    const actions = actionSets[role] || actionSets.consumer;
    container.innerHTML = actions.map(a => `
      <a href="${a.href}" class="dash-action-btn">
        <span class="dash-action-icon">${a.icon}</span>
        <span class="dash-action-label">${a.label}</span>
        <span class="dash-action-arrow">→</span>
      </a>
    `).join('');
  }

  // ── Save Profile ──────────────────────────────────────────
  const profileForm = document.getElementById('profile-form');
  const profileMsg  = document.getElementById('profile-msg');

  if (profileForm) {
    profileForm.addEventListener('submit', function (e) {
      e.preventDefault();

      const user = auth.currentUser;
      if (!user) return;

      const name = document.getElementById('edit-name').value.trim();
      const role = document.getElementById('edit-role').value;

      if (!name) {
        profileMsg.textContent = '⚠ Please enter your name.';
        profileMsg.style.color = '#F5A623'; return;
      }

      const saveBtn = document.getElementById('save-profile-btn');
      saveBtn.textContent = 'Saving…';
      saveBtn.disabled = true;

      Promise.all([
        user.updateProfile({ displayName: name }),
        db.collection('users').doc(user.uid).update({ name: name, role: role })
      ])
        .then(function () {
          profileMsg.style.color = '#7BE08A';
          profileMsg.textContent = '✓ Profile updated!';
          document.getElementById('dash-name').textContent = name;
          document.getElementById('dash-avatar').textContent =
            name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
          document.getElementById('dash-role-badge').textContent = roleLabel(role);
          renderActions(role);
        })
        .catch(function (err) {
          profileMsg.style.color = '#F5A623';
          profileMsg.textContent = '⚠ ' + err.message;
        })
        .finally(function () {
          saveBtn.textContent = 'Save Changes';
          saveBtn.disabled = false;
        });
    });
  }

  // ── Reset Password ────────────────────────────────────────
  const resetBtn = document.getElementById('reset-pw-btn');
  const resetMsg = document.getElementById('reset-msg');

  if (resetBtn) {
    resetBtn.addEventListener('click', function () {
      const user = auth.currentUser;
      if (!user) return;

      resetBtn.textContent = 'Sending…';
      resetBtn.disabled = true;

      auth.sendPasswordResetEmail(user.email)
        .then(function () {
          resetMsg.style.color = '#7BE08A';
          resetMsg.textContent = '✓ Reset link sent to ' + user.email;
        })
        .catch(function (err) {
          resetMsg.style.color = '#F5A623';
          resetMsg.textContent = '⚠ ' + err.message;
        })
        .finally(function () {
          resetBtn.textContent = 'Send Reset Email';
          resetBtn.disabled = false;
        });
    });
  }

  // ── Logout button ─────────────────────────────────────────
  const logoutBtn = document.getElementById('dash-logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', function () {
      auth.signOut().then(function () {
        window.location.href = 'pitch.html';
      });
    });
  }

})();
