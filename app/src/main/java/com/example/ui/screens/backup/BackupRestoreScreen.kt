package com.example.ui.screens.backup

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.state.BackupUiState
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val backupState by viewModel.backupUiState.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingJsonContent by remember { mutableStateOf<String?>(null) }

    // Launcher for creating backup JSON file
    val createBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { destinationUri ->
            if (backupState is BackupUiState.Success) {
                val jsonContent = (backupState as BackupUiState.Success).jsonContent ?: ""
                try {
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        output.write(jsonContent.toByteArray())
                    }
                    Toast.makeText(context, "فایل پشتیبان با موفقیت ذخیره شد", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for picking backup JSON file
    val openBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            try {
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    val jsonContent = input.bufferedReader().use { it.readText() }
                    pendingJsonContent = jsonContent
                    showRestoreConfirmDialog = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن فایل پشتیبان: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "پشتیبان‌گیری و بازیابی داده‌ها",
                        color = MetallicGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("backup_back_button")
                    ) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(MetallicGold, DarkMetallicGold))),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MetallicGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "حفاظت جامع از اطلاعات حسابداری",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "با تهیه بکاپ منظم، فاکتورها، کالاهای انبار و لیست مشتریان خود را همواره ایمن نگه دارید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }

            // Export Backup Action
            Card(
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = StatusGreen)
                        Text(
                            "تهیه نسخه پشتیبان (خروجی JSON)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "ساخت یک فایل کاملا استاندارد از کلیه داده‌های فروشگاه جهت انتقال به گوشی دیگر یا ذخیره امن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )

                    Button(
                        onClick = { viewModel.exportJsonBackup() },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_backup_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = DarkObsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ایجاد نسخه پشتیبان جدید", color = DarkObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Import Backup Action
            Card(
                colors = CardDefaults.cardColors(containerColor = SmokyCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = DarkMetallicGold)
                        Text(
                            "بازیابی از فایل پشتیبان",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "انتخاب فایل پشتیبان قبلی (.json) برای بازیابی اطلاعات در پایگاه داده فروشگاه.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )

                    OutlinedButton(
                        onClick = { openBackupFileLauncher.launch("application/json") },
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(DarkMetallicGold, MetallicGold))),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_backup_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, tint = MetallicGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("انتخاب و بازیابی فایل JSON", color = MetallicGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Backup Status Indicator
            when (backupState) {
                is BackupUiState.Processing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SmokyCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = MetallicGold, modifier = Modifier.size(24.dp))
                            Text("در حال پردازش داده‌ها ...", color = TextWhite)
                        }
                    }
                }
                is BackupUiState.Success -> {
                    val state = backupState as BackupUiState.Success
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusGreen.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(StatusGreen, StatusGreen))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(state.message, color = StatusGreen, fontWeight = FontWeight.Bold)
                            if (state.jsonContent != null) {
                                Button(
                                    onClick = {
                                        val fileName = "gildar_backup_${System.currentTimeMillis()}.json"
                                        createBackupFileLauncher.launch(fileName)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null, tint = DarkObsidian)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ذخیره فایل در حافظه گوشی", color = DarkObsidian)
                                }
                            }
                        }
                    }
                }
                is BackupUiState.Error -> {
                    val state = backupState as BackupUiState.Error
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GoldAlertOrange.copy(alpha = 0.15f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(GoldAlertOrange, GoldAlertOrange))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = GoldAlertOrange)
                            Text(state.errorMessage, color = GoldAlertOrange)
                        }
                    }
                }
                BackupUiState.Idle -> {}
            }
        }
    }

    // Confirmation Dialog for Restore
    if (showRestoreConfirmDialog && pendingJsonContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingJsonContent = null
            },
            title = {
                Text("تایید بازیابی اطلاعات", color = GoldAlertOrange, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("آیا از بازیابی اطلاعات از این فایل بکاپ اطمینان دارید؟ داده‌های موجود بهروزرسانی خواهند شد.", color = TextWhite)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingJsonContent ?: ""
                        showRestoreConfirmDialog = false
                        pendingJsonContent = null
                        viewModel.importJsonRestore(json)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAlertOrange)
                ) {
                    Text("بازیابی", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingJsonContent = null
                    }
                ) {
                    Text("انصراف", color = TextGray)
                }
            },
            containerColor = SmokyCard
        )
    }
}
