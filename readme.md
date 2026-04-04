<div align="center">

# TEAM RESKYU
### *Rescue Food. Feed India. Zero Waste.*

**Technocrats Innovation Challenge 2026 — Social Impact Track**

[![Android](https://img.shields.io/badge/Android-Kotlin%20%7C%20Jetpack%20Compose-3DDC84?style=for-the-badge&logo=android)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20FCM%20%7C%20Auth-FFCA28?style=for-the-badge&logo=firebase)](https://firebase.google.com)
[![Gemini](https://img.shields.io/badge/Google%20Gemini-2.0%20Flash-4285F4?style=for-the-badge&logo=google)](https://deepmind.google/technologies/gemini/)
[![Razorpay](https://img.shields.io/badge/Razorpay-UPI%20Payments-072654?style=for-the-badge)](https://razorpay.com)
[![Web](https://img.shields.io/badge/Web-HTML%20%7C%20JS%20%7C%20Firebase-F7DF1E?style=for-the-badge&logo=javascript)](https://reskyu-app.netlify.app)

> **35–45% of restaurant food is wasted daily in India.**  
> **₹85,000 Crore+ lost to food waste every year.**  
> **1 in 5 Indians faces food insecurity.**  
> We built RESKYU to close that gap — in real time, with zero logistics.

🌐 **Live Demo:** [reskyu.netlify.app/pitch](https://reskyu.netlify.app/pitch)

</div>

---

## 📌 The Problem

India is caught in a cruel paradox: restaurants discard tonnes of quality food every single day *while* millions of students, daily-wage workers, and vulnerable communities go to bed hungry. Existing solutions — food banks, manual NGO drives, delivery-based apps — all require complex logistics, advance planning, or significant cost, making them too slow and too siloed to handle perishable end-of-day surplus at scale.

---

## 💡 Our Solution

**RESKYU** is India's first AI-powered food surplus marketplace that connects restaurants with nearby hungry people — in under 3 minutes, with *zero delivery, zero logistics, and zero waste.*

The flow is elegantly simple:

```
Restaurant lists surplus  →  Nearby consumers & NGOs get notified  →  Pay in-app  →  Walk in & pick up
```

No riders. No cold chain. No middlemen. Just rescued food, at walkable distance.

---

## 🏗️ Architecture Overview

RESKYU is a **full-stack, multi-platform ecosystem** built entirely for this hackathon:

```
┌─────────────────────────────────────────────────────────────┐
│                        RESKYU ECOSYSTEM                     │
├──────────────────┬──────────────────┬───────────────────────┤
│  ReskyuConsumer  │  ReskyuMerchant  │      ReskyuWeb        │
│  (Android App)   │  (Android App)   │ (Landing + Dashboards)│
├──────────────────┴──────────────────┴───────────────────────┤
│               Firebase (Firestore · Auth · FCM)             │
├─────────────────────────────────────────────────────────────┤
│      Gemini 2.0 Flash · Razorpay · Cloudinary · OSMDroid    │
└─────────────────────────────────────────────────────────────┘
```

---

## 📱 Apps & Features

### 🎓 ReskyuConsumer — *For Students & Consumers*

The consumer app is the discovery and purchasing side of the marketplace.

| Feature | Description |
|---|---|
| 🗺️ **Live Map Feed** | Real-time location-aware listings sorted by proximity using GeoHash queries and OSMDroid maps |
| 🍱 **Listing Types** | Both standard surplus meals and **Mystery Boxes** (curated surprise food bags at 50–80% off) |
| 🔔 **Instant Push Alerts** | Firebase Cloud Messaging delivers hyper-local notifications the moment a nearby restaurant lists |
| 💳 **In-App UPI Payments** | Secure Razorpay checkout — no cash, no queues, no no-shows |
| 🎟️ **QR Pickup Ticket** | ZXing-generated QR code confirms the order; merchant scans to complete pickup |
| 📦 **My Orders** | Full order history with status tracking (Active → Completed → Disputed) |
| 👤 **Profile & Auth** | Firebase Auth (email/password + Google SSO) with editable user profiles |

**Tech Stack:** Kotlin · Jetpack Compose · Material 3 · Firebase (Auth, Firestore, FCM) · Razorpay · Gemini AI · Retrofit · OSMDroid · ZXing · Coil · Coroutines

---

### 🍽️ ReskyuMerchant — *For Restaurants & Food Businesses*

The merchant app is the supply side — designed to make listing surplus food as fast as possible.

| Feature | Description |
|---|---|
| 🤖 **SurplusIQ (Gemini AI)** | Analyses 30 days of sales history, cancellation rates, day-of-week trends, and closing time to predict *exactly how many meals to prep today* — with confidence score, best listing time, and pricing hint |
| 📋 **Surplus Posting** | One-tap listing creation with item name, quantity, price, dietary tag (Veg/Non-Veg/Vegan/Jain), pickup window, and Cloudinary image upload |
| 🎁 **Mystery Box Listings** | Special listing type with price range, box type (Bakery/Sweets/Meals), and item count — drives impulse buys and reduces partial surplus |
| 📷 **QR Scanner** | CameraX + ML Kit on-device barcode scan to verify and mark consumer pickups complete |
| 🌱 **ESG Analytics Dashboard** | Live metrics: meals rescued, CO₂ saved (kg), food waste diverted (kg), weekly revenue, top-selling items, order completion rate, and recovery rate charts (MPAndroidChart) |
| ⏱️ **Auto Expiry Worker** | WorkManager background job (every 15 min) automatically marks expired listings `SOLD_OUT` — even when the app is closed |
| 📍 **GPS Onboarding** | Fused Location Provider captures merchant GPS coordinates and GeoHash during signup for proximity-aware consumer feeds |

**Tech Stack:** Kotlin · Jetpack Compose · Material 3 · Firebase (Auth, Firestore, FCM) · Gemini 2.0 Flash · OkHttp · ML Kit (Barcode Scanning) · CameraX · MPAndroidChart · WorkManager · Coil

---

### 🌐 ReskyuWeb — *Landing Page, Dashboards & NGO Portal*

A production-quality web presence deployed on Netlify with Firebase backend.

| Page | Description |
|---|---|
| **`pitch.html`** | Animated marketing site — mission, how-it-works, audience cards, early-access waitlist |
| **`restaurant-dashboard.html`** | Web dashboard for merchants: live listing management, order history, analytics |
| **`ngo-dashboard.html`** | NGO portal with Browse Surplus → Claim modal flow, impact stats (meals claimed, people fed, CO₂ averted), milestone badges, and downloadable Impact Certificates |
| **`dashboard.html`** | Consumer web portal with listing discovery and order tracking |
| **`contact.html`** | Partnership enquiry form wired to Firestore |

**NGO-specific features:** Verified NGOs can browse and claim unclaimed surplus listings at ₹0. Automatic routing of end-of-window surplus to registered NGOs. Milestone system (🥉 10 meals → 🥇 100 meals → 🏆 Food Hero 500).

**Tech Stack:** Vanilla HTML/CSS/JS · Firebase (Auth, Firestore) · Chart.js · Service Worker (PWA/offline) · Netlify

---

## 🤖 AI Feature Deep-Dive: SurplusIQ

SurplusIQ is the intelligence layer that makes RESKYU more than a marketplace.

**Powered by:** Gemini 2.0 Flash (via Google Generative AI SDK)

**What it does:**
A rich context object is assembled from Firestore data — 7-day and 30-day sales history, revenue trends, top items, veg/non-veg ratios, mystery box percentages, average sell-out time, cancellation rate, closing time, day-of-week, and month — then sent to Gemini with a structured prompt.

**What it returns:**
```
Predicted meals to prepare today: 24
Confidence: 87%
Reasoning: "Friday trends + high weekend demand detected"
Best time to list: "6 – 7 PM"
Pricing hint: "Try ₹79 — sweet spot for Fridays"
Today's tip: "Paneer dishes sell 2× faster on weekends"
```

**Smart unlock system:** SurplusIQ activates only after a restaurant completes `≥10` rescues (to ensure enough data for a meaningful prediction), with a real-time progress bar showing how close they are to unlocking it.

**Daily caching:** Predictions are cached in Firestore (`lastPredictionDate`). The app shows the cached result within the same day and re-queries Gemini on new calendar days — keeping API costs near zero while always showing fresh insights.

---

## 🌿 Impact Model

Every interaction on RESKYU creates measurable social and environmental impact:

| Metric | Per Meal Rescued |
|---|---|
| 🌍 CO₂ avoided | ~2–3 kg (vs. landfill decomposition) |
| 💰 Consumer saving | 50–80% off restaurant retail price |
| 🍱 NGO cost | ₹0 — always free for verified orgs |
| 🚗 Delivery emissions | ~Zero — walkable self-pickup only |
| 📊 Merchant benefit | Turns sunk cost (prepped surplus) into recovered revenue |

---

## 🔐 Security & Trust

- **Upfront UPI payment** — eliminates consumer no-shows entirely
- **Atomic Firestore transactions** — `mealsLeft` counter is decremented in a transaction to prevent overselling
- **QR code verification** — ZXing-generated, ML Kit-scanned, one-time-use per order
- **Firebase Auth** — email/password with Google SSO; merchant and consumer accounts are separate namespaced apps
- **NGO verification gate** — NGO accounts are tagged and verified before gaining zero-cost access to surplus routing
- **Cloudinary image hosting** — merchant food photos served via CDN, never from device storage

---

## 🗂️ Repository Structure

```
tic-Reskyu/
├── ReskyuConsumer/          # Android app — consumer side
│   └── app/src/main/java/com/reskyu/consumer/
│       ├── ui/              # Jetpack Compose screens
│       │   ├── home/        # HomeScreen, ListingCard, HomeViewModel
│       │   ├── detail/      # ListingDetailScreen
│       │   ├── orders/      # MyOrdersScreen, OrderCard
│       │   ├── auth/        # LoginScreen, SignupScreen
│       │   ├── profile/     # ProfileScreen
│       │   ├── notifications/
│       │   └── ...
│       ├── data/            # Models, Repositories, Remote APIs
│       │   ├── model/       # Listing, Claim, User, PaymentState, ...
│       │   └── remote/      # Retrofit, Razorpay, Gemini, Cloudinary
│       └── service/         # ReskuMessagingService (FCM)
│
├── ReskyuMerchant/          # Android app — merchant/restaurant side
│   └── app/src/main/java/com/reskyu/merchant/
│       ├── ui/              # Jetpack Compose screens
│       │   ├── analytics/   # EsgAnalyticsScreen + SurplusIQ
│       │   ├── dashboard/   # MerchantDashboard
│       │   ├── post_listing/ # PostListingScreen
│       │   ├── live_listings/ # Merchant's active listing view
│       │   ├── orders/      # MerchantOrdersScreen + QR scanner
│       │   └── ...
│       ├── data/            # Models, Repositories
│       │   └── model/       # SurplusIqContext, SurplusIqResult, EsgStats, ...
│       └── service/         # ListingExpiryWorker, MerchantMessagingService
│
└── ReskyuWeb/               # Web frontend (Netlify-hosted)
    ├── pitch.html           # Main landing / pitch page
    ├── restaurant-dashboard.html
    ├── ngo-dashboard.html
    ├── dashboard.html
    ├── css/                 # pitch.css, dashboard.css, role-dashboard.css
    └── js/                  # pitch.js, auth.js, firebase-config.js, ...
```

---

## ⚡ Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- A Firebase project (Firestore, Auth, FCM enabled)
- Google Gemini API key
- Razorpay API key (test mode works)
- Cloudinary account (free tier sufficient)

### Consumer App
```bash
cd ReskyuConsumer

# Add to local.properties:
# GEMINI_API_KEY=your_key
# RAZORPAY_KEY_ID=your_key
# CLOUDINARY_CLOUD_NAME=your_name
# NODE_API_BASE_URL=your_backend_url

# Place google-services.json in app/
# Then build and run
```

### Merchant App
```bash
cd ReskyuMerchant

# Add to local.properties:
# GEMINI_API_KEY=your_key

# Place google-services.json in app/
# Then build and run
```

### Web
```bash
cd ReskyuWeb

# Add your Firebase config to js/firebase-config.js
# Open pitch.html in a browser, or deploy to Netlify
```

---

## 🏆 Why RESKYU Wins

| Criterion | Our Approach |
|---|---|
| **Real Problem** | Food waste + food insecurity — two crises, one marketplace |
| **Working Product** | Three functional platforms built and deployed during the hackathon |
| **AI Innovation** | SurplusIQ (Gemini 2.0 Flash) turns sales history into actionable daily intelligence |
| **Zero Logistics** | Auto-routing to NGOs, walkable pickup — no delivery infrastructure needed |
| **Scalability** | GeoHash-based proximity, Firestore real-time sync, WorkManager background expiry — built to scale city by city |
| **Monetisation** | Merchant platform fee (% of rescued revenue) — sustainable model that aligns incentives |
| **Impact** | Every meal rescued = CO₂ saved + a person fed + revenue recovered |

---

## 👥 Team

**Team Name:** Reskyu  
**Hackathon:** Technocrats Innovation Challenge 2026  
**Contact:** [reskyu123@gmail.com](mailto:reskyu123@gmail.com)

---

<div align="center">

*"India wastes enough food to feed millions — yet millions go hungry every day.*  
*RESKYU bridges that gap with zero logistics and zero waste."*

**🍱 Rescue Food. Feed India. Zero Waste.**

</div>
