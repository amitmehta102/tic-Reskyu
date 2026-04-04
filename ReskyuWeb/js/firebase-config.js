
<script type="module">
  // Import the functions you need from the SDKs you need
  import {initializeApp} from "https://www.gstatic.com/firebasejs/12.11.0/firebase-app.js";
  import {getAnalytics} from "https://www.gstatic.com/firebasejs/12.11.0/firebase-analytics.js";
  // TODO: Add SDKs for Firebase products that you want to use
  // https://firebase.google.com/docs/web/setup#available-libraries

  // Your web app's Firebase configuration
  // For Firebase JS SDK v7.20.0 and later, measurementId is optional
  const firebaseConfig = {
    apiKey: "AIzaSyAMkJofOs2VWHRN_dtPhGAsRbe2vjsaiqQ",
  authDomain: "reskyu-3247b.firebaseapp.com",
  projectId: "reskyu-3247b",
  storageBucket: "reskyu-3247b.firebasestorage.app",
  messagingSenderId: "372967507223",
  appId: "1:372967507223:web:d5817011485b99a04f6352",
  measurementId: "G-1H77G7M342"
  };

  // Initialize Firebase
  const app = initializeApp(firebaseConfig);
  const analytics = getAnalytics(app);
</script>