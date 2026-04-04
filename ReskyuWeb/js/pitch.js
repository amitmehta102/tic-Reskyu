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
  const scrollTopBtn = document.getElementById('scroll-top-btn');

  function onScroll() {
    const scrollTop = window.scrollY;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    progressBar.style.width = (scrollTop / docHeight * 100) + '%';
    navbar.classList.toggle('scrolled', scrollTop > 40);
    setActiveLink();
    // Scroll-to-top button visibility
    if (scrollTopBtn) {
      if (scrollTop > 400) {
        scrollTopBtn.removeAttribute('hidden');
        scrollTopBtn.classList.add('visible');
      } else {
        scrollTopBtn.classList.remove('visible');
        // Hide after fade-out
        setTimeout(() => {
          if (!scrollTopBtn.classList.contains('visible')) {
            scrollTopBtn.setAttribute('hidden', '');
          }
        }, 320);
      }
    }
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

      // Save to Firestore if Firebase ready, else just redirect
      const savePromise = (typeof window.RESKYU_SAVE_WAITLIST === 'function')
        ? window.RESKYU_SAVE_WAITLIST(val)
        : Promise.resolve();

      savePromise
        .then(() => {
          // Redirect to dedicated success/confirmation page
          window.location.href = 'early-access-success.html?email=' + encodeURIComponent(val);
        })
        .catch(err => {
          console.error('[RESKYU Waitlist]', err);
          // Still redirect on DB error — don't block the user
          window.location.href = 'early-access-success.html?email=' + encodeURIComponent(val);
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

  // Polls until window[fnName] is ready or timeout expires
  function waitForFirebase(fnName, timeoutMs, callback) {
    if (typeof window[fnName] === 'function') {
      callback(window[fnName]); return;
    }
    var elapsed = 0;
    var interval = setInterval(function () {
      elapsed += 200;
      if (typeof window[fnName] === 'function') {
        clearInterval(interval);
        callback(window[fnName]);
      } else if (elapsed >= timeoutMs) {
        clearInterval(interval);
        callback(null); // timed out
      }
    }, 200);
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

      loginMsg.textContent = '⏳ Connecting…';
      loginMsg.style.color = 'rgba(254,250,242,.5)';

      // Wait up to 8s for Firebase to finish loading, then proceed
      waitForFirebase('RESKYU_LOGIN', 8000, function (loginFn) {
        if (!loginFn) {
          loginMsg.textContent = '⚠ Could not connect to authentication service. Please refresh and try again.';
          loginMsg.style.color = '#F5A623'; return;
        }
        loginMsg.textContent = '⏳ Logging in…';
        loginFn(email, pass)
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

      signupMsg.textContent = '⏳ Connecting…';
      signupMsg.style.color = 'rgba(254,250,242,.5)';

      // Wait up to 8s for Firebase to finish loading, then proceed
      waitForFirebase('RESKYU_SIGNUP', 8000, function (signupFn) {
        if (!signupFn) {
          signupMsg.textContent = '⚠ Could not connect. Please refresh and try again.';
          signupMsg.style.color = '#F5A623'; return;
        }
        signupMsg.textContent = '⏳ Creating your account…';
        signupFn(name, email, pass, role)
          .then(() => {
            signupMsg.style.color = '#7BE08A';
            signupMsg.textContent = '🎉 Account created! Welcome to RESKYU.';
            signupForm.reset();
            setTimeout(() => { closeModal(); window.location.href = 'dashboard.html'; }, 1000);
          })
          .catch(err => {
            signupMsg.style.color = '#F5A623';
            signupMsg.textContent = '⚠ ' + firebaseErrorMessage(err.code);
          });
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

/* ════════════════════════════════════════════
   SCROLL TO TOP — click handler (#7)
════════════════════════════════════════════ */
(function () {
  'use strict';
  var btn = document.getElementById('scroll-top-btn');
  if (!btn) return;
  btn.addEventListener('click', function () {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });
})();

/* ════════════════════════════════════════════
   PWA INSTALL PROMPT (#6)
   Captures beforeinstallprompt, shows custom
   banner, remembers dismiss for 30 days.
════════════════════════════════════════════ */
(function () {
  'use strict';

  var DISMISS_KEY  = 'reskyu_pwa_dismissed';
  var deferredPrompt = null;
  var banner  = document.getElementById('pwa-banner');
  var installBtn = document.getElementById('pwa-install-btn');
  var dismissBtn = document.getElementById('pwa-dismiss-btn');

  if (!banner) return;

  // Don't show if dismissed within 30 days
  var dismissed = localStorage.getItem(DISMISS_KEY);
  if (dismissed && Date.now() - parseInt(dismissed, 10) < 30 * 86400000) return;

  // Capture the install event
  window.addEventListener('beforeinstallprompt', function (e) {
    e.preventDefault();
    deferredPrompt = e;
    // Show banner after a short delay so it doesn't pop immediately
    setTimeout(function () {
      banner.removeAttribute('hidden');
    }, 3000);
  });

  // Install button
  if (installBtn) {
    installBtn.addEventListener('click', function () {
      if (!deferredPrompt) return;
      deferredPrompt.prompt();
      deferredPrompt.userChoice.then(function (result) {
        if (result.outcome === 'accepted') {
          banner.setAttribute('hidden', '');
          localStorage.setItem(DISMISS_KEY, Date.now().toString());
        }
        deferredPrompt = null;
      });
    });
  }

  // Dismiss button
  if (dismissBtn) {
    dismissBtn.addEventListener('click', function () {
      banner.setAttribute('hidden', '');
      localStorage.setItem(DISMISS_KEY, Date.now().toString());
    });
  }

  // Hide once installed
  window.addEventListener('appinstalled', function () {
    banner.setAttribute('hidden', '');
  });
})();

/* ════════════════════════════════════════════
   COOKIE CONSENT BANNER (#8)
   Shows once, stores choice in localStorage.
   'accepted' → analytics can run
   'declined' → analytics disabled
════════════════════════════════════════════ */
(function () {
  'use strict';

  var COOKIE_KEY = 'reskyu_cookie_consent';
  var banner     = document.getElementById('cookie-banner');
  var acceptBtn  = document.getElementById('cookie-accept');
  var declineBtn = document.getElementById('cookie-decline');

  if (!banner) return;

  // Already decided — don't show again
  if (localStorage.getItem(COOKIE_KEY)) return;

  // Show banner after 1.5s
  var showTimer = setTimeout(function () {
    banner.removeAttribute('hidden');
  }, 1500);

  function dismiss(choice) {
    clearTimeout(showTimer);
    localStorage.setItem(COOKIE_KEY, choice);
    banner.setAttribute('hidden', '');
  }

  if (acceptBtn) {
    acceptBtn.addEventListener('click', function () {
      dismiss('accepted');
      // Tell Google Analytics consent was given (if GA is loaded)
      if (typeof gtag === 'function') {
        gtag('consent', 'update', {
          analytics_storage: 'granted',
        });
      }
    });
  }

  if (declineBtn) {
    declineBtn.addEventListener('click', function () {
      dismiss('declined');
    });
  }
})();

/* ════════════════════════════════════════════
   DARK / LIGHT MODE TOGGLE (#10)
════════════════════════════════════════════ */
(function () {
  'use strict';
  var THEME_KEY = 'reskyu_theme';
  var html      = document.documentElement;
  var toggleBtn = document.getElementById('theme-toggle');

  // Apply saved preference immediately (before paint)
  var saved = localStorage.getItem(THEME_KEY);
  if (saved === 'light') html.classList.add('light-mode');

  if (!toggleBtn) return;

  toggleBtn.addEventListener('click', function () {
    // Briefly disable transitions for instant swap
    html.style.transition = 'background .35s ease, color .35s ease';
    html.classList.toggle('light-mode');
    var isLight = html.classList.contains('light-mode');
    localStorage.setItem(THEME_KEY, isLight ? 'light' : 'dark');
    // Remove inline transition after swap is done
    setTimeout(function () { html.style.transition = ''; }, 400);
  });
})();

/* ════════════════════════════════════════════
   HERO STAT COUNTER ANIMATION (#11)
   Runs when hero-stats scrolls into view.
   Each [data-count-to] element counts up from 0.
   [data-count-glow] elements get a glow reveal.
════════════════════════════════════════════ */
(function () {
  'use strict';

  var statsSection = document.getElementById('hero-stats');
  if (!statsSection) return;

  var animated = false;

  function easeOut(t) { return 1 - Math.pow(1 - t, 3); }

  function countUp(el) {
    var to      = parseInt(el.dataset.countTo, 10);
    var prefix  = el.dataset.countPrefix  || '';
    var suffix  = el.dataset.countSuffix  || '';
    var label   = el.dataset.countLabel   || ''; // e.g. "35–" prepended before the animated number
    var dur     = 1400; // ms
    var start   = null;

    el.classList.add('stat-animated');

    function tick(ts) {
      if (!start) start = ts;
      var elapsed = ts - start;
      var progress = Math.min(elapsed / dur, 1);
      var current  = Math.round(easeOut(progress) * to);
      el.textContent = prefix + label + current + suffix;
      if (progress < 1) requestAnimationFrame(tick);
      else el.textContent = prefix + label + to + suffix; // Ensure exact final value
    }
    requestAnimationFrame(tick);
  }

  function runAnimations() {
    if (animated) return;
    animated = true;

    var nums = statsSection.querySelectorAll('.hero-stat-num');
    nums.forEach(function (el, i) {
      var delay = i * 120;
      if (el.hasAttribute('data-count-glow')) {
        setTimeout(function () { el.classList.add('stat-animated'); }, delay);
      } else if (el.dataset.countTo) {
        setTimeout(function () { countUp(el); }, delay);
      }
    });
  }

  // Trigger when stats strip enters viewport
  if ('IntersectionObserver' in window) {
    var obs = new IntersectionObserver(function (entries) {
      if (entries[0].isIntersecting) { runAnimations(); obs.disconnect(); }
    }, { threshold: 0.5 });
    obs.observe(statsSection);
  } else {
    // Fallback: run immediately
    runAnimations();
  }
})();
