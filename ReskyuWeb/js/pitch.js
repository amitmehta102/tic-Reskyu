/**
 * RESKYU — Landing Page JS (TGTG-Inspired Redesign)
 * Handles: page load, sticky nav, mobile menu, scroll progress,
 *          active nav link, scroll-reveal, step slider, form.
 */
(function () {
  'use strict';

  // ── Page load fade-in ─────────────────────────────
  window.addEventListener('load', () => {
    document.body.classList.add('loaded');
  });
  setTimeout(() => document.body.classList.add('loaded'), 400);

  const navbar      = document.getElementById('navbar');
  const progressBar = document.getElementById('progress-bar');
  const hamburger   = document.getElementById('hamburger-btn');
  const navLinks    = document.querySelector('.nav-links');
  const navAnchors  = document.querySelectorAll('.nav-links a[href^="#"]');

  // ── Scroll: progress bar + navbar states ─────────────
  function onScroll() {
    const scrollTop = window.scrollY;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    progressBar.style.width = (scrollTop / docHeight * 100) + '%';
    navbar.classList.toggle('scrolled', scrollTop > 40);
    setActiveLink();
  }

  // ── Active nav link by section in view ───────────────
  function setActiveLink() {
    const mid = window.innerHeight / 2;
    navAnchors.forEach(a => {
      const sec = document.querySelector(a.getAttribute('href'));
      if (!sec) return;
      const { top, bottom } = sec.getBoundingClientRect();
      a.classList.toggle('active', top <= mid && bottom > mid);
    });
  }

  // ── Mobile hamburger menu ─────────────────────────────
  if (hamburger) {
    hamburger.addEventListener('click', () => {
      const isOpen = navLinks.classList.toggle('open');
      const spans = hamburger.querySelectorAll('span');
      spans[0].style.transform = isOpen ? 'rotate(45deg) translate(5px,5px)'   : '';
      spans[1].style.opacity   = isOpen ? '0' : '1';
      spans[2].style.transform = isOpen ? 'rotate(-45deg) translate(5px,-5px)' : '';
    });
  }

  // Close menu on link click
  navAnchors.forEach(a => {
    a.addEventListener('click', () => {
      navLinks.classList.remove('open');
      if (hamburger) hamburger.querySelectorAll('span').forEach(s => { s.style.transform = ''; s.style.opacity = ''; });
    });
  });

  // ── Scroll-reveal: fade-up ────────────────────────────
  const fadeObserver = new IntersectionObserver(entries => {
    entries.forEach(e => {
      if (e.isIntersecting) {
        e.target.classList.add('visible');
        fadeObserver.unobserve(e.target);
      }
    });
  }, { threshold: 0.1 });

  document.querySelectorAll('.fade-up').forEach(el => fadeObserver.observe(el));

  // ── HOW IT WORKS — Step Slider ────────────────────────
  const steps       = document.querySelectorAll('.how-step');
  const panels      = document.querySelectorAll('.how-panel');
  const dots        = document.querySelectorAll('.how-dot');
  const tabs        = document.querySelectorAll('.how-tab');
  const prevBtn     = document.getElementById('how-prev');
  const nextBtn     = document.getElementById('how-next');
  let   currentStep = 0;
  let   autoTimer;

  function goToStep(idx) {
    // clamp
    idx = (idx + steps.length) % steps.length;

    // steps
    steps.forEach((s, i) => s.classList.toggle('active', i === idx));
    // panels
    panels.forEach((p, i) => p.classList.toggle('active', i === idx));
    // dots
    dots.forEach((d, i)  => d.classList.toggle('active', i === idx));
    // tabs
    tabs.forEach((t, i)  => t.classList.toggle('active', i === idx));

    currentStep = idx;
  }

  function startAuto() {
    clearInterval(autoTimer);
    autoTimer = setInterval(() => goToStep(currentStep + 1), 4500);
  }

  if (prevBtn) prevBtn.addEventListener('click', () => { goToStep(currentStep - 1); startAuto(); });
  if (nextBtn) nextBtn.addEventListener('click', () => { goToStep(currentStep + 1); startAuto(); });

  dots.forEach(d => d.addEventListener('click', () => { goToStep(+d.dataset.idx); startAuto(); }));
  tabs.forEach(t => t.addEventListener('click', () => { goToStep(+t.dataset.tab); startAuto(); }));

  if (steps.length > 0) {
    goToStep(0);
    startAuto();
  }

  // ── Email form (early access) — saves to Firestore ──────
  const form    = document.getElementById('early-access-form');
  const input   = document.getElementById('email-input');
  const formMsg = document.getElementById('form-msg');

  if (form) {
    form.addEventListener('submit', e => {
      e.preventDefault();
      const val = input.value.trim();
      if (!val || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
        formMsg.textContent = 'Please enter a valid email address.';
        formMsg.style.color = '#F5A623';
        return;
      }
      formMsg.textContent = '⏳ Saving...';
      formMsg.style.color = 'var(--muted-teal)';

      // Save to Firestore if Firebase ready, else just show success
      const savePromise = (typeof window.RESKYU_SAVE_WAITLIST === 'function')
        ? window.RESKYU_SAVE_WAITLIST(val)
        : Promise.resolve();

      savePromise
        .then(() => {
          formMsg.textContent = '🎉 You\'re on the list! We\'ll be in touch soon.';
          formMsg.style.color = '#7BE08A';
          input.value = '';
        })
        .catch(err => {
          console.error('[RESKYU Waitlist]', err);
          // Still show success to user (don't block on DB error)
          formMsg.textContent = '🎉 You\'re on the list! We\'ll be in touch soon.';
          formMsg.style.color = '#7BE08A';
          input.value = '';
        });
    });
  }

  // ── Init ─────────────────────────────────────────────
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
})();

/* ════════════════════════════════════════════
   AUTH MODAL — open / close / tab / forms
════════════════════════════════════════════ */
(function () {
  'use strict';

  const modal      = document.getElementById('auth-modal');
  const backdrop   = document.getElementById('auth-backdrop');
  const closeBtn   = document.getElementById('auth-close');
  const openLogin  = document.getElementById('open-login');
  const openSignup = document.getElementById('open-signup');
  const authTabs   = document.querySelectorAll('.auth-tab');
  const authPanels = document.querySelectorAll('.auth-panel');
  const indicator  = document.querySelector('.auth-tab-indicator');

  if (!modal) return;

  // ── Open / Close ──────────────────────────────────────
  function openModal(startTab) {
    modal.removeAttribute('hidden');
    // tiny tick so "open" class triggers animation
    requestAnimationFrame(() => modal.classList.add('open'));
    document.body.style.overflow = 'hidden';
    switchTab(startTab || 'tab-login');
  }

  function closeModal() {
    modal.classList.remove('open');
    setTimeout(() => {
      modal.setAttribute('hidden', '');
      document.body.style.overflow = '';
    }, 280);
  }

  if (openLogin)  openLogin.addEventListener('click',  () => openModal('tab-login'));
  if (openSignup) openSignup.addEventListener('click', () => openModal('tab-signup'));
  closeBtn.addEventListener('click', closeModal);
  backdrop.addEventListener('click', closeModal);
  document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

  // ── Tab switching ─────────────────────────────────────
  function switchTab(tabId) {
    authTabs.forEach(t => {
      const isActive = t.id === tabId;
      t.classList.toggle('active', isActive);
    });
    authPanels.forEach(p => {
      const isActive = p.id === authTabs[tabId === 'tab-login' ? 0 : 1].dataset.target;
      p.classList.toggle('active', isActive);
      if (isActive) p.removeAttribute('hidden');
      else          p.setAttribute('hidden', '');
    });
    // Slide indicator
    if (indicator) indicator.classList.toggle('right', tabId === 'tab-signup');
  }

  authTabs.forEach(t => t.addEventListener('click', () => switchTab(t.id)));

  // "Switch" inline links (e.g. "Don't have an account? Sign Up")
  document.querySelectorAll('.auth-switch-btn').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.target));
  });

  // ── Forgot Password panel ─────────────────────────────
  function showForgotPanel() {
    const tabsBar = document.getElementById('auth-tabs-bar');
    authPanels.forEach(p => {
      if (p.id === 'panel-forgot') {
        p.classList.add('active'); p.removeAttribute('hidden');
      } else {
        p.classList.remove('active'); p.setAttribute('hidden', '');
      }
    });
    if (tabsBar) tabsBar.style.visibility = 'hidden';
  }

  function hideForgotPanel() {
    const tabsBar = document.getElementById('auth-tabs-bar');
    if (tabsBar) tabsBar.style.visibility = '';
    switchTab('tab-login');
  }

  const showForgotBtn = document.getElementById('show-forgot');
  const backToLogin   = document.getElementById('back-to-login');
  if (showForgotBtn) showForgotBtn.addEventListener('click', showForgotPanel);
  if (backToLogin)   backToLogin.addEventListener('click', hideForgotPanel);

  // ── Forgot form submit ────────────────────────────────
  const forgotForm = document.getElementById('forgot-form');
  const forgotMsg  = document.getElementById('forgot-msg');
  if (forgotForm) {
    forgotForm.addEventListener('submit', e => {
      e.preventDefault();
      const email = document.getElementById('forgot-email').value.trim();
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        forgotMsg.textContent = '⚠ Please enter a valid email.';
        forgotMsg.style.color = '#F5A623'; return;
      }
      if (typeof window.RESKYU_FORGOT_PASSWORD !== 'function') {
        forgotMsg.textContent = '⚠ Firebase not configured.';
        forgotMsg.style.color = '#F5A623'; return;
      }
      forgotMsg.textContent = '⏳ Sending…';
      forgotMsg.style.color = 'rgba(254,250,242,.5)';
      window.RESKYU_FORGOT_PASSWORD(email)
        .then(() => {
          forgotMsg.style.color = '#7BE08A';
          forgotMsg.textContent = '✓ Reset link sent! Check your inbox.';
          forgotForm.reset();
        })
        .catch(err => {
          forgotMsg.style.color = '#F5A623';
          forgotMsg.textContent = '⚠ ' + firebaseErrorMessage(err.code);
        });
    });
  }

  // ── Google Sign-In ────────────────────────────────────
  function handleGoogleAuth(btnId) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.addEventListener('click', () => {
      if (typeof window.RESKYU_GOOGLE_LOGIN !== 'function') {
        alert('Firebase not configured. See firebase-config.js'); return;
      }
      btn.textContent = 'Signing in…';
      btn.disabled = true;
      window.RESKYU_GOOGLE_LOGIN()
        .then(() => {
          closeModal();
          setTimeout(() => { window.location.href = 'dashboard.html'; }, 400);
        })
        .catch(err => {
          btn.disabled = false;
          btn.innerHTML = `<img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" width="18" height="18" /> Continue with Google`;
          console.error('[Google Login]', err.code, err.message);
        });
    });
  }
  handleGoogleAuth('google-login-btn');
  handleGoogleAuth('google-signup-btn');

  // ── Password visibility toggles ───────────────────────
  document.querySelectorAll('.auth-pw-toggle').forEach(btn => {
    btn.addEventListener('click', () => {
      const input = document.getElementById(btn.dataset.target);
      if (!input) return;
      const isHidden = input.type === 'password';
      input.type = isHidden ? 'text' : 'password';
      btn.textContent = isHidden ? '🙈' : '👁';
    });
  });

  // ── Login form — Firebase Auth ────────────────────────
  const loginForm = document.getElementById('login-form');
  const loginMsg  = document.getElementById('login-msg');

  if (loginForm) {
    loginForm.addEventListener('submit', e => {
      e.preventDefault();
      const email = document.getElementById('login-email').value.trim();
      const pass  = document.getElementById('login-password').value;

      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        loginMsg.textContent = '⚠ Please enter a valid email.';
        loginMsg.style.color = '#F5A623'; return;
      }
      if (!pass) {
        loginMsg.textContent = '⚠ Please enter your password.';
        loginMsg.style.color = '#F5A623'; return;
      }

      loginMsg.textContent = '⏳ Logging in…';
      loginMsg.style.color = 'rgba(254,250,242,.5)';

      if (typeof window.RESKYU_LOGIN !== 'function') {
        loginMsg.textContent = '⚠ Firebase not configured yet. See firebase-config.js';
        loginMsg.style.color = '#F5A623'; return;
      }

      window.RESKYU_LOGIN(email, pass)
        .then(() => {
          loginMsg.style.color = '#7BE08A';
          loginMsg.textContent = '✓ Welcome back!';
          loginForm.reset();
          setTimeout(() => { closeModal(); window.location.href = 'dashboard.html'; }, 900);
        })
        .catch(err => {
          loginMsg.style.color = '#F5A623';
          loginMsg.textContent = '⚠ ' + firebaseErrorMessage(err.code);
        });
    });
  }

  // ── Sign Up form — Firebase Auth + Firestore ─────────
  const signupForm = document.getElementById('signup-form');
  const signupMsg  = document.getElementById('signup-msg');

  if (signupForm) {
    signupForm.addEventListener('submit', e => {
      e.preventDefault();
      const name  = document.getElementById('signup-name').value.trim();
      const email = document.getElementById('signup-email').value.trim();
      const pass  = document.getElementById('signup-password').value;
      const role  = document.getElementById('signup-role').value;

      if (!name) {
        signupMsg.textContent = '⚠ Please enter your name.';
        signupMsg.style.color = '#F5A623'; return;
      }
      if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        signupMsg.textContent = '⚠ Please enter a valid email.';
        signupMsg.style.color = '#F5A623'; return;
      }
      if (pass.length < 8) {
        signupMsg.textContent = '⚠ Password must be at least 8 characters.';
        signupMsg.style.color = '#F5A623'; return;
      }
      if (!role) {
        signupMsg.textContent = '⚠ Please select your role.';
        signupMsg.style.color = '#F5A623'; return;
      }

      signupMsg.textContent = '⏳ Creating your account…';
      signupMsg.style.color = 'rgba(254,250,242,.5)';

      if (typeof window.RESKYU_SIGNUP !== 'function') {
        signupMsg.textContent = '⚠ Firebase not configured yet. See firebase-config.js';
        signupMsg.style.color = '#F5A623'; return;
      }

      window.RESKYU_SIGNUP(name, email, pass, role)
        .then(() => {
          signupMsg.style.color = '#7BE08A';
          signupMsg.textContent = '🎉 Account created! Welcome to RESKYU.';
          signupForm.reset();
          setTimeout(closeModal, 1600);
        })
        .catch(err => {
          signupMsg.style.color = '#F5A623';
          signupMsg.textContent = '⚠ ' + firebaseErrorMessage(err.code);
        });
    });
  }

  // ── Firebase error code → human-readable message ──────
  function firebaseErrorMessage(code) {
    const messages = {
      'auth/email-already-in-use':    'This email is already registered. Try logging in.',
      'auth/invalid-email':           'Please enter a valid email address.',
      'auth/weak-password':           'Password is too weak. Use at least 8 characters.',
      'auth/wrong-password':          'Incorrect password. Please try again.',
      'auth/user-not-found':          'No account found with this email.',
      'auth/too-many-requests':       'Too many attempts. Please wait a moment.',
      'auth/network-request-failed':  'Network error. Check your connection.',
      'auth/invalid-credential':      'Invalid email or password.',
    };
    return messages[code] || 'Something went wrong. Please try again.';
  }

})();
