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

  // ── Email form (early access) ─────────────────────────
  const form    = document.getElementById('early-access-form');
  const input   = document.getElementById('email-input');
  const formMsg = document.getElementById('form-msg');

  if (form) {
    form.addEventListener('submit', e => {
      e.preventDefault();
      const val = input.value.trim();
      if (!val || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
        formMsg.textContent = 'Please enter a valid email address.';
        formMsg.style.color = '#E8521A';
        return;
      }
      formMsg.textContent = '🎉 You\'re on the list! We\'ll be in touch soon.';
      formMsg.style.color = 'var(--teal2)';
      input.value = '';
    });
  }

  // ── Init ─────────────────────────────────────────────
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
})();
