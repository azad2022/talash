package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.util.BarcodeGenerator
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// ==========================================
// 1. PIN & LOCK SCREEN (AUTHENTICATOR)
// ==========================================
@Composable
fun PinLockScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val isSetup by viewModel.isPinSetupRequired.collectAsState()
    var pinValue by remember { mutableStateOf("") }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (ThemeConfig.isLightMode) {
                        listOf(DarkObsidian, Color(0xFFF1F5F9))
                    } else {
                        listOf(DarkObsidian, Color(0xFF14120F))
                    }
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "قفل امنیتی",
                    tint = MetallicGold,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(bottom = 12.dp)
                )
                Text(
                    text = if (isSetup) "تعریف رمز عبور اولیه" else "سیستم امنیتی طلاش",
                    style = MaterialTheme.typography.titleLarge,
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = if (isSetup) 
                        "جهت رمزگذاری اطلاعات، یک رمز ۴ رقمی بنویسید" 
                    else "رمز عبور خود را جهت ورود به گالری طلا وارد کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // PIN Indicator Bubbles
            Row(
                modifier = Modifier.padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val isActive = pinValue.length >= i
                    val color = if (viewModel.pinError) Color.Red else if (isActive) MetallicGold else CharcoalBorder
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, if (isActive) MetallicGold else Color.Transparent, CircleShape)
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { char ->
                            val isSpecial = char == "C" || char == "OK"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSpecial) SmokyBronze else SmokyCard)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSpecial) MetallicGold.copy(0.4f) else CharcoalBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        when (char) {
                                            "C" -> if (pinValue.isNotEmpty()) pinValue = pinValue.dropLast(1)
                                            "OK" -> {
                                                if (pinValue.length == 4) {
                                                    if (isSetup) {
                                                        viewModel.registerPin(pinValue)
                                                    } else {
                                                        viewModel.verifyPin(pinValue)
                                                    }
                                                }
                                            }
                                            else -> {
                                                if (pinValue.length < 4) {
                                                    pinValue += char
                                                    // Trigger verify check automatically upon inputting 4th digit
                                                    if (pinValue.length == 4 && !isSetup) {
                                                        viewModel.verifyPin(pinValue)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$char"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (char == "OK") MetallicGold else TextWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. DASHBOARD HOME
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToTab: (String) -> Unit,
    onGoldPriceClick: () -> Unit = {}
) {
    val listInvoices by viewModel.invoices.collectAsState()
    val listProducts by viewModel.products.collectAsState()
    val listRepairs by viewModel.repairs.collectAsState()
    val listInstallments by viewModel.installments.collectAsState()

    val userConfig by viewModel.userConfig.collectAsState()
    val dailyPrice: Double = (userConfig?.dailyGoldPrice ?: BigDecimal.ZERO).toDouble()
    val taxRate: Double = (userConfig?.taxPercent ?: BigDecimal("9.0")).toDouble()

    // Calculating Inventory Value and dynamic stats
    val totalInventoryValue = listProducts.sumOf { product ->
        val itemEst = product.calculateAssetValue(
            dailyPrice18k = dailyPrice,
            rateGold24k = viewModel.rateGold24k,
            rateGoldMelted = viewModel.rateGoldMelted,
            rateGoldOunce = viewModel.rateGoldOunce,
            rateCoin1g = viewModel.rateCoin1g,
            rateCoinQuarter = viewModel.rateCoinQuarter,
            rateCoinHalf = viewModel.rateCoinHalf,
            rateCoinEmami = viewModel.rateCoinEmami,
            rateCoinBahar = viewModel.rateCoinBahar,
            rateCurrencyUsd = viewModel.rateCurrencyUsd,
            rateCurrencyTether = viewModel.rateCurrencyTether,
            rateCurrencyEur = viewModel.rateCurrencyEur,
            rateCurrencyAed = viewModel.rateCurrencyAed,
            rateCurrencyGbp = viewModel.rateCurrencyGbp,
            taxRate = taxRate
        )
        itemEst * product.stock
    }
    val totalGoldWeight = listProducts.sumOf { it.weightGram.toDouble() * it.stock }
    val totalPiecesCount = listProducts.sumOf { it.stock }

    val totalTodaySales = listInvoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.invoice.totalAmount) }.toDouble()
    val estimatedTodayProfit = totalTodaySales * 0.07 // 7% legal standard profit in gold trading
    val readyRepairs = listRepairs.filter { it.repair.status == "READY" }
    val lowStockCount = listProducts.filter { it.stock <= it.minStock }.size
    val overdueInstallments = listInstallments.filter { !it.paid && it.dueDate < System.currentTimeMillis() }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 340.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Store header
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            val shimmerOffset by infiniteTransition.animateFloat(
                initialValue = -300f,
                targetValue = 500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val rotationYAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val cogRotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            val blinkAlphaVal by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(SmokyCard)
                            .border(1.dp, MetallicGold.copy(alpha = 0.3f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    this.rotationY = rotationYAngle
                                    this.cameraDistance = 12f * density
                                }
                        ) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f + size.height * 0.05f
                            val ringRadius = size.minDimension * 0.32f
                            val strokeWidthVal = size.minDimension * 0.1f

                            // Draw the metallic gold ring band (torus)
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF78350F), // Dark gold
                                        Color(0xFFFFD700), // Bright gold
                                        Color(0xFFFFFBEB), // Very pale gold accent
                                        Color(0xFFFFD700),
                                        Color(0xFF78350F)
                                    )
                                ),
                                radius = ringRadius,
                                center = Offset(cx, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthVal)
                            )

                            // Gem/Diamond on top
                            val gemSize = size.minDimension * 0.18f
                            val gemCenterY = cy - ringRadius

                            // 4-pointed diamond cut path
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(cx, gemCenterY - gemSize)
                                lineTo(cx + gemSize * 0.8f, gemCenterY)
                                lineTo(cx, gemCenterY + gemSize * 0.8f)
                                lineTo(cx - gemSize * 0.8f, gemCenterY)
                                close()
                            }

                            // Glowing radial shade inside the gem
                            drawPath(
                                path = path,
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, Color(0xFFE0F2FE), Color(0xFF38BDF8)),
                                    center = Offset(cx, gemCenterY),
                                    radius = gemSize
                                )
                            )

                            // Highlighting facet lines for 3D realism
                            drawLine(
                                color = Color.White,
                                start = Offset(cx, gemCenterY - gemSize),
                                end = Offset(cx, gemCenterY + gemSize * 0.8f),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(cx - gemSize * 0.8f, gemCenterY),
                                end = Offset(cx + gemSize * 0.8f, gemCenterY),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                    val goldenShimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            LightMetallicGold,
                            MetallicGold,
                            Color.White,
                            MetallicGold,
                            LightMetallicGold
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 180f, 100f)
                    )

                    Column(
                        modifier = Modifier
                            .scale(pulseScale)
                            .padding(start = 4.dp)
                    ) {
                        Text(
                            text = "زرمدیر طلاش",
                            fontSize = 24.sp,
                            fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                            style = androidx.compose.ui.text.TextStyle(brush = goldenShimmerBrush),
                            fontWeight = FontWeight.Bold
                        )
                        val isOnlineMode = viewModel.goldPriceMode == "ONLINE"
                        val blinkColor = if (isOnlineMode) Color(0xFF22C55E) else Color(0xFFEF4444)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onGoldPriceClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = if (isOnlineMode) "بروزرسانی آنلاین نرخ" else "تنظیم دستی نرخ",
                                tint = blinkColor.copy(alpha = blinkAlphaVal),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        this.rotationZ = cogRotationAngle
                                    }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (dailyPrice <= 0.0) "طلا ۱۸ عیار: تنظیم نشده" else "طلا ۱۸ عیار: ${viewModel.formatCurrency(dailyPrice)} تومان",
                                fontSize = 16.sp,
                                fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                                color = if (ThemeConfig.isLightMode) DarkMetallicGold else LightMetallicGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (ThemeConfig.isLightMode) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f))
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "خروج",
                            tint = MetallicGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Main Inventory Value Card (Elegant Dark Design Aspect)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (ThemeConfig.isLightMode) {
                                listOf(DarkGreyCard, Color(0xFFE2E8F0))
                            } else {
                                listOf(DarkGreyCard, Color(0xFF111112))
                            }
                        )
                    )
                    .border(1.dp, MetallicGold, RoundedCornerShape(24.dp))
                    .drawBehind {
                        // Ambient top-left gold glow
                        drawCircle(
                            color = Color(0xFFFFD700).copy(alpha = 0.05f),
                            radius = size.width / 2.5f,
                            center = Offset(0f, 0f)
                        )
                    }
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ارزش کل برآوردی موجودی انبار",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(
                            onClick = { viewModel.toggleDashboardValuesVisibility() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (viewModel.isDashboardValuesHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "تغییر وضعیت نمایش مقادیر پیشخوان",
                                tint = MetallicGold.copy(alpha = 0.82f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currencyText = if (viewModel.isDashboardValuesHidden) {
                            androidx.compose.ui.text.AnnotatedString("••••••••")
                        } else {
                            formatColoredSeparators(
                                text = viewModel.formatCurrency(totalInventoryValue),
                                digitColor = MetallicGold,
                                separatorColor = if (ThemeConfig.isLightMode) Color(0xFF64748B) else Color(0xFFE5A93C).copy(alpha = 0.82f) // a distinct color like slate-500 in light and warm bronze-gold in dark mode
                            )
                        }
                        Text(
                            text = currencyText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 30.sp
                        )
                        if (!viewModel.isDashboardValuesHidden) {
                            Text(
                                text = "تومان",
                                fontSize = 11.sp,
                                color = TextGray,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = LightGrayLine)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Total Gold Weight
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وزن کل طلای انبار (۱۸)", color = TextGray, fontSize = 9.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%,.2f", totalGoldWeight)} گرم",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(LightGrayLine)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Total Items
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تعداد کل کالاها", color = TextGray, fontSize = 9.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "$totalPiecesCount قطعه",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Metric Grid (Col 2 Layout for secondary indicators)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Today's Legal 7% Profit (سود قانونی امروز)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SmokyCard)
                        .border(1.dp, MetallicGold, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "سود خالص امروز (۷٪)",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (viewModel.isDashboardValuesHidden) "••••••••" else viewModel.formatCurrency(estimatedTodayProfit),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isDashboardValuesHidden) TextGray else (if (estimatedTodayProfit > 0) com.example.ui.theme.StatusGreen else TextWhite)
                        )
                    }
                }

                // Today's Cash Turnout (فروش نقدی سیستم)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SmokyCard)
                        .border(1.dp, MetallicGold, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = "فروش کل امروز",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.formatCurrency(totalTodaySales),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }
        }

        // Quick Operations Row (Daily Closing & Stocktake)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab("daily_closing") },
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SmokyBronze),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LockClock, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("بستن روز", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("محاسبه و بستن حساب", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTab("stock_take") },
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SmokyBronze),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("انبارگردانی", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("تطبیق بارکد و موجودی", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Action Notifications Center (Alerts Styled with design instructions)
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
            if (lowStockCount > 0 || readyRepairs.isNotEmpty() || overdueInstallments.isNotEmpty()) {
                val alertColor = Color(0xFFFF4500)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(alertColor.copy(alpha = 0.08f))
                        .border(1.dp, alertColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(alertColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "هشدار",
                                    tint = alertColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "هشدارهای فوری گالری",
                                color = alertColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (overdueInstallments.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(alertColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "تعداد ${overdueInstallments.size} مورد قسط معوق سررسید گذشته دارید!",
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (readyRepairs.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MetallicGold)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "تعداد ${readyRepairs.size} سفارش تعمیر آماده تحویل است.",
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (lowStockCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GoldAlertOrange)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "موجودی $lowStockCount کالا در انبار رو به اتمام است!",
                                color = TextWhite,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Sales Category Shares Chart (Bespoke Horizontal Bar Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, MetallicGold)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "تفکیک و سهم وزن موجودی انبار بر اساس کلاس دارایی ها",
                        color = TextGray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Right
                    )

                    // Gather weights per category using ProductCategory enum
                    val categoryWeights = ProductCategory.values().associateWith { pc ->
                        listProducts.filter { 
                            if (pc == ProductCategory.OTHER) {
                                it.category !in ProductCategory.allTitles()
                            } else {
                                it.category == pc.title
                            }
                        }.sumOf { it.weightGram.toDouble() * it.stock }
                    }
                    val totalStockWeight = categoryWeights.values.sum()

                    if (totalStockWeight > 0) {
                        // Filter, sort and display categories with weight > 0
                        val sortedCategories = categoryWeights.filter { it.value > 0.0 }
                            .toList()
                            .sortedByDescending { it.second }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sortedCategories.forEach { (pc, weight) ->
                                val proportion = (weight / totalStockWeight).toFloat()
                                val percentText = String.format(Locale.US, "%.1f%%", proportion * 100f)
                                
                                val widthFractionState by animateFloatAsState(
                                    targetValue = proportion,
                                    animationSpec = tween(durationMillis = 1000, delayMillis = 100),
                                    label = "bar_fill_${pc.name}"
                                )

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Row 1: Category Info & Weight share
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Weight and % tag (Left in RTL is logical end, standard we display on left)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "($percentText)",
                                                color = MetallicGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${String.format(Locale.US, "%,.2f", weight)} گرم",
                                                color = TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Category Name with Icon (Right in RTL/End side)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = pc.title,
                                                color = TextWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(pc.color.copy(alpha = 0.15f))
                                                    .border(1.dp, pc.color.copy(alpha = 0.4f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = pc.icon,
                                                    contentDescription = pc.title,
                                                    tint = pc.color,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 2: Premium Horizontal Bar Indicator with subtle light bar glow
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DarkObsidian.copy(alpha = 0.5f))
                                    ) {
                                        // Active Bar filled with Category Color and slight gradient glow
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = widthFractionState)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            pc.color.copy(alpha = 0.8f),
                                                            pc.color,
                                                            pc.color.copy(alpha = 0.95f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("کالایی در انبار تعریف نشده است.", color = TextGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(this.maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = "Android",
                        tint = Color(0xFF3DDC84), // Android Green
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "نسخه 1.0.2",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.00f,
        label = "press_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MetallicGold.copy(alpha = 0.06f))
            .border(1.dp, MetallicGold.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MetallicGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = title,
            color = TextWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// ==========================================
// 3. GOLD CALCULATOR SCREEN
// ==========================================
@Composable
fun CalculatorScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isWagePercent by remember { mutableStateOf(viewModel.calcWageType == "PERCENT") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "محاسبه‌گر پیشرفته و لوکس طلا",
                style = MaterialTheme.typography.titleMedium,
                color = MetallicGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                "فرمولاسیون محاسبه نرخ با جزئیات ارزش افزوده و سود اصناف مستقل",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }

        // Live Inputs form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Gram weight
                    OutlinedTextField(
                        value = viewModel.calcWeight,
                        onValueChange = { viewModel.calcWeight = it },
                        label = { Text("وزن طلا (گرم)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedLabelColor = MetallicGold,
                            unfocusedLabelColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("weight_input")
                    )

                    // Base gold price today
                    OutlinedTextField(
                        value = viewModel.calcGoldPriceToday,
                        onValueChange = { viewModel.calcGoldPriceToday = it },
                        label = { Text("قیمت گرم طلای عیار ۱۸ مبنا (تومان)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedLabelColor = MetallicGold,
                            unfocusedLabelColor = TextGray,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("base_gold_input")
                    )

                    // Karat Chip chooser
                    Text("عیار سنجی کالا", color = TextWhite, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(18, 21, 22, 24).forEach { kVal ->
                            val selected = viewModel.calcKarat == kVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) MetallicGold else SmokyBronze)
                                    .border(1.dp, if (selected) MetallicGold else CharcoalBorder, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.calcKarat = kVal },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$kVal عیار",
                                    color = if (selected) DarkObsidian else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Wage Value and Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.calcWageValue,
                            onValueChange = { viewModel.calcWageValue = it },
                            label = { Text(if (isWagePercent) "درصد اجرت (%)" else "اجرت ثابت (تومان/گرم)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedLabelColor = MetallicGold,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.5f).testTag("wage_input")
                        )

                        // Wage type button switcher
                        Button(
                            onClick = {
                                isWagePercent = !isWagePercent
                                viewModel.calcWageType = if (isWagePercent) "PERCENT" else "FIXED"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                            border = BorderStroke(1.dp, MetallicGold.copy(0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(56.dp)
                        ) {
                            Text(if (isWagePercent) "درصدی" else "مبلغ ثابت", fontSize = 11.sp, color = MetallicGold, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Discounts and taxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.calcDiscount,
                            onValueChange = { viewModel.calcDiscount = it },
                            label = { Text("تخفیف دستی", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = viewModel.calcTaxRate,
                            onValueChange = { viewModel.calcTaxRate = it },
                            label = { Text("مالیات %", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Output Result glowing Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyBronze.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MetallicGold.copy(0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("صورت ریز محاسبات طلا", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    CalculationRow("ارزش خام طلا:", "${viewModel.formatCurrency(viewModel.calcBaseGoldPrice)} تومان")
                    CalculationRow("اجرت کارمزد ساخت:", "${viewModel.formatCurrency(viewModel.calcWagePrice)} تومان")
                    CalculationRow("سود تک فروشی (۷٪ قانونی):", "${viewModel.formatCurrency(viewModel.calcDealerProfit)} تومان")
                    CalculationRow("مالیات و عوارض دولت:", "${viewModel.formatCurrency(viewModel.calcTaxAndDuty)} تومان")
                    
                    if ((viewModel.calcDiscount.toDoubleOrNull() ?: 0.0) > 0) {
                        CalculationRow("تخفیف کسر شده:", "- ${viewModel.formatCurrency(viewModel.calcDiscount.toDoubleOrNull() ?: 0.0)} تومان", Color.Red)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MetallicGold.copy(0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("جمع کل بهای خریدار:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "${viewModel.formatCurrency(viewModel.calcFinalAmount)} تومان",
                            color = MetallicGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            modifier = Modifier.testTag("calculator_result")
                        )
                    }
                }
            }
        }

        // Fast actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.loadCalculatorPriceToAppConfig()
                        Toast.makeText(context, "قیمت و مالیات مبنای کل سیستم تنظیم شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ست کردن قیمت مبنا", color = MetallicGold, fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        // Reset Calculator form fields to standard
                        viewModel.calcWeight = "1.0"
                        viewModel.calcWageValue = "10"
                        viewModel.calcDiscount = "0"
                        Toast.makeText(context, "فرست بازنشانی انجام شد", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, CharcoalBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("صفر کردن فرم", color = TextWhite, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CalculationRow(label: String, value: String, valueColor: Color = TextWhite) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}


// ==========================================
// 4. INVENTORY MANAGEMENT (STOCKS)
// ==========================================
@Composable
fun WarehouseScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val listProducts by viewModel.products.collectAsState()
    val userConfig by viewModel.userConfig.collectAsState()
    val dailyPrice: Double = (userConfig?.dailyGoldPrice ?: BigDecimal.ZERO).toDouble()
    val taxRate: Double = (userConfig?.taxPercent ?: BigDecimal("9.0")).toDouble()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("همه") }
    
    // Dialog triggers
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var isEditDialogOpen by remember { mutableStateOf(false) }
    var selectedProductToEdit by remember { mutableStateOf<Product?>(null) }
    var selectedProductToBarcodePrint by remember { mutableStateOf<Product?>(null) }
    var isScannerOpen by remember { mutableStateOf(false) }
    var outOfStockProductForDialog by remember { mutableStateOf<Product?>(null) }
    var hardwareScannerEnabled by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(hardwareScannerEnabled) {
        com.example.hardware.barcode.HardwareBarcodeBus.setEnabled(hardwareScannerEnabled)
        onDispose { com.example.hardware.barcode.HardwareBarcodeBus.setEnabled(false) }
    }

    LaunchedEffect(Unit) {
        com.example.hardware.barcode.HardwareBarcodeBus.scans.collect { code ->
            searchQuery = code
        }
    }

    // Add state variables
    var addName by remember { mutableStateOf("") }
    var addCustomBarcode by remember { mutableStateOf("") }
    var isInnerScannerOpenForAdd by remember { mutableStateOf(false) }
    var isInnerScannerOpenForEdit by remember { mutableStateOf(false) }
    var addCategory by remember { mutableStateOf("انگشتر") }
    var addWeight by remember { mutableStateOf("") }
    var addWageValue by remember { mutableStateOf("") }
    var addWageType by remember { mutableStateOf("PERCENT") }
    var addKarat by remember { mutableStateOf(18) }
    var addStock by remember { mutableStateOf("1") }
    var addMinStock by remember { mutableStateOf("1") }
    var addPurchasePrice by remember { mutableStateOf("") }

    var img1 by remember { mutableStateOf<String?>(null) }
    var img2 by remember { mutableStateOf<String?>(null) }
    var img3 by remember { mutableStateOf<String?>(null) }
    var img4 by remember { mutableStateOf<String?>(null) }
    var img5 by remember { mutableStateOf<String?>(null) }

    val pickerLauncher1 = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it)
            if (path != null) img1 = path
        }
    }
    val pickerLauncher2 = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it)
            if (path != null) img2 = path
        }
    }
    val pickerLauncher3 = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it)
            if (path != null) img3 = path
        }
    }
    val pickerLauncher4 = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it)
            if (path != null) img4 = path
        }
    }
    val pickerLauncher5 = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val path = viewModel.saveImageToInternalStorage(context, it)
            if (path != null) img5 = path
        }
    }

    // Filter items
    val filteredProducts = listProducts.filter { prod ->
        val barcode = "G-${prod.id.toString().padStart(6, '0')}"
        val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                            barcode.contains(searchQuery, ignoreCase = true) ||
                            prod.customBarcode.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryFilter == "همه" || prod.category == selectedCategoryFilter
        matchesSearch && matchesCategory
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("انبارداری دیجیتال طلا و جواهرات", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
                        Text("ثبت موجودی گرمی، عیار سنجی و هشدار اتمام کالاها", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                    
                    FloatingActionButton(
                        onClick = { 
                            img1 = null
                            img2 = null
                            img3 = null
                            img4 = null
                            img5 = null
                            isAddDialogOpen = true 
                        },
                        containerColor = MetallicGold,
                        contentColor = DarkObsidian,
                        modifier = Modifier.size(44.dp).testTag("add_product_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "کالا جدید")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "جستجو", tint = TextGray) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextGray, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = { isScannerOpen = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MetallicGold.copy(0.12f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode,
                                    contentDescription = "اسکن بارکد",
                                    tint = MetallicGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    placeholder = { Text("جستجوی نام یا بارکد کالا...", color = TextGray, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MetallicGold,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Horizontal Categories Scroll Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (listOf("همه") + ProductCategory.allTitles()).forEach { cat ->
                        val selected = selectedCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) MetallicGold else SmokyCard)
                                .border(1.dp, if (selected) MetallicGold else CharcoalBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedCategoryFilter = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat, color = if (selected) DarkObsidian else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Products list
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.DynamicFeed, contentDescription = "خالی", tint = TextGray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("کالایی یافت نشد", color = TextGray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, MetallicGold)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(product.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val productBarcode = "G-${product.id.toString().padStart(6, '0')}"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MetallicGold.copy(0.08f))
                                            .border(0.5.dp, MetallicGold.copy(0.2f), RoundedCornerShape(4.dp))
                                            .clickable { selectedProductToBarcodePrint = product }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.QrCode,
                                            contentDescription = "مشاهد بارکد اختصاصی کالا",
                                            tint = MetallicGold,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "بارکد: $productBarcode",
                                            color = MetallicGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text("دسته بندی: ${product.category} | عیار: ${product.karat}", color = TextGray, fontSize = 11.sp)
                                }

                                // Badge showing current stocks
                                val isLowStock = product.stock <= product.minStock
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isLowStock) Color.Red.copy(0.2f) else SmokyBronze)
                                        .border(1.dp, if (isLowStock) Color.Red else MetallicGold, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "موجودی: ${product.stock}",
                                        color = if (isLowStock) Color.Red else MetallicGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val imageUrls = listOfNotNull(product.imagePath, product.imagePath2, product.imagePath3, product.imagePath4, product.imagePath5).filter { it.isNotEmpty() }
                            if (imageUrls.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(imageUrls) { path ->
                                        var isFullScreenOpen by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, CharcoalBorder, RoundedCornerShape(6.dp))
                                                .clickable { isFullScreenOpen = true }
                                        ) {
                                            coil.compose.AsyncImage(
                                                model = java.io.File(path),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        }
                                        if (isFullScreenOpen) {
                                            Dialog(onDismissRequest = { isFullScreenOpen = false }) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(Color.Black)
                                                ) {
                                                    coil.compose.AsyncImage(
                                                        model = java.io.File(path),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                    )
                                                    IconButton(
                                                        onClick = { isFullScreenOpen = false },
                                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Close, contentDescription = "بستن", tint = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "وزن خالص: ${viewModel.formatWeight(product.weightGram)} گرم",
                                        color = TextWhite,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "کارمزد: ${if (product.wageType == "PERCENT") "${product.wagePrice}%" else "${viewModel.formatCurrency(product.wagePrice)}تومان"}",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            selectedProductToEdit = product
                                            addName = product.name
                                            addCustomBarcode = product.customBarcode
                                            addCategory = product.category
                                            addWeight = product.weightGram.toString()
                                            addWageValue = product.wagePrice.toString()
                                            addWageType = product.wageType
                                            addKarat = product.karat
                                            addStock = product.stock.toString()
                                            addMinStock = product.minStock.toString()
                                            addPurchasePrice = if (product.purchasePrice > BigDecimal.ZERO) product.purchasePrice.toLong().toString() else ""
                                            img1 = product.imagePath
                                            img2 = product.imagePath2
                                            img3 = product.imagePath3
                                            img4 = product.imagePath4
                                            img5 = product.imagePath5
                                            isEditDialogOpen = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "ویرایش", tint = MetallicGold, modifier = Modifier.size(18.dp))
                                    }

                                    // Add shortcut button to append Item to Billing Cart directly!
                                    Button(
                                        onClick = {
                                            if (product.stock <= 0) {
                                                outOfStockProductForDialog = product
                                            } else {
                                                viewModel.addItemToDraft(product, 1)
                                                Toast.makeText(context, "${product.name} به سبد فاکتور اضافه شد", Toast.LENGTH_SHORT).show()
                                                onNavigateToTab?.invoke("invoice")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                                        border = BorderStroke(1.dp, MetallicGold.copy(0.3f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.ShoppingCart, contentDescription = "سبد خرید", tint = MetallicGold, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("افزودن به فاکتور", fontSize = 10.sp, color = MetallicGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (product.purchasePrice > BigDecimal.ZERO) {
                                Spacer(Modifier.height(12.dp))

                                val estimatedSalePrice = product.calculateAssetValue(
                                    dailyPrice18k = dailyPrice,
                                    rateGold24k = viewModel.rateGold24k,
                                    rateGoldMelted = viewModel.rateGoldMelted,
                                    rateGoldOunce = viewModel.rateGoldOunce,
                                    rateCoin1g = viewModel.rateCoin1g,
                                    rateCoinQuarter = viewModel.rateCoinQuarter,
                                    rateCoinHalf = viewModel.rateCoinHalf,
                                    rateCoinEmami = viewModel.rateCoinEmami,
                                    rateCoinBahar = viewModel.rateCoinBahar,
                                    rateCurrencyUsd = viewModel.rateCurrencyUsd,
                                    rateCurrencyTether = viewModel.rateCurrencyTether,
                                    rateCurrencyEur = viewModel.rateCurrencyEur,
                                    rateCurrencyAed = viewModel.rateCurrencyAed,
                                    rateCurrencyGbp = viewModel.rateCurrencyGbp,
                                    taxRate = taxRate
                                )
                                val singleProfit = estimatedSalePrice - product.purchasePrice.toDouble()
                                val totalProfit = singleProfit * product.stock
                                val isProfit = singleProfit >= 0.0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "قیمت خرید: ${viewModel.formatCurrency(product.purchasePrice)} تومان",
                                            color = TextGray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            "ارزش فروش روز: ${viewModel.formatCurrency(estimatedSalePrice)} تومان",
                                            color = MetallicGold.copy(0.9f),
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Profit / Loss Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isProfit) Color(0xFF00FF87).copy(0.12f) else Color.Red.copy(0.12f))
                                            .border(1.dp, if (isProfit) Color(0xFF00FF87) else Color.Red, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isProfit) "سود تخمینی: ${viewModel.formatCurrency(singleProfit)} تومان" else "زیان تخمینی: ${viewModel.formatCurrency(-singleProfit)} تومان",
                                                color = if (isProfit) Color(0xFF00FF87) else Color.Red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (product.stock > 1) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = "سود کل (${product.stock} عدد): ${viewModel.formatCurrency(totalProfit)}",
                                                    color = if (isProfit) Color(0xFF00FF87).copy(0.8f) else Color.Red.copy(0.8f),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (outOfStockProductForDialog != null) {
            val prod = outOfStockProductForDialog!!
            Dialog(onDismissRequest = { outOfStockProductForDialog = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.5.dp, Color.Red),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "هشدار موجودی",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "موجودی ناکافی کالا",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "موجودی کالای «${prod.name}» در حال حاضر صفر (۰) است و امکان افزودن آن به فاکتور فروش وجود ندارد.",
                            color = TextWhite,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { outOfStockProductForDialog = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("متوجه شدم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ADD NEW PRODUCT DIALOG MODAL
        if (isAddDialogOpen) {
            Dialog(onDismissRequest = { isAddDialogOpen = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ثبت طلای جدید در ویترین انبار", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        
                        OutlinedTextField(
                            value = addName, 
                            onValueChange = { addName = it }, 
                            label = { Text("نام کالا (طرح)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = addCustomBarcode,
                            onValueChange = { addCustomBarcode = it },
                            label = { Text("بارکد اختصاصی کالا (اختیاری)") },
                            trailingIcon = {
                                IconButton(onClick = { isInnerScannerOpenForAdd = true }) {
                                    Icon(Icons.Filled.QrCode, contentDescription = "اسکن بارکد کالا", tint = MetallicGold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder
                            ),
                            placeholder = { Text("مثلا: 12345678 (یا کلیک روی آیکون اسکن)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Select Category
                        Text("دسته‌بندی جواهر:", fontSize = 11.sp, color = TextGray)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            ProductCategory.allTitles().forEach { cat ->
                                val selected = addCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { addCategory = cat },
                                    label = { Text(cat) },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = addWeight, 
                                onValueChange = { addWeight = it }, 
                                label = { Text("وزن (گرم)") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            OutlinedTextField(
                                value = addStock, 
                                onValueChange = { addStock = it }, 
                                label = { Text("موجودی") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = addWageValue, onValueChange = { addWageValue = it }, label = { Text("کد/مقدار اجرت") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            
                            Button(
                                onClick = { addWageType = if (addWageType == "PERCENT") "FIXED" else "PERCENT" },
                                colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                                modifier = Modifier.weight(0.8f).padding(top = 8.dp)
                            ) {
                                Text(if (addWageType == "PERCENT") "درصد" else "تومان هرگرم", fontSize = 10.sp, color = MetallicGold)
                            }
                        }

                        // Karats choice
                        Text("عیار طلا:", fontSize = 11.sp, color = TextGray)
                        Row {
                            listOf(18, 21, 22, 24).forEach { kVal ->
                                val sel = addKarat == kVal
                                Button(
                                    onClick = { addKarat = kVal },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (sel) MetallicGold else SmokyBronze),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) {
                                    Text(kVal.toString(), fontSize = 11.sp, color = if (sel) DarkObsidian else TextWhite)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = addMinStock, 
                            onValueChange = { addMinStock = it }, 
                            label = { Text("حداقل هشدار موجودی") }, 
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = addPurchasePrice, 
                            onValueChange = { addPurchasePrice = it }, 
                            label = { Text("قیمت خرید (تومان)") }, 
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ProductImageRow(
                            img1 = img1,
                            img2 = img2,
                            img3 = img3,
                            img4 = img4,
                            img5 = img5,
                            onPick1 = { pickerLauncher1.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick2 = { pickerLauncher2.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick3 = { pickerLauncher3.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick4 = { pickerLauncher4.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick5 = { pickerLauncher5.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onClear1 = { img1 = null },
                            onClear2 = { img2 = null },
                            onClear3 = { img3 = null },
                            onClear4 = { img4 = null },
                            onClear5 = { img5 = null }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val finalWeight = addWeight.toDoubleOrNull() ?: 0.0
                                    val finalWage = addWageValue.toDoubleOrNull() ?: 0.0
                                    val finalStock = addStock.toIntOrNull() ?: 1
                                    val minAlert = addMinStock.toIntOrNull() ?: 1
                                    val finalPurchasePrice = addPurchasePrice.toDoubleOrNull() ?: 0.0
                                    if (addName.isNotEmpty() && finalWeight > 0) {
                                        viewModel.addProduct(
                                            addName, 
                                            addCategory, 
                                            finalWeight, 
                                            addKarat, 
                                            finalWage, 
                                            addWageType, 
                                            finalStock, 
                                            minAlert, 
                                            finalPurchasePrice,
                                            img1,
                                            img2,
                                            img3,
                                            img4,
                                            img5,
                                            customBarcode = addCustomBarcode
                                        )
                                        isAddDialogOpen = false
                                        // Reset fields
                                        addName = ""
                                        addCustomBarcode = ""
                                        addWeight = ""
                                        addWageValue = ""
                                        addStock = "1"
                                        addPurchasePrice = ""
                                        img1 = null
                                        img2 = null
                                        img3 = null
                                        img4 = null
                                        img5 = null
                                    } else {
                                        Toast.makeText(context, "نام کالا و وزن را وارد کنید", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("تایید جدید", color = DarkObsidian, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { isAddDialogOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze), modifier = Modifier.weight(1f)) {
                                Text("انصراف", color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        // EDIT PRODUCT DIALOG MODAL
        if (isEditDialogOpen && selectedProductToEdit != null) {
            Dialog(onDismissRequest = { isEditDialogOpen = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ویرایش مشخصات کالا", color = MetallicGold, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = addName, 
                            onValueChange = { addName = it }, 
                            label = { Text("نام کالا") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = addCustomBarcode,
                            onValueChange = { addCustomBarcode = it },
                            label = { Text("بارکد اختصاصی کالا (اختیاری)") },
                            trailingIcon = {
                                IconButton(onClick = { isInnerScannerOpenForEdit = true }) {
                                    Icon(Icons.Filled.QrCode, contentDescription = "اسکن بارکد کالا", tint = MetallicGold)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder
                            ),
                            placeholder = { Text("مثلا: 12345678 (یا کلیک روی آیکون اسکن)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = addWeight, 
                                onValueChange = { addWeight = it }, 
                                label = { Text("وزن (گرم)") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )

                            OutlinedTextField(
                                value = addStock, 
                                onValueChange = { addStock = it }, 
                                label = { Text("موجودی") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = addWageValue, 
                                onValueChange = { addWageValue = it }, 
                                label = { Text("اجرت") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = addMinStock, 
                                onValueChange = { addMinStock = it }, 
                                label = { Text("کف آلارم") }, 
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                modifier = Modifier.weight(1f), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        OutlinedTextField(
                            value = addPurchasePrice, 
                            onValueChange = { addPurchasePrice = it }, 
                            label = { Text("قیمت خرید (تومان)") }, 
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        ProductImageRow(
                            img1 = img1,
                            img2 = img2,
                            img3 = img3,
                            img4 = img4,
                            img5 = img5,
                            onPick1 = { pickerLauncher1.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick2 = { pickerLauncher2.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick3 = { pickerLauncher3.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick4 = { pickerLauncher4.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onPick5 = { pickerLauncher5.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            onClear1 = { img1 = null },
                            onClear2 = { img2 = null },
                            onClear3 = { img3 = null },
                            onClear4 = { img4 = null },
                            onClear5 = { img5 = null }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val finalWeight = addWeight.toDoubleOrNull() ?: 0.0
                                    val finalWage = addWageValue.toDoubleOrNull() ?: 0.0
                                    val finalStock = addStock.toIntOrNull() ?: 1
                                    val minAlert = addMinStock.toIntOrNull() ?: 1
                                    val finalPurchasePrice = addPurchasePrice.toDoubleOrNull() ?: 0.0
                                    val prodId = selectedProductToEdit?.id ?: 0
                                    if (addName.isNotEmpty() && prodId > 0) {
                                        viewModel.editProduct(
                                            prodId, 
                                            addName, 
                                            selectedProductToEdit?.category ?: addCategory, 
                                            finalWeight, 
                                            addKarat, 
                                            finalWage, 
                                            addWageType, 
                                            finalStock, 
                                            minAlert, 
                                            finalPurchasePrice,
                                            img1,
                                            img2,
                                            img3,
                                            img4,
                                            img5,
                                            customBarcode = addCustomBarcode
                                        )
                                        isEditDialogOpen = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ثبت ویرایش", color = DarkObsidian, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val prod = selectedProductToEdit
                                    if (prod != null) {
                                        viewModel.deleteProduct(prod)
                                        isEditDialogOpen = false
                                        Toast.makeText(context, "کالا با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("حذف کالا", color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        // --- BARCODE DISPATCHER DLGS ---
        if (isScannerOpen) {
            com.example.ui.components.BarcodeScannerDialog(
                onDismissRequest = { isScannerOpen = false },
                onBarcodeScanned = { code ->
                    searchQuery = code
                    isScannerOpen = false
                }
            )
        }

        if (isInnerScannerOpenForAdd) {
            com.example.ui.components.BarcodeScannerDialog(
                onDismissRequest = { isInnerScannerOpenForAdd = false },
                onBarcodeScanned = { code ->
                    addCustomBarcode = code
                    isInnerScannerOpenForAdd = false
                }
            )
        }

        if (isInnerScannerOpenForEdit) {
            com.example.ui.components.BarcodeScannerDialog(
                onDismissRequest = { isInnerScannerOpenForEdit = false },
                onBarcodeScanned = { code ->
                    addCustomBarcode = code
                    isInnerScannerOpenForEdit = false
                }
            )
        }

        if (selectedProductToBarcodePrint != null) {
            val product = selectedProductToBarcodePrint!!
            val barcodeStr = "G-${product.id.toString().padStart(6, '0')}"
            Dialog(onDismissRequest = { selectedProductToBarcodePrint = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صدور بارکد اختصاصی کالا",
                                color = MetallicGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            IconButton(onClick = { selectedProductToBarcodePrint = null }) {
                                Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextWhite)
                            }
                        }

                        // Product short summary card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SmokyBronze),
                            border = BorderStroke(1.dp, CharcoalBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(product.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("دسته: ${product.category} | عیار: ${product.karat}", color = TextGray, fontSize = 11.sp)
                                }
                                Text("${viewModel.formatWeight(product.weightGram)} گرم", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Generate Bitmaps
                        val barcodeBitmap = remember(barcodeStr) {
                            BarcodeGenerator.generateBarcodeBitmap(barcodeStr, width = 450, height = 130)
                        }
                        val qrCodeBitmap = remember(barcodeStr) {
                            BarcodeGenerator.generateQRCodeBitmap(barcodeStr, width = 280, height = 280)
                        }

                        var labelDpi by remember { mutableStateOf(203) }
                        var selectedBarcodeTab by remember { mutableStateOf(0) } // 0 = Barcode (CODE_128), 1 = QR Code

                        Text("رزولوشن لیبل", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(203, 300).forEach { dpi ->
                                val selected = labelDpi == dpi
                                Button(
                                    onClick = { labelDpi = dpi },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) MetallicGold else SmokyBronze,
                                        contentColor = if (selected) DarkObsidian else TextWhite
                                    )
                                ) {
                                    Text(dpi.toString() + " DPI", fontSize = 11.sp)
                                }
                            }
                        }
                        TabRow(
                            selectedTabIndex = selectedBarcodeTab,
                            containerColor = Color.Transparent,
                            contentColor = MetallicGold,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedBarcodeTab == 0,
                                onClick = { selectedBarcodeTab = 0 },
                                text = { Text("بارکد خطی (Code-128)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedBarcodeTab == 1,
                                onClick = { selectedBarcodeTab = 1 },
                                text = { Text("کد پاسخ سریع (QR Code)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Render Selected Barcode Display Frame
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MetallicGold.copy(0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    if (selectedBarcodeTab == 0) {
                                        if (barcodeBitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = barcodeBitmap.asImageBitmap(),
                                                contentDescription = "بارکد خطی",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(90.dp)
                                            )
                                        } else {
                                            Text("خطا در تولید بارکد", color = Color.Red, fontSize = 12.sp)
                                        }
                                    } else {
                                        if (qrCodeBitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = qrCodeBitmap.asImageBitmap(),
                                                contentDescription = "QR کد کالا",
                                                modifier = Modifier.size(100.dp)
                                            )
                                        } else {
                                            Text("خطا در تولید کد QR", color = Color.Red, fontSize = 12.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = barcodeStr,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Print instructions and confirmation button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.printProductLabelToHardware(product, dpi = labelDpi) { result ->
                                        when (result) {
                                            is com.example.hardware.core.HardwareResult.Success ->
                                                Toast.makeText(context, "اتیکت به چاپگر ارسال شد.", Toast.LENGTH_SHORT).show()
                                            is com.example.hardware.core.HardwareResult.Failure ->
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, tint = DarkObsidian, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ارسال به لیبل‌پرینتر", color = DarkObsidian, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { selectedProductToBarcodePrint = null },
                                colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                                border = BorderStroke(1.dp, CharcoalBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.7f)
                            ) {
                                Text("بستن", color = TextWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. INVOICE CHECKOUT CHASSIS (BILLING ENGINE)
// ==========================================
@Composable
fun InvoiceScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTab: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val customersList by viewModel.customers.collectAsState()
    val productsList by viewModel.products.collectAsState()
    val cartItems = viewModel.draftItems

    var weightedItemName by remember { mutableStateOf("") }
    var weightedItemCategory by remember { mutableStateOf("انگشتر") }
    var weightedItemWeight by remember { mutableStateOf("") }
    var isInvoiceScannerOpen by remember { mutableStateOf(false) }
    var hardwareScannerEnabled by remember { mutableStateOf(false) }
    val hardwareStableWeight by viewModel.hardwareLatestStableWeight.collectAsState()

    androidx.compose.runtime.DisposableEffect(hardwareScannerEnabled) {
        com.example.hardware.barcode.HardwareBarcodeBus.setEnabled(hardwareScannerEnabled)
        onDispose { com.example.hardware.barcode.HardwareBarcodeBus.setEnabled(false) }
    }

    LaunchedEffect(productsList) {
        com.example.hardware.barcode.HardwareBarcodeBus.scans.collect { code ->
            when (val resolved = com.example.domain.util.BarcodeResolver.resolveProductExact(code, productsList)) {
                is com.example.domain.util.ProductResolution.Single -> viewModel.addItemToDraft(resolved.product)
                is com.example.domain.util.ProductResolution.Ambiguous ->
                    Toast.makeText(context, "این بارکد برای چند کالا مشترک است و فروش متوقف شد.", Toast.LENGTH_LONG).show()
                com.example.domain.util.ProductResolution.NotFound ->
                    Toast.makeText(context, "کالایی با این بارکد پیدا نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // State Variables for Checkout Drawer
    var isCustomerSelectorExpand by remember { mutableStateOf(false) }

    if (isCustomerSelectorExpand) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isCustomerSelectorExpand = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, MetallicGold),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "انتخاب مشتری از لیست",
                        color = MetallicGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

                    if (customersList.isEmpty()) {
                        Text(
                            text = "هیچ مشتری در سیستم ثبت نشده است. لطفا ابتدا از بخش مشتریان حساب بسازید.",
                            color = TextGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            items(customersList) { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SmokyBronze.copy(alpha = 0.2f))
                                        .clickable {
                                            viewModel.draftCustomer = cust
                                            isCustomerSelectorExpand = false
                                        }
                                        .border(1.dp, CharcoalBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("تلفن: ${cust.phone}", color = TextGray, fontSize = 11.sp)
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle, 
                                        contentDescription = "انتخاب مشتری", 
                                        tint = MetallicGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { isCustomerSelectorExpand = false },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                        border = BorderStroke(1.dp, CharcoalBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("بستن", color = TextWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("اتصال ترازو و وزن سریع", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = weightedItemWeight,
                        onValueChange = { weightedItemWeight = it },
                        label = { Text("وزن (گرم)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val stable = hardwareStableWeight
                                val onlyItem = cartItems.singleOrNull()
                                if (stable == null) {
                                    Toast.makeText(context, "هنوز وزن پایدار دریافت نشده است.", Toast.LENGTH_SHORT).show()
                                } else if (onlyItem == null) {
                                    Toast.makeText(context, "برای ثبت وزن مستقیم، سبد باید دقیقاً یک قلم داشته باشد.", Toast.LENGTH_SHORT).show()
                                } else if (onlyItem.qty != 1) {
                                    Toast.makeText(context, "برای وزن‌کشی مستقیم، تعداد این قلم باید دقیقاً ۱ باشد.", Toast.LENGTH_SHORT).show()
                                } else {
                                    weightedItemWeight = stable.grams.stripTrailingZeros().toPlainString()
                                    viewModel.applyStableWeightToDraft(onlyItem.product.id, stable.grams)
                                    Toast.makeText(context, "وزن پایدار برای قلم ثبت و قیمت با وزن جدید بازمحاسبه شد.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ثبت در قلم فاکتور", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val stable = hardwareStableWeight
                                if (stable != null) {
                                    weightedItemWeight = stable.grams.stripTrailingZeros().toPlainString()
                                } else {
                                    Toast.makeText(context, "هنوز وزن پایدار از ترازو دریافت نشده است.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ثبت وزن پایدار", fontSize = 11.sp)
                        }
                        Text(
                            hardwareStableWeight?.grams?.stripTrailingZeros()?.toPlainString()?.plus(" گرم ✓") ?: "ترازو متصل نیست/وزن پایدار ندارد",
                            color = if (hardwareStableWeight != null) StatusGreen else TextGray,
                            fontSize = 10.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {

            if (isInvoiceScannerOpen) {
                com.example.ui.components.BarcodeScannerDialog(
                    onDismissRequest = { isInvoiceScannerOpen = false },
                    onBarcodeScanned = { code ->
                        when (val resolved = com.example.domain.util.BarcodeResolver.resolveProductExact(code, productsList)) {
                            is com.example.domain.util.ProductResolution.Single -> {
                                val product = resolved.product
                                if (product.stock > 0) {
                                    viewModel.addItemToDraft(product, 1)
                                    Toast.makeText(context, "کالای ${product.name} به فاکتور اضافه شد", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "کالای ${product.name} موجودی انبار ندارد!", Toast.LENGTH_LONG).show()
                                }
                            }
                            is com.example.domain.util.ProductResolution.Ambiguous -> {
                                Toast.makeText(context, "این بارکد برای چند کالا مشترک است؛ فروش متوقف شد.", Toast.LENGTH_LONG).show()
                            }
                            com.example.domain.util.ProductResolution.NotFound -> {
                                Toast.makeText(context, "کالایی با بارکد $code در انبار یافت نشد!", Toast.LENGTH_LONG).show()
                            }
                        }
                        isInvoiceScannerOpen = false
                  
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        "صندوق ثبت فاکتور لوکس فروشگاهی",
                        style = MaterialTheme.typography.titleMedium,
                        color = MetallicGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "صورت حساب رسمی، تخفیف، انتخاب نوع تسویه نقدی و اقساطی",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { hardwareScannerEnabled = !hardwareScannerEnabled },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (hardwareScannerEnabled) StatusGreen.copy(alpha = 0.14f)
                                else SmokyBronze
                            )
                    ) {
                        Text(
                            "HID",
                            color = if (hardwareScannerEnabled) StatusGreen else TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { isInvoiceScannerOpen = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MetallicGold.copy(0.12f))
                    ) {
                        Icon(
                            Icons.Filled.QrCode,
                            contentDescription = "اسکن با دوربین",
                            tint = MetallicGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

        // A. Customer Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, MetallicGold.copy(0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("خریدار فاکتور:", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        
                        Button(
                            onClick = { isCustomerSelectorExpand = !isCustomerSelectorExpand },
                            colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (viewModel.draftCustomer == null) "انتخاب مشتری" else "تغییر مشتری", fontSize = 10.sp, color = MetallicGold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (viewModel.draftCustomer == null) {
                        Text("خریدار مشخص نشده است (پیشفرض: مشتری متفرقه)", color = TextGray, fontSize = 12.sp)
                    } else {
                        val c = viewModel.draftCustomer
                        if (c != null) {
                            Column {
                                Text("نام خریدار: ${c.name}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("تلفن تماس: ${c.phone}", color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }



        // B. Invoice Items in Assembly Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("سبد اقلام فاکتور شده الحاقی", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (cartItems.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearInvoiceCart() }) {
                        Text("خالی کردن سبد", color = Color.Red, fontSize = 11.sp)
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SmokyCard)
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سبد کالا خالی است.", color = TextGray, fontSize = 12.sp)
                        Text("از تب انبار، دکمه [افزودن به فاکتور] را کلیک کنید.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        } else {
            items(cartItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, CharcoalBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.product.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("بها کل: ${viewModel.formatCurrency(item.exactSalePrice)} تومان", color = MetallicGold, fontSize = 12.sp)
                        }

                        // Actions for qty adjusting
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                if (item.qty > 1) {
                                    viewModel.addItemToDraft(item.product, -1)
                                } else {
                                    viewModel.draftItems.remove(item)
                                }
                            }) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "کاهش", tint = TextGray)
                            }
                            Text(item.qty.toString(), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = {
                                viewModel.addItemToDraft(item.product, 1)
                            }) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "افزایش", tint = MetallicGold)
                            }
                        }
                    }
                }
            }
        }

        // C. Payment Settings panel
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, CharcoalBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("تنظیمات و بهای فاکتور", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        // Payment type chooser
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("CASH" to "تسویه نقدی", "INSTALLMENT" to "سند اقساطی").forEach { (typeKey, label) ->
                                val sel = viewModel.draftPaymentType == typeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (sel) MetallicGold else SmokyBronze)
                                        .border(1.dp, if (sel) MetallicGold else CharcoalBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.draftPaymentType = typeKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (sel) DarkObsidian else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Subtraction Inputs
                        OutlinedTextField(
                            value = viewModel.draftDiscountInput,
                            onValueChange = { viewModel.draftDiscountInput = it },
                            label = { Text("تخفیف کلی فاکتور (تومان)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (viewModel.draftPaymentType == "INSTALLMENT") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = viewModel.draftPrepaymentInput,
                                    onValueChange = { viewModel.draftPrepaymentInput = it },
                                    label = { Text("پیش پرداخت نقدی", color = TextGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = viewModel.draftInstallmentsCountInput,
                                    onValueChange = { viewModel.draftInstallmentsCountInput = it },
                                    label = { Text("تعداد ماه اقساط", color = TextGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(color = MetallicGold.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

                        // Checkout Summary Text
                        CalculationRow("بهای کل ناخالص کالاها:", "${viewModel.formatCurrency(viewModel.draftCartTotalAmount)} تومان")
                        if ((viewModel.draftDiscountInput.toDoubleOrNull() ?: 0.0) > 0) {
                            CalculationRow("تخفیف کسر شده:", "- ${viewModel.formatCurrency(viewModel.draftDiscountInput.toDoubleOrNull() ?: 0.0)} تومان", Color.Red)
                        }
                        CalculationRow("مبلغ خالص پرداختی فاکتور:", "${viewModel.formatCurrency(viewModel.draftFinalPriceAfterDiscount)} تومان", MetallicGold)

                        if (viewModel.draftPaymentType == "INSTALLMENT") {
                            val prep = viewModel.draftPrepaymentInput.toDoubleOrNull() ?: 0.0
                            val counts = viewModel.draftInstallmentsCountInput.toIntOrNull() ?: 3
                            val remVal = (viewModel.draftFinalPriceAfterDiscount - prep).coerceAtLeast(0.0)
                            val perMonth = if (counts > 0) remVal / counts else 0.0
                            CalculationRow("مبلغ اقساط ماهانه:", "${viewModel.formatCurrency(perMonth)} تومان / ماه", GoldAlertOrange)
                        }

                        Spacer(Modifier.height(4.dp))

                        // Final Invoice registration Trigger
                        Button(
                            onClick = {
                                val success = viewModel.submitCurrentDraftInvoice(
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    },
                                    onSuccess = { newInvoiceId ->
                                        if (newInvoiceId > 0) {
                                            Toast.makeText(context, "فاکتور با موفقیت صادر شد", Toast.LENGTH_SHORT).show()
                                            viewModel.showInvoiceReceiptSimulationById(context, newInvoiceId)
                                            onNavigateToTab?.invoke("reports")
                                        } else {
                                            Toast.makeText(context, "خطا در ثبت فاکتور", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                if (!success) {
                                    Toast.makeText(context, "سبد فاکتور خالی است.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("finalize_invoice_button")
                        ) {
                            Text("ثبت قطعی و صدور سند فاکتور", color = DarkObsidian, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 6. CUSTOMER REGISTRY & LEDGER (INSTALLMENTS)
// ==========================================
@Composable
fun CustomersScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customersList by viewModel.customers.collectAsState()
    val allInstallments by viewModel.installments.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isNewCustDialogOpen by remember { mutableStateOf(false) }
    var selectedCustomerDetail by remember { mutableStateOf<Customer?>(null) }

    // Forms
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newNationalId by remember { mutableStateOf("") }
    var newAbout by remember { mutableStateOf("") }

    val filteredCustomers = customersList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("پرونده و حساب مشتریان گالری", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
                    Text("تاریخچه خرید، تسویه بدهی و ردیابی اقساط تا سررسید", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }

                FloatingActionButton(
                    onClick = { isNewCustDialogOpen = true },
                    containerColor = MetallicGold,
                    contentColor = DarkObsidian,
                    modifier = Modifier.size(44.dp).testTag("add_customer_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "مشتری جدید")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("نام یا شماره موبایل مشتری...", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "جستجو", tint = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredCustomers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("مشتری مناسبی پیدا نشد", color = TextGray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(filteredCustomers) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCustomerDetail = customer },
                            colors = CardDefaults.cardColors(containerColor = SmokyCard),
                            border = BorderStroke(1.dp, CharcoalBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(customer.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("موبایل: ${customer.phone}", color = TextGray, fontSize = 12.sp)
                                    if (customer.nationalId.isNotEmpty()) {
                                        Text("کد ملی: ${customer.nationalId}", color = MetallicGold.copy(0.85f), fontSize = 11.sp)
                                    }
                                    if (customer.address.isNotEmpty()) {
                                        Text("آدرس: ${customer.address}", color = TextGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "جزییات",
                                    tint = MetallicGold
                                )
                            }
                        }
                    }
                }
            }
        }

        // RETAIL ACCOUNT LEDGER DETAILS MODAL (INSTANT DRAWER OVERLAY)
        if (selectedCustomerDetail != null) {
            val c = selectedCustomerDetail
            if (c != null) {
                // Find list of active installments for this specific client
                val listInvoiceIds = viewModel.invoices.value.filter { it.invoice.customerId == c.id }.map { it.invoice.id }
                val activeInstalmentsFiltered = allInstallments.filter { listInvoiceIds.contains(it.invoiceId) }

                Dialog(onDismissRequest = { selectedCustomerDetail = null }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, MetallicGold),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(600.dp)
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("دفترچه اقساط مشتری: ${c.name}", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                
                                IconButton(onClick = { selectedCustomerDetail = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextWhite)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("تلفن همراه: ${c.phone}", color = TextWhite)
                            if (c.nationalId.isNotEmpty()) {
                                Text("کد ملی: ${c.nationalId}", color = MetallicGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("مکانی کاربری: ${c.address}", color = TextGray, fontSize = 12.sp)
                            if (c.about.isNotEmpty()) {
                                Text("درباره مشتری: ${c.about}", color = MetallicGold.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(color = MetallicGold.copy(0.2f), modifier = Modifier.padding(vertical = 12.dp))

                            Text("صورت ریز قسط بندیهای فعال معاملات:", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            
                            if (activeInstalmentsFiltered.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("هیچ بدهکاری اقساطی برای این کاربر ثبت نشده است.", color = TextGray, fontSize = 12.sp)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(activeInstalmentsFiltered) { installment ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = SmokyBronze.copy(0.2f)),
                                            border = BorderStroke(1.dp, CharcoalBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("مبلغ قسط: ${viewModel.formatCurrency(installment.amount)} تومان", color = TextWhite, fontSize = 13.sp)
                                                    Text("سررسید پرداخت: ${com.example.ui.util.JalaliCalendar.getJalaliDate(installment.dueDate)}", color = TextGray, fontSize = 11.sp)
                                                    if (installment.paid && installment.paymentDate != null) {
                                                        Text("پرداخت شده در: ${com.example.ui.util.JalaliCalendar.getJalaliDate(installment.paymentDate)}", color = com.example.ui.theme.StatusGreen, fontSize = 10.sp)
                                                    }
                                                }

                                                // Paid Checkbox
                                                Button(
                                                    onClick = {
                                                        viewModel.toggleInstallmentPaid(installment.id, installment.paid)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (installment.paid) Color.DarkGray else MetallicGold
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text(
                                                        text = if (installment.paid) "تسویه شده" else "ثبت پرداخت قسط",
                                                        fontSize = 9.sp,
                                                        color = if (installment.paid) TextWhite else DarkObsidian,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            val customerInvoices = viewModel.invoices.value.filter { it.invoice.customerId == c.id }
                            if (customerInvoices.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SmokyBronze),
                                    border = BorderStroke(1.dp, CharcoalBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Shield, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "این مشتری دارای ${customerInvoices.size} فقره فاکتور ثبت‌شده است. به جهت حفظ اسناد مالی و قوانین حسابداری طلافروشی، حذف این پرونده غیرمجاز است.",
                                            color = TextWhite,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.deleteCustomer(c)
                                        selectedCustomerDetail = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    Text("حذف کلی پرونده مشتری", color = TextWhite)
                                }
                            }
                        }
                    }
                }
            }
        }

        // CREATE NEW CUSTOMER MODAL
        if (isNewCustDialogOpen) {
            Dialog(onDismissRequest = { 
                isNewCustDialogOpen = false 
                newName = ""
                newPhone = ""
                newAddress = ""
                newNationalId = ""
                newAbout = ""
            }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("ثبت پرونده مشتری جدید", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("نام و نام خانوادگی") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("شماره همراه (موبایل)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newNationalId,
                            onValueChange = { newNationalId = it },
                            label = { Text("کد ملی") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            label = { Text("آدرس محل سکونت") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newAbout,
                            onValueChange = { newAbout = it },
                            label = { Text("درباره مشتری (مثال: خوش حساب)") },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MetallicGold, unfocusedBorderColor = CharcoalBorder, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { 
                                    isNewCustDialogOpen = false 
                                    newName = ""
                                    newPhone = ""
                                    newAddress = ""
                                    newNationalId = ""
                                    newAbout = ""
                                }, 
                                colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze), 
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("انصراف", color = TextWhite)
                            }

                            Button(
                                onClick = {
                                    if (newName.isNotEmpty() && newPhone.isNotEmpty()) {
                                        viewModel.addCustomer(newName, newPhone, newAddress, newNationalId, newAbout)
                                        isNewCustDialogOpen = false
                                        newName = ""
                                        newPhone = ""
                                        newAddress = ""
                                        newNationalId = ""
                                        newAbout = ""
                                    } else {
                                        Toast.makeText(context, "مشخصات را کامل وارد کنید", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ثبت مشتری", color = DarkObsidian, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 7. REPAIRS CENTER
// ==========================================
@Composable
fun RepairsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listRepairs by viewModel.repairs.collectAsState()
    val customersList by viewModel.customers.collectAsState()

    var isAddDialogOpen by remember { mutableStateOf(false) }
    var selectedRepairToUpdate by remember { mutableStateOf<RepairWithCustomer?>(null) }
    
    // Forms
    var repairDesc by remember { mutableStateOf("") }
    var repairCost by remember { mutableStateOf("") }
    var repairUpfront by remember { mutableStateOf("") }
    var selectedCustForRepair by remember { mutableStateOf<Customer?>(null) }
    var isCustMenuExpand by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Transparent)
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مرکز تعمیرات سفارشات طلا", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
                    Text("رهگیری جزییات ذوب، فریم، فاکتور دستمزد تعمیرات", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }

                FloatingActionButton(
                    onClick = { isAddDialogOpen = true },
                    containerColor = MetallicGold,
                    contentColor = DarkObsidian,
                    modifier = Modifier.size(44.dp).testTag("add_repair_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "سفارش تعمیر جدید")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (listRepairs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Handyman, contentDescription = "خالی", tint = TextGray, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("هیچ سفارش تعمیری ثبت نشده است.", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(listRepairs) { repairWC ->
                        val r = repairWC.repair
                        val custName = repairWC.customer?.name ?: "مشتری متفرقه"
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRepairToUpdate = repairWC },
                            colors = CardDefaults.cardColors(containerColor = SmokyCard),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (r.status == "READY") MetallicGold else CharcoalBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text("سفارش: " + r.description, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("مالک سفارش: $custName", color = TextGray, fontSize = 11.sp)
                                    }

                                    // Custom visual status indicator
                                    val (statusText, statusColor) = when (r.status) {
                                        "PENDING_APPROVAL" -> "در انتظار تایید" to Color.Gray
                                        "UNDER_REPAIR" -> "درحال تعمیر" to GoldAlertOrange
                                        "READY" -> "آماده تحویل" to MetallicGold
                                        "DELIVERED" -> "تحویل داده شده" to com.example.ui.theme.StatusGreen
                                        else -> "ناشناس" to TextWhite
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusColor.copy(0.15f))
                                            .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(statusText, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("هزینه برآورد: ${viewModel.formatCurrency(r.estimatedCost)} تومان", color = TextWhite, fontSize = 11.sp)
                                    Text("بیعانه: ${viewModel.formatCurrency(r.upfrontPayment)} تومان", color = TextGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // CREATE NEW REPAIR RECIPE MODAL
        if (isAddDialogOpen) {
            Dialog(onDismissRequest = { isAddDialogOpen = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ثبت سفارش تعمیرات جدید کالا", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                        // Client picker button
                        Text("مالک حقیقی کار تعمیری:", color = TextGray, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SmokyBronze)
                                .clickable { isCustMenuExpand = !isCustMenuExpand },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (selectedCustForRepair == null) "برای جستجوی مشتری کلیک کنید" else selectedCustForRepair?.name ?: "",
                                color = MetallicGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (isCustMenuExpand) {
                            Card(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                colors = CardDefaults.cardColors(containerColor = SmokyBronze.copy(0.3f)),
                                border = BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    items(customersList) { c ->
                                        Text(
                                            text = c.name + " (" + c.phone + ")",
                                            color = TextWhite,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedCustForRepair = c
                                                    isCustMenuExpand = false
                                                }
                                                .padding(vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(value = repairDesc, onValueChange = { repairDesc = it }, label = { Text("شرح کامل نوع خرابی و طلا (مثلاً رفع کجی حلقه)") })
                        OutlinedTextField(value = repairCost, onValueChange = { repairCost = it }, label = { Text("پیش‌بینی کل بهای کار مزد تعمیر") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = repairUpfront, onValueChange = { repairUpfront = it }, label = { Text("بیعانه دریافتی کارگاه") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val c = selectedCustForRepair
                                    val est = repairCost.toDoubleOrNull() ?: 0.0
                                    val upf = repairUpfront.toDoubleOrNull() ?: 0.0
                                    if (c != null && repairDesc.isNotEmpty() && est > 0) {
                                        viewModel.addRepair(c.id, repairDesc, est, upf)
                                        isAddDialogOpen = false
                                        repairDesc = ""
                                        repairCost = ""
                                        repairUpfront = ""
                                        selectedCustForRepair = null
                                    } else {
                                        Toast.makeText(context, "الزامات خریدار و نرخ عیاب تکمیل نشده است.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ثبت قبض تعمیر", color = DarkObsidian, fontWeight = FontWeight.Bold)
                            }

                            Button(onClick = { isAddDialogOpen = false }, colors = ButtonDefaults.buttonColors(containerColor = SmokyCard), border = BorderStroke(1.dp, CharcoalBorder), modifier = Modifier.weight(1f)) {
                                Text("انصراف", color = TextWhite)
                            }
                        }
                    }
                }
            }
        }

        // UPDATE REPAIR STATUS OVERLAY MODAL
        if (selectedRepairToUpdate != null) {
            val repWC = selectedRepairToUpdate
            val r = repWC?.repair
            if (r != null) {
                Dialog(onDismissRequest = { selectedRepairToUpdate = null }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, MetallicGold),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("تغییر وضعیت یا حذف کار تعمیری", color = MetallicGold, fontWeight = FontWeight.Bold)
                            Text("محصول: " + r.description, color = TextWhite, fontSize = 13.sp)

                            HorizontalDivider(color = MetallicGold.copy(0.2f))

                            listOf(
                                "PENDING_APPROVAL" to "در انتظار تایید کارگاه",
                                "UNDER_REPAIR" to "در دست تعمیر و ریخته گری",
                                "READY" to "آماده تحویل (فراخوان مشتری)",
                                "DELIVERED" to "تحویل داده شده کالا به صاحبش"
                            ).forEach { (statKey, labelText) ->
                                val sel = r.status == statKey
                                Button(
                                    onClick = {
                                        viewModel.updateRepairStatusFlow(r.id, statKey)
                                        selectedRepairToUpdate = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (sel) MetallicGold else SmokyBronze),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(labelText, color = if (sel) DarkObsidian else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = MetallicGold.copy(0.2f))

                            Button(
                                onClick = {
                                    viewModel.deleteRepairRecord(r)
                                    selectedRepairToUpdate = null
                                    Toast.makeText(context, "سفارش به صورت کلی منقضی و حذف شد.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("لغو کامل و حذف سفارش", color = TextWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 8. REPORTS & CENTRAL LOG RECORDS
// ==========================================
@Composable
fun ReportsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Text("گزارش مالی", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
        Text("دریافت خلاصه عملکرد، خروجی‌های رسمی فاکتورها، تراکنشها و مدیریت سود قانونی گالری گیلدار", style = MaterialTheme.typography.bodySmall, color = TextGray)

        Spacer(modifier = Modifier.height(14.dp))

        // FINANCIAL REPORT PANEL
        val invoices = viewModel.invoices.collectAsState().value
        val activeInvoices = invoices.filter { !it.invoice.paymentType.startsWith("CANCELLED") }
        val totalSales = activeInvoices.sumOf { it.invoice.totalAmount }
        val completedInstPayments = viewModel.installments.collectAsState().value.filter { it.paid }.sumOf { it.amount }
        var invoiceToCancel by remember { mutableStateOf<InvoiceWithDetails?>(null) }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, CharcoalBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("خلاصه وضعیت عملکردی کل سیستم", color = MetallicGold, fontWeight = FontWeight.Bold)
                        CalculationRow("تعداد کل فاکتورهای صادر شده:", "${invoices.size} مورد")
                        CalculationRow("جمع ارزش فروش کالاها:", "${viewModel.formatCurrency(totalSales)} تومان", MetallicGold)
                        CalculationRow("مجموع وصولی اقساط بازار:", "${viewModel.formatCurrency(completedInstPayments)} تومان")
                        
                        // PDF print export option removed as requested
                    }
                }
            }

            item {
                val prefs = remember { context.getSharedPreferences("receipt_prefs", Context.MODE_PRIVATE) }
                var shopNameInput by remember { mutableStateOf(prefs.getString("receipt_shop_name", "گالری طلای گیلدار (شعبه مرکزی)") ?: "گالری طلای گیلدار (شعبه مرکزی)") }
                var titleInput by remember { mutableStateOf(prefs.getString("receipt_title", "فاکتور فروش معتبر کالا") ?: "فاکتور فروش معتبر کالا") }
                var footerInput by remember { mutableStateOf(prefs.getString("receipt_footer", "از خرید و حسن انتخاب شما سپاسگزاریم.") ?: "از خرید و حسن انتخاب شما سپاسگزاریم.") }
                var addressInput by remember { mutableStateOf(prefs.getString("receipt_address", "آدرس: گالری اصلی طلا، تهران") ?: "آدرس: گالری اصلی طلا، تهران") }
                
                                var paperWidthMm by remember { mutableStateOf(prefs.getInt("receipt_paper_mm", 80)) }
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.EditNote, contentDescription = "ویرایش رسید", tint = MetallicGold, modifier = Modifier.size(22.dp))
                                Column {
                                    Text("شخصی‌سازی و طراحی رسید چاپی", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("طراحی لوگو متنی هدر، پیام تشکر فوتر و آدرس گالری", color = TextGray, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "باز کردن",
                                    tint = MetallicGold
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = CharcoalBorder)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Fields
                            OutlinedTextField(
                                value = shopNameInput,
                                onValueChange = { shopNameInput = it },
                                label = { Text("نام گالری / فروشگاه", color = TextGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = titleInput,
                                onValueChange = { titleInput = it },
                                label = { Text("عنوان رسید فاکتور", color = TextGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = footerInput,
                                onValueChange = { footerInput = it },
                                label = { Text("متن فوتر (تشکر و سپاسگزاری)", color = TextGray, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                label = { Text("آدرس گالری و اطلاعات تماس", color = TextGray, fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )

                            Text("عرض رول فیش", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(58 to "۵۸ میلی‌متر", 80 to "۸۰ میلی‌متر").forEach { (mm, label) ->
                                    val selected = paperWidthMm == mm
                                    Button(
                                        onClick = { paperWidthMm = mm },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) MetallicGold else SmokyBronze,
                                            contentColor = if (selected) DarkObsidian else TextWhite
                                        )
                                    ) {
                                        Text(label, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Live preview title
                            Text("پیش‌نمایش زنده رسید حرارتی چاپی:", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))

                            // Mini Thermal Paper Scroll View
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9F6))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text("===============================", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily)
                                    Text(shopNameInput, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text(titleInput, color = Color.Black, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text("===============================", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily)
                                    
                                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                        Text("شماره فاکتور: ۴۵", color = Color.Black, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("تاریخ صدور: ۱۴۰۵/۰۴/۰۵ ۱۲:۳۰", color = Color.Black, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("نام مشتری: مریم عابدی", color = Color.Black, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("-------------------------------", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, modifier = Modifier.fillMaxWidth())
                                        Text("شرح کالا / عیار / وزن / فی کل", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("-------------------------------", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, modifier = Modifier.fillMaxWidth())
                                        Text("دستبند طلا ۱۸ عیار لوکس", color = Color.Black, fontSize = 9.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("  ۱ عدد | فی: ۱۲,۵۰۰,۰۰۰ تومان", color = Color.Black, fontSize = 8.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                        Text("-------------------------------", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily, modifier = Modifier.fillMaxWidth())
                                        Text("جمع نهایی: ۱۲,۵۰۰,۰۰۰ تومان", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                    }
                                    
                                    Text("===============================", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily)
                                    Text(footerInput, color = Color.Black, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text(addressInput, color = Color.Black, fontSize = 8.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    Text("===============================", color = Color.DarkGray, fontSize = 9.sp, fontFamily = com.example.ui.theme.VazirmatnFontFamily)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Save button
                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putString("receipt_shop_name", shopNameInput)
                                        .putString("receipt_title", titleInput)
                                        .putString("receipt_footer", footerInput)
                                        .putString("receipt_address", addressInput)
                                        .putInt("receipt_paper_mm", paperWidthMm)
                                        .apply()
                                    Toast.makeText(context, "طراحی رسید اختصاصی شما با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                    isExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = "ذخیره", tint = DarkObsidian, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ذخیره نهایی قالب رسید اختصاصی", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text("سیاهه فاکتورهای فروش ثبت شده نهایی:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            if (invoices.isEmpty()) {
                item {
                    Text("تاکنون هیچ فاکتوری برای گالری صادر نشده است.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                items(invoices) { itemWC ->
                    val isCancelled = itemWC.invoice.paymentType.startsWith("CANCELLED")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, if (isCancelled) Color(0xFFFF5252).copy(alpha = 0.5f) else CharcoalBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("سند فاکتور شماره: ${itemWC.invoice.id}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (isCancelled) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFF5252).copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("ابطال‌شده", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${viewModel.formatCurrency(itemWC.invoice.totalAmount)} تومان",
                                    color = if (isCancelled) TextGray else MetallicGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("خریدار: ${itemWC.customer?.name ?: "مشتری متفرقه"}", color = TextGray, fontSize = 11.sp)
                                Text("تاریخ صدور: ${com.example.ui.util.JalaliCalendar.getJalaliDateTime(itemWC.invoice.date)}", color = TextGray, fontSize = 11.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val paymentLabel = when {
                                    itemWC.invoice.paymentType.startsWith("CANCELLED") -> "باطل شده"
                                    itemWC.invoice.paymentType == "CASH" -> "تماماً نقدی"
                                    else -> "قسط بندی شده"
                                }
                                Text("نوع تسویه: $paymentLabel", color = TextWhite, fontSize = 11.sp)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (!isCancelled) {
                                        OutlinedButton(
                                            onClick = { invoiceToCancel = itemWC },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("ابطال فاکتور", fontSize = 9.sp, color = Color(0xFFFF5252))
                                        }
                                    }

                                    Button(
                                        onClick = { viewModel.queueInvoicePrintReceipt(context, itemWC) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Filled.Print, contentDescription = "رسید چاپی", tint = MetallicGold, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("چاپ فیش", fontSize = 9.sp, color = MetallicGold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Void Invoice Confirmation Dialog
        invoiceToCancel?.let { invWC ->
            AlertDialog(
                onDismissRequest = { invoiceToCancel = null },
                title = { Text("ابطال رسمی فاکتور شماره ${invWC.invoice.id}", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "آیا از ابطال این فاکتور اطمینان دارید؟\nسند فاکتور جهت حفظ دقیق سوابق مالی و ممیزی صنف طلا در سیستم باقی می‌ماند اما باطل اعلام می‌شود و موجودی اقلام فاکتور به انبار برگردانده خواهد شد.",
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelInvoice(
                                invoice = invWC.invoice,
                                onSuccess = { invoiceToCancel = null }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = TextWhite)
                    ) {
                        Text("بله، ابطال فاکتور")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { invoiceToCancel = null }) {
                        Text("انصراف", color = TextGray)
                    }
                },
                containerColor = SmokyCard,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ==========================================
// 8.5. APP LOG RECORDS / HISTORICAL AUDIT
// ==========================================
@Composable
fun AuditLogsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val systemLogs by viewModel.logs.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Text("گزارشات برنامه و ممیزی سیستم", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
        Text("کنترل لحظه‌ای تمامی تغییرات، ردیابی اقدامات و رخدادهای عملکردی گالری گیلدار", style = MaterialTheme.typography.bodySmall, color = TextGray)

        Spacer(modifier = Modifier.height(14.dp))

        if (systemLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ لاگ عملکردی ثبت نشده است.", color = TextGray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(systemLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, CharcoalBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("رخداد: " + log.action, color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(com.example.ui.util.JalaliCalendar.getJalaliDateTimeWithSeconds(log.timestamp), color = TextGray, fontSize = 11.sp)
                            }
                            Text("جزییات رانش: " + log.details, color = TextWhite, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 9. CONFIGURATION & CORE SETTINGS
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userConfig by viewModel.userConfig.collectAsState()

    var isWipeDialogConfirmOpen by remember { mutableStateOf(false) }
    var priceSettingInput by remember(userConfig) { mutableStateOf(userConfig?.dailyGoldPrice?.let { if (it > BigDecimal.ZERO) it.toLong().toString() else "0" } ?: "0") }
    var taxSettingInput by remember { mutableStateOf(userConfig?.taxPercent?.toString() ?: "9.0") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("تنظیمات هسته نرم‌افزار", style = MaterialTheme.typography.titleMedium, color = MetallicGold, fontWeight = FontWeight.Bold)
            Text("کنترل متغیرهای کسب‌وکار و پشتیبان‌گیری محلی اطلاعات مغازه", style = MaterialTheme.typography.bodySmall, color = TextGray)
        }

        // 1. Lux Aesthetic Theme Selector Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_selector_card"),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MetallicGold.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = MetallicGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "جلوه بصری و تم نرم‌افزار",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "شخصی‌سازی اتمسفر و پالت رنگی هماهنگ با نور محیط کار شما",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Dark Theme Choice
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable { ThemeConfig.isLightMode = false },
                            colors = CardDefaults.cardColors(
                                containerColor = if (!ThemeConfig.isLightMode) SmokyBronze else SmokyCard.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(
                                width = if (!ThemeConfig.isLightMode) 2.dp else 1.dp,
                                color = if (!ThemeConfig.isLightMode) MetallicGold else CharcoalBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (!ThemeConfig.isLightMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(70.dp)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                                    colors = listOf(MetallicGold.copy(0.12f), Color.Transparent),
                                                    radius = 140f
                                                )
                                            )
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (!ThemeConfig.isLightMode) DarkObsidian else SmokyBronze),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Brightness3,
                                                contentDescription = "تم تاریک لوکس",
                                                tint = if (!ThemeConfig.isLightMode) MetallicGold else TextGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (!ThemeConfig.isLightMode) 5.dp else 1.dp,
                                                    color = if (!ThemeConfig.isLightMode) MetallicGold else TextGray,
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    Column {
                                        Text(
                                            "تاریک لوکس",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (!ThemeConfig.isLightMode) MetallicGold else TextWhite
                                        )
                                        Text(
                                            "بافت مخملی سیاه-طلایی",
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }

                        // Light Theme Choice
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(115.dp)
                                .clickable { ThemeConfig.isLightMode = true },
                            colors = CardDefaults.cardColors(
                                containerColor = if (ThemeConfig.isLightMode) LightMetallicGold.copy(alpha = 0.5f) else SmokyCard.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(
                                width = if (ThemeConfig.isLightMode) 2.dp else 1.dp,
                                color = if (ThemeConfig.isLightMode) MetallicGold else CharcoalBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (ThemeConfig.isLightMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(70.dp)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                                    colors = listOf(MetallicGold.copy(0.15f), Color.Transparent),
                                                    radius = 140f
                                                )
                                            )
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (ThemeConfig.isLightMode) Color.White else SmokyBronze),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.WbSunny,
                                                contentDescription = "تم روشن",
                                                tint = if (ThemeConfig.isLightMode) MetallicGold else TextGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = if (ThemeConfig.isLightMode) 5.dp else 1.dp,
                                                    color = if (ThemeConfig.isLightMode) MetallicGold else TextGray,
                                                    shape = CircleShape
                                                )
                                        )
                                    }

                                    Column {
                                        Text(
                                            "روشن مدرن",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (ThemeConfig.isLightMode) DarkMetallicGold else TextWhite
                                        )
                                        Text(
                                            "شفاف و خوانا زیر نور محیط",
                                            fontSize = 10.sp,
                                            color = TextGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }



        item {
            BaseRatesSettingsCard(viewModel)
        }

        // 2. Backup & Restore Operations Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, CharcoalBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("سیستم بکاپ گیری آفلاین دیتابیس", color = MetallicGold, fontWeight = FontWeight.Bold)
                    Text("تمام فایل‌های خروجی در یک پوشه تحت نام Gildar_Backup ذخیره می‌شوند.", color = TextGray, fontSize = 11.sp)

                    Button(
                        onClick = {
                            val msg = viewModel.databaseManualBackup(context)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                        border = BorderStroke(1.dp, MetallicGold.copy(0.3f)),
                        modifier = Modifier.fillMaxWidth().testTag("backup_db_button")
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = "بکاپ گیری", tint = MetallicGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بکاپ گیری دستی محلی", color = MetallicGold)
                    }

                    Text("جهت بازگردانی، می‌توانید در سیستم بازیابی فایل استفاده مجدد کنید.", color = TextGray, fontSize = 11.sp)
                }
            }
        }

        // 3. Destructive factory wipe Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(1.dp, Color.Red.copy(0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("بخش حاکمیتی تخریب اطلاعات", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text("توجه داشته باشید که تایید این دکمه همه‌ی اسناد فاکتور، کالاها و مشتریان را به کلی پاک می‌کند.", color = TextGray, fontSize = 11.sp)

                    Button(
                        onClick = { isWipeDialogConfirmOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth().testTag("wipe_db_button")
                    ) {
                        Text("حذف کلی و هارد ریست پایگاه داده", color = TextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // WIPE CONFIRMATOR MODAL
    if (isWipeDialogConfirmOpen) {
        Dialog(onDismissRequest = { isWipeDialogConfirmOpen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = BorderStroke(2.dp, Color.Red),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("آیا از هارد ریست کل سیستم مطمئن هستید؟", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text("این عملیات غیر قابل بازگشت است و تمام اطلاعات حذف می‌شوند.", color = TextWhite, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                viewModel.wipeSoftwareDatabase()
                                isWipeDialogConfirmOpen = false
                                Toast.makeText(context, "کل پایگاه داده گیلدار با موفقیت هارد ریست شد.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("بله، پاک شود", color = TextWhite, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isWipeDialogConfirmOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = SmokyCard),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("لغو", color = TextWhite)
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 10. BLUETOOTH PRINT RECEIPT SIMULATOR MODAL (INTERACTIVE & PRINTABLE)
// ==========================================
fun printReceipt(context: Context, textPayload: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager
        val jobName = "Gildar_Receipt_${System.currentTimeMillis()}"
        val webView = android.webkit.WebView(context)
        
        val htmlContent = """
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: monospace;
                        font-size: 13px;
                        line-height: 1.4;
                        direction: rtl;
                        text-align: right;
                        white-space: pre-wrap;
                        margin: 20px;
                        padding: 0;
                        color: #000000;
                    }
                </style>
            </head>
            <body>
$textPayload
            </body>
            </html>
        """.trimIndent()
        
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(
                    jobName,
                    printAdapter,
                    android.print.PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در برقراری ارتباط با سرویس چاپ: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PrinterReceiptSimulatorDialog(
    viewModel: ShopViewModel,
    payloadText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F6)), // Styled to emulate thermal paper scroll
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(2.dp, MetallicGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(580.dp)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "پیش‌نمایش نهایی رسید",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(
                            "مقادیر مالی و مشخصات سند دقیقاً مطابق فاکتور ثبت‌شده چاپ می‌شوند",
                            fontSize = 8.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "بستن", tint = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Immutable receipt preview: financial fields must match the saved invoice exactly.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = payloadText,
                            fontFamily = com.example.ui.theme.VazirmatnFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Right
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: 1. Real Print, 2. Mimic cut & dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.printReceiptRasterToHardware(payloadText, viewModel.receiptPaperDots(context)) { result ->
                                when (result) {
                                    is com.example.hardware.core.HardwareResult.Success -> {
                                        Toast.makeText(context, "ارسال رسید به چاپگر با موفقیت انجام شد.", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                    is com.example.hardware.core.HardwareResult.Failure -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = "ارسال به چاپگر حرارتی", tint = DarkObsidian, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ارسال به فیش‌پرینتر", color = DarkObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "برش و خروج", tint = MetallicGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("خروج و برش رسید", color = MetallicGold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductImageRow(
    img1: String?,
    img2: String?,
    img3: String?,
    img4: String?,
    img5: String?,
    onPick1: () -> Unit,
    onPick2: () -> Unit,
    onPick3: () -> Unit,
    onPick4: () -> Unit,
    onPick5: () -> Unit,
    onClear1: () -> Unit,
    onClear2: () -> Unit,
    onClear3: () -> Unit,
    onClear4: () -> Unit,
    onClear5: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text("تصاویر کالا (حداکثر ۵ تصویر):", fontSize = 11.sp, color = TextGray, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            ImageSlotItem(img = img1, onPick = onPick1, onClear = onClear1)
            ImageSlotItem(img = img2, onPick = onPick2, onClear = onClear2)
            ImageSlotItem(img = img3, onPick = onPick3, onClear = onClear3)
            ImageSlotItem(img = img4, onPick = onPick4, onClear = onClear4)
            ImageSlotItem(img = img5, onPick = onPick5, onClear = onClear5)
        }
    }
}

@Composable
fun ImageSlotItem(
    img: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SmokyBronze)
            .border(
                1.dp, 
                if (img != null) MetallicGold else CharcoalBorder, 
                RoundedCornerShape(8.dp)
            )
            .clickable { if (img == null) onPick() },
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            coil.compose.AsyncImage(
                model = java.io.File(img),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "حذف",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "افزودن تصویر",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text("افزودن تصویر", fontSize = 8.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun HelpScreen(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableStateOf<Int?>(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header intro
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = "راهنمای جامع نرم‌افزار طلاش",
                    style = MaterialTheme.typography.titleMedium,
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "مرجع کامل آموزش امکانات، محاسبات صنف طلا و عملکرد هوشمند برنامه",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }

        // Help Categories
        val helpItems = listOf(
            HelpTopic(
                title = "۱. معرفی طلاش و امنیت آفلاین",
                icon = Icons.Filled.Security,
                content = "طلاش دستیار هوشمند و آفلاین ویژه زرگران، گالری‌های طلا و جواهرات است.\n\n" +
                        "• امنیت مطلق: تمام داده‌ها به صورت محلی و کاملاً رمزگذاری شده روی دستگاه شما ذخیره می‌شوند و نیاز به هیچ‌گونه اینترنت یا سرور بیرونی ندارد.\n" +
                        "• قفل امنیتی: با فعال‌ساز رمز عبور (PIN) در بخش پیشانی و منوی تنظیمات، از دسترسی غیرمجاز به اطلاعات گالری طلا و جواهرآلات خود محافظت کنید."
            ),
            HelpTopic(
                title = "۲. پیشخوان و سیستم قیمت‌گذاری آنلاین",
                icon = Icons.Filled.Dashboard,
                content = "پیشخوان به عنوان مرکز فرماندهی مغازه شما عمل می‌کند:\n\n" +
                        "• ارزش کل ویترین: با تغییر آنلاین یا دستی نرخ طلا، کل دارایی انبار شما در لحظه بازطراحی و محاسبه می‌شود.\n" +
                        "• به روزرسانی نرخ: می‌توانید نرخ هر گرم طلای ۱۸ عیار را به صورت دستی وارد کنید یا با فعال‌سازی کلید وب‌سرویس از سرور رسمی (BrsApi.ir) دریافت نمایید.\n" +
                        "• هشدارهای پایش: پایش خودکار اقلام رو به اتمام انبار و چک‌های مشتریان در این بخش نمایش داده می‌شود."
            ),
            HelpTopic(
                title = "۳. انبارداری دیجیتالی و ثبت محصولات",
                icon = Icons.Filled.Warehouse,
                content = "در زبانه انبار، فرآیند ثبت و دسته‌بندی طلا و جواهرات بهینه شده است:\n\n" +
                        "• فرمول محاسبه قیمت پایه برای هر محصول:\n" +
                        "مبنا = وزن کالا × (نرخ مصوب طلا + اجرت ساخت)\n" +
                        "• درصد سود گالری (استاندارد ۷٪) و مالیات ارزش افزوده (معمولاً ۹٪) به قیمت پایه اضافه می‌شوند تا قیمت نهایی فروش به دست آید.\n" +
                        "• بارکدخوان: برنامه قابلیت اسکن کدهای فیزیکی یا تولید شناسه منحصر به فرد تصویری را دارد."
            ),
            HelpTopic(
                title = "۴. فاکتور نویسی رسمی طلا فروش",
                icon = Icons.Filled.Description,
                content = "سیستم صدور فاکتور رسمی تمامی ملزومات قانونی را پوشش می‌دهد:\n\n" +
                        "• افزودن سریع: کافیست کالا را از لیست انبار انتخاب کرده و به سبد خرید مشتری بیفزایید.\n" +
                        "• فاکتور نهایی شفاف: وزن دقیق، بهای لحظه‌ای گرم، اجرت، سود و مالیات به تفکیک و کاملاً قانونی در جدول محاسبه می‌شوند.\n" +
                        "• شبیه‌ساز فیزیکی چاپگر: پس از ثبت فاکتور، رسید رسمی با طراحی استاندارد حرارتی فیش‌پرینتر به همراه بارکد اختصاصی تولید شده و قابل پرینت است."
            ),
            HelpTopic(
                title = "۵. سفارش‌های ساخت، تعمیرات و مشتریان",
                icon = Icons.Filled.Handyman,
                content = "مدیریت ارتباط با مشتری و کارهای کارگاهی شما در یک‌جا:\n\n" +
                        "• دفتر مشتریان: ثبت اطلاعات تماس، حساب‌های باز و سوابق مشتریان خوش‌حساب مغازه.\n" +
                        "• سفارش‌های تعمیرات: ثبت وزن اولیه جواهر تعمیری، عیار، اجرت توافقی و تغییر وضعیت پویای کارگاه (ثبت اولیه ➔ در حال تعمیر ➔ آماده تحویل ➔ تحویل شده)."
            ),
            HelpTopic(
                title = "۶. تحلیل‌گر مالی و انواع گزارشات",
                icon = Icons.Filled.Assessment,
                content = "دسترسی به نمودارها و امارهای کلان مغازه:\n\n" +
                        "• توزیع موجودی: مشاهده درصد تنوع کالاهای ویترین بر اساس وزن و تعداد.\n" +
                        "• دارایی گالری: توازن نقدینگی، سودهای دریافتی حاصل از فروش‌ها و موجودی‌های نقدی صنف طلا در بازه‌های زمانی مختلف."
            ),
            HelpTopic(
                title = "۷. راهنمای تنظیم نرخ مبنا و چرخ‌دنده چشمک‌زن پیشخوان",
                icon = Icons.Filled.Settings,
                content = "در نرم‌افزار طلاش، مدیریت نرخ مبنا به دو روش هوشمند انجام می‌شود:\n\n" +
                        "• چرخ‌دنده چشمک‌زن پیشخوان:\n" +
                        "  در هدر صفحه پیشخوان، در کنار قیمت روز طلا یک دکمه با آیکون چرخ‌دنده وجود دارد که وضعیت نرخ را پایش می‌کند:\n" +
                        "  ۱. چرخ‌دنده با رنگ سبز چشمک‌زن: نشان‌دهنده اتصال موفق وب‌سرویس و دریافت آنلاین و خودکار نرخ لحظه‌ای طلا از بازار آزاد است.\n" +
                        "  ۲. چرخ‌دنده با رنگ قرمز چشمک‌زن: نشان‌دهنده تنظیم دستی نرخ طلا به صورت آفلاین و مستقل است.\n\n" +
                        "• استفاده از وب‌سرویس شخصی:\n" +
                        "  شما در بخش تنظیمات نرخ پایه می‌توانید آدرس و کلید API وب‌سرویس دلخواه خود را قرار دهید. برای ثبت‌نام و دریافت کلید اختصاصی و پرسرعت وب‌سرویس نرخ، می‌توانید به وب‌سایت‌های ارائه‌دهنده معتبر مانند BrsApi.ir (سامانه مرجع وب‌سرویس نرخ طلا و ارز کشور) یا دیگر منابع توسعه‌دهندگان مراجعه کرده و پس از دریافت توکن، آن را در بخش تنظیمات طلاش وارد نمایید تا برنامه به وب‌سرویس شخصی شما متصل شده و نرخ لحظه‌ای را خودکار به‌روزرسانی کند."
            )
        )

        helpItems.forEachIndexed { index, topic ->
            item {
                val isExpanded = expandedSection == index
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedSection = if (isExpanded) null else index },
                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                    border = BorderStroke(1.dp, if (isExpanded) MetallicGold else CharcoalBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MetallicGold.copy(0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = topic.icon,
                                        contentDescription = null,
                                        tint = MetallicGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = topic.title,
                                    color = if (isExpanded) MetallicGold else TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (isExpanded) MetallicGold else TextGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = topic.content,
                                    color = TextWhite.copy(0.9f),
                                    fontSize = 12.sp,
                                    lineHeight = 22.sp,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class HelpTopic(
    val title: String,
    val icon: ImageVector,
    val content: String
)

fun formatColoredSeparators(text: String, digitColor: androidx.compose.ui.graphics.Color, separatorColor: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        for (char in text) {
            val isDigit = (char in '۰'..'۹') || (char in '0'..'9')
            if (isDigit) {
                pushStyle(style = androidx.compose.ui.text.SpanStyle(color = digitColor))
                append(char)
                pop()
            } else {
                pushStyle(style = androidx.compose.ui.text.SpanStyle(color = separatorColor))
                append(char)
                pop()
            }
        }
    }
}

@Composable
fun PluginStoreScreen(viewModel: com.example.ui.viewmodel.ShopViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "hourglass_anim")
    val hourglassRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hourglass_rot"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkObsidian)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SmokyCard),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Polished coming soon hourglass icon with animation
                Icon(
                    imageVector = Icons.Filled.HourglassEmpty,
                    contentDescription = "بزودی",
                    tint = MetallicGold,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            this.rotationZ = hourglassRotation
                        }
                )

                Text(
                    text = "فروشگاه پلاگین و افزونه‌ها (به‌زودی)",
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = com.example.ui.theme.VazirmatnFontFamily
                )

                Text(
                    text = "شما در حال استفاده از نسخه بتا نرم افزار طلاش هستید. در آپدیت‌های بعدی امکانات بیشتری افزوده خواهد شد.",
                    color = TextWhite,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                // Developer contact details box (کادر ارتباط با برنامه نویس)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkObsidian),
                    border = BorderStroke(1.dp, CharcoalBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "راه ارتباطی با نویسنده برنامه:",
                            color = MetallicGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "ایمیل",
                                    tint = MetallicGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "azadazerakhsh@gmail.com",
                                    color = TextWhite,
                                    fontSize = 10.sp,
                                    fontFamily = com.example.ui.theme.VazirmatnFontFamily
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("email", "azadazerakhsh@gmail.com")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "ایمیل با موفقیت کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "کپی", tint = TextGray, modifier = Modifier.size(12.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = "تلفن",
                                    tint = MetallicGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "09109310711",
                                    color = TextWhite,
                                    fontSize = 10.sp,
                                    fontFamily = com.example.ui.theme.VazirmatnFontFamily
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("phone", "09109310711")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "شماره تلفن با موفقیت کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "کپی", tint = TextGray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun BaseRatesSettingsCard(viewModel: com.example.ui.viewmodel.ShopViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var localMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(viewModel.goldPriceMode) }
    var localApiKey by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(viewModel.goldPriceApiKey) }

    val userConfigState = viewModel.userConfig.collectAsState()
    val dailyPrice = (userConfigState.value?.dailyGoldPrice ?: BigDecimal.ZERO).toDouble()

    androidx.compose.material3.Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SmokyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(32.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(MetallicGold.copy(0.12f)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Paid,
                        contentDescription = null,
                        tint = MetallicGold,
                        modifier = androidx.compose.ui.Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        "سامانه نرخ‌های مبنا و وب‌سرویس آنلاین",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "تنظیم دستی یا اتصال پویا به بازار کشور جهت ارزیابی دارایی‌ها",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }

            HorizontalDivider(color = CharcoalBorder, thickness = 1.dp)

            // Switcher
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(DarkObsidian)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(if (localMode == "OFFLINE") MetallicGold else Color.Transparent)
                        .clickable { 
                            localMode = "OFFLINE"
                            viewModel.updateGoldPriceMode("OFFLINE")
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "آفلاین (تعیین دستی مستقل)",
                        color = if (localMode == "OFFLINE") DarkObsidian else TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                        .background(if (localMode == "ONLINE") MetallicGold else Color.Transparent)
                        .clickable { 
                            localMode = "ONLINE"
                            viewModel.updateGoldPriceMode("ONLINE")
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "آنلاین (اتصال به BrsApi.ir)",
                        color = if (localMode == "ONLINE") DarkObsidian else TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (localMode == "ONLINE") {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = localApiKey,
                        onValueChange = { 
                            localApiKey = it
                            viewModel.updateGoldPriceApiKey(it)
                        },
                        label = { Text("کلید وب‌سرویس (API Key)") },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = MetallicGold,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedLabelColor = MetallicGold,
                            unfocusedLabelColor = TextGray
                        ),
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    )

                    if (viewModel.isFetchingOnlineGoldPrice) {
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally).padding(vertical = 4.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MetallicGold,
                                modifier = androidx.compose.ui.Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("در حال فراخوانی وب‌سرویس قیمت‌ها...", color = MetallicGold, fontSize = 11.sp)
                        }
                    } else {
                        androidx.compose.material3.Button(
                            onClick = {
                                viewModel.updateGoldPriceApiKey(localApiKey)
                                viewModel.updateGoldPriceMode("ONLINE")
                                viewModel.syncOnlineGoldPrice()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isOnlineGoldPriceSuccess) StatusGreen else SmokyBronze
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MetallicGold.copy(0.3f)),
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(14.dp), tint = if (viewModel.isOnlineGoldPriceSuccess) Color.White else MetallicGold)
                                Text(
                                    if (viewModel.isOnlineGoldPriceSuccess) "نرخ‌ها با موفقیت همگام‌سازی شدند" else "همگام‌سازی و بروزرسانی آنی آنلاین",
                                    color = if (viewModel.isOnlineGoldPriceSuccess) Color.White else MetallicGold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    viewModel.onlineGoldPriceError?.let { err ->
                        Text(err, color = Color.Red, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Right, modifier = androidx.compose.ui.Modifier.fillMaxWidth())
                    }
                }
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))

            Text("لیست زنده نرخ دارایی‌های انبار طلا و سکه:", fontSize = 12.sp, color = MetallicGold, fontWeight = FontWeight.Bold)

            // Grid containing 14 items
            val ratesList = listOf<Triple<String, String, Double>>(
                Triple("طلای ۱۸ عیار (مبنا)", "rate_gold_18k", dailyPrice),
                Triple("طلای ۲۴ عیار", "rate_gold_24k", viewModel.rateGold24k),
                Triple("طلای آبشده نقدی", "rate_gold_melted", viewModel.rateGoldMelted),
                Triple("سکه یک گرمی", "rate_coin_1g", viewModel.rateCoin1g),
                Triple("ربع سکه", "rate_coin_quarter", viewModel.rateCoinQuarter),
                Triple("نیم سکه", "rate_coin_half", viewModel.rateCoinHalf),
                Triple("سکه امامی", "rate_coin_emami", viewModel.rateCoinEmami),
                Triple("سکه بهار آزادی", "rate_coin_bahar", viewModel.rateCoinBahar),
                Triple("دلار آمریکا (تومان)", "rate_currency_usd", viewModel.rateCurrencyUsd),
                Triple("دلار تتر (تومان)", "rate_currency_tether", viewModel.rateCurrencyTether),
                Triple("یورو (تومان)", "rate_currency_eur", viewModel.rateCurrencyEur),
                Triple("درهم امارات (تومان)", "rate_currency_aed", viewModel.rateCurrencyAed),
                Triple("پوند انگلیس (تومان)", "rate_currency_gbp", viewModel.rateCurrencyGbp)
            )

            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ratesList.forEach { (label, key, price) ->
                    var inputValue by androidx.compose.runtime.remember(price) { androidx.compose.runtime.mutableStateOf(if (price > 0.0) price.toLong().toString() else "0") }

                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .background(DarkObsidian.copy(0.6f))
                            .border(1.dp, CharcoalBorder, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        Column(modifier = androidx.compose.ui.Modifier.weight(1.5f)) {
                            Text(label, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(androidx.compose.ui.Modifier.height(4.dp))
                            Text(
                                text = if (price <= 0.0) {
                                    "تنظیم نشده"
                                } else {
                                    if (key == "rate_gold_ounce") "${viewModel.formatCurrency(price)} $" else "${viewModel.formatCurrency(price)} تومان"
                                },
                                color = if (price <= 0.0) TextGray else MetallicGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (localMode == "OFFLINE") {
                            androidx.compose.material3.OutlinedTextField(
                                value = inputValue,
                                onValueChange = { inputValue = it },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextWhite),
                                singleLine = true,
                                modifier = androidx.compose.ui.Modifier
                                    .width(130.dp)
                                    .height(50.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder
                                ),
                                trailingIcon = {
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            val dbl = inputValue.toDoubleOrNull()
                                            if (dbl != null && dbl > 0.0) {
                                                if (key == "rate_gold_18k") {
                                                    viewModel.updateGoldPrice(dbl)
                                                } else {
                                                    viewModel.updateRateSetting(key, dbl)
                                                }
                                                android.widget.Toast.makeText(context, "بهای $label بروزرسانی شد", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = "ذخیره", tint = StatusGreen, modifier = androidx.compose.ui.Modifier.size(16.dp))
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = androidx.compose.ui.Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .background(StatusGreen.copy(0.1f))
                                    .border(1.dp, StatusGreen.copy(0.3f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(StatusGreen)
                                )
                                Text("اتصال آنلاین", color = StatusGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


