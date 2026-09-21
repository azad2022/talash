package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// High fidelity data structure representing a separate shard of the gemstone
data class GemFacet(
    val id: Int,
    val points: (w: Float, h: Float, midX: Float, midY: Float) -> List<Offset>,
    val color: Color,
    val startX: Float,      // scattered offset X (in pixels)
    val startY: Float,      // scattered offset Y (in pixels)
    val startRot: Float,    // scattered rotation (in degrees)
    val startScale: Float   // scattered initial scale
)

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 1. Ring core animations
    val animRingAlpha = remember { Animatable(0f) }
    val animRingScale = remember { Animatable(0.4f) }
    
    // 2. Gem global container animations
    val animGemAlpha = remember { Animatable(0f) }
    val animGemYOffset = remember { Animatable(-180f) } // subtle settling down
    val animGemRotation = remember { Animatable(-180f) }
    val animGemScale = remember { Animatable(1.6f) }
    
    // 3. Facets Assembly Master Progress (0f = fully scattered, 1f = perfectly locked together)
    val animAssembly = remember { Animatable(0f) }
    
    // 4. Lock flash & shockwave animations
    var showFlash by remember { mutableStateOf(false) }
    val animFlashAlpha = remember { Animatable(0f) }
    val animFlashScale = remember { Animatable(0.2f) }
    val animShockwaveRadius = remember { Animatable(10f) }
    val animShockwaveAlpha = remember { Animatable(0f) }

    // 5. Brand text details entry animations
    val animTextAlpha = remember { Animatable(0f) }
    val animTextScale = remember { Animatable(0.85f) }
    val animStarProgress = remember { Animatable(0f) }

    // Particle sparkles background
    val particles = remember {
        List(25) {
            Sparkle(
                xPct = Random.nextFloat(),
                yPct = Random.nextFloat() * 0.7f + 0.15f,
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                alpha = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    // Facets definition matching diamond-cut ruby
    val gemFacets = remember {
        listOf(
            GemFacet(
                id = 1, // Central Brilliant Table facet (vibrant pure ruby light)
                points = { w, h, midX, midY ->
                    val ptA = Offset(midX - w * 0.22f, midY - h * 0.35f)
                    val ptB = Offset(midX + w * 0.22f, midY - h * 0.35f)
                    val ptG = Offset(midX - w * 0.16f, midY - h * 0.05f)
                    val ptH = Offset(midX + w * 0.16f, midY - h * 0.05f)
                    listOf(ptA, ptB, ptH, ptG)
                },
                color = Color(0xFFFF3B5C),
                startX = -40f,
                startY = -240f,
                startRot = 360f,
                startScale = 0.3f
            ),
            GemFacet(
                id = 2, // Left Upper Crown facet (rich crimson facet)
                points = { w, h, midX, midY ->
                    val ptA = Offset(midX - w * 0.22f, midY - h * 0.35f)
                    val ptC = Offset(midX - w * 0.45f, midY - h * 0.05f)
                    val ptG = Offset(midX - w * 0.16f, midY - h * 0.05f)
                    listOf(ptA, ptG, ptC)
                },
                color = Color(0xFFDC143C),
                startX = -220f,
                startY = -120f,
                startRot = -270f,
                startScale = 0.2f
            ),
            GemFacet(
                id = 3, // Right Upper Crown facet (blazing pure red)
                points = { w, h, midX, midY ->
                    val ptB = Offset(midX + w * 0.22f, midY - h * 0.35f)
                    val ptD = Offset(midX + w * 0.45f, midY - h * 0.05f)
                    val ptH = Offset(midX + w * 0.16f, midY - h * 0.05f)
                    listOf(ptB, ptD, ptH)
                },
                color = Color(0xFFFF4B6E),
                startX = 220f,
                startY = -120f,
                startRot = 450f,
                startScale = 0.2f
            ),
            GemFacet(
                id = 4, // Left Pavilion shard (mystic deep shadow ruby)
                points = { w, h, midX, midY ->
                    val ptC = Offset(midX - w * 0.45f, midY - h * 0.05f)
                    val ptE = Offset(midX, midY + h * 0.45f)
                    val ptG = Offset(midX - w * 0.16f, midY - h * 0.05f)
                    listOf(ptG, ptC, ptE)
                },
                color = Color(0xFF8B001A),
                startX = -185f,
                startY = 180f,
                startRot = -390f,
                startScale = 0.25f
            ),
            GemFacet(
                id = 5, // Right Pavilion shard (deep fire burgundy)
                points = { w, h, midX, midY ->
                    val ptD = Offset(midX + w * 0.45f, midY - h * 0.05f)
                    val ptE = Offset(midX, midY + h * 0.45f)
                    val ptH = Offset(midX + w * 0.16f, midY - h * 0.05f)
                    listOf(ptD, ptH, ptE)
                },
                color = Color(0xFFA60B26),
                startX = 185f,
                startY = 180f,
                startRot = 310f,
                startScale = 0.25f
            ),
            GemFacet(
                id = 6, // Center Star Core pavilion (vibrant radiant refraction core)
                points = { w, h, midX, midY ->
                    val ptE = Offset(midX, midY + h * 0.45f)
                    val ptG = Offset(midX - w * 0.16f, midY - h * 0.05f)
                    val ptH = Offset(midX + w * 0.16f, midY - h * 0.05f)
                    listOf(ptG, ptH, ptE)
                },
                color = Color(0xFFE61B43),
                startX = 0f,
                startY = 260f,
                startRot = -540f,
                startScale = 0.15f
            )
        )
    }

    LaunchedEffect(Unit) {
        // High fidelity golden shimmer synth chime audio load on startup
        launch { playLuxuryShimmer() }

        // Step 1: Elegant Solid Gold Ring fades-in and scales dynamically via buttery low spring
        launch {
            animRingAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1100, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animRingScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        delay(900)

        // Step 2: The Ruby Shards fly in from scattered coordinates towards the center basket!
        launch {
            animGemAlpha.animateTo(1f, tween(600, easing = LinearEasing))
        }
        launch {
            animGemRotation.animateTo(0f, tween(1800, easing = CubicBezierEasing(0.1f, 0.9f, 0.15f, 1f)))
        }
        launch {
            animGemScale.animateTo(1.0f, tween(1800, easing = CubicBezierEasing(0.1f, 0.9f, 0.15f, 1f)))
        }
        launch {
            animGemYOffset.animateTo(0f, tween(1800, easing = CubicBezierEasing(0.12f, 0.85f, 0.15f, 1f)))
        }

        // Master piece-assembly progress
        animAssembly.animateTo(
            targetValue = 1f,
            animationSpec = tween(1900, easing = CubicBezierEasing(0.1f, 0.9f, 0.12f, 1.0f))
        )

        // Play pristine snap locking synth track
        launch { playLaserLockSnap() }

        // Step 3: Beautiful Locking Snap Flash Shockwave!
        showFlash = true
        launch {
            animFlashScale.animateTo(3.8f, tween(450, easing = LinearOutSlowInEasing))
        }
        launch {
            animFlashAlpha.animateTo(1f, tween(80, easing = LinearEasing))
            animFlashAlpha.animateTo(0f, tween(380, easing = LinearEasing))
        }
        launch {
            animShockwaveRadius.animateTo(220f, tween(700, easing = LinearOutSlowInEasing))
        }
        launch {
            animShockwaveAlpha.animateTo(0.85f, tween(40, easing = LinearEasing))
            animShockwaveAlpha.animateTo(0f, tween(650, easing = LinearEasing))
        }

        // Step 4: Luxe Brand Title reveals
        launch {
            animTextAlpha.animateTo(1f, tween(1100, easing = LinearOutSlowInEasing))
        }
        launch {
            animTextScale.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }
        launch {
            animStarProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        // Keep presenting the fully assembled ring for a while longer, then complete splash screen
        delay(3600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .clickable { onFinished() } // Allow skip
    ) {
        // Sparkle backdrop layers
        particles.forEach { sparkle ->
            val progressY = (sparkle.yPct - (animStarProgress.value * sparkle.speed))
            val finalY = if (progressY < 0f) progressY + 1f else progressY
            val sinVal = kotlin.math.sin(finalY.toDouble() * 3.1415926535)
            val alphaMultiplier = sinVal.toFloat()
            
            Box(
                modifier = Modifier
                    .offset(
                        x = (sparkle.xPct * 320f).dp,
                        y = (finalY * 700f).dp
                    )
                    .alpha((sparkle.alpha * alphaMultiplier).coerceIn(0f, 1f))
                    .size(sparkle.size.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(LightMetallicGold, Color.Transparent),
                            radius = sparkle.size
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Requirement: ALWAYS Center the Ring & gem absolutely on any device screen layout with its sub-brand info
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .scale(animRingScale.value)
                    .alpha(animRingAlpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Draw luxury yellow gold band with reflections
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val ringCenter = Offset(size.width / 2f, size.height * 0.5f)
                    val radius = 64.dp.toPx()
                    
                    val metallicGradient = Brush.sweepGradient(
                        colors = listOf(
                            MetallicGold,
                            DarkMetallicGold,
                            LightMetallicGold,
                            DarkMetallicGold,
                            MetallicGold
                        ),
                        center = ringCenter
                    )

                    drawCircle(
                        brush = metallicGradient,
                        radius = radius,
                        center = ringCenter,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Inner band gloss & shadow loops config
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = radius - 6.dp.toPx(),
                        center = ringCenter,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = LightMetallicGold.copy(alpha = 0.45f),
                        radius = radius + 6.dp.toPx(),
                        center = ringCenter,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw Prong / Crown setting cups
                    val cupCenterX = size.width / 2f
                    val cupCenterY = ringCenter.y - radius - 2.dp.toPx()
                    val cupWidth = 24.dp.toPx()
                    val cupHeight = 16.dp.toPx()
                    
                    val bezelPath = Path().apply {
                        moveTo(cupCenterX - cupWidth / 2f, cupCenterY - cupHeight)
                        quadraticBezierTo(
                            cupCenterX, cupCenterY,
                            cupCenterX + cupWidth / 2f, cupCenterY - cupHeight
                        )
                        lineTo(cupCenterX + cupWidth / 2.5f, cupCenterY + 4.dp.toPx())
                        lineTo(cupCenterX - cupWidth / 2.5f, cupCenterY + 4.dp.toPx())
                        close()
                    }
                    drawPath(bezelPath, brush = metallicGradient)

                    // Four secure golden claw prongs
                    drawCircle(
                        color = LightMetallicGold,
                        radius = 3.dp.toPx(),
                        center = Offset(cupCenterX - cupWidth / 2f, cupCenterY - cupHeight)
                    )
                    drawCircle(
                        color = LightMetallicGold,
                        radius = 3.dp.toPx(),
                        center = Offset(cupCenterX + cupWidth / 2f, cupCenterY - cupHeight)
                    )
                }

                // Interactive assembly gemstone Box
                val gemOffsetDp = animGemYOffset.value
                val assemblyProgress = animAssembly.value
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 16.dp + gemOffsetDp.dp)
                        .scale(animGemScale.value)
                        .rotate(animGemRotation.value)
                        .alpha(animGemAlpha.value)
                        .size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height * 0.9f
                        val midX = w / 2f
                        val midY = h / 2f

                        // Draw each facet along its dynamic assembly trajectory path
                        gemFacets.forEach { facet ->
                            val dx = facet.startX * (1f - assemblyProgress)
                            val dy = facet.startY * (1f - assemblyProgress)
                            val rot = facet.startRot * (1f - assemblyProgress)
                            val sc = facet.startX * 0f + facet.startScale + (1.0f - facet.startScale) * assemblyProgress
                            
                            val facetPoints = facet.points(w, h, midX, midY)
                            
                            withTransform({
                                translate(left = dx, top = dy)
                                rotate(degrees = rot, pivot = Offset(midX, midY))
                                scale(scaleX = sc, scaleY = sc, pivot = Offset(midX, midY))
                            }) {
                                val path = Path().apply {
                                    if (facetPoints.isNotEmpty()) {
                                        moveTo(facetPoints[0].x, facetPoints[0].y)
                                        for (i in 1 until facetPoints.size) {
                                            lineTo(facetPoints[i].x, facetPoints[i].y)
                                        }
                                        close()
                                    }
                                }
                                drawPath(path, color = facet.color)
                            }
                        }

                        // Specular specular gold highlights
                        if (assemblyProgress > 0.85f) {
                            val highlightAlpha = ((assemblyProgress - 0.85f) / 0.15f) * 0.65f
                            val highlightPaint = Paint().apply {
                                color = Color.White.copy(alpha = highlightAlpha)
                                style = PaintingStyle.Stroke
                                strokeWidth = 1.dp.toPx()
                            }
                            
                            val ptA = Offset(midX - w * 0.22f, midY - h * 0.35f)
                            val ptB = Offset(midX + w * 0.22f, midY - h * 0.35f)
                            val ptC = Offset(midX - w * 0.45f, midY - h * 0.05f)
                            val ptD = Offset(midX + w * 0.45f, midY - h * 0.05f)
                            val ptE = Offset(midX, midY + h * 0.45f)
                            val ptG = Offset(midX - w * 0.16f, midY - h * 0.05f)
                            val ptH = Offset(midX + w * 0.16f, midY - h * 0.05f)

                            drawContext.canvas.drawPath(
                                Path().apply {
                                    moveTo(ptA.x, ptA.y)
                                    lineTo(ptB.x, ptB.y)
                                    lineTo(ptD.x, ptD.y)
                                    lineTo(ptE.x, ptE.y)
                                    lineTo(ptC.x, ptC.y)
                                    close()
                                    moveTo(ptG.x, ptG.y)
                                    lineTo(ptA.x, ptA.y)
                                    moveTo(ptH.x, ptH.y)
                                    lineTo(ptB.x, ptB.y)
                                    moveTo(ptG.x, ptG.y)
                                    lineTo(ptE.x, ptE.y)
                                    moveTo(ptH.x, ptH.y)
                                    lineTo(ptE.x, ptE.y)
                                },
                                highlightPaint
                            )
                        }
                    }
                }

                // Snap Shockwave Ring overlay triggers dynamically when assembled!
                if (showFlash) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ringCenter = Offset(size.width / 2f, size.height * 0.5f)
                        val radius = 64.dp.toPx()
                        val cupCenterY = ringCenter.y - radius - 6.dp.toPx()

                        // Golden-orange shockwave expanding out
                        if (animShockwaveAlpha.value > 0f) {
                            drawCircle(
                                color = MetallicGold.copy(alpha = animShockwaveAlpha.value),
                                radius = animShockwaveRadius.value.dp.toPx(),
                                center = Offset(size.width / 2f, cupCenterY),
                                style = Stroke(width = 4.dp.toPx() * (1f - (animShockwaveRadius.value / 220f)))
                            )
                            
                            drawCircle(
                                color = Color(0xFFFF4D6D).copy(alpha = animShockwaveAlpha.value * 0.6f),
                                radius = (animShockwaveRadius.value * 0.72f).dp.toPx(),
                                center = Offset(size.width / 2f, cupCenterY),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // High brilliancy center glow
                        if (animFlashAlpha.value > 0f) {
                            val flashBrush = Brush.radialGradient(
                                colors = listOf(Color.White, Color(0xFFFF3B5C).copy(0.6f), Color.Transparent),
                                center = Offset(size.width / 2f, cupCenterY),
                                radius = 42.dp.toPx() * animFlashScale.value
                            )
                            drawCircle(
                                brush = flashBrush,
                                radius = 42.dp.toPx() * animFlashScale.value,
                                center = Offset(size.width / 2f, cupCenterY)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sub Brand layout - elegantly aligned directly beneath the ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(animTextScale.value)
                    .alpha(animTextAlpha.value)
            ) {
                Text(
                    text = "سامانه مدیریت و حسابداری طلافروشان",
                    fontSize = 20.sp,
                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MetallicGold,
                    textAlign = TextAlign.Center
                )

                // Subtitle indicator bar
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(SmokyBronze, CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(MetallicGold, CircleShape)
                        )
                        Text(
                            text = "تلاش کن طلاش میکنی",
                            fontSize = 13.sp,
                            fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = if (ThemeConfig.isLightMode) DarkMetallicGold else LightMetallicGold
                        )
                    }
                }
            }
        }
    }
}

// Physical digital synthesis modeling of luxurious clinking gold coins bouncing on a solid floor (3000Hz - 7000Hz harmonics)
private fun playLuxuryShimmer() {
    try {
        val sampleRate = 44100
        val durationMs = 1700
        val numSamples = durationMs * sampleRate / 1000
        val generatedSnd = ByteArray(2 * numSamples)

        // Simulated gravitational bounce delay points for dropping coins
        val bounceTimes = doubleArrayOf(0.0, 0.14, 0.26, 0.36, 0.44, 0.50, 0.55, 0.59, 0.62, 0.64, 0.66, 0.675, 0.685, 0.692, 0.697, 0.70)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            
            // Find active/current bounce impact index
            var activeBounceIdx = -1
            for (k in bounceTimes.indices) {
                if (t >= bounceTimes[k]) {
                    activeBounceIdx = k
                } else {
                    break
                }
            }
            
            var signal = 0.0
            if (activeBounceIdx != -1) {
                val bounceTime = bounceTimes[activeBounceIdx]
                val dt = t - bounceTime
                
                // Rapid exponential decay on impact strike
                val decay = kotlin.math.exp(-38.0 * dt)
                
                // Gradual loss of kinetic energy on subsequent bounces
                val intensity = java.lang.Math.pow(0.76, activeBounceIdx.toDouble())
                
                // Highly metal-resonant modes of jewelry/coin clinking
                val f1 = 3250.0  
                val f2 = 4150.0  
                val f3 = 5900.0  
                val f4 = 7100.0  
                
                val wave = (0.42 * kotlin.math.sin(2.0 * java.lang.Math.PI * f1 * dt) +
                            0.33 * kotlin.math.sin(2.0 * java.lang.Math.PI * f2 * dt) +
                            0.15 * kotlin.math.sin(2.0 * java.lang.Math.PI * f3 * dt) +
                            0.10 * kotlin.math.sin(2.0 * java.lang.Math.PI * f4 * dt))
                
                signal = wave * decay * intensity
            }
            
            // Lingering crystalline shimmer tail ring
            val tailDecay = kotlin.math.exp(-5.0 * t)
            val tailResonance = 0.08 * kotlin.math.sin(2.0 * java.lang.Math.PI * 3250.0 * t) * tailDecay
            
            val totalSignal = (signal + tailResonance).coerceIn(-1.0, 1.0) * 0.48
            
            val val16 = (totalSignal * 32767).toInt()
            generatedSnd[2 * i] = (val16 and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = (val16 and 0xff00).shr(8).toByte()
        }

        val audioTrack = android.media.AudioTrack(
            android.media.AudioManager.STREAM_MUSIC,
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            generatedSnd.size,
            android.media.AudioTrack.MODE_STATIC
        )
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
        Thread {
            try {
                Thread.sleep(2000)
                audioTrack.release()
            } catch (e: Exception) {}
        }.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Programmatic synthesis for custom high frequency precision gem lock click (F#6)
private fun playLaserLockSnap() {
    try {
        val sampleRate = 44100
        val durationMs = 350
        val numSamples = durationMs * sampleRate / 1000
        val generatedSnd = ByteArray(2 * numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val f = 1480.0
            val envelope = kotlin.math.exp(-22.0 * t)
            val click = kotlin.math.sin(2.0 * java.lang.Math.PI * 4000.0 * t) * kotlin.math.exp(-120.0 * t)
            val signal = (0.75 * kotlin.math.sin(2.0 * java.lang.Math.PI * f * t) * envelope) + (0.25 * click)
            
            val val16 = (signal * 32767).toInt().coerceIn(-32768, 32767)
            generatedSnd[2 * i] = (val16 and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = (val16 and 0xff00).shr(8).toByte()
        }

        val audioTrack = android.media.AudioTrack(
            android.media.AudioManager.STREAM_MUSIC,
            sampleRate,
            android.media.AudioFormat.CHANNEL_OUT_MONO,
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            generatedSnd.size,
            android.media.AudioTrack.MODE_STATIC
        )
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()
        Thread {
            try {
                Thread.sleep(500)
                audioTrack.release()
            } catch (e: Exception) {}
        }.start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

data class Sparkle(
    val xPct: Float,
    val yPct: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
