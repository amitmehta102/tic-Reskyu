package com.reskyu.merchant.ui.orders

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * Full-screen QR scanner screen powered by CameraX + ML Kit Barcode Scanning.
 *
 * Behaviour:
 *  1. Requests CAMERA permission if not already granted (using ActivityResult API).
 *  2. Shows live camera preview with an animated viewfinder overlay.
 *  3. On first successful QR decode → calls [onScanResult] and returns.
 *     Subsequent frames are ignored after first valid scan.
 *
 * Expected QR payload: {"orderId":"<id>","merchantId":"<uid>"}
 * Also supports pipe format: "<orderId>|<merchantId>" and plain orderId.
 */
@Composable
fun QrScannerScreen(
    onScanResult : (rawValue: String) -> Unit,
    onBack       : () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDeniedPermanently = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            hasCameraPermission -> {
                CameraPreview(onScanResult = onScanResult)
                ScannerOverlay()
            }
            permissionDeniedPermanently -> {
                PermissionDenied(onBack = onBack)
            }
            else -> {
                // Waiting for user response
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF52B788))
                }
            }
        }

        // Back button always visible
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector        = Icons.Rounded.ArrowBackIosNew,
                contentDescription = "Back",
                tint               = Color.White
            )
        }
    }
}

// ── Camera preview + ML Kit analyser ──────────────────────────────────────────

@Composable
private fun CameraPreview(onScanResult: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned        by remember { mutableStateOf(false) }

    val previewView = remember { PreviewView(context) }
    val executor    = remember { Executors.newSingleThreadExecutor() }
    val scanner     = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    LaunchedEffect(previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analyser = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(executor) { imageProxy ->
                        if (scanned) { imageProxy.close(); return@setAnalyzer }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                        ?.rawValue
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { rawValue ->
                                            if (!scanned) {
                                                scanned = true
                                                onScanResult(rawValue)
                                            }
                                        }
                                }
                                .addOnFailureListener { Log.w("QrScanner", it) }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analyser
                )
            } catch (e: Exception) {
                Log.e("QrScanner", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory  = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

// ── Animated viewfinder overlay ───────────────────────────────────────────────

@Composable
private fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val scanLineFraction by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line_fraction"
    )

    val GreenAccent = Color(0xFF52B788)
    val windowSize  = 240.dp
    val sideMargin  = 48.dp

    Box(modifier = Modifier.fillMaxSize()) {

        // Dark overlay — four strips around the scan window
        Column(modifier = Modifier.fillMaxSize()) {
            // Top strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            // Middle row: left | window | right
            Row(
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(windowSize)
            ) {
                Box(modifier = Modifier.width(sideMargin).fillMaxHeight().background(Color.Black.copy(alpha = 0.55f)))
                // Scan window — clear, with animated scan line
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = windowSize * scanLineFraction)
                            .background(GreenAccent.copy(alpha = 0.85f))
                    )
                }
                Box(modifier = Modifier.width(sideMargin).fillMaxHeight().background(Color.Black.copy(alpha = 0.55f)))
            }
            // Bottom strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // Corner brackets inside scan window
        Box(
            modifier         = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(windowSize)) {
                val bracketSize  = 28.dp
                val bracketStroke = 3.dp
                val bracketColor  = GreenAccent
                val bracketRadius = 4.dp

                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .size(bracketSize)
                            .align(align)
                            .background(Color.Transparent)
                    ) {
                        val topWidth    = if (align == Alignment.TopStart || align == Alignment.TopEnd) bracketStroke else 0.dp
                        val bottomWidth = if (align == Alignment.BottomStart || align == Alignment.BottomEnd) bracketStroke else 0.dp
                        val startWidth  = if (align == Alignment.TopStart || align == Alignment.BottomStart) bracketStroke else 0.dp
                        val endWidth    = if (align == Alignment.TopEnd || align == Alignment.BottomEnd) bracketStroke else 0.dp

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top    = if (topWidth > 0.dp) 0.dp else bracketStroke,
                                    bottom = if (bottomWidth > 0.dp) 0.dp else bracketStroke,
                                    start  = if (startWidth > 0.dp) 0.dp else bracketStroke,
                                    end    = if (endWidth > 0.dp) 0.dp else bracketStroke
                                )
                                .clip(
                                    RoundedCornerShape(
                                        topStart    = if (align == Alignment.TopStart)    bracketRadius else 0.dp,
                                        topEnd      = if (align == Alignment.TopEnd)      bracketRadius else 0.dp,
                                        bottomStart = if (align == Alignment.BottomStart) bracketRadius else 0.dp,
                                        bottomEnd   = if (align == Alignment.BottomEnd)   bracketRadius else 0.dp
                                    )
                                )
                                .background(Color.Transparent)
                        )
                    }
                }

                // Draw explicit bracket lines using Box borders
                // Top-left
                Box(modifier = Modifier.size(bracketSize).align(Alignment.TopStart)) {
                    Box(modifier = Modifier.fillMaxWidth().height(bracketStroke).background(bracketColor))
                    Box(modifier = Modifier.fillMaxHeight().width(bracketStroke).background(bracketColor))
                }
                // Top-right
                Box(modifier = Modifier.size(bracketSize).align(Alignment.TopEnd)) {
                    Box(modifier = Modifier.fillMaxWidth().height(bracketStroke).background(bracketColor))
                    Box(modifier = Modifier.fillMaxHeight().width(bracketStroke).align(Alignment.CenterEnd).background(bracketColor))
                }
                // Bottom-left
                Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomStart)) {
                    Box(modifier = Modifier.fillMaxWidth().height(bracketStroke).align(Alignment.BottomStart).background(bracketColor))
                    Box(modifier = Modifier.fillMaxHeight().width(bracketStroke).background(bracketColor))
                }
                // Bottom-right
                Box(modifier = Modifier.size(bracketSize).align(Alignment.BottomEnd)) {
                    Box(modifier = Modifier.fillMaxWidth().height(bracketStroke).align(Alignment.BottomStart).background(bracketColor))
                    Box(modifier = Modifier.fillMaxHeight().width(bracketStroke).align(Alignment.CenterEnd).background(bracketColor))
                }
            }
        }

        // Instruction text at bottom
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text      = "Point camera at customer's QR code",
                    fontSize  = 14.sp,
                    color     = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text     = "Order will complete automatically on scan",
                fontSize = 12.sp,
                color    = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Permission denied state ───────────────────────────────────────────────────

@Composable
private fun PermissionDenied(onBack: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🚫", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text       = "Camera Permission Denied",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Please enable camera access in\nSettings → App Permissions\nto use the QR scanner.",
            fontSize  = 14.sp,
            color     = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onBack) {
            Text("Go Back", color = Color.White.copy(alpha = 0.6f))
        }
    }
}
