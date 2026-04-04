/* ============================================================
   RESKYU — Onboarding Overlay
   Shows once after first signup. Checks Firestore for
   onboarded flag and marks it done after user finishes.
   ============================================================ */
(function () {
  'use strict';

  var STEPS = {
    restaurant: [
      {
        icon: '🎉',
        title: 'Welcome to RESKYU!',
        body:  'You\'re now part of India\'s food rescue movement. A few seconds to get you started:'
      },
      {
        icon: '📦',
        title: 'List Your Surplus',
        body:  'At the end of each day, add your unsold food to RESKYU. Students and NGOs nearby will see it and pick it up directly — no delivery needed.'
      },
      {
        icon: '💰',
        title: 'Earn & Save the Planet',
        body:  'You earn revenue on food that would have been wasted, reduce your kitchen\'s CO₂ footprint, and get a real ESG score to share with stakeholders.'
      },
      {
        icon: '🚀',
        title: 'You\'re All Set!',
        body:  'Head to your dashboard to list your first surplus item. It takes under 60 seconds. Every listing makes a difference!'
      }
    ],
    ngo: [
      {
        icon: '🎉',
        title: 'Welcome to RESKYU!',
        body:  'Your NGO is now part of a network rescuing food from restaurants every day. Let\'s get you started:'
      },
      {
        icon: '🗺️',
        title: 'Browse Available Surplus',
        body:  'Restaurants near you list end-of-day surplus at 50–80% off. You can claim it for free or at a minimal cost to feed your community.'
      },
      {
        icon: '🌱',
        title: 'Track Your Impact',
        body:  'Every meal you claim is logged automatically. See your total meals rescued, CO₂ saved, and people fed — and download your impact certificate.'
      },
      {
        icon: '🚀',
        title: 'You\'re All Set!',
        body:  'Go to Browse Surplus now to see what\'s available near you. Listings update in real-time every evening!'
      }
    ],
    consumer: [
      {
        icon: '🎉',
        title: 'Welcome to RESKYU!',
        body:  'You can now rescue surplus food from restaurants near you at 50–80% off. Here\'s how to get started:'
      },
      {
        icon: '🍛',
        title: 'Find Surplus Near You',
        body:  'Browse restaurants listing their end-of-day meals at a fraction of the price. Great food, zero waste, massive savings.'
      },
      {
        icon: '📱',
        title: 'Pay & Pickup',
        body:  'Pay securely via UPI, then walk in and collect your meal during the pickup window — no delivery wait, no extra fees.'
      },
      {
        icon: '🚀',
        title: 'You\'re All Set!',
        body:  'Check your dashboard to browse available listings near you. New items appear every evening!'
      }
    ]
  };

  // ── Inject CSS ────────────────────────────────────────────
  var styleId = 'reskyu-onboarding-css';
  if (!document.getElementById(styleId)) {
    var s = document.createElement('style');
    s.id = styleId;
    s.textContent = `
      #onboarding-overlay {
        position: fixed; inset: 0; z-index: 100000;
        background: rgba(7,5,17,.88);
        backdrop-filter: blur(12px);
        display: flex; align-items: center; justify-content: center;
        padding: 24px;
        opacity: 0; transition: opacity .35s ease;
      }
      #onboarding-overlay.show { opacity: 1; }

      .ob-card {
        background: linear-gradient(145deg, #1A1535, #0E0A1F);
        border: 1px solid rgba(254,250,242,.1);
        border-radius: 24px;
        padding: 44px 40px 36px;
        max-width: 480px; width: 100%;
        text-align: center;
        box-shadow: 0 40px 80px rgba(0,0,0,.6);
        position: relative;
      }

      .ob-step-dots {
        display: flex; justify-content: center; gap: 8px;
        margin-bottom: 32px;
      }
      .ob-dot {
        width: 8px; height: 8px; border-radius: 50%;
        background: rgba(254,250,242,.15);
        transition: background .3s, width .3s;
      }
      .ob-dot.active {
        background: #F5A623; width: 24px; border-radius: 4px;
      }

      .ob-icon {
        font-size: 3.2rem; margin-bottom: 20px;
        animation: obBounce .5s cubic-bezier(.34,1.56,.64,1);
      }
      @keyframes obBounce {
        0%   { transform: scale(.5); opacity:0; }
        100% { transform: scale(1);  opacity:1; }
      }

      .ob-title {
        font-family: 'Space Grotesk', sans-serif;
        font-weight: 900; font-size: 1.5rem;
        color: #FEFAF2; margin-bottom: 14px;
        letter-spacing: -.02em;
      }
      .ob-body {
        font-family: 'DM Sans', sans-serif;
        font-size: .95rem; line-height: 1.7;
        color: rgba(254,250,242,.6);
        margin-bottom: 36px;
      }

      .ob-btn-row {
        display: flex; gap: 12px; justify-content: center;
      }
      .ob-btn-next {
        background: linear-gradient(135deg, #F5A623, #D4891A);
        color: #0E0A1F; border: none; border-radius: 50px;
        padding: 14px 36px; font-family: 'Space Grotesk', sans-serif;
        font-weight: 800; font-size: .95rem; cursor: pointer;
        transition: transform .15s, box-shadow .15s;
        box-shadow: 0 4px 20px rgba(245,166,35,.3);
      }
      .ob-btn-next:hover { transform: translateY(-2px); box-shadow: 0 8px 28px rgba(245,166,35,.45); }
      .ob-btn-skip {
        background: none; border: 1px solid rgba(254,250,242,.15);
        color: rgba(254,250,242,.4); border-radius: 50px;
        padding: 14px 24px; font-family: 'Space Grotesk', sans-serif;
        font-weight: 600; font-size: .88rem; cursor: pointer;
        transition: border-color .15s, color .15s;
      }
      .ob-btn-skip:hover { border-color: rgba(254,250,242,.35); color: rgba(254,250,242,.7); }

      .ob-step-fade { animation: obFade .35s ease; }
      @keyframes obFade {
        0%   { opacity:0; transform: translateX(20px); }
        100% { opacity:1; transform: translateX(0); }
      }

      @media (max-width: 520px) {
        .ob-card { padding: 32px 24px 28px; }
        .ob-title { font-size: 1.25rem; }
      }
    `;
    document.head.appendChild(s);
  }

  var overlay   = null;
  var stepIndex = 0;
  var steps     = [];

  // ── Public API ─────────────────────────────────────────────
  window.Onboarding = {
    show: function (role, onComplete) {
      steps = STEPS[role] || STEPS.consumer;
      stepIndex = 0;
      render(onComplete);
    },

    // Call this from auth.js after a new user signs up
    checkAndShow: function (user, db) {
      if (!user || !db) return;
      db.collection('users').doc(user.uid).get().then(function (doc) {
        if (!doc.exists) return;
        var data = doc.data();
        if (data.onboarded) return; // already seen it

        var role = data.role || 'consumer';
        Onboarding.show(role, function () {
          // Mark as onboarded
          db.collection('users').doc(user.uid)
            .update({ onboarded: true }).catch(function () {});
        });
      });
    }
  };

  // ── Render overlay ────────────────────────────────────────
  function render(onComplete) {
    if (overlay) overlay.parentNode.removeChild(overlay);

    overlay = document.createElement('div');
    overlay.id = 'onboarding-overlay';

    var dots = steps.map(function (_, i) {
      return '<div class="ob-dot' + (i === 0 ? ' active' : '') + '"></div>';
    }).join('');

    overlay.innerHTML = `
      <div class="ob-card">
        <div class="ob-step-dots">${dots}</div>
        <div id="ob-step-content" class="ob-step-fade">
          <div class="ob-icon">${steps[0].icon}</div>
          <h2 class="ob-title">${steps[0].title}</h2>
          <p class="ob-body">${steps[0].body}</p>
        </div>
        <div class="ob-btn-row">
          <button class="ob-btn-skip" id="ob-skip">Skip</button>
          <button class="ob-btn-next" id="ob-next">Next →</button>
        </div>
      </div>
    `;
    document.body.appendChild(overlay);

    requestAnimationFrame(function () {
      requestAnimationFrame(function () { overlay.classList.add('show'); });
    });

    overlay.querySelector('#ob-next').addEventListener('click', function () { advance(onComplete); });
    overlay.querySelector('#ob-skip').addEventListener('click', function () { close(onComplete); });
  }

  function advance(onComplete) {
    stepIndex++;
    if (stepIndex >= steps.length) { close(onComplete); return; }

    var content = document.getElementById('ob-step-content');
    var nextBtn  = document.getElementById('ob-next');
    var isLast   = stepIndex === steps.length - 1;

    // Update dots
    overlay.querySelectorAll('.ob-dot').forEach(function (d, i) {
      d.classList.toggle('active', i === stepIndex);
    });

    // Animate content
    content.classList.remove('ob-step-fade');
    void content.offsetWidth; // reflow
    content.classList.add('ob-step-fade');
    content.querySelector('.ob-icon').textContent  = steps[stepIndex].icon;
    content.querySelector('.ob-title').textContent = steps[stepIndex].title;
    content.querySelector('.ob-body').textContent  = steps[stepIndex].body;

    nextBtn.textContent = isLast ? '🚀 Get Started' : 'Next →';
  }

  function close(onComplete) {
    if (!overlay) return;
    overlay.style.opacity = '0';
    setTimeout(function () {
      if (overlay && overlay.parentNode) overlay.parentNode.removeChild(overlay);
      overlay = null;
      if (onComplete) onComplete();
    }, 350);
  }

})();
