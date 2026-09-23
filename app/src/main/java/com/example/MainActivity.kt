package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.hardware.barcode.HardwareBarcodeBus
import com.example.ui.screens.hardware.HardwareCenterScreen
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// Manual MVVM ViewModel Factory representing Clean Architecture
class ShopViewModelFactory(
    private val repository: com.example.data.repository.ShopRepository,
    private val appContext: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        return if (HardwareBarcodeBus.onKeyEvent(event)) true else super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val view = androidx.compose.ui.platform.LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val activityWindow = this@MainActivity.window
                        val controller = androidx.core.view.WindowCompat.getInsetsController(activityWindow, view)
                        controller.isAppearanceLightStatusBars = ThemeConfig.isLightMode
                        controller.isAppearanceLightNavigationBars = ThemeConfig.isLightMode
                    }
                }

                // Fetch the manual dependency container lazy singletons
                val app = LocalContext.current.applicationContext as GildarApp
                val factory = remember { ShopViewModelFactory(app.repository, app.applicationContext) }
                val mainViewModel: ShopViewModel = viewModel(factory = factory)

                val authenticated by mainViewModel.isAuthenticated.collectAsState()

                var showSplash by rememberSaveable { mutableStateOf(true) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else if (!authenticated) {
                        PinLockScreen(viewModel = mainViewModel)
                    } else {
                        MainAppContainerShell(viewModel = mainViewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainAppContainerShell(viewModel: ShopViewModel) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("home") }
    val userConfig by viewModel.userConfig.collectAsState()

    var isScrolling by remember { mutableStateOf(false) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (abs(available.y) > 2f) {
                    isScrolling = true
                    scrollJob?.cancel()
                    scrollJob = scope.launch {
                        delay(1200)
                        isScrolling = false
                    }
                }
                return Offset.Zero
            }
        }
    }

    var isFloatingMenuExpanded by remember { mutableStateOf(false) }
    var showWelcomePopup by rememberSaveable { mutableStateOf(false) }
    var showGoldPriceEditDialog by remember { mutableStateOf(false) }

    // Live daily rate tracker shown in Top bar
    val rawGoldPrice = userConfig?.dailyGoldPrice ?: java.math.BigDecimal.ZERO
    val formattedGoldRate = if (rawGoldPrice <= java.math.BigDecimal.ZERO) "تنظیم نشده" else "${viewModel.formatCurrency(rawGoldPrice)} تومان"

    // Gold Price Editing Popup Dialog
    if (showGoldPriceEditDialog) {
        var localMode by remember { mutableStateOf(viewModel.goldPriceMode) }
        var localApiKey by remember { mutableStateOf(viewModel.goldPriceApiKey) }
        var goldPriceInput by remember(rawGoldPrice) { mutableStateOf(if (rawGoldPrice > java.math.BigDecimal.ZERO) rawGoldPrice.toLong().toString() else "0") }
        
        AlertDialog(
            onDismissRequest = { showGoldPriceEditDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = MetallicGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "تنظیم نرخ مبنای طلا",
                        color = MetallicGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Segmented Selector for Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CharcoalBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (localMode == "OFFLINE") MetallicGold else Color.Transparent)
                                .clickable { localMode = "OFFLINE" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "آفلاین (تنظیم دستی)",
                                color = if (localMode == "OFFLINE") DarkObsidian else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (localMode == "ONLINE") MetallicGold else Color.Transparent)
                                .clickable { localMode = "ONLINE" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "آنلاین (BrsApi.ir)",
                                color = if (localMode == "ONLINE") DarkObsidian else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (localMode == "OFFLINE") {
                        Text(
                            text = "نرخ مبنای فعلی طلا در کل سیستم: $formattedGoldRate",
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = goldPriceInput,
                            onValueChange = { goldPriceInput = it },
                            label = { Text("بهای هر گرم طلای ۱۸ عیار (تومان)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedLabelColor = MetallicGold,
                                unfocusedLabelColor = TextGray
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "در حالت آنلاین، قیمت هر گرم طلای ۱۸ عیار به‌صورت خودکار از وب‌سرویس BarsApi دریافت می‌شود.",
                                color = TextGray,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                            
                            OutlinedTextField(
                                value = localApiKey,
                                onValueChange = { localApiKey = it },
                                label = { Text("کلید وب‌سرویس (API Key)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite,
                                    focusedBorderColor = MetallicGold,
                                    unfocusedBorderColor = CharcoalBorder,
                                    focusedLabelColor = MetallicGold,
                                    unfocusedLabelColor = TextGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (viewModel.isFetchingOnlineGoldPrice) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = MetallicGold,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        "در حال دریافت ارزیابی بازار...",
                                        color = MetallicGold,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.updateGoldPriceApiKey(localApiKey)
                                        viewModel.updateGoldPriceMode("ONLINE")
                                        viewModel.syncOnlineGoldPrice()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (viewModel.isOnlineGoldPriceSuccess) StatusGreen else CharcoalBorder,
                                        contentColor = if (viewModel.isOnlineGoldPriceSuccess) (if (ThemeConfig.isLightMode) Color.White else DarkObsidian) else TextWhite
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = localApiKey.isNotEmpty()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("به‌روزرسانی و دریافت نرخ آنلاین", fontSize = 12.sp)
                                    }
                                }
                            }

                            viewModel.onlineGoldPriceError?.let { errorMsg ->
                                Text(
                                    text = errorMsg,
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Text(
                                text = "نرخ مبنای فعلی طلا در کل سیستم: $formattedGoldRate",
                                color = TextWhite,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showGoldPriceEditDialog = false }
                    ) {
                        Text("انصراف", color = TextGray)
                    }
                    Button(
                        onClick = {
                            viewModel.updateGoldPriceMode(localMode)
                            viewModel.updateGoldPriceApiKey(localApiKey)
                            if (localMode == "OFFLINE") {
                                val newPrice = goldPriceInput.toDoubleOrNull()
                                if (newPrice != null && newPrice > 0.0) {
                                    viewModel.updateGoldPrice(newPrice)
                                }
                            } else {
                                if (localApiKey.isNotEmpty()) {
                                    viewModel.syncOnlineGoldPrice()
                                }
                            }
                            showGoldPriceEditDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MetallicGold,
                            contentColor = DarkObsidian
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ذخیره", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null,
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, MetallicGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { paddingValues ->
            val menuItems = listOf(
                Triple("بستن روز طلافروشی", Icons.Filled.LockClock, "daily_closing"),
                Triple("انبارگردانی با بارکد", Icons.Filled.QrCodeScanner, "stock_take"),
                Triple("پشتیبان‌گیری و بازیابی", Icons.Filled.Backup, "backup_restore"),
                Triple("تجهیزات و اتصال سریع", Icons.Filled.Extension, "hardware_center"),
                Triple("سفارش تعمیر", Icons.Filled.Handyman, "repairs"),
                Triple("گزارش مالی", Icons.Filled.Assessment, "reports"),
                Triple("گزارشات برنامه", Icons.Filled.Visibility, "audit_logs"),
                Triple("تنظیمات نرم‌افزار", Icons.Filled.Settings, "settings"),
                Triple("راهنمای نرم‌افزار", Icons.AutoMirrored.Filled.Help, "help")
            )

            val bottomPaddingAnimated = 0.dp

            val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        start = paddingValues.calculateStartPadding(layoutDirection),
                        end = paddingValues.calculateEndPadding(layoutDirection)
                    )
                    .nestedScroll(nestedScrollConnection)
            ) {
            // Screen Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = bottomPaddingAnimated)
            ) {
                // Tab Switcher Router
                when (activeTab) {
                    "home" -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it },
                        onGoldPriceClick = { showGoldPriceEditDialog = true }
                    )
                    "warehouse" -> WarehouseScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it }
                    )
                    "invoice" -> InvoiceScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it }
                    )
                    "customers" -> CustomersScreen(viewModel = viewModel)
                    "daily_closing" -> com.example.ui.screens.closing.DailyClosingScreen(
                        viewModel = viewModel,
                        onBack = { activeTab = "home" }
                    )
                    "stock_take" -> com.example.ui.screens.stocktake.StockTakeScreen(
                        viewModel = viewModel,
                        onBack = { activeTab = "home" }
                    )
                    "backup_restore" -> com.example.ui.screens.backup.BackupRestoreScreen(
                        viewModel = viewModel,
                        onBack = { activeTab = "home" }
                    )
                    "hardware_center" -> HardwareCenterScreen(viewModel = viewModel, onBack = { activeTab = "home" })
                    "repairs" -> RepairsScreen(viewModel = viewModel)
                    "reports" -> ReportsScreen(viewModel = viewModel)
                    "audit_logs" -> AuditLogsScreen(viewModel = viewModel)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                    "help" -> HelpScreen(viewModel = viewModel)
                }
            }

            // GORGEOUS GLASS/DIM OVERLAY FOR EXPANDED MENU
            val dimAlpha by animateFloatAsState(
                targetValue = if (isFloatingMenuExpanded) 0.65f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "floatingMenuDim"
            )

            if (dimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha))
                        .clickable(enabled = isFloatingMenuExpanded) { isFloatingMenuExpanded = false }
                )
            }

            // GORGEOUS VERTICAL EXPANDABLE MORE MENU STACK (ALIGNED TO BOTTOM START ABOVE THE "بیشتر" BUTTON)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, bottom = 115.dp)
                    .zIndex(99f),
                contentAlignment = Alignment.BottomStart
            ) {
                val animFraction by animateFloatAsState(
                    targetValue = if (isFloatingMenuExpanded) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "menu_vertical_fraction"
                )

                if (animFraction > 0.05f) {
                    menuItems.forEachIndexed { index, (label, icon, route) ->
                        val isSelected = activeTab == route

                        // Stack vertically dynamic with item count
                        val targetY = - ((menuItems.size - 1 - index) * 48).dp

                        // Clean entrance movement: slide upwards slightly and fade/scale
                        val currentY = -20.dp + (targetY + 20.dp) * animFraction
                        val currentScale = 0.8f + 0.2f * animFraction
                        val currentAlpha = animFraction

                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .offset(x = 0.dp, y = currentY)
                                .graphicsLayer {
                                    scaleX = currentScale
                                    scaleY = currentScale
                                    alpha = currentAlpha
                                }
                                .clip(RoundedCornerShape(20.dp))
                                .background(SmokyCard)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) MetallicGold else CharcoalBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable(enabled = isFloatingMenuExpanded && animFraction > 0.8f) {
                                    activeTab = route
                                    isFloatingMenuExpanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MetallicGold else SmokyBronze)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else MetallicGold.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) DarkObsidian else MetallicGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                color = if (isSelected) MetallicGold else TextWhite.copy(0.95f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Action Bar (Unobscured and transparent floating buttons with five items)
    AnimatedVisibility(
        visible = !isScrolling && !WindowInsets.isImeVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SmokyCard)
                .border(
                    width = 1.5.dp,
                    color = MetallicGold.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item 1: More (بیشتر)
            val isMoreActive = isFloatingMenuExpanded || activeTab in listOf("daily_closing", "stock_take", "backup_restore", "hardware_center", "repairs", "reports", "audit_logs", "settings", "help")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isMoreActive) MetallicGold.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isMoreActive) MetallicGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isFloatingMenuExpanded = !isFloatingMenuExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "بیشتر",
                    tint = if (isMoreActive) MetallicGold else TextWhite.copy(0.6f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "بیشتر",
                    color = if (isMoreActive) MetallicGold else TextWhite.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Item 2: Home (پیشخوان)
            val isHome = activeTab == "home"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isHome) MetallicGold.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isHome) MetallicGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        activeTab = "home"
                        isFloatingMenuExpanded = false
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "پیشخوان",
                    tint = if (isHome) MetallicGold else TextWhite.copy(0.6f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "پیشخوان",
                    color = if (isHome) MetallicGold else TextWhite.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Item 3: Warehouse (انبار کالا)
            val isWarehouse = activeTab == "warehouse"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isWarehouse) MetallicGold.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isWarehouse) MetallicGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        activeTab = "warehouse"
                        isFloatingMenuExpanded = false
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBasket,
                    contentDescription = "انبار کالا",
                    tint = if (isWarehouse) MetallicGold else TextWhite.copy(0.6f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "انبار کالا",
                    color = if (isWarehouse) MetallicGold else TextWhite.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Item 4: Invoice (فاکتور) - Middle button with a '+' on its icon
            val isInvoice = activeTab == "invoice"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isInvoice) MetallicGold.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isInvoice) MetallicGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        activeTab = "invoice"
                        isFloatingMenuExpanded = false
                    }
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = "فاکتور",
                        tint = if (isInvoice) MetallicGold else TextWhite.copy(0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                    // A mini plus overlay (+) sign on bottom right
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MetallicGold)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = DarkObsidian,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "فاکتور",
                    color = if (isInvoice) MetallicGold else TextWhite.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Item 5: Customers (مشتریان)
            val isCustomers = activeTab == "customers"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isCustomers) MetallicGold.copy(alpha = 0.12f) else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (isCustomers) MetallicGold else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        activeTab = "customers"
                        isFloatingMenuExpanded = false
                    }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.People,
                    contentDescription = "مشتریان",
                    tint = if (isCustomers) MetallicGold else TextWhite.copy(0.6f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "مشتریان",
                    color = if (isCustomers) MetallicGold else TextWhite.copy(0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

    // PRINT BILL SIMULATOR DIALOG OVERLAY
    if (viewModel.isShowingPrinterReceiptSimulation && viewModel.activePrintJobPayload != null) {
        PrinterReceiptSimulatorDialog(
            viewModel = viewModel,
            payloadText = viewModel.activePrintJobPayload ?: "",
            onDismiss = {
                viewModel.isShowingPrinterReceiptSimulation = false
                viewModel.activePrintJobPayload = null
            }
        )
    }
}

@Composable
fun SystemWelcomeGuideDialog(
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    
    val pages = listOf(
        GuidePage(
            title = "به نرم‌افزار «طلاش» خوش آمدید!",
            description = "سیستم هوشمند، لوکس و آفلاین مدیریت گالری طلا و جواهرات شما. تمام محاسبات مالی صنف طلا و مدیریت مشتریان، به ساده‌ترین شکل و با امنیت کامل در دستان شماست.",
            icon = Icons.Filled.Stars,
            color = MetallicGold
        ),
        GuidePage(
            title = "پیشخوان هوشمند نظارتی",
            description = "تحلیل کاملاً آفلاین ارزش کل ویترین طلا بر اساس تغییرات لحظه‌ای نرخ گرم طلا، نمایش سود ۷٪ قانونی صنف، پایش و هشدارهای سررسید یا کسری اقلام انبار.",
            icon = Icons.Filled.Dashboard,
            color = MetallicGold
        ),
        GuidePage(
            title = "انبارداری و صدور فاکتور رسمی",
            description = "سبد وزنی کالا در نمودارهای پیشرفته، مدیریت اقساط سررسیدگذشته همراه با ثبت تعمیرات و صدور فاکتور فروش رسمی متصل به ماژول چاپگر رسید.",
            icon = Icons.Filled.Description,
            color = MetallicGold
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (currentPage < pages.size - 1) {
                        currentPage++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (currentPage < pages.size - 1) "بعدی" else "متوجه شدم و ورود",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            if (currentPage > 0) {
                TextButton(
                    onClick = { currentPage-- }
                ) {
                    Text("قبلی", color = TextGray, fontSize = 12.sp)
                }
            } else {
                TextButton(
                    onClick = onDismiss
                ) {
                    Text("انصراف", color = TextGray, fontSize = 12.sp)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(pages[currentPage].color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = pages[currentPage].icon,
                        contentDescription = null,
                        tint = pages[currentPage].color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = pages[currentPage].title,
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = pages[currentPage].description,
                    color = TextWhite,
                    fontSize = 13.sp,
                    lineHeight = 22.sp
                )
                
                // Indicators dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (currentPage == index) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (currentPage == index) MetallicGold else TextGray.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        },
        containerColor = SmokyCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, MetallicGold.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    )
}

data class GuidePage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MoreMenuButton(
    imageVector: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkObsidian)
            .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = imageVector,
                contentDescription = label,
                tint = MetallicGold,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
