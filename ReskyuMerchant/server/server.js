/**
 * Reskyu Merchant — Render Proxy Server
 *
 * Routes:
 *   POST /upload           → uploads image to Cloudinary, returns { secure_url }
 *   POST /notify/new-drop  → sends FCM push to consumer topic "new_drops"
 *
 * Environment variables (set on Render dashboard OR in .env for local dev):
 *   CLOUDINARY_CLOUD_NAME
 *   CLOUDINARY_API_KEY
 *   CLOUDINARY_API_SECRET
 *   FIREBASE_SERVICE_ACCOUNT  (full JSON string of Firebase Admin SDK service account)
 */

// Load .env in local dev (no-op on Render where vars are set in the dashboard)
require("dotenv").config();

const express = require("express");
const cors    = require("cors");
const multer  = require("multer");
const { v2: cloudinary } = require("cloudinary");
const admin   = require("firebase-admin");
const fs      = require("fs");
const path    = require("path");

const app  = express();
const port = process.env.PORT || 3000;

// ── Middleware ────────────────────────────────────────────────────────────────
app.use(cors());
app.use(express.json());

// ── Cloudinary config ─────────────────────────────────────────────────────────
cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key:    process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// ── Firebase Admin init ───────────────────────────────────────────────────────
try {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT || "{}");
  if (serviceAccount.project_id) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
    console.log("Firebase Admin initialized for project:", serviceAccount.project_id);
  } else {
    console.warn("FIREBASE_SERVICE_ACCOUNT not set — FCM notifications disabled");
  }
} catch (e) {
  console.error("Firebase Admin init failed:", e.message);
}

// ── Multer — temp disk storage for uploads ────────────────────────────────────
const upload = multer({
  dest: "/tmp/uploads/",
  limits: { fileSize: 10 * 1024 * 1024 }, // 10 MB max
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /upload
// Receives: multipart/form-data with field "file"
// Returns:  { secure_url: "https://res.cloudinary.com/..." }
// ─────────────────────────────────────────────────────────────────────────────
app.post("/upload", upload.single("file"), async (req, res) => {
  if (!req.file) {
    return res.status(400).json({ error: "No file received. Send file as multipart field 'file'." });
  }

  try {
    console.log(`Uploading ${req.file.originalname} (${(req.file.size / 1024).toFixed(1)} KB)…`);

    const result = await cloudinary.uploader.upload(req.file.path, {
      folder:          "reskyu/listings",
      resource_type:   "image",
      transformation:  [{ width: 800, height: 600, crop: "limit", quality: "auto" }],
    });

    // Clean up temp file
    fs.unlinkSync(req.file.path);

    console.log("Upload success:", result.secure_url);
    return res.status(200).json({ secure_url: result.secure_url });

  } catch (err) {
    console.error("Cloudinary upload error:", err);
    // Clean up temp file on error
    if (req.file?.path && fs.existsSync(req.file.path)) {
      fs.unlinkSync(req.file.path);
    }
    return res.status(500).json({ error: err.message || "Upload failed" });
  }
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /notify/new-drop
// Receives: { businessName, heroItem, discountedPrice, listingId }
// Returns:  { success: true, messageId }
// ─────────────────────────────────────────────────────────────────────────────
app.post("/notify/new-drop", async (req, res) => {
  const { businessName, heroItem, discountedPrice, listingId } = req.body;

  if (!heroItem || !listingId) {
    return res.status(400).json({ error: "heroItem and listingId are required" });
  }

  if (!admin.apps.length) {
    return res.status(503).json({ error: "Firebase not configured — set FIREBASE_SERVICE_ACCOUNT" });
  }

  try {
    const message = {
      notification: {
        title: `🍱 New Drop! ${businessName || "Nearby Restaurant"}`,
        body:  `${heroItem} for just ₹${discountedPrice || "?"} — grab it before it's gone!`,
      },
      data: {
        listingId: listingId,
        type:      "NEW_DROP",
        click_action: "FLUTTER_NOTIFICATION_CLICK",
      },
      topic: "new_drops", // Consumer app subscribes to this topic
    };

    const messageId = await admin.messaging().send(message);
    console.log(`FCM sent to topic 'new_drops', messageId: ${messageId}`);
    return res.status(200).json({ success: true, messageId });

  } catch (err) {
    console.error("FCM error:", err);
    return res.status(500).json({ error: err.message });
  }
});

// ── Health check ──────────────────────────────────────────────────────────────
app.get("/", (req, res) => {
  res.json({
    status:  "ok",
    service: "Reskyu Merchant API",
    routes: [
      "POST /upload           — Cloudinary image upload",
      "POST /notify/new-drop  — FCM push notification",
    ],
  });
});

// ── Start ─────────────────────────────────────────────────────────────────────
app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});
