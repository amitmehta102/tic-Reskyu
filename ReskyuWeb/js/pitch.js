/**
 * RESKYU — Pitch Deck Interactive Logic
 * Technocrats Institute of Technology, Bhopal · Hackathon 2026
 *
 * Features:
 *  - Auto-generated nav dots
 *  - Intersection Observer → active slide tracking
 *  - Intersection Observer → fade-up scroll reveal
 *  - Top progress bar
 *  - Slide counter (e.g. 03 / 10)
 *  - Keyboard arrow navigation (↑ ↓ ← →)
 */

(function () {
  'use strict';

  const slides      = document.querySelectorAll('.slide');
  const progressBar = document.getElementById('progress-bar');
  const counter     = document.getElementById('slide-counter');
  const navDots     = document.getElementById('nav-dots');
  const total       = slides.length;
  let   current     = 0;

  // ── Build nav dots ─────────────────────────────────────────────
  slides.forEach((_, i) => {
    const btn = document.createElement('button');
    btn.className = 'dot' + (i === 0 ? ' active' : '');
    btn.setAttribute('aria-label', 'Go to slide ' + (i + 1));
    btn.addEventListener('click', () => scrollToSlide(i));
    navDots.appendChild(btn);
  });

  // ── Update UI (progress bar, counter, active dot) ──────────────
  function updateUI(idx) {
    current = idx;

    // Progress bar — 0% on slide 1, 100% on last slide
    const pct = ((idx) / (total - 1)) * 100;
    progressBar.style.width = pct + '%';

    // Slide counter — zero-padded
    const num = String(idx + 1).padStart(2, '0');
    counter.innerHTML = '<span>' + num + '</span> / ' + String(total).padStart(2, '0');

    // Nav dots
    document.querySelectorAll('.dot').forEach((d, i) => {
      d.classList.toggle('active', i === idx);
    });
  }

  // ── Smooth scroll to a slide ───────────────────────────────────
  function scrollToSlide(idx) {
    slides[idx].scrollIntoView({ behavior: 'smooth' });
  }

  // ── Intersection Observer — active slide tracking ──────────────
  const slideObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting && e.intersectionRatio > 0.5) {
          const idx = Array.from(slides).indexOf(e.target);
          updateUI(idx);
        }
      });
    },
    { threshold: 0.5 }
  );
  slides.forEach((s) => slideObserver.observe(s));

  // ── Intersection Observer — fade-up scroll reveal ──────────────
  const fadeObserver = new IntersectionObserver(
    (entries) => {
      entries.forEach((e) => {
        if (e.isIntersecting) {
          e.target.classList.add('visible');
          fadeObserver.unobserve(e.target); // animate once
        }
      });
    },
    { threshold: 0.15 }
  );
  document.querySelectorAll('.fade-up').forEach((el) => fadeObserver.observe(el));

  // ── Keyboard navigation ────────────────────────────────────────
  document.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
      e.preventDefault();
      if (current < total - 1) scrollToSlide(current + 1);
    } else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
      e.preventDefault();
      if (current > 0) scrollToSlide(current - 1);
    }
  });

  // ── Initialise ─────────────────────────────────────────────────
  updateUI(0);
})();
