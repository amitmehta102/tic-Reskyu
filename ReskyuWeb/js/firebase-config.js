// ============================================================
//  RESKYU — Firebase Configuration (Compat SDK)
//  Works with CDN <script> tags — no bundler/npm needed.
//  NOTE: Do NOT use import/export or <script> tags in this file.
//        This is a plain .js file loaded by a <script src="..."> tag.
// ============================================================

const firebaseConfig = {
  apiKey:            "AIzaSyAMkJofOs2VWHRN_dtPhGAsRbe2vjsaiqQ",
  authDomain:        "reskyu-3247b.firebaseapp.com",
  projectId:         "reskyu-3247b",
  storageBucket:     "reskyu-3247b.firebasestorage.app",
  messagingSenderId: "372967507223",
  appId:             "1:372967507223:web:d5817011485b99a04f6352",
  measurementId:     "G-1H77G7M342"
};

// Initialize Firebase (Compat SDK — loaded via CDN <script> tags in HTML)
firebase.initializeApp(firebaseConfig);

// Expose globally so auth.js, dashboard.js, ngo-dashboard.js, etc. can use them
window.RESKYU_AUTH = firebase.auth();
window.RESKYU_DB   = firebase.firestore();

console.log('[RESKYU] Firebase initialized ✓');