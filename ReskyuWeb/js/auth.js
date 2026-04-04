/* ============================================================
   RESKYU — Firebase Auth & Firestore Logic  (v2)
   ✅ Email/Password login + signup
   ✅ Google Sign-In (OAuth popup)
   ✅ Forgot Password (email reset)
   ✅ Redirect to /dashboard after login
   ✅ Auth state → nav user pill
   ✅ Waitlist saves to Firestore
   ============================================================ */

(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', function () {

    const auth = window.RESKYU_AUTH;
    const db   = window.RESKYU_DB;
    if (!auth || !db) {
      console.warn('[RESKYU] Firebase not initialised. Check firebase-config.js');
      return;
    }

    const USERS_COL    = 'users';
    const WAITLIST_COL = 'waitlist';

    // ── Google Provider ───────────────────────────────────
    const googleProvider = new firebase.auth.GoogleAuthProvider();
    googleProvider.setCustomParameters({ prompt: 'select_account' });

    // ── Auth State Observer ───────────────────────────────
    auth.onAuthStateChanged(function (user) {
      if (user) {
        db.collection(USERS_COL).doc(user.uid).get().then(function (doc) {
          const profile = doc.exists ? doc.data() : {};
          setNavLoggedIn(profile.name || user.displayName || user.email, user.uid);
        });
      } else {
        setNavLoggedOut();
      }
    });

    // ── Exposed API for pitch.js ──────────────────────────
    window.RESKYU_SIGNUP = function (name, email, password, role) {
      return auth.createUserWithEmailAndPassword(email, password)
        .then(function (cred) {
          const user = cred.user;
          return Promise.all([
            user.updateProfile({ displayName: name }),
            db.collection(USERS_COL).doc(user.uid).set({
              uid: user.uid, name: name, email: email, role: role,
              createdAt: firebase.firestore.FieldValue.serverTimestamp()
            })
          ]);
        });
    };

    window.RESKYU_LOGIN = function (email, password) {
      return auth.signInWithEmailAndPassword(email, password);
    };

    window.RESKYU_GOOGLE_LOGIN = function () {
      return auth.signInWithPopup(googleProvider).then(function (result) {
        const user = result.user;
        // Save to Firestore only if first sign-in
        return db.collection(USERS_COL).doc(user.uid).get().then(function (doc) {
          if (!doc.exists) {
            return db.collection(USERS_COL).doc(user.uid).set({
              uid: user.uid,
              name: user.displayName || '',
              email: user.email,
              role: 'consumer',  // default; user can change in dashboard
              createdAt: firebase.firestore.FieldValue.serverTimestamp()
            });
          }
        });
      });
    };

    window.RESKYU_LOGOUT = function () {
      return auth.signOut();
    };

    window.RESKYU_FORGOT_PASSWORD = function (email) {
      return auth.sendPasswordResetEmail(email);
    };

    window.RESKYU_SAVE_WAITLIST = function (email) {
      return db.collection(WAITLIST_COL).doc(email).set({
        email: email,
        signedUpAt: firebase.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    };

    // ── NAV: Logged In ────────────────────────────────────
    function setNavLoggedIn(displayName) {
      const btnLogin  = document.getElementById('open-login');
      const btnSignup = document.getElementById('open-signup');
      const existing  = document.getElementById('nav-user-menu');
      if (existing) existing.remove();
      if (btnLogin)  btnLogin.style.display  = 'none';
      if (btnSignup) btnSignup.style.display = 'none';

      const navRight = document.querySelector('.nav-right');
      if (!navRight) return;

      const initials = displayName
        ? displayName.split(' ').map(function(w){ return w[0]; }).join('').toUpperCase().slice(0,2)
        : '?';

      const userMenu = document.createElement('div');
      userMenu.id = 'nav-user-menu';
      userMenu.className = 'nav-user-menu';
      userMenu.innerHTML = `
        <div class="nav-user-pill" id="nav-user-pill" tabindex="0" role="button">
          <span class="nav-avatar">${initials}</span>
          <span class="nav-username">${displayName}</span>
          <span class="nav-caret">▾</span>
        </div>
        <div class="nav-dropdown" id="nav-dropdown" hidden>
          <div class="nav-dropdown-email">${displayName}</div>
          <a href="dashboard.html" class="nav-dropdown-item">📋 My Dashboard</a>
          <button class="nav-dropdown-item" id="nav-logout-btn">🚪 Log Out</button>
        </div>
      `;
      navRight.appendChild(userMenu);

      const pill     = userMenu.querySelector('#nav-user-pill');
      const dropdown = userMenu.querySelector('#nav-dropdown');

      pill.addEventListener('click', function () {
        dropdown.hasAttribute('hidden')
          ? dropdown.removeAttribute('hidden')
          : dropdown.setAttribute('hidden', '');
      });
      document.addEventListener('click', function (e) {
        if (!userMenu.contains(e.target)) dropdown.setAttribute('hidden', '');
      });
      userMenu.querySelector('#nav-logout-btn').addEventListener('click', function () {
        window.RESKYU_LOGOUT();
      });
    }

    // ── NAV: Logged Out ───────────────────────────────────
    function setNavLoggedOut() {
      var existing = document.getElementById('nav-user-menu');
      if (existing) existing.remove();
      var btnLogin  = document.getElementById('open-login');
      var btnSignup = document.getElementById('open-signup');
      if (btnLogin)  btnLogin.style.display  = '';
      if (btnSignup) btnSignup.style.display = '';
    }

  }); // end DOMContentLoaded

})();
