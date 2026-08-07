package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.MetallicGold
import com.example.ui.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    onDismissRequest: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    val rawContext = LocalContext.current
    val context = remember(rawContext) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            rawContext.createAttributionContext("default")
        } else {
            rawContext
        }
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "برای اسکن بارکد به دسترسی دوربین نیاز است", Toast.LENGTH_LONG).show()
                onDismissRequest()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                BarcodeScannerContent(
                    onBarcodeScanned = { code ->
                        onBarcodeScanned(code)
                    },
                    onClose = onDismissRequest
                )
            }
        }
    }
}

@Composable
fun BarcodeScannerContent(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val rawContext = LocalContext.current
    val context = remember(rawContext) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            rawContext.createAttributionContext("default")
        } else {
            rawContext
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val playBeep = {
        try {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    val cameraProviderState = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProviderState.value = cameraProvider
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                cameraProviderState.value?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(cameraProviderState.value, isFlashOn) {
        val cameraProvider = cameraProviderState.value ?: return@LaunchedEffect
        
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val analysisExecutor = Executors.newSingleThreadExecutor()
        imageAnalysis.setAnalyzer(
            analysisExecutor,
            BarcodeAnalyzer { barcode ->
                playBeep()
                onBarcodeScanned(barcode)
            }
        )

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            camera?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ScannerOverlay()

        ScanningLine()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "بستن",
                    tint = Color.White
                )
            }

            Text(
                text = "بارکدخوان طلاش",
                color = MetallicGold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            IconButton(
                onClick = {
                    isFlashOn = !isFlashOn
                    camera?.cameraControl?.enableTorch(isFlashOn)
                },
                modifier = Modifier
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                    .size(44.dp)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Filled.FlashOff else Icons.Filled.FlashOn,
                    contentDescription = "فلاش",
                    tint = if (isFlashOn) MetallicGold else Color.White
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkObsidian.copy(0.85f)),
            border = BorderStroke(1.dp, MetallicGold.copy(0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "بارکد خطی یا QR کالا را در کادر قرار دهید",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ردیابی و مطابقت کالا در انبار به صورت خودکار انجام می‌شود",
                    color = Color.White.copy(0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val boxWidth = 280.dp.toPx()
        val boxHeight = 220.dp.toPx()
        val left = (width - boxWidth) / 2
        val top = (height - boxHeight) / 2

        drawRect(
            color = Color.Black.copy(alpha = 0.6f)
        )

        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = androidx.compose.ui.geometry.Size(width, top)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, top + boxHeight),
            size = androidx.compose.ui.geometry.Size(width, height - (top + boxHeight))
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(left, boxHeight)
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(left + boxWidth, top),
            size = androidx.compose.ui.geometry.Size(width - (left + boxWidth), boxHeight)
        )

        val lineLen = 28.dp.toPx()
        val strokeWidth = 3.dp.toPx()
        val goldColor = Color(0xFFF4B323)

        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left, top + lineLen)
                lineTo(left, top)
                lineTo(left + lineLen, top)
            },
            color = goldColor,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + boxWidth - lineLen, top)
                lineTo(left + boxWidth, top)
                lineTo(left + boxWidth, top + lineLen)
            },
            color = goldColor,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left, top + boxHeight - lineLen)
                lineTo(left, top + boxHeight)
                lineTo(left + lineLen, top + boxHeight)
            },
            color = goldColor,
            style = Stroke(width = strokeWidth)
        )

        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + boxWidth - lineLen, top + boxHeight)
                lineTo(left + boxWidth, top + boxHeight)
                lineTo(left + boxWidth, top + boxHeight - lineLen)
            },
            color = goldColor,
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun ScanningLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val currentYPercent by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanner_laser"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxWidthDp = 280.dp
        val boxHeightDp = 220.dp

        val screenWidthDp = maxWidth
        val screenHeightDp = maxHeight

        val leftDp = (screenWidthDp - boxWidthDp) / 2
        val topDp = (screenHeightDp - boxHeightDp) / 2
        val laserLineY = topDp + (boxHeightDp * currentYPercent)

        Box(
            modifier = Modifier
                .offset(x = leftDp, y = laserLineY)
                .width(boxWidthDp)
                .height(3.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFF4B323).copy(0.2f),
                            Color(0xFF00FF87),
                            Color(0xFFF4B323).copy(0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
