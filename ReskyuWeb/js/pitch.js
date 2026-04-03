/**
 * RESKYU — Landing Page JS
 * Handles: sticky nav, mobile menu, scroll progress,
 *          active nav link, scroll-reveal animations.
 */
(function () {
  'use strict';

  const navbar      = document.getElementById('navbar');
  const progressBar = document.getElementById('progress-bar');
  const hamburger   = document.querySelector('.hamburger');
  const navLinks    = document.querySelector('.nav-links');
  const navAnchors  = document.querySelectorAll('.nav-links a[href^="#"]');

  // ── Scroll: progress bar + navbar border ─────────────
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
      navLinks.classList.toggle('open');
      const spans = hamburger.querySelectorAll('span');
      spans[0].style.transform = navLinks.classList.contains('open') ? 'rotate(45deg) translate(5px,5px)'   : '';
      spans[1].style.opacity   = navLinks.classList.contains('open') ? '0' : '1';
      spans[2].style.transform = navLinks.classList.contains('open') ? 'rotate(-45deg) translate(5px,-5px)' : '';
    });
  }

  // Close menu on link click
  navAnchors.forEach(a => {
    a.addEventListener('click', () => {
      navLinks.classList.remove('open');
      hamburger.querySelectorAll('span').forEach(s => { s.style.transform = ''; s.style.opacity = ''; });
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
  }, { threshold: 0.12 });

  document.querySelectorAll('.fade-up').forEach(el => fadeObserver.observe(el));

  // ── Email form (early access) ─────────────────────────
  const form   = document.getElementById('early-access-form');
  const input  = document.getElementById('email-input');
  const formMsg= document.getElementById('form-msg');

  if (form) {
    form.addEventListener('submit', e => {
      e.preventDefault();
      const val = input.value.trim();
      if (!val || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) {
        formMsg.textContent = 'Please enter a valid email.';
        formMsg.style.color = 'var(--accent)';
        return;
      }
      formMsg.textContent = '🎉 You\'re on the list! We\'ll be in touch.';
      formMsg.style.color = 'var(--green3)';
      input.value = '';
    });
  }

  // ── Init ─────────────────────────────────────────────
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();
})();
