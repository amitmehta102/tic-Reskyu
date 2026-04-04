/* ============================================================
   RESKYU — Toast Notification System
   Usage: Toast.show('Message', 'success' | 'error' | 'warning' | 'info')
   ============================================================ */
window.Toast = (function () {

  // ── Inject styles once ───────────────────────────────────
  var styleId = 'reskyu-toast-css';
  if (!document.getElementById(styleId)) {
    var s = document.createElement('style');
    s.id = styleId;
    s.textContent = `
      #toast-container {
        position: fixed; top: 24px; right: 24px; z-index: 99999;
        display: flex; flex-direction: column; gap: 10px;
        pointer-events: none;
      }
      .toast {
        display: flex; align-items: flex-start; gap: 12px;
        min-width: 280px; max-width: 360px;
        padding: 14px 18px;
        border-radius: 14px;
        backdrop-filter: blur(16px);
        border: 1px solid rgba(254,250,242,.12);
        box-shadow: 0 8px 32px rgba(0,0,0,.45);
        font-family: 'DM Sans', 'Space Grotesk', sans-serif;
        font-size: .88rem; line-height: 1.5;
        color: #FEFAF2;
        pointer-events: all;
        transform: translateX(120%);
        opacity: 0;
        transition: transform .32s cubic-bezier(.34,1.56,.64,1), opacity .25s ease;
        cursor: pointer;
      }
      .toast.show  { transform: translateX(0); opacity: 1; }
      .toast.hide  { transform: translateX(120%); opacity: 0; }

      .toast-success { background: rgba(18,30,20,.92); border-color: rgba(123,224,138,.35); }
      .toast-error   { background: rgba(30,10,10,.92); border-color: rgba(255,100,100,.35); }
      .toast-warning { background: rgba(30,22,8,.92);  border-color: rgba(245,166,35,.35);  }
      .toast-info    { background: rgba(10,18,30,.92); border-color: rgba(100,160,255,.35); }

      .toast-icon  { font-size: 1.2rem; flex-shrink: 0; margin-top: 1px; }
      .toast-body  { flex: 1; }
      .toast-title { font-weight: 800; font-size: .82rem; letter-spacing: .04em;
                     margin-bottom: 2px; text-transform: uppercase; }
      .toast-success .toast-title { color: #7BE08A; }
      .toast-error   .toast-title { color: #FF8080; }
      .toast-warning .toast-title { color: #F5A623; }
      .toast-info    .toast-title { color: #70B8FF; }
      .toast-msg   { color: rgba(254,250,242,.8); }
      .toast-close { font-size: 1rem; opacity: .4; flex-shrink: 0;
                     align-self: center; line-height: 1; }
      .toast-progress {
        position: absolute; bottom: 0; left: 0; height: 3px;
        border-radius: 0 0 14px 14px;
        transition: width linear;
      }
      .toast { position: relative; overflow: hidden; }
      .toast-success .toast-progress { background: #7BE08A; }
      .toast-error   .toast-progress { background: #FF8080; }
      .toast-warning .toast-progress { background: #F5A623; }
      .toast-info    .toast-progress { background: #70B8FF; }
      @media (max-width: 480px) {
        #toast-container { top: auto; bottom: 80px; right: 12px; left: 12px; }
        .toast { max-width: 100%; }
      }
    `;
    document.head.appendChild(s);
  }

  // ── Create container ─────────────────────────────────────
  function getContainer() {
    var c = document.getElementById('toast-container');
    if (!c) {
      c = document.createElement('div');
      c.id = 'toast-container';
      document.body.appendChild(c);
    }
    return c;
  }

  var ICONS = { success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️' };
  var LABELS = { success: 'Success', error: 'Error', warning: 'Warning', info: 'Info' };

  // ── Public show() ─────────────────────────────────────────
  function show(message, type, duration) {
    type     = type || 'info';
    duration = duration || 4000;

    var el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.innerHTML =
      '<span class="toast-icon">' + (ICONS[type] || 'ℹ️') + '</span>' +
      '<div class="toast-body">' +
        '<div class="toast-title">' + (LABELS[type] || type) + '</div>' +
        '<div class="toast-msg">' + message + '</div>' +
      '</div>' +
      '<span class="toast-close">✕</span>' +
      '<div class="toast-progress"></div>';

    getContainer().appendChild(el);

    // Animate in
    requestAnimationFrame(function () {
      requestAnimationFrame(function () { el.classList.add('show'); });
    });

    // Progress bar
    var bar = el.querySelector('.toast-progress');
    bar.style.width = '100%';
    bar.style.transitionDuration = duration + 'ms';
    requestAnimationFrame(function () {
      requestAnimationFrame(function () { bar.style.width = '0%'; });
    });

    // Auto-dismiss
    var timer = setTimeout(function () { dismiss(el); }, duration);

    // Click to dismiss
    el.addEventListener('click', function () {
      clearTimeout(timer); dismiss(el);
    });
  }

  function dismiss(el) {
    el.classList.remove('show');
    el.classList.add('hide');
    setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 350);
  }

  // ── Convenience wrappers ──────────────────────────────────
  return {
    show:    show,
    success: function (m, d) { show(m, 'success', d); },
    error:   function (m, d) { show(m, 'error',   d); },
    warning: function (m, d) { show(m, 'warning', d); },
    info:    function (m, d) { show(m, 'info',    d); }
  };

})();
