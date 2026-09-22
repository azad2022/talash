package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    // Single, robust entrance animation
    val animProgress = remember { Animatable(0f) }
    var hasFinished by remember { mutableStateOf(false) }

    fun safeFinish() {
        if (!hasFinished) {
            hasFinished = true
            onFinished()
        }
    }

    LaunchedEffect(Unit) {
        // Smooth entrance over 800ms
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        // Brief pause to display the brand elegantly, total splash duration ~1.3 seconds
        delay(500)
        safeFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                safeFinish()
            }
    ) {
        // Skip button in the top corner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { safeFinish() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "رد کردن ‹",
                    color = MetallicGold,
                    fontSize = 13.sp,
                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Center Content: Golden Ring + Brilliant Gemstone + App Brand
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val scale = 0.75f + (animProgress.value * 0.25f)
            val alpha = animProgress.value

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(scale)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                // Background radial glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerPt = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MetallicGold.copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = centerPt,
                            radius = size.width * 0.48f
                        ),
                        radius = size.width * 0.48f,
                        center = centerPt
                    )

                    // Draw Gold Ring Band
                    val ringRadius = size.width * 0.28f
                    val sweepBrush = Brush.sweepGradient(
                        colors = listOf(
                            LightMetallicGold,
                            MetallicGold,
                            DarkMetallicGold,
                            MetallicGold,
                            LightMetallicGold
                        ),
                        center = centerPt
                    )

                    drawCircle(
                        brush = sweepBrush,
                        radius = ringRadius,
                        center = centerPt,
                        style = Stroke(width = 12.dp.toPx())
                    )

                    // Outer and inner shimmer borders for metallic realism
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = ringRadius - 6.dp.toPx(),
                        center = centerPt,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = LightMetallicGold.copy(alpha = 0.6f),
                        radius = ringRadius + 6.dp.toPx(),
                        center = centerPt,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Crown Prongs setting on top of the ring
                    val crownY = centerPt.y - ringRadius - 2.dp.toPx()
                    val crownW = 28.dp.toPx()
                    val crownH = 16.dp.toPx()

                    val crownPath = Path().apply {
                        moveTo(centerPt.x - crownW / 2f, crownY - crownH)
                        quadraticBezierTo(centerPt.x, crownY, centerPt.x + crownW / 2f, crownY - crownH)
                        lineTo(centerPt.x + crownW / 2.8f, crownY + 4.dp.toPx())
                        lineTo(centerPt.x - crownW / 2.8f, crownY + 4.dp.toPx())
                        close()
                    }
                    drawPath(crownPath, brush = sweepBrush)

                    // Left and Right golden prongs
                    drawCircle(
                        color = LightMetallicGold,
                        radius = 3.5.dp.toPx(),
                        center = Offset(centerPt.x - crownW / 2f, crownY - crownH)
                    )
                    drawCircle(
                        color = LightMetallicGold,
                        radius = 3.5.dp.toPx(),
                        center = Offset(centerPt.x + crownW / 2f, crownY - crownH)
                    )

                    // Brilliant Cut Ruby Gemstone in the crown setting
                    val gemW = 34.dp.toPx()
                    val gemH = 28.dp.toPx()
                    val gemCenterY = crownY - crownH * 0.6f

                    val ptTopLeft = Offset(centerPt.x - gemW * 0.35f, gemCenterY - gemH * 0.5f)
                    val ptTopRight = Offset(centerPt.x + gemW * 0.35f, gemCenterY - gemH * 0.5f)
                    val ptMidLeft = Offset(centerPt.x - gemW * 0.5f, gemCenterY)
                    val ptMidRight = Offset(centerPt.x + gemW * 0.5f, gemCenterY)
                    val ptBottom = Offset(centerPt.x, gemCenterY + gemH * 0.55f)

                    // Ruby crown facet (top)
                    val crownFacetPath = Path().apply {
                        moveTo(ptTopLeft.x, ptTopLeft.y)
                        lineTo(ptTopRight.x, ptTopRight.y)
                        lineTo(ptMidRight.x, ptMidRight.y)
                        lineTo(ptMidLeft.x, ptMidLeft.y)
                        close()
                    }
                    drawPath(crownFacetPath, color = Color(0xFFFF2E56))

                    // Ruby pavilion facet (bottom cone)
                    val pavilionFacetPath = Path().apply {
                        moveTo(ptMidLeft.x, ptMidLeft.y)
                        lineTo(ptMidRight.x, ptMidRight.y)
                        lineTo(ptBottom.x, ptBottom.y)
                        close()
                    }
                    drawPath(pavilionFacetPath, color = Color(0xFFB3092B))

                    // Center table highlight
                    val tableHighlight = Path().apply {
                        moveTo(centerPt.x - gemW * 0.2f, gemCenterY - gemH * 0.3f)
                        lineTo(centerPt.x + gemW * 0.2f, gemCenterY - gemH * 0.3f)
                        lineTo(centerPt.x, gemCenterY + gemH * 0.2f)
                        close()
                    }
                    drawPath(tableHighlight, color = Color(0xFFFF6B88))

                    // Sparkle facet white highlight line
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = ptTopLeft,
                        end = ptTopRight,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .scale(scale)
                    .alpha(alpha)
            ) {
                Text(
                    text = "طَلاش",
                    fontSize = 36.sp,
                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                    fontWeight = FontWeight.Black,
                    color = LightMetallicGold,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "سامانه مدیریت و حسابداری طلافروشان",
                    fontSize = 18.sp,
                    fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MetallicGold,
                    textAlign = TextAlign.Center
                )

                // Subtitle badge
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(SmokyBronze, CircleShape)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                            color = LightMetallicGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Sleek subtle loading indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(alpha)
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = MetallicGold,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
