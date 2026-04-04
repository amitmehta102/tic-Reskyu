// ============================================================
//  RESKYU — Firebase Configuration (Compat SDK)
//  This works with the CDN scripts in pitch.html — no bundler needed.
//  Your real project keys are already filled in below.
// ============================================================

const firebaseConfig = {
  apiKey:            "AIzaSyAMkJofOs2VWHRN_dtPhGAsRbe2vjsaiqQ",
  authDomain:        "reskyu-3247b.firebaseapp.com",
  projectId:         "reskyu-3247b",
  storageBucket:     "reskyu-3247b.firebasestorage.app",
  messagingSenderId: "372967507223",
  appId:             "1:372967507223:web:fcb7d7684d96c6d44f6352",
  measurementId:     "G-F2JL1FEWXP"
};

// Initialize Firebase using the Compat SDK (loaded via CDN in pitch.html)
firebase.initializeApp(firebaseConfig);

// Expose globally so auth.js and pitch.js can use them
window.RESKYU_AUTH = firebase.auth();
window.RESKYU_DB   = firebase.firestore();

console.log('[RESKYU] Firebase initialized ✓');
