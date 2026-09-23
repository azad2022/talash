package com.example.ui.screens.hardware

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.hardware.barcode.HardwareBarcodeBus
import com.example.hardware.core.HardwareConnectionState
import com.example.hardware.core.HardwareDeviceType
import com.example.hardware.core.SerialConnectionSettings
import com.example.ui.theme.DarkObsidian
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SmokyBronze
import com.example.ui.theme.SmokyCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HardwareCenterScreen(viewModel: ShopViewModel, onBack: () -> Unit) {
    var pairedDevices by remember { mutableStateOf(viewModel.pairedBluetoothDevices()) }
    var usbDevices by remember { mutableStateOf(viewModel.usbSerialDevices()) }
    var selectedType by remember { mutableStateOf(HardwareDeviceType.SCALE) }
    var showDialog by remember { mutableStateOf(false) }
    var lastScan by remember { mutableStateOf<String?>(null) }
    var hardwareScannerEnabled by remember { mutableStateOf(false) }
    var baudRate by remember { mutableStateOf(9600) }
    var dataBits by remember { mutableStateOf(8) }
    var parity by remember { mutableStateOf(0) }
    var stopBits by remember { mutableStateOf(1) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pairedDevices = viewModel.pairedBluetoothDevices()
    }
    DisposableEffect(hardwareScannerEnabled) {
        HardwareBarcodeBus.setEnabled(hardwareScannerEnabled)
        onDispose { HardwareBarcodeBus.setEnabled(false) }
    }

    LaunchedEffect(Unit) {
        HardwareBarcodeBus.scans.collectLatest { lastScan = it }
    }

    val connectionState by viewModel.hardwareConnectionState.collectAsState()
    val stableWeight by viewModel.hardwareLatestStableWeight.collectAsState()
    val connectedDevices by viewModel.hardwareConnectedDevices.collectAsState()
    val hardwareLastError by viewModel.hardwareLastError.collectAsState()
    val connectedDeviceTypes = connectedDevices.keys

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تجهیزات و اتصال سریع", color = MetallicGold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkObsidian)
            )
        },
        containerColor = DarkObsidian
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 100.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SmokyCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("وضعیت اتصال", color = MetallicGold, fontSize = 13.sp)
                        Text(
                            when (connectionState) {
                                HardwareConnectionState.CONNECTED -> "متصل"
                                HardwareConnectionState.CONNECTING -> "در حال اتصال"
                                HardwareConnectionState.ERROR -> "خطا"
                                HardwareConnectionState.DISCONNECTED -> "قطع"
                            },
                            color = when (connectionState) {
                                HardwareConnectionState.CONNECTED -> StatusGreen
                                HardwareConnectionState.ERROR -> Color(0xFFFF5252)
                                else -> TextGray
                            }
                        )
                        hardwareLastError?.let { error ->
                            Text("خطا: " + error, color = Color(0xFFFF5252), fontSize = 11.sp)
                        }
                        if (connectedDevices.isNotEmpty()) {
                            connectedDevices.forEach { entry ->
                                val type = entry.key
                                val device = entry.value
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(hardwareTypeLabel(type) + ": " + device.name, color = TextWhite, fontSize = 11.sp)
                                    TextButton(onClick = { viewModel.disconnectHardware(type) }) {
                                        Text("قطع", color = Color(0xFFFF5252), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SmokyCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("کاربرد دستگاه Bluetooth", color = MetallicGold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            HardwareTypeButton(HardwareDeviceType.SCALE, "ترازو", Icons.Filled.Scale, selectedType) { selectedType = it }
                            HardwareTypeButton(HardwareDeviceType.RECEIPT_PRINTER, "فیش", Icons.Filled.Print, selectedType) { selectedType = it }
                            HardwareTypeButton(HardwareDeviceType.LABEL_PRINTER, "لیبل", Icons.Filled.QrCodeScanner, selectedType) { selectedType = it }
                        }
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= 31) permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                            else pairedDevices = viewModel.pairedBluetoothDevices()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Bluetooth, contentDescription = null)
                            Text("دستگاه‌های جفت‌شده")
                        }
                    }
                }
            }
            items(pairedDevices) { device ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SmokyBronze), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, color = TextWhite, fontSize = 12.sp)
                            Text(device.address, color = TextGray, fontSize = 10.sp)
                        }
                        Button(onClick = {
                            viewModel.connectBluetoothHardware(device.name, device.address, selectedType)
                            showDialog = true
                        }) { Text("اتصال", fontSize = 11.sp) }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SmokyCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("تجهیزات USB / Serial", color = MetallicGold, fontSize = 13.sp)
                        Text("تنظیمات خط سریال", color = TextWhite, fontSize = 11.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(4800, 9600, 19200, 38400, 57600, 115200).forEach { rate ->
                                Button(
                                    onClick = { baudRate = rate },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(rate.toString(), fontSize = 8.sp)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(7, 8).forEach { bits ->
                                Button(
                                    onClick = { dataBits = bits },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(bits.toString() + " bit", fontSize = 8.sp)
                                }
                            }
                            listOf(0 to "None", 2 to "Even", 1 to "Odd").forEach { (value, label) ->
                                Button(
                                    onClick = { parity = value },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(label, fontSize = 8.sp)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1 to "1 stop", 2 to "2 stop").forEach { (value, label) ->
                                Button(
                                    onClick = { stopBits = value },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp)
                                ) {
                                    Text(label, fontSize = 8.sp)
                                }
                            }
                        }

                        Button(
                            onClick = { usbDevices = viewModel.usbSerialDevices() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("جستجوی تجهیزات USB")
                        }
                    }
                }
            }

            items(usbDevices) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyBronze),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, color = TextWhite, fontSize = 12.sp)
                            Text("USB Serial • " + (device.vendorId?.toString() ?: "-") + ":" + (device.productId?.toString() ?: "-"), color = TextGray, fontSize = 10.sp)
                        }
                        Button(onClick = {
                            viewModel.connectUsbHardware(
                                device.id.toIntOrNull() ?: -1,
                                device.name,
                                selectedType,
                                SerialConnectionSettings(baudRate = baudRate, dataBits = dataBits, stopBits = stopBits, parity = parity)
                            )
                            showDialog = true
                        }) {
                            Text("اتصال", fontSize = 11.sp)
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SmokyCard)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("وزن پایدار", color = MetallicGold, fontSize = 13.sp)
                        Text(stableWeight?.grams?.stripTrailingZeros()?.toPlainString() ?: "—", color = if (stableWeight != null) StatusGreen else TextGray, fontSize = 20.sp)
                        stableWeight?.let { Text("تعداد نمونه پایدار: " + it.samples.toString(), color = TextGray, fontSize = 10.sp) }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SmokyCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("بارکدخوان فیزیکی", color = MetallicGold, fontSize = 13.sp)
                        Text(
                            if (hardwareScannerEnabled) "حالت دریافت اسکن فعال است."
                            else "حالت دریافت اسکن خاموش است.",
                            color = if (hardwareScannerEnabled) StatusGreen else TextGray,
                            fontSize = 10.sp
                        )
                        Button(
                            onClick = { hardwareScannerEnabled = !hardwareScannerEnabled },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (hardwareScannerEnabled) "خاموش کردن HID" else "فعال کردن HID")
                        }
                        Text(
                            lastScan?.let { "آخرین اسکن: $it" } ?: "آخرین اسکن: —",
                            color = TextWhite,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("اتصال دستگاه", color = MetallicGold) },
            text = { Text("درخواست اتصال ارسال شد. نتیجه واقعی در وضعیت اتصال نمایش داده می‌شود.", color = TextWhite) },
            confirmButton = { Button(onClick = { showDialog = false }) { Text("باشه") } }
        )
    }
}

private fun hardwareTypeLabel(type: HardwareDeviceType): String = when (type) {
    HardwareDeviceType.SCALE -> "ترازو"
    HardwareDeviceType.BARCODE_SCANNER -> "بارکدخوان"
    HardwareDeviceType.RECEIPT_PRINTER -> "فیش‌پرینتر"
    HardwareDeviceType.LABEL_PRINTER -> "لیبل‌پرینتر"
}

@Composable
private fun HardwareTypeButton(
    type: HardwareDeviceType,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: HardwareDeviceType,
    onSelect: (HardwareDeviceType) -> Unit
) {
    OutlinedButton(onClick = { onSelect(type) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
        Icon(icon, contentDescription = label, tint = if (selected == type) MetallicGold else TextGray)
        Text(label, color = if (selected == type) MetallicGold else TextWhite, fontSize = 10.sp)
    }
}