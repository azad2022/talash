package com.example.ui.screens.stocktake

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StockTakeItem
import com.example.data.model.StockTakeSession
import com.example.data.repository.StockTakeApplyResult
import com.example.data.repository.StockTakeScanResult
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.theme.*
import com.example.ui.util.JalaliCalendar
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTakeScreen(
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val activeSession by viewModel.activeStockTakeSession.collectAsState()
    val allSessions by viewModel.stockTakeSessions.collectAsState()
    val stockTakeItems by viewModel.currentStockTakeItems.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    var startNotesInput by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    var manualBarcodeInput by remember { mutableStateOf("") }
    var lastScanMessage by remember { mutableStateOf<String?>(null) }
    var lastScanIsSuccess by remember { mutableStateOf(true) }

    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showReviewReconciliationDialog by remember { mutableStateOf(false) }
    var reviewItemsList by remember { mutableStateOf<List<StockTakeItem>>(emptyList()) }
    var showFinalApplySuccessDialog by remember { mutableStateOf<StockTakeApplyResult?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, DISCREPANT, COUNTED, UNCOUNTED

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "انبارگردانی موبایلی با بارکد",
                            color = MetallicGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkObsidian
                )
            )
        },
        containerColor = DarkObsidian
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val session = activeSession
            if (session == null) {
                // NO ACTIVE SESSION VIEW
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SmokyCard),
                            border = BorderStroke(1.dp, MetallicGold)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(SmokyBronze),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory,
                                        contentDescription = null,
                                        tint = MetallicGold,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = "شروع انبارگردانی دوره‌ای",
                                    color = MetallicGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "با شروع انبارگردانی، تصویری از موجودی فعلی انبار ثبت می‌شود و می‌توانید اقلام ویترین و گاوصندوق را با بارکدخوان اسکن نمایید تا مغایرت‌ها مشخص شوند.",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Button(
                                    onClick = {
                                        startNotesInput = ""
                                        showStartDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MetallicGold,
                                        contentColor = DarkObsidian
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("شروع نشست انبارگردانی جدید", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "تاریخچه نشست‌های قبلی انبارگردانی",
                            color = MetallicGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    val pastSessions = allSessions.filter { it.status == "COMPLETED" || it.status == "CANCELLED" }
                    if (pastSessions.isEmpty()) {
                        item {
                            Text(
                                text = "تاکنون هیچ نشست انبارگردانی نهایی یا لغو نشده است.",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        items(pastSessions) { s ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                border = BorderStroke(1.dp, CharcoalBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "نشست شماره #${s.id}",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (s.status == "COMPLETED") StatusGreen.copy(alpha = 0.2f)
                                                    else Color(0xFFFF5252).copy(alpha = 0.2f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (s.status == "COMPLETED") "نهایی شده" else "لغو شده",
                                                color = if (s.status == "COMPLETED") StatusGreen else Color(0xFFFF5252),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "تاریخ: ${JalaliCalendar.getJalaliDateTime(s.startedAt)}",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("اقلام بررسی‌شده:", color = TextGray, fontSize = 11.sp)
                                        Text("${s.totalCountedPieces} از ${s.totalExpectedPieces} قلم", color = TextWhite, fontSize = 11.sp)
                                    }
                                    val pieceDiff = s.totalCountedPieces - s.totalExpectedPieces
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تفاضل شمارش با موجودی اولیه:", color = TextGray, fontSize = 11.sp)
                                        Text(
                                            text = if (pieceDiff == 0) "منطبق (۰)" else if (pieceDiff > 0) "+$pieceDiff مازاد" else "$pieceDiff کسری",
                                            color = if (pieceDiff == 0) StatusGreen else Color(0xFFFF5252),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    s.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                                        Text("یادداشت: $notes", color = TextGray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ACTIVE SESSION VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Header Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        border = BorderStroke(1.dp, MetallicGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (session.status == "REVIEW_REQUIRED") Color(0xFFFF9800) else Color(0xFF00E676))
                                    )
                                    Text("نشست انبارگردانی #${session.id}", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (session.status == "REVIEW_REQUIRED") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFF9800).copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("نیازمند بازبینی", color = Color(0xFFFF9800), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showCancelConfirmDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                                ) {
                                    Text("لغو نشست", fontSize = 11.sp)
                                }
                            }

                            if (session.status == "REVIEW_REQUIRED") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, Color(0xFFFF9800))
                                ) {
                                    Text(
                                        text = "توجه: در حین این نشست انبارگردانی، موجودی برخی اقلام یا لیست کالاها تغییر کرده است. لطفاً پیش از نهایی‌سازی، دکمه «بررسی مغایرت‌ها» را جهت تطبیق نهایی بزنید.",
                                        color = Color(0xFFFFB300),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(8.dp),
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            // Metrics summary
                            val totalItems = stockTakeItems.size
                            val countedItems = stockTakeItems.count { it.isCounted }
                            val uncountedItems = stockTakeItems.count { !it.isCounted }
                            val matchedItems = stockTakeItems.count {
                                it.isCounted && it.countedStock == it.expectedStockAtStart &&
                                        it.status != "NEEDS_REVIEW" && it.status != "NEW_PRODUCT_DURING_SESSION"
                            }
                            val discrepantItems = stockTakeItems.count {
                                it.isCounted && (it.countedStock != it.expectedStockAtStart ||
                                        it.status == "NEEDS_REVIEW" || it.status == "NEW_PRODUCT_DURING_SESSION")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                MetricColumn(title = "کل اقلام", value = "$totalItems", color = TextWhite)
                                MetricColumn(title = "شمارش‌شده", value = "$countedItems", color = MetallicGold)
                                MetricColumn(title = "شمارش‌نشده", value = "$uncountedItems", color = if (uncountedItems > 0) Color(0xFFFF9800) else TextGray)
                                MetricColumn(title = "منطبق", value = "$matchedItems", color = StatusGreen)
                                MetricColumn(title = "مغایرت", value = "$discrepantItems", color = if (discrepantItems > 0) Color(0xFFFF5252) else TextGray)
                            }

                            val progressFraction = if (totalItems > 0) countedItems.toFloat() / totalItems.toFloat() else 0f
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MetallicGold,
                                trackColor = CharcoalBorder
                            )
                        }
                    }

                    // Barcode Scanning & Input Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualBarcodeInput,
                            onValueChange = { manualBarcodeInput = it },
                            label = { Text("بارکد یا کد کالا") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = MetallicGold,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedLabelColor = MetallicGold,
                                unfocusedLabelColor = TextGray
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (manualBarcodeInput.isNotBlank()) {
                                        viewModel.scanBarcodeForStockTake(manualBarcodeInput.trim()) { res ->
                                            handleScanResult(res) { msg, success ->
                                                lastScanMessage = msg
                                                lastScanIsSuccess = success
                                            }
                                        }
                                        manualBarcodeInput = ""
                                        focusManager.clearFocus()
                                    }
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (manualBarcodeInput.isNotBlank()) {
                                    viewModel.scanBarcodeForStockTake(manualBarcodeInput.trim()) { res ->
                                        handleScanResult(res) { msg, success ->
                                            lastScanMessage = msg
                                            lastScanIsSuccess = success
                                        }
                                    }
                                    manualBarcodeInput = ""
                                    focusManager.clearFocus()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SmokyBronze),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("ثبت", color = MetallicGold, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showCameraScanner = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "دوربین", tint = DarkObsidian)
                        }
                    }

                    // Feedback Banner for Last Scan
                    AnimatedVisibility(visible = lastScanMessage != null) {
                        lastScanMessage?.let { msg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (lastScanIsSuccess) StatusGreen.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, if (lastScanIsSuccess) StatusGreen else Color(0xFFFF5252))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg,
                                        color = if (lastScanIsSuccess) StatusGreen else Color(0xFFFF5252),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { lastScanMessage = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = TextWhite)
                                    }
                                }
                            }
                        }
                    }

                    // Filter Chips & Search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("همه", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "DISCREPANT",
                            onClick = { selectedFilter = "DISCREPANT" },
                            label = { Text("مغایرت‌ها", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "COUNTED",
                            onClick = { selectedFilter = "COUNTED" },
                            label = { Text("شمارش‌شده", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "UNCOUNTED",
                            onClick = { selectedFilter = "UNCOUNTED" },
                            label = { Text("شمارش‌نشده", fontSize = 11.sp) }
                        )
                    }

                    // Items List
                    val filteredList = stockTakeItems.filter { item ->
                        val matchesSearch = searchQuery.isBlank() ||
                                item.productName.contains(searchQuery, ignoreCase = true) ||
                                item.productBarcode.contains(searchQuery, ignoreCase = true) ||
                                item.productCategory.contains(searchQuery, ignoreCase = true)

                        val matchesFilter = when (selectedFilter) {
                            "DISCREPANT" -> item.isCounted && (item.countedStock != item.expectedStockAtStart ||
                                    item.status == "NEEDS_REVIEW" || item.status == "NEW_PRODUCT_DURING_SESSION")
                            "COUNTED" -> item.isCounted
                            "UNCOUNTED" -> !item.isCounted
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (filteredList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("هیچ کالایی با این فیلتر یافت نشد.", color = TextGray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { item ->
                                StockTakeItemCard(
                                    item = item,
                                    onIncrement = {
                                        viewModel.manualUpdateStockTakeItemCount(item.productId, item.countedStock + 1)
                                    },
                                    onDecrement = {
                                        if (item.countedStock > 0) {
                                            viewModel.manualUpdateStockTakeItemCount(item.productId, item.countedStock - 1)
                                        }
                                    },
                                    onConfirmZero = {
                                        viewModel.manualUpdateStockTakeItemCount(item.productId, 0)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom Reconciliation Action Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = DarkObsidian
                    ) {
                        Button(
                            onClick = {
                                viewModel.prepareStockTakeReview(session.id) { result ->
                                    result.getOrNull()?.let { reviewedItems ->
                                        reviewItemsList = reviewedItems
                                        showReviewReconciliationDialog = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MetallicGold,
                                contentColor = DarkObsidian
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("بررسی مغایرت‌ها و ثبت نهایی انبارگردانی", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Camera Barcode Scanner Dialog
    if (showCameraScanner && activeSession != null) {
        BarcodeScannerDialog(
            onDismissRequest = { showCameraScanner = false },
            onBarcodeScanned = { scannedCode ->
                showCameraScanner = false
                viewModel.scanBarcodeForStockTake(scannedCode) { res ->
                    handleScanResult(res) { msg, success ->
                        lastScanMessage = msg
                        lastScanIsSuccess = success
                    }
                }
            }
        )
    }

    // Start Session Dialog
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = {
                Text("شروع نشست انبارگردانی", color = MetallicGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "این عملیات موجودی فعلی کلیه کالاهای انبار را به عنوان مبنای تطبیق ثبت می‌کند.",
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = startNotesInput,
                        onValueChange = { startNotesInput = it },
                        label = { Text("یادداشت شروع (اختیاری)") },
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startStockTakeSession(startNotesInput.ifBlank { null }) {
                            showStartDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = DarkObsidian)
                ) {
                    Text("شروع شمارش", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("انصراف", color = TextGray)
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Cancel Confirm Dialog
    if (showCancelConfirmDialog && activeSession != null) {
        val s = activeSession!!
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("لغو نشست انبارگردانی", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "آیا از لغو این نشست اطمینان دارید؟ تمام شمارش‌های این نشست بدون اعمال تغییر در موجودی انبار بایگانی خواهند شد.",
                    color = TextWhite,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelStockTakeSession(s.id) {
                            showCancelConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = TextWhite)
                ) {
                    Text("بله، لغو نشست")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("ادامه شمارش", color = TextGray)
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Review & Reconciliation Sheet/Dialog
    if (showReviewReconciliationDialog && activeSession != null) {
        val session = activeSession!!
        val discrepancies = reviewItemsList.filter {
            it.status != "MATCHED" && (it.countedStock != it.expectedStockAtStart || it.status == "NEEDS_REVIEW")
        }

        val uncountedItems = reviewItemsList.filter { !it.isCounted }
        val changedItems = reviewItemsList.filter { it.status == "NEEDS_REVIEW" || it.status == "NEW_PRODUCT_DURING_SESSION" }

        AlertDialog(
            onDismissRequest = { showReviewReconciliationDialog = false },
            title = {
                Text("تطبیق نهایی و تعدیل انبار", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uncountedItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFFF5252))
                        ) {
                            Text(
                                text = "هشدار: تعداد ${uncountedItems.size} قلم کالا هنوز شمرده نشده‌اند. جهت ثبت قطعی، باید وضعیت تمام کالاها مشخص شود.",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (changedItems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFFF9800))
                        ) {
                            Text(
                                text = "توجه: تعداد ${changedItems.size} قلم کالا در حین انبارگردانی دستخوش تغییر همزمان شده‌اند.",
                                color = Color(0xFFFFB300),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Text(
                        text = "خلاصه مغایرت‌های کشف‌شده (${discrepancies.size} قلم دارای مغایرت):",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (discrepancies.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "عالی است! هیچ مغایرتی بین موجودی دفتری و شمارش فیزیکی وجود ندارد.",
                                color = StatusGreen,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(discrepancies) { itm ->
                                val statusText = when {
                                    itm.status == "NEW_PRODUCT_DURING_SESSION" -> "کالای جدید حین انبارگردانی"
                                    itm.status == "NEEDS_REVIEW" -> "نیازمند بررسی (تغییر در حین انبارگردانی)"
                                    !itm.isCounted -> "شمارش‌نشده"
                                    itm.countedStock < itm.expectedStockAtStart -> "کسری (${itm.expectedStockAtStart - itm.countedStock})"
                                    itm.countedStock > itm.expectedStockAtStart -> "مازاد (+${itm.countedStock - itm.expectedStockAtStart})"
                                    else -> "منطبق"
                                }
                                val statusColor = when {
                                    itm.status == "NEW_PRODUCT_DURING_SESSION" || itm.status == "NEEDS_REVIEW" -> Color(0xFFFF9800)
                                    !itm.isCounted -> TextGray
                                    itm.countedStock < itm.expectedStockAtStart -> Color(0xFFFF5252)
                                    itm.countedStock > itm.expectedStockAtStart -> Color(0xFFFFB300)
                                    else -> StatusGreen
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SmokyBronze, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itm.productName, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("دفتری: ${itm.expectedStockAtStart} | شمارش: ${if (itm.isCounted) itm.countedStock else "---"}", color = TextGray, fontSize = 10.sp)
                                    }
                                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "با ثبت نهایی، موجودی اقلام در انبار کالا دقیقاً برابر تعداد شمارش‌شده تنظیم شده و گزارش حسابرسی بایگانی می‌شود.",
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyStockTakeAdjustments(session.id) { result ->
                            showReviewReconciliationDialog = false
                            result.onSuccess { applyRes ->
                                showFinalApplySuccessDialog = applyRes
                            }.onFailure { err ->
                                lastScanMessage = err.message ?: "خطا در اعمال تعدیل انبار"
                                lastScanIsSuccess = false
                            }
                        }
                    },
                    enabled = uncountedItems.isEmpty() && changedItems.isEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = DarkObsidian)
                ) {
                    Text("تایید و اعمال تعدیل انبار", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewReconciliationDialog = false }) {
                    Text("بازگشت", color = TextGray)
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Final Apply Result Dialog
    showFinalApplySuccessDialog?.let { res ->
        AlertDialog(
            onDismissRequest = { showFinalApplySuccessDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusGreen)
                    Text("انبارگردانی با موفقیت تکمیل شد", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("تعداد اقلام تعدیل‌شده در انبار: ${res.adjustedCount}", color = TextWhite, fontSize = 12.sp)
                    if (res.blockedCount > 0) {
                        Text(
                            "تعداد اقلام حفظ‌شده به دلیل تغییر حین شمارش: ${res.blockedCount}",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp
                        )
                    }
                    Text("موجودی کل انبار با موفقیت به‌روزرسانی شد.", color = TextGray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFinalApplySuccessDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = DarkObsidian)
                ) {
                    Text("تایید")
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun StockTakeItemCard(
    item: StockTakeItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onConfirmZero: () -> Unit
) {
    val statusColor = when {
        item.status == "NEW_PRODUCT_DURING_SESSION" -> Color(0xFFFF9800)
        item.status == "NEEDS_REVIEW" -> Color(0xFFFF9800)
        item.status == "ADJUSTED" -> MetallicGold
        !item.isCounted -> TextGray
        item.countedStock == item.expectedStockAtStart -> StatusGreen
        item.countedStock < item.expectedStockAtStart -> Color(0xFFFF5252)
        else -> Color(0xFFFFB300)
    }

    val statusBadgeText = when {
        item.status == "NEW_PRODUCT_DURING_SESSION" -> "کالای جدید حین انبارگردانی"
        item.status == "NEEDS_REVIEW" -> "هشدار تغییر حین شمارش"
        item.status == "ADJUSTED" -> "تعدیل‌شده"
        !item.isCounted -> "شمارش‌نشده"
        item.countedStock == item.expectedStockAtStart -> "منطبق"
        item.countedStock < item.expectedStockAtStart -> "کسری (${item.expectedStockAtStart - item.countedStock})"
        else -> "مازاد (+${item.countedStock - item.expectedStockAtStart})"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SmokyCard),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isCounted) statusColor.copy(alpha = 0.5f) else CharcoalBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.productName,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "بارکد: ${item.productBarcode} | دسته: ${item.productCategory}",
                    color = TextGray,
                    fontSize = 10.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "موجودی دفتری: ${item.expectedStockAtStart}",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusBadgeText,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick counter controls & zero confirmation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!item.isCounted) {
                    OutlinedButton(
                        onClick = onConfirmZero,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp),
                        border = BorderStroke(1.dp, CharcoalBorder),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("شمارش ۰", fontSize = 10.sp, color = TextGray)
                    }
                }

                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SmokyBronze)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "کاهش", tint = TextWhite, modifier = Modifier.size(14.dp))
                }

                Box(
                    modifier = Modifier
                        .widthIn(min = 36.dp)
                        .height(32.dp)
                        .background(DarkObsidian, RoundedCornerShape(6.dp))
                        .border(1.dp, if (item.isCounted) MetallicGold.copy(alpha = 0.5f) else CharcoalBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (item.isCounted) "${item.countedStock}" else "-",
                        color = if (item.isCounted) MetallicGold else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MetallicGold)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "افزایش", tint = DarkObsidian, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = title, color = TextGray, fontSize = 10.sp)
    }
}

private fun handleScanResult(
    result: StockTakeScanResult,
    callback: (message: String, isSuccess: Boolean) -> Unit
) {
    when (result) {
        is StockTakeScanResult.Success -> {
            callback("اسکن شد: ${result.item.productName} (شمارش: ${result.item.countedStock})", true)
        }
        is StockTakeScanResult.NotFound -> {
            callback("کالایی با بارکد ${result.barcode} در انبار یافت نشد!", false)
        }
    }
}
