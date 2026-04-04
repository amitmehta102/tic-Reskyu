/* ============================================================
   RESKYU — Analytics Module (js/analytics.js)
   Google Analytics 4 (GA4) integration + custom event helpers

   SETUP: Replace G-XXXXXXXXXX below with your GA4 Measurement ID
   Get it from: analytics.google.com → Admin → Data Streams → Measurement ID
   ============================================================ */

;(function () {
  'use strict';

  var GA_ID = 'G-XXXXXXXXXX'; // ← REPLACE with your GA4 Measurement ID

  // ── Load GA4 async (non-blocking) ───────────────────────
  function loadGA() {
    if (window._gaLoaded) return;
    window._gaLoaded = true;

    window.dataLayer = window.dataLayer || [];
    window.gtag = function () { dataLayer.push(arguments); };
    gtag('js', new Date());
    gtag('config', GA_ID, {
      send_page_view: true,          // auto page_view on load
      anonymize_ip:  true,           // GDPR-friendly
      cookie_flags:  'SameSite=None;Secure'
    });

    var s   = document.createElement('script');
    s.async = true;
    s.src   = 'https://www.googletagmanager.com/gtag/js?id=' + GA_ID;
    document.head.appendChild(s);

    console.log('[RESKYU Analytics] GA4 loaded (' + GA_ID + ')');
  }

  // Load GA4 on first user interaction (keeps scores clean for bot traffic)
  function onFirstInteraction() {
    loadGA();
    document.removeEventListener('click',     onFirstInteraction);
    document.removeEventListener('scroll',    onFirstInteraction);
    document.removeEventListener('touchstart',onFirstInteraction);
  }
  document.addEventListener('click',      onFirstInteraction, { once: true, passive: true });
  document.addEventListener('scroll',     onFirstInteraction, { once: true, passive: true });
  document.addEventListener('touchstart', onFirstInteraction, { once: true, passive: true });

  // ── Public event tracking API ───────────────────────────
  window.Analytics = {
    // Generic event
    event: function (name, params) {
      if (!window.gtag) { loadGA(); }
      gtag('event', name, params || {});
    },

    // Auth events
    signup:      function (role)    { this.event('sign_up',      { method: 'email', role: role }); },
    login:       function (method)  { this.event('login',        { method: method || 'email' }); },
    logout:      function ()        { this.event('logout'); },
    googleLogin: function ()        { this.event('login',        { method: 'google' }); },

    // Engagement events
    openModal:   function (type)    { this.event('modal_open',   { modal_type: type }); },
    closeModal:  function ()        { this.event('modal_close'); },
    clickCTA:    function (label)   { this.event('cta_click',    { cta_label: label }); },
    waitlistJoin:function (email)   { this.event('waitlist_join',{ email_domain: email.split('@')[1] }); },
    contactForm: function (type)    { this.event('contact_form', { contact_type: type }); },

    // Dashboard events
    listSurplus: function ()        { this.event('surplus_listed'); },
    claimSurplus:function ()        { this.event('surplus_claimed'); },

    // Scroll depth (call from scroll handler)
    scrollDepth: function (pct)     {
      var key = 'reskyu_scroll_' + pct;
      if (sessionStorage.getItem(key)) return;
      sessionStorage.setItem(key, '1');
      this.event('scroll_depth', { percent: pct });
    }
  };

  // ── Auto-track scroll depth milestones ──────────────────
  var scrollMilestones = [25, 50, 75, 90];
  window.addEventListener('scroll', function () {
    if (!window.Analytics) return;
    var pct = Math.round(
      (window.scrollY / (document.documentElement.scrollHeight - window.innerHeight)) * 100
    );
    scrollMilestones.forEach(function (m) {
      if (pct >= m) Analytics.scrollDepth(m);
    });
  }, { passive: true });

  // ── Auto-track key button clicks ─────────────────────────
  document.addEventListener('click', function (e) {
    if (!window.Analytics) return;
    var btn = e.target.closest('button, a[href]');
    if (!btn) return;

    var id = btn.id;
    if (id === 'open-login')  Analytics.openModal('login');
    if (id === 'open-signup') Analytics.openModal('signup');
    if (btn.matches('.hero-actions .btn-cream'))         Analytics.clickCTA('hero_signup');
    if (btn.matches('.hero-actions .btn-outline-cream')) Analytics.clickCTA('hero_learn_more');
    if (btn.matches('.cta-section .btn-cream'))          Analytics.clickCTA('cta_section_join');
    if (btn.matches('.nav-cta'))                         Analytics.clickCTA('nav_cta');
  }, { passive: true });

})();
