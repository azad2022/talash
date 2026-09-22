package com.example.ui.screens.closing

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyClosing
import com.example.data.repository.TodaySummaryPreview
import com.example.ui.theme.*
import com.example.ui.util.JalaliCalendar
import com.example.ui.viewmodel.ShopViewModel
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyClosingScreen(
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val dailyClosings by viewModel.dailyClosings.collectAsState()
    val todayPreview by viewModel.todaySummaryPreview.collectAsState()
    val isLoading by viewModel.isDailyClosingLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Today, 1: History
    var showConfirmCloseDialog by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf<DailyClosing?>(null) }
    var reopenReasonInput by remember { mutableStateOf("") }
    var operationMessage by remember { mutableStateOf<String?>(null) }

    // Physical reconciliation inputs
    var physicalCashInput by remember { mutableStateOf("") }
    var physicalGoldWeightInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadTodaySummaryPreview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockClock,
                            contentDescription = null,
                            tint = MetallicGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "بستن روز طلافروشی",
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
                actions = {
                    IconButton(onClick = { viewModel.loadTodaySummaryPreview() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "بروزرسانی",
                            tint = MetallicGold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SmokyCard)
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 0) MetallicGold else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "بستن روز کاری امروز",
                        color = if (selectedTab == 0) DarkObsidian else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == 1) MetallicGold else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "سوابق روزهای بسته شده (${dailyClosings.size})",
                        color = if (selectedTab == 1) DarkObsidian else TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            operationMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SmokyBronze),
                    border = BorderStroke(1.dp, MetallicGold)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = msg, color = TextWhite, fontSize = 12.sp)
                        IconButton(onClick = { operationMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "بستن", tint = TextWhite)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // Today's Closing View
                if (isLoading && todayPreview == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MetallicGold)
                    }
                } else {
                    val preview = todayPreview
                    if (preview != null) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            item {
                                // Status banner if already closed
                                if (preview.existingClosing != null && preview.existingClosing.status == "CLOSED") {
                                    val closing = preview.existingClosing
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.5.dp, StatusGreen)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusGreen)
                                                    Text(
                                                        text = "روز کاری بسته و قفل شده است",
                                                        color = StatusGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Text(
                                                    text = "نسخه بستن: ${closing.revision}",
                                                    color = TextGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = "زمان بستن: ${JalaliCalendar.getJalaliDateTime(closing.closedAt)}",
                                                color = TextWhite,
                                                fontSize = 12.sp
                                            )
                                            closing.optionalNotes?.takeIf { it.isNotEmpty() }?.let { notes ->
                                                Text(
                                                    text = "یادداشت: $notes",
                                                    color = TextGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    showReopenDialog = closing
                                                    reopenReasonInput = ""
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicGold),
                                                border = BorderStroke(1.dp, MetallicGold),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("بازگشایی مجدد روز کاری", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                        border = BorderStroke(1.dp, CharcoalBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("وضعیت روز: در حال معامله (باز)", color = MetallicGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("تاریخ کاری: ${preview.displayedPersianDate}", color = TextGray, fontSize = 11.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                            )
                                        }
                                    }
                                }
                            }

                            // Financial Overview Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                    border = BorderStroke(1.dp, CharcoalBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "خلاصه مبادلات و فروش امروز",
                                            color = MetallicGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        HorizontalDivider(color = CharcoalBorder)

                                        ClosingMetricRow(
                                            label = "تعداد فاکتورهای صادر شده:",
                                            value = "${preview.invoiceCount} فقره",
                                            icon = Icons.Filled.ReceiptLong
                                        )
                                        ClosingMetricRow(
                                            label = "مجموع مبلغ فروش فاکتورها:",
                                            value = "${viewModel.formatCurrency(preview.salesTotal)} تومان",
                                            icon = Icons.Filled.Payments,
                                            valueColor = MetallicGold
                                        )
                                        ClosingMetricRow(
                                            label = "دریافتی نقد فاکتورها:",
                                            value = "${viewModel.formatCurrency(preview.paidTotal)} تومان",
                                            icon = Icons.Filled.AccountBalanceWallet
                                        )
                                        ClosingMetricRow(
                                            label = "اقساط جدید ثبت‌شده امروز:",
                                            value = "${viewModel.formatCurrency(preview.installmentCreatedTotal)} تومان",
                                            icon = Icons.Filled.CalendarMonth
                                        )
                                        ClosingMetricRow(
                                            label = "اقساط وصول‌شده در امروز:",
                                            value = "${viewModel.formatCurrency(preview.installmentCollectedTotal)} تومان",
                                            icon = Icons.Filled.PriceCheck,
                                            valueColor = StatusGreen
                                        )
                                        ClosingMetricRow(
                                            label = "تعداد اقساط معوق کل مشتریان:",
                                            value = "${preview.overdueInstallmentCount} قسط معوق",
                                            icon = Icons.Filled.Warning,
                                            valueColor = if (preview.overdueInstallmentCount > 0) Color(0xFFFF5252) else StatusGreen
                                        )
                                    }
                                }
                            }

                            // Inventory & Valuation Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                    border = BorderStroke(1.dp, CharcoalBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "موجودی و ارزش‌گذاری انبار طلا",
                                            color = MetallicGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        HorizontalDivider(color = CharcoalBorder)

                                        ClosingMetricRow(
                                            label = "تعداد اقلام فیزیکی انبار:",
                                            value = "${preview.inventoryPieceCount} عدد",
                                            icon = Icons.Filled.ShoppingBag
                                        )
                                        ClosingMetricRow(
                                            label = "مجموع وزن طلای موجود در انبار:",
                                            value = "${viewModel.formatWeight(preview.inventoryWeight)} گرم",
                                            icon = Icons.Filled.Scale,
                                            valueColor = MetallicGold
                                        )
                                        ClosingMetricRow(
                                            label = "نرخ مبنای هر گرم ۱۸ عیار امروز:",
                                            value = "${viewModel.formatCurrency(preview.goldRateAtClose)} تومان",
                                            icon = Icons.Filled.TrendingUp
                                        )
                                        ClosingMetricRow(
                                            label = "ارزش کل طلای انبار در نرخ امروز:",
                                            value = "${viewModel.formatCurrency(preview.inventoryValue)} تومان",
                                            icon = Icons.Filled.Diamond,
                                            valueColor = MetallicGold
                                        )
                                        ClosingMetricRow(
                                            label = "سفارشات فعال تعمیرات:",
                                            value = "${preview.openRepairsCount} سفارش",
                                            icon = Icons.Filled.Handyman
                                        )
                                    }
                                }
                            }

                            // Physical Reconciliation Form (Cash & Gold)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                    border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "مغایرت‌گیری فیزیکی گاوصندوق و ویترین (اختیاری)",
                                            color = MetallicGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "جهت تطبیق وجوه نقد گاوصندوق و ترازوی فیزیکی با ارقام دفتری سیستم، می‌توانید مقادیر واقعی را وارد کنید:",
                                            color = TextGray,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )

                                        OutlinedTextField(
                                            value = physicalCashInput,
                                            onValueChange = { physicalCashInput = it },
                                            label = { Text("موجودی فیزیکی نقد در گاوصندوق (تومان)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                                        val physCash = physicalCashInput.toDoubleOrNull()?.let { BigDecimal.valueOf(it) }
                                        if (physCash != null) {
                                            val expectedCash = preview.paidTotal.add(preview.installmentCollectedTotal)
                                            val diff = physCash.subtract(expectedCash)
                                            val diffColor = if (diff >= BigDecimal.ZERO) StatusGreen else Color(0xFFFF5252)
                                            Text(
                                                text = "مغایرت نقد (فیزیکی - سیستم): ${viewModel.formatCurrency(diff)} تومان",
                                                color = diffColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedTextField(
                                            value = physicalGoldWeightInput,
                                            onValueChange = { physicalGoldWeightInput = it },
                                            label = { Text("وزن فیزیکی طلای موجود طبق ترازو (گرم)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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

                                        val physGold = physicalGoldWeightInput.toDoubleOrNull()?.let { BigDecimal.valueOf(it) }
                                        if (physGold != null) {
                                            val diffGold = physGold.subtract(preview.inventoryWeight)
                                            val diffColor = if (diffGold.abs() < BigDecimal("0.05")) StatusGreen else Color(0xFFFF5252)
                                            Text(
                                                text = "مغایرت وزن طلا: ${viewModel.formatWeight(diffGold)} گرم",
                                                color = diffColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedTextField(
                                            value = notesInput,
                                            onValueChange = { notesInput = it },
                                            label = { Text("یادداشت روزانه طلافروشی") },
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

                                        Button(
                                            onClick = { showConfirmCloseDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MetallicGold,
                                                contentColor = DarkObsidian
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Lock, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (preview.existingClosing?.status == "CLOSED") "بروزرسانی بستن روز" else "بستن رسمی و قفل روز کاری",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // History of closed days
                if (dailyClosings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تاکنون هیچ روز کاری بسته نشده است.",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(dailyClosings) { closing ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (closing.status == "CLOSED") CharcoalBorder else Color(0xFFFFB300)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "روز کاری: ${closing.displayedPersianDate}",
                                            color = MetallicGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (closing.status == "CLOSED") StatusGreen.copy(alpha = 0.2f) else Color(0xFFFFB300).copy(alpha = 0.2f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = if (closing.status == "CLOSED") "بسته شده" else "بازگشایی شده",
                                                color = if (closing.status == "CLOSED") StatusGreen else Color(0xFFFFB300),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "بسته شده در: ${JalaliCalendar.getJalaliDateTime(closing.closedAt)}",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("فروش کل:", color = TextGray, fontSize = 11.sp)
                                        Text("${viewModel.formatCurrency(closing.salesTotal)} تومان", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("دریافتی نقد:", color = TextGray, fontSize = 11.sp)
                                        Text("${viewModel.formatCurrency(closing.paidTotal)} تومان", color = TextWhite, fontSize = 11.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("تعداد فاکتورها:", color = TextGray, fontSize = 11.sp)
                                        Text("${closing.invoiceCount} عدد", color = TextWhite, fontSize = 11.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("وزن کل طلای انبار:", color = TextGray, fontSize = 11.sp)
                                        Text("${viewModel.formatWeight(closing.inventoryWeight)} گرم", color = MetallicGold, fontSize = 11.sp)
                                    }

                                    if (closing.status == "CLOSED") {
                                        OutlinedButton(
                                            onClick = {
                                                showReopenDialog = closing
                                                reopenReasonInput = ""
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicGold),
                                            border = BorderStroke(1.dp, MetallicGold),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("بازگشایی روز", fontSize = 11.sp)
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

    // Confirmation Dialog for Closing
    if (showConfirmCloseDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmCloseDialog = false },
            title = {
                Text(
                    text = "تایید بستن روز کاری",
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "آیا از بستن رسمی روز کاری اطمینان دارید؟ تمام شاخص‌های مالی و تراز انبار به عنوان سند قطعی ثبت می‌شوند.",
                    color = TextWhite,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val physCash = physicalCashInput.toDoubleOrNull()?.let { BigDecimal.valueOf(it) }
                        val physGold = physicalGoldWeightInput.toDoubleOrNull()?.let { BigDecimal.valueOf(it) }
                        viewModel.closeDay(
                            physicalCash = physCash,
                            physicalGoldWeight = physGold,
                            notes = notesInput.ifBlank { null }
                        ) { result ->
                            showConfirmCloseDialog = false
                            if (result.isSuccess) {
                                operationMessage = "روز کاری با موفقیت بسته و ثبت گردید."
                            } else {
                                operationMessage = "خطا در بستن روز: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = DarkObsidian)
                ) {
                    Text("بستن قطعی روز", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCloseDialog = false }) {
                    Text("انصراف", color = TextGray)
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Reopen Day Dialog
    showReopenDialog?.let { closingToReopen ->
        AlertDialog(
            onDismissRequest = { showReopenDialog = null },
            title = {
                Text(
                    text = "بازگشایی روز کاری ${closingToReopen.displayedPersianDate}",
                    color = MetallicGold,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "جهت ثبت و ممیزی در سیستم، لطفاً دلیل بازگشایی روز را ذکر کنید:",
                        color = TextWhite,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = reopenReasonInput,
                        onValueChange = { reopenReasonInput = it },
                        label = { Text("دلیل بازگشایی (الزامی)") },
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
                        if (reopenReasonInput.isBlank()) return@Button
                        viewModel.reopenDay(closingToReopen.id, reopenReasonInput) { result ->
                            showReopenDialog = null
                            if (result.isSuccess) {
                                operationMessage = "روز کاری با موفقیت بازگشایی گردید."
                            } else {
                                operationMessage = "خطا در بازگشایی روز: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    enabled = reopenReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = DarkObsidian)
                ) {
                    Text("تایید و بازگشایی", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenDialog = null }) {
                    Text("انصراف", color = TextGray)
                }
            },
            containerColor = SmokyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun ClosingMetricRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = TextWhite
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MetallicGold.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Text(text = label, color = TextGray, fontSize = 12.sp)
        }
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
