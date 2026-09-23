package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.*
import com.example.hardware.HardwareManager
import com.example.hardware.PairedBluetoothDevice
import com.example.hardware.core.HardwareConnectionState
import com.example.hardware.core.HardwareDeviceType
import com.example.hardware.core.HardwareResult
import com.example.hardware.print.EscPosEncoder
import com.example.hardware.print.GoldLabelFormatter
import com.example.hardware.print.ReceiptFormatter
import com.example.hardware.print.ReceiptRasterRenderer
import com.example.hardware.print.GoldLabelZplEncoder
import com.example.hardware.print.LabelPrinterProtocol
import com.example.hardware.scale.WeightComparison
import com.example.hardware.scale.WeightComparisonEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ShopViewModel(private val repository: ShopRepository, appContext: Context? = null) : ViewModel() {

    private val hardwareManager = appContext?.applicationContext?.let { HardwareManager(it, viewModelScope) }
    private val defaultHardwareState = MutableStateFlow(HardwareConnectionState.DISCONNECTED)
    private val defaultStableWeight = MutableStateFlow<com.example.hardware.core.StableWeight?>(null)
    val hardwareConnectionState: StateFlow<HardwareConnectionState> = hardwareManager?.connectionState ?: defaultHardwareState
    val hardwareConnectedDevice: StateFlow<com.example.hardware.core.HardwareDevice?> = hardwareManager?.connectedDevice ?: MutableStateFlow(null)
    val hardwareLatestStableWeight: StateFlow<com.example.hardware.core.StableWeight?> = hardwareManager?.latestStableWeight ?: defaultStableWeight
    val hardwareLastError: StateFlow<String?> = hardwareManager?.lastError ?: MutableStateFlow(null)
    private val defaultComparison = MutableStateFlow<WeightComparison?>(null)
    var lastWeightComparison: WeightComparison? by mutableStateOf(null)
        private set


    // --- SECURITY & PIN AUTHFLOW ---
    var pinError by mutableStateOf(false)

    // --- DASHBOARD VISIBILITY MASK ---
    var isDashboardValuesHidden by mutableStateOf(false)
        private set

    fun toggleDashboardValuesVisibility() {
        isDashboardValuesHidden = !isDashboardValuesHidden
    }
    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isPinSetupRequired = MutableStateFlow(false)
    val isPinSetupRequired: StateFlow<Boolean> = _isPinSetupRequired.asStateFlow()

    fun skipPinSetup() {
        _isAuthenticated.value = true
        _isPinSetupRequired.value = false
    }

    // --- STATE FLOWS FROM DB ---
    val userConfig: StateFlow<User?> = repository.userConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val customers: StateFlow<List<Customer>> = repository.customers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products: StateFlow<List<Product>> = repository.products.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val invoices: StateFlow<List<InvoiceWithDetails>> = repository.invoices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val installments: StateFlow<List<Installment>> = repository.installments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val repairs: StateFlow<List<RepairWithCustomer>> = repository.repairs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<AuditLog>> = repository.logs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- INTEGRATED BRSApi.ir GOLD PRICE ONLINE CONFIG ---
    var goldPriceMode by mutableStateOf(repository.getGoldPriceMode()) // "OFFLINE" or "ONLINE"
        private set

    var goldPriceApiKey by mutableStateOf(repository.getGoldPriceApiKey())
        private set

    var isFetchingOnlineGoldPrice by mutableStateOf(false)
        private set

    var isOnlineGoldPriceSuccess by mutableStateOf(false)
        private set

    var onlineGoldPriceError by mutableStateOf<String?>(null)
        private set

    // Individual baseline rates
    var rateGold24k by mutableStateOf(repository.getRate("rate_gold_24k", 0.0))
    var rateGoldMelted by mutableStateOf(repository.getRate("rate_gold_melted", 0.0))
    var rateGoldOunce by mutableStateOf(repository.getRate("rate_gold_ounce", 0.0))
    var rateCoin1g by mutableStateOf(repository.getRate("rate_coin_1g", 0.0))
    var rateCoinQuarter by mutableStateOf(repository.getRate("rate_coin_quarter", 0.0))
    var rateCoinHalf by mutableStateOf(repository.getRate("rate_coin_half", 0.0))
    var rateCoinEmami by mutableStateOf(repository.getRate("rate_coin_emami", 0.0))
    var rateCoinBahar by mutableStateOf(repository.getRate("rate_coin_bahar", 0.0))
    var rateCurrencyUsd by mutableStateOf(repository.getRate("rate_currency_usd", 0.0))
    var rateCurrencyTether by mutableStateOf(repository.getRate("rate_currency_tether", 0.0))
    var rateCurrencyEur by mutableStateOf(repository.getRate("rate_currency_eur", 0.0))
    var rateCurrencyAed by mutableStateOf(repository.getRate("rate_currency_aed", 0.0))
    var rateCurrencyGbp by mutableStateOf(repository.getRate("rate_currency_gbp", 0.0))

    fun updateRateSetting(key: String, value: Double) {
        repository.setRate(key, value)
        when (key) {
            "rate_gold_24k" -> rateGold24k = value
            "rate_gold_melted" -> rateGoldMelted = value
            "rate_gold_ounce" -> rateGoldOunce = value
            "rate_coin_1g" -> rateCoin1g = value
            "rate_coin_quarter" -> rateCoinQuarter = value
            "rate_coin_half" -> rateCoinHalf = value
            "rate_coin_emami" -> rateCoinEmami = value
            "rate_coin_bahar" -> rateCoinBahar = value
            "rate_currency_usd" -> rateCurrencyUsd = value
            "rate_currency_tether" -> rateCurrencyTether = value
            "rate_currency_eur" -> rateCurrencyEur = value
            "rate_currency_aed" -> rateCurrencyAed = value
            "rate_currency_gbp" -> rateCurrencyGbp = value
        }
    }

    fun updateGoldPriceMode(mode: String) {
        goldPriceMode = mode
        repository.setGoldPriceMode(mode)
        if (mode == "ONLINE" && goldPriceApiKey.isNotEmpty()) {
            syncOnlineGoldPrice()
        }
    }

    fun updateGoldPriceApiKey(key: String) {
        goldPriceApiKey = key
        repository.setGoldPriceApiKey(key)
        if (goldPriceMode == "ONLINE" && key.isNotEmpty()) {
            syncOnlineGoldPrice()
        }
    }

    fun syncOnlineGoldPrice() {
        if (goldPriceApiKey.isEmpty()) {
            onlineGoldPriceError = "کلید API هنوز تنظیم نشده است. ابتدا کلید خود را وارد کنید."
            isOnlineGoldPriceSuccess = false
            return
        }
        viewModelScope.launch {
            isFetchingOnlineGoldPrice = true
            onlineGoldPriceError = null
            isOnlineGoldPriceSuccess = false
            try {
                // Fetch 18k Price
                val fetchedPrice = repository.fetchOnlineGoldPrice(goldPriceApiKey)
                if (fetchedPrice > 0) {
                    updateGoldPrice(fetchedPrice)
                }

                // Fetch multi asset rates (all 13 categories)
                val fullRates = repository.fetchOnlineRates(goldPriceApiKey)
                fullRates.forEach { (key, priceVal) ->
                    if (priceVal > 0) {
                        updateRateSetting(key, priceVal)
                    }
                }

                isOnlineGoldPriceSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
                onlineGoldPriceError = "خطا در اتصال: ${e.localizedMessage ?: e.message ?: "خطای ناشناخته شبکه"}"
                isOnlineGoldPriceSuccess = false
            } finally {
                isFetchingOnlineGoldPrice = false
            }
        }
    }

    init {
        viewModelScope.launch {
            val user = repository.getOrInitializeUser()
            _isPinSetupRequired.value = user.pinHash.isEmpty()
            _isAuthenticated.value = user.pinHash.isEmpty() // Auto authenticate if no pin set
            calcGoldPriceToday = if (user.dailyGoldPrice > BigDecimal.ZERO) user.dailyGoldPrice.toLong().toString() else "0"
            repository.logAction("APP_LAUNCH", "ورود به پایگاه داده گیلدار")
            
            // Auto sync online gold price if config is ONLINE
            if (goldPriceMode == "ONLINE" && goldPriceApiKey.isNotEmpty()) {
                syncOnlineGoldPrice()
            }
        }
    }

    // --- SECURITY ACTIONS ---
    fun registerPin(pin: String) {
        viewModelScope.launch {
            val hash = hashString(pin)
            val updatedUser = repository.getOrInitializeUser().copy(pinHash = hash)
            repository.updateUser(updatedUser)
            _isPinSetupRequired.value = false
            _isAuthenticated.value = true
            repository.logAction("SET_PIN", "تعریف رمز عبور امن برای نرم افزار")
        }
    }

    fun verifyPin(pin: String): Boolean {
        var match = false
        viewModelScope.launch {
            val user = repository.getOrInitializeUser()
            val hash = hashString(pin)
            if (user.pinHash == hash) {
                _isAuthenticated.value = true
                pinError = false
                match = true
                repository.logAction("LOGIN_SUCCESS", "ورود موفق به سیستم با رمز عبور")
            } else {
                pinError = true
                repository.logAction("LOGIN_FAIL", "تلاش ناموفق برای ورود به سیستم")
            }
        }
        return match
    }

    fun logout() {
        viewModelScope.launch {
            _isAuthenticated.value = false
            repository.logAction("LOGOUT", "خروج کاربر از سیستم")
        }
    }

    // --- CALCULATOR LIVE STATE (Persian Style) ---
    var calcWeight by mutableStateOf("1.0")
    var calcKarat by mutableStateOf(18)
    var calcGoldPriceToday by mutableStateOf("0") // Raw string to manage in Tomans
    var calcWageValue by mutableStateOf("10") // Per gram or percent
    var calcWageType by mutableStateOf("PERCENT") // PERCENT or FIXED
    var calcDiscount by mutableStateOf("0")
    var calcTaxRate by mutableStateOf("9")

    // Derived values computed in real-time with standard precision via CalculateGoldPriceUseCase
    val calcResult: com.example.domain.usecase.GoldCalculationResult
        get() {
            val w = calcWeight.toDoubleOrNull() ?: 0.0
            val p = calcGoldPriceToday.toDoubleOrNull() ?: 0.0
            val wv = calcWageValue.toDoubleOrNull() ?: 0.0
            val taxRateVal = calcTaxRate.toDoubleOrNull() ?: 9.0
            return calculateGoldPriceUseCase.execute(
                weightGram = w,
                karat = calcKarat,
                wagePrice = wv,
                wageType = calcWageType,
                goldPricePerGram18k = p,
                profitPercent = 7.0,
                taxPercent = taxRateVal
            )
        }

    val calcBaseGoldPrice: Double get() = calcResult.baseGoldPrice
    val calcWagePrice: Double get() = calcResult.wageAmount
    val calcDealerProfit: Double get() = calcResult.profitAmount
    val calcTaxAndDuty: Double get() = calcResult.taxAmount
    val calcFinalAmount: Double
        get() {
            val disc = calcDiscount.toDoubleOrNull() ?: 0.0
            return (calcResult.totalPrice - disc).coerceAtLeast(0.0)
        }

    // --- CALCULATOR CONVERSION TO INVOICE CART ---
    fun loadCalculatorPriceToAppConfig() {
        viewModelScope.launch {
            val todayPrice = calcGoldPriceToday.toDoubleOrNull() ?: 0.0
            val taxVal = calcTaxRate.toDoubleOrNull() ?: 9.0
            val user = repository.getOrInitializeUser().copy(
                dailyGoldPrice = BigDecimal.valueOf(todayPrice),
                taxPercent = BigDecimal.valueOf(taxVal)
            )
            repository.updateUser(user)
        }
    }

    fun updateGoldPrice(newPrice: Double) {
        viewModelScope.launch {
            val user = repository.getOrInitializeUser().copy(dailyGoldPrice = BigDecimal.valueOf(newPrice))
            repository.updateUser(user)
        }
    }

    // --- CUSTOMERS ---
    fun addCustomer(name: String, phone: String, address: String, nationalId: String = "", about: String = "") {
        viewModelScope.launch {
            val customer = Customer(name = name, phone = phone, address = address, nationalId = nationalId, about = about)
            repository.insertCustomer(customer)
        }
    }

    fun editCustomer(id: Int, name: String, phone: String, address: String, nationalId: String = "", about: String = "") {
        viewModelScope.launch {
            val customer = Customer(id = id, name = name, phone = phone, address = address, nationalId = nationalId, about = about)
            repository.insertCustomer(customer)
        }
    }

    var customerActionErrorMessage by mutableStateOf<String?>(null)

    fun deleteCustomer(customer: Customer, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteCustomer(customer)
                customerActionErrorMessage = null
            } catch (e: Exception) {
                val msg = e.message ?: "خطا در حذف مشتری"
                customerActionErrorMessage = msg
                onError(msg)
            }
        }
    }

    fun cancelInvoice(invoice: SaleInvoice, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteInvoice(invoice)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "خطا در ابطال فاکتور")
            }
        }
    }

    // --- INVENTORY ---
    fun addProduct(
        name: String, 
        category: String, 
        weight: Double, 
        karat: Int, 
        wageVal: Double, 
        wageType: String, 
        stock: Int, 
        minStock: Int, 
        purchasePrice: Double = 0.0,
        imagePath: String? = null,
        imagePath2: String? = null,
        imagePath3: String? = null,
        imagePath4: String? = null,
        imagePath5: String? = null,
        customBarcode: String = ""
    ) {
        viewModelScope.launch {
            val product = Product(
                name = name,
                category = category,
                weightGram = weight,
                karat = karat,
                wagePrice = wageVal,
                wageType = wageType,
                stock = stock,
                minStock = minStock,
                purchasePrice = purchasePrice,
                imagePath = imagePath,
                imagePath2 = imagePath2,
                imagePath3 = imagePath3,
                imagePath4 = imagePath4,
                imagePath5 = imagePath5,
                customBarcode = customBarcode
            )
            repository.insertProduct(product)
        }
    }

    fun editProduct(
        id: Int, 
        name: String, 
        category: String, 
        weight: Double, 
        karat: Int, 
        wageVal: Double, 
        wageType: String, 
        stock: Int, 
        minStock: Int, 
        purchasePrice: Double = 0.0,
        imagePath: String? = null,
        imagePath2: String? = null,
        imagePath3: String? = null,
        imagePath4: String? = null,
        imagePath5: String? = null,
        customBarcode: String = ""
    ) {
        viewModelScope.launch {
            val product = Product(
                id = id,
                name = name,
                category = category,
                weightGram = weight,
                karat = karat,
                wagePrice = wageVal,
                wageType = wageType,
                stock = stock,
                minStock = minStock,
                purchasePrice = purchasePrice,
                imagePath = imagePath,
                imagePath2 = imagePath2,
                imagePath3 = imagePath3,
                imagePath4 = imagePath4,
                imagePath5 = imagePath5,
                customBarcode = customBarcode
            )
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun saveImageToInternalStorage(context: Context, uri: android.net.Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val directory = File(context.filesDir, "product_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "img_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- OUTSTANDING INVOICE BAG (CART ENGINE) ---
    var draftCustomer: Customer? by mutableStateOf(null)
    val draftItems = mutableStateListOf<InvoiceItemDraft>()
    var draftPaymentType by mutableStateOf("CASH") // CASH, INSTALLMENT
    var draftDiscountInput by mutableStateOf("0")
    var draftPrepaymentInput by mutableStateOf("0")
    var draftInstallmentsCountInput by mutableStateOf("3")

    data class InvoiceItemDraft(
        val product: Product,
        val qty: Int,
        val customGramPrice: Double,
        val exactSalePrice: Double,
        val customWeight: BigDecimal? = null
    )

    fun clearInvoiceCart() {
        draftCustomer = null
        draftItems.clear()
        draftPaymentType = "CASH"
        draftDiscountInput = "0"
        draftPrepaymentInput = "0"
        draftInstallmentsCountInput = "3"
    }

    fun addItemToDraft(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val user = repository.getOrInitializeUser()
            val estimatedPrice = product.calculateAssetValue(
                dailyPrice18k = user.dailyGoldPrice.toDouble(),
                rateGold24k = rateGold24k,
                rateGoldMelted = rateGoldMelted,
                rateGoldOunce = rateGoldOunce,
                rateCoin1g = rateCoin1g,
                rateCoinQuarter = rateCoinQuarter,
                rateCoinHalf = rateCoinHalf,
                rateCoinEmami = rateCoinEmami,
                rateCoinBahar = rateCoinBahar,
                rateCurrencyUsd = rateCurrencyUsd,
                rateCurrencyTether = rateCurrencyTether,
                rateCurrencyEur = rateCurrencyEur,
                rateCurrencyAed = rateCurrencyAed,
                rateCurrencyGbp = rateCurrencyGbp,
                taxRate = user.taxPercent.toDouble()
            )
            val unitPriceBd = BigDecimal.valueOf(estimatedPrice).setScale(0, RoundingMode.HALF_UP)
            val index = draftItems.indexOfFirst { it.product.id == product.id }
            if (index >= 0) {
                val current = draftItems[index]
                val newQty = current.qty + quantity
                val newTotalBd = unitPriceBd.multiply(BigDecimal.valueOf(newQty.toLong())).setScale(0, RoundingMode.HALF_UP)
                draftItems[index] = current.copy(qty = newQty, exactSalePrice = newTotalBd.toDouble())
            } else {
                val totalBd = unitPriceBd.multiply(BigDecimal.valueOf(quantity.toLong())).setScale(0, RoundingMode.HALF_UP)
                val customGramPriceBd = if (product.weightGram > BigDecimal.ZERO) {
                    unitPriceBd.divide(product.weightGram.setScale(3, RoundingMode.HALF_UP), 0, RoundingMode.HALF_UP)
                } else {
                    unitPriceBd
                }
                draftItems.add(
                    InvoiceItemDraft(
                        product = product,
                        qty = quantity,
                        customGramPrice = customGramPriceBd.toDouble(),
                        exactSalePrice = totalBd.toDouble(),
                        customWeight = null
                    )
                )
            }
        }
    }

    fun applyStableWeightToDraft(productId: Int, measuredWeight: BigDecimal) {
        if (measuredWeight <= BigDecimal.ZERO) return
        viewModelScope.launch {
            val index = draftItems.indexOfFirst { it.product.id == productId }
            if (index < 0) return@launch
            val current = draftItems[index]
            val user = repository.getOrInitializeUser()
            val weightedProduct = current.product.copy(weightGram = measuredWeight)
            val unitPrice = weightedProduct.calculateAssetValue(
                dailyPrice18k = user.dailyGoldPrice.toDouble(),
                rateGold24k = rateGold24k,
                rateGoldMelted = rateGoldMelted,
                rateGoldOunce = rateGoldOunce,
                rateCoin1g = rateCoin1g,
                rateCoinQuarter = rateCoinQuarter,
                rateCoinHalf = rateCoinHalf,
                rateCoinEmami = rateCoinEmami,
                rateCoinBahar = rateCoinBahar,
                rateCurrencyUsd = rateCurrencyUsd,
                rateCurrencyTether = rateCurrencyTether,
                rateCurrencyEur = rateCurrencyEur,
                rateCurrencyAed = rateCurrencyAed,
                rateCurrencyGbp = rateCurrencyGbp,
                taxRate = user.taxPercent.toDouble()
            )
            val unitPriceBd = BigDecimal.valueOf(unitPrice).setScale(0, RoundingMode.HALF_UP)
            val total = unitPriceBd.multiply(BigDecimal.valueOf(current.qty.toLong())).setScale(0, RoundingMode.HALF_UP)
            draftItems[index] = current.copy(
                customGramPrice = if (measuredWeight > BigDecimal.ZERO) unitPriceBd.divide(measuredWeight, 0, RoundingMode.HALF_UP).toDouble() else current.customGramPrice,
                exactSalePrice = total.toDouble(),
                customWeight = measuredWeight
            )
        }
    }

    val draftCartTotalAmount: Double
        get() {
            val totalBd = draftItems.fold(java.math.BigDecimal.ZERO) { acc, item ->
                acc.add(java.math.BigDecimal.valueOf(item.exactSalePrice).setScale(0, java.math.RoundingMode.HALF_UP))
            }
            return totalBd.toDouble()
        }

    val draftFinalPriceAfterDiscount: Double
        get() {
            val subtotalBd = java.math.BigDecimal.valueOf(draftCartTotalAmount).setScale(0, java.math.RoundingMode.HALF_UP)
            val discVal = draftDiscountInput.toDoubleOrNull() ?: 0.0
            val discBd = java.math.BigDecimal.valueOf(discVal).setScale(0, java.math.RoundingMode.HALF_UP)
            return subtotalBd.subtract(discBd).max(java.math.BigDecimal.ZERO).toDouble()
        }

    fun submitCurrentDraftInvoice(
        onError: ((String) -> Unit)? = null,
        onSuccess: (Int) -> Unit
    ): Boolean {
        if (draftItems.isEmpty()) return false
        val customerId = draftCustomer?.id ?: 0

        val discVal = draftDiscountInput.toDoubleOrNull() ?: 0.0
        val prepaymentVal = draftPrepaymentInput.toDoubleOrNull() ?: 0.0
        val instCountVal = draftInstallmentsCountInput.toIntOrNull() ?: 0

        viewModelScope.launch {
            try {
                val user = repository.getOrInitializeUser()

                // Convert drafts to SaleItem deterministically
                val itemsToSave = draftItems.map { draft ->
                    val draftTotalBd = java.math.BigDecimal.valueOf(draft.exactSalePrice).setScale(0, java.math.RoundingMode.HALF_UP)
                    val qtyBd = java.math.BigDecimal.valueOf(draft.qty.toLong())
                    val unitPriceBd = draftTotalBd.divide(qtyBd, 0, java.math.RoundingMode.HALF_UP)
                    SaleItem(
                        invoiceId = 0,
                        productId = draft.product.id,
                        quantity = draft.qty,
                        unitPrice = unitPriceBd,
                        total = draftTotalBd,
                        customWeight = draft.customWeight ?: draft.product.weightGram,
                        customName = draft.product.name
                    )
                }

                // Subtotal = exact sum of item totals
                val subtotalBd = itemsToSave.fold(java.math.BigDecimal.ZERO) { acc, item ->
                    acc.add(item.total.setScale(0, java.math.RoundingMode.HALF_UP))
                }

                val discBd = java.math.BigDecimal.valueOf(discVal).setScale(0, java.math.RoundingMode.HALF_UP)
                val finalPayableBd = subtotalBd.subtract(discBd).max(java.math.BigDecimal.ZERO)
                val prepaymentBd = java.math.BigDecimal.valueOf(prepaymentVal).setScale(0, java.math.RoundingMode.HALF_UP)
                val remainingBalanceBd = finalPayableBd.subtract(prepaymentBd).max(java.math.BigDecimal.ZERO)

                // Back-calculated tax using BigDecimal
                val taxEst = invoiceCalculatorUseCase.calculateBackTax(
                    totalAmount = finalPayableBd,
                    taxPercent = user.taxPercent
                )

                val saleInvoice = SaleInvoice(
                    customerId = customerId,
                    totalAmount = finalPayableBd,
                    discount = discBd,
                    tax = taxEst,
                    paidAmount = if (draftPaymentType == "CASH") finalPayableBd else prepaymentBd,
                    paymentType = draftPaymentType,
                    installmentsCount = instCountVal,
                    prepayment = prepaymentBd
                )

                // Create installments with exact remainder distribution
                val installmentsList = mutableListOf<Installment>()
                if (draftPaymentType == "INSTALLMENT" && instCountVal > 0) {
                    val installmentAmounts = invoiceCalculatorUseCase.calculateInstallments(
                        remainingAmount = remainingBalanceBd,
                        installmentsCount = instCountVal
                    )
                    val calendar = Calendar.getInstance()
                    for (amount in installmentAmounts) {
                        calendar.add(Calendar.MONTH, 1)
                        installmentsList.add(
                            Installment(
                                invoiceId = 0,
                                dueDate = calendar.timeInMillis,
                                amount = amount,
                                paid = false
                            )
                        )
                    }
                }

                val generatedInvoiceId = repository.createInvoice(
                    invoice = saleInvoice,
                    items = itemsToSave,
                    installments = installmentsList
                ).toInt()

                clearInvoiceCart()
                onSuccess(generatedInvoiceId)
            } catch (e: Exception) {
                onError?.invoke(e.localizedMessage ?: e.message ?: "خطا در ثبت فاکتور")
            }
        }

        return true
    }

    fun showInvoiceReceiptSimulationById(context: Context, invoiceId: Int) {
        viewModelScope.launch {
            repository.getInvoiceById(invoiceId).take(1).collect { invoice ->
                if (invoice != null) {
                    queueInvoicePrintReceipt(context, invoice)
                }
            }
        }
    }

    // --- INSTALLMENTS PAYMENTS ---
    fun toggleInstallmentPaid(id: Int, currentState: Boolean) {
        viewModelScope.launch {
            repository.payInstallment(id, !currentState)
        }
    }

    // --- REPAIRS ---
    fun addRepair(customerId: Int, description: String, estimatedCost: Double, upfront: Double) {
        viewModelScope.launch {
            val r = Repair(
                customerId = customerId,
                description = description,
                estimatedCost = estimatedCost,
                upfrontPayment = upfront,
                status = "PENDING_APPROVAL"
            )
            repository.insertRepair(r)
        }
    }

    fun updateRepairStatusFlow(repairId: Int, status: String) {
        viewModelScope.launch {
            repository.updateRepairStatus(repairId, status)
        }
    }

    fun deleteRepairRecord(repair: Repair) {
        viewModelScope.launch {
            repository.deleteRepair(repair)
        }
    }

    // --- AUDIT SYSTEM LOGGING ---
    fun logEvent(tag: String, text: String) {
        viewModelScope.launch {
            repository.logAction(tag, text)
        }
    }

    // --- SYSTEM RESET ---
    fun wipeSoftwareDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            _isPinSetupRequired.value = true
            _isAuthenticated.value = true
        }
    }

    // --- BLUETOOTH THERMAL PRINTER PREVIEW GENERATION ---
    var activePrintJobPayload by mutableStateOf<String?>(null)
    var isShowingPrinterReceiptSimulation by mutableStateOf(false)

    fun queueInvoicePrintReceipt(context: Context, invoice: InvoiceWithDetails) {
        val prefs = context.getSharedPreferences("receipt_prefs", Context.MODE_PRIVATE)
        val profile = com.example.hardware.print.ReceiptProfile(
            shopName = prefs.getString("receipt_shop_name", "گالری طلای گیلدار (شعبه مرکزی)") ?: "گالری طلای گیلدار (شعبه مرکزی)",
            title = prefs.getString("receipt_title", "فاکتور فروش معتبر کالا") ?: "فاکتور فروش معتبر کالا",
            footer = prefs.getString("receipt_footer", "از خرید و حسن انتخاب شما سپاسگزاریم.") ?: "از خرید و حسن انتخاب شما سپاسگزاریم.",
            address = prefs.getString("receipt_address", "آدرس گالری") ?: "آدرس گالری"
        )
        activePrintJobPayload = ReceiptFormatter.format(
            invoice = invoice,
            productsById = products.value.associateBy { it.id },
            profile = profile
        )
        isShowingPrinterReceiptSimulation = true
        viewModelScope.launch {
            repository.logAction("RECEIPT_PREVIEW_READY", "پیش‌نمایش رسید داده‌محور فاکتور شماره ${{invoice.invoice.id}")
        }
    }

    fun pairedBluetoothDevices(): List<PairedBluetoothDevice> =
        hardwareManager?.pairedBluetoothDevices() ?: emptyList()

    fun connectBluetoothHardware(
        name: String,
        address: String,
        type: HardwareDeviceType
    ) {
        hardwareManager?.connectBluetooth(name, address, type)
    }

    fun usbSerialDevices(): List<com.example.hardware.core.HardwareDevice> =
        hardwareManager?.usbSerialDevices().orEmpty()

    fun connectUsbHardware(
        deviceId: Int,
        name: String,
        type: HardwareDeviceType
    ) {
        hardwareManager?.connectUsb(deviceId, name, type)
    }

    fun disconnectHardware() {
        hardwareManager?.let { manager ->
            viewModelScope.launch { manager.disconnect() }
        }
    }

    fun printReceiptTextToHardware(payload: String, onResult: (HardwareResult<Unit>) -> Unit = {}) {
        if (payload.isBlank()) {
            onResult(HardwareResult.Failure("محتوای رسید برای چاپ خالی است."))
            return
        }
        hardwareManager?.writeFor(
            HardwareDeviceType.RECEIPT_PRINTER,
            EscPosEncoder.encodeText(payload),
            onResult
        ) ?: onResult(HardwareResult.Failure("مدیریت تجهیزات سخت‌افزاری در دسترس نیست."))
    }

    fun printReceiptRasterToHardware(payload: String, onResult: (HardwareResult<Unit>) -> Unit = {}) {
        if (payload.isBlank()) {
            onResult(HardwareResult.Failure("محتوای رسید برای چاپ خالی است."))
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = ReceiptRasterRenderer.render(payload)
                val bytes = EscPosEncoder.encodeRaster(bitmap)
                hardwareManager?.writeFor(HardwareDeviceType.RECEIPT_PRINTER, bytes) { result ->
                    bitmap.recycle()
                    onResult(result)
                } ?: run {
                    bitmap.recycle()
                    onResult(HardwareResult.Failure("مدیریت تجهیزات سخت‌افزاری در دسترس نیست."))
                }
            } catch (e: Exception) {
                onResult(HardwareResult.Failure("ساخت تصویر رسید برای چاپ ناموفق بود.", e))
            }
        }
    }

    fun printActiveReceiptToHardware(onResult: (HardwareResult<Unit>) -> Unit = {}) =
        printReceiptRasterToHardware(activePrintJobPayload.orEmpty(), onResult)

    fun printProductLabelToHardware(
        product: Product,
        protocol: LabelPrinterProtocol = LabelPrinterProtocol.ZPL,
        onResult: (HardwareResult<Unit>) -> Unit = {}
    ) {
        val barcode = com.example.domain.util.BarcodeResolver.getCanonicalBarcode(product)
        val bytes = when (protocol) {
            LabelPrinterProtocol.ZPL -> GoldLabelZplEncoder.encode(product, barcode)
            LabelPrinterProtocol.ESC_POS_RASTER -> EscPosEncoder.encodeText(GoldLabelFormatter.text(product, barcode))
        }
        hardwareManager?.writeFor(HardwareDeviceType.LABEL_PRINTER, bytes, onResult)
            ?: onResult(HardwareResult.Failure("مدیریت تجهیزات سخت‌افزاری در دسترس نیست."))
    }

    fun compareWeight(expected: BigDecimal, measured: BigDecimal, tolerance: BigDecimal = BigDecimal("0.005")): WeightComparison {
        val comparison = WeightComparisonEngine.compare(expected, measured, tolerance)
        lastWeightComparison = comparison
        return comparison
    }

    fun clearWeightComparison() {
        lastWeightComparison = null
    }

    // --- NATIVE PDF EXPORTER (RTL Persian Layouts to Downloads) ---
    fun exportPerformanceReportToPdf(context: Context, rangeType: String): String {
        val outputPdfFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir,
            "Gildar_Report_${rangeType}_${System.currentTimeMillis()}.pdf"
        )
        try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Centered A4 Dimensions
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Paints
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 14f
                color = AndroidColor.BLACK
                textAlign = Paint.Align.RIGHT
            }

            val titlePaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 20f
                isFakeBoldText = true
                color = AndroidColor.rgb(180, 140, 10) // Gold Leaf Accent Colour
                textAlign = Paint.Align.RIGHT
            }

            val borderPaint = Paint().apply {
                color = AndroidColor.rgb(200, 180, 50)
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }

            // Draw clean layout border
            canvas.drawRect(20f, 20f, 575f, 822f, borderPaint)

            // Dynamic header
            val headerText = "گزارش مالی و عملکرد گالری طلا و جواهرات گیلدار"
            canvas.drawText(headerText, 550f, 60f, titlePaint)
            
            val subtitleInfo = "نوع بازه: $rangeType - فایل گزارش آفلاین"
            canvas.drawText(subtitleInfo, 550f, 90f, textPaint)

            // Computed Stats
            val listInvoices = invoices.value
            val totalInvoicedSales = listInvoices.sumOf { it.invoice.totalAmount }
            val completedInstPayments = installments.value.filter { it.paid }.sumOf { it.amount }
            val pendingInstPayments = installments.value.filter { !it.paid }.sumOf { it.amount }
            val countInvs = listInvoices.size
            val activeRepairs = repairs.value.filter { it.repair.status != "DELIVERED" }.size

            var currentY = 150f
            val spacing = 35f

            val statLines = listOf(
                "جمع کل فاکتورهای صادر شده: ${formatCurrency(totalInvoicedSales)} تومان",
                "تعداد کل معاملات ثبت شده: $countInvs عدد",
                "اقساط معوق وصول شده: ${formatCurrency(completedInstPayments)} تومان",
                "مجموع اقساط باقیمانده بازار: ${formatCurrency(pendingInstPayments)} تومان",
                "تعداد کارهای در دست تعمیر فعال: $activeRepairs مورد",
                "تاریخ تهیه گزارش: ${com.example.ui.util.JalaliCalendar.getJalaliDateTime(System.currentTimeMillis())}"
            )

            statLines.forEach { line ->
                canvas.drawText(line, 550f, currentY, textPaint)
                currentY += spacing
            }

            // Draw dividing line
            canvas.drawLine(40f, currentY, 550f, currentY, borderPaint)
            currentY += 40f

            val tPaintCentered = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = AndroidColor.DKGRAY
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("این سند در حافظه داخلی محلی نرم افزار گیلدار به صورت آفلاین تولید شده است.", 297f, currentY, tPaintCentered)

            pdfDoc.finishPage(page)

            val fos = FileOutputStream(outputPdfFile)
            pdfDoc.writeTo(fos)
            pdfDoc.close()
            fos.close()

            viewModelScope.launch {
                repository.logAction("REPORT_PDF", "خروجی PDF گزارش عملکرد مالی بازه $rangeType")
            }
            return outputPdfFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return "ERROR: ${e.localizedMessage}"
        }
    }

    // --- SECURED OFFLINE DATABASE BACKUP & RESTORE ENGINE ---
    fun databaseManualBackup(context: Context): String {
        return try {
            val dbName = "gildar_gold_shop.db"
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) return "دیتابیس یافت نشد"

            val backupDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "GildarBackups"
            )
            if (!backupDir.exists()) backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "Gildar_Backup_$timestamp.db")

            // Copy main db
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Copy WAL/SHM helper files if present to prevent checkpoints drop
            val walFile = File(dbFile.absolutePath + "-wal")
            if (walFile.exists()) {
                val backupWal = File(backupFile.absolutePath + "-wal")
                FileInputStream(walFile).use { input ->
                    FileOutputStream(backupWal).use { output -> input.copyTo(output) }
                }
            }

            val shmFile = File(dbFile.absolutePath + "-shm")
            if (shmFile.exists()) {
                val backupShm = File(backupFile.absolutePath + "-shm")
                FileInputStream(shmFile).use { input ->
                    FileOutputStream(backupShm).use { output -> input.copyTo(output) }
                }
            }

            viewModelScope.launch {
                repository.logAction("DB_BACKUP", "پشتیبان گیری دستی در پوشه دانلودها: ${backupFile.name}")
            }
            "ذخیره شد در: دانلودها/GildarBackups/${backupFile.name}"
        } catch (e: Exception) {
            e.printStackTrace()
            "خطا در پشتیبان گیری: ${e.localizedMessage}"
        }
    }

    fun databaseManualRestore(context: Context, backupFilePath: String): Boolean {
        return try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) return false

            val dbName = "gildar_gold_shop.db"
            val dbFile = context.getDatabasePath(dbName)

            // Close DB instance first
            AppDatabase.getInstance(context).close()

            // Copy file back
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Restore wal/shm
            val backupWal = File(backupFile.absolutePath + "-wal")
            val targetWal = File(dbFile.absolutePath + "-wal")
            if (backupWal.exists()) {
                FileInputStream(backupWal).use { input ->
                    FileOutputStream(targetWal).use { output -> input.copyTo(output) }
                }
            } else if (targetWal.exists()) {
                targetWal.delete()
            }

            val backupShm = File(backupFile.absolutePath + "-shm")
            val targetShm = File(dbFile.absolutePath + "-shm")
            if (backupShm.exists()) {
                FileInputStream(backupShm).use { input ->
                    FileOutputStream(targetShm).use { output -> input.copyTo(output) }
                }
            } else if (targetShm.exists()) {
                targetShm.delete()
            }

            // Re-open DB and log
            viewModelScope.launch {
                val updatedUser = repository.getOrInitializeUser()
                _isPinSetupRequired.value = updatedUser.pinHash.isEmpty()
                _isAuthenticated.value = updatedUser.pinHash.isEmpty()
                repository.logAction("DB_RESTORE", "بازیابی موفقیت آمیز دیتابیس از فایل خارجی")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- DOMAIN USE CASES & BACKUP UI STATE ---
    private val calculateGoldPriceUseCase = com.example.domain.usecase.CalculateGoldPriceUseCase()
    private val invoiceCalculatorUseCase = com.example.domain.usecase.InvoiceCalculatorUseCase()

    private val _backupUiState = MutableStateFlow<com.example.ui.state.BackupUiState>(com.example.ui.state.BackupUiState.Idle)
    val backupUiState: StateFlow<com.example.ui.state.BackupUiState> = _backupUiState.asStateFlow()

    fun resetBackupUiState() {
        _backupUiState.value = com.example.ui.state.BackupUiState.Idle
    }

    fun exportJsonBackup() {
        viewModelScope.launch {
            _backupUiState.value = com.example.ui.state.BackupUiState.Processing
            try {
                val json = repository.exportBackupJson()
                _backupUiState.value = com.example.ui.state.BackupUiState.Success("پشتیبان‌گیری با موفقیت تولید شد", json)
            } catch (e: Exception) {
                _backupUiState.value = com.example.ui.state.BackupUiState.Error("خطا در ایجاد پشتیبان: ${e.message}")
            }
        }
    }

    fun importJsonRestore(jsonString: String) {
        viewModelScope.launch {
            _backupUiState.value = com.example.ui.state.BackupUiState.Processing
            val result = repository.importRestoreJson(jsonString)
            if (result.isSuccess) {
                _backupUiState.value = com.example.ui.state.BackupUiState.Success("داده‌ها با موفقیت بازیابی شدند")
            } else {
                val errMsg = result.exceptionOrNull()?.message ?: "خطا در بازیابی پشتیبان"
                _backupUiState.value = com.example.ui.state.BackupUiState.Error(errMsg)
            }
        }
    }

    fun calculateGoldPrice(
        weightGram: Double,
        karat: Int = 18,
        wagePrice: Double,
        wageType: String,
        goldPricePerGram18k: Double,
        profitPercent: Double = 7.0,
        taxPercent: Double = 9.0
    ): com.example.domain.usecase.GoldCalculationResult {
        return calculateGoldPriceUseCase.execute(
            weightGram = weightGram,
            karat = karat,
            wagePrice = wagePrice,
            wageType = wageType,
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = profitPercent,
            taxPercent = taxPercent
        )
    }

    // --- CURRENCY & NUMBER STRING FORMATTERS (Persian localized) ---
    fun formatCurrency(amount: BigDecimal): String {
        return try {
            val format = NumberFormat.getInstance(Locale("fa", "IR"))
            format.format(amount.setScale(0, RoundingMode.HALF_UP).toBigInteger())
        } catch (e: Exception) {
            try {
                val format = NumberFormat.getInstance(Locale("fa", "IR"))
                format.format(amount)
            } catch (ex: Exception) {
                amount.setScale(0, RoundingMode.HALF_UP).toPlainString()
            }
        }
    }

    fun formatCurrency(amount: Double): String = formatCurrency(BigDecimal.valueOf(amount))

    fun formatWeight(weight: BigDecimal): String {
        return try {
            val format = NumberFormat.getInstance(Locale("fa", "IR"))
            format.minimumFractionDigits = 3
            format.maximumFractionDigits = 3
            format.format(weight)
        } catch (e: Exception) {
            weight.setScale(3, RoundingMode.HALF_UP).toPlainString()
        }
    }

    fun formatWeight(weight: Double): String = formatWeight(BigDecimal.valueOf(weight))

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- DAILY CLOSING ---
    val dailyClosings: StateFlow<List<DailyClosing>> = repository.dailyClosings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _todaySummaryPreview = MutableStateFlow<TodaySummaryPreview?>(null)
    val todaySummaryPreview: StateFlow<TodaySummaryPreview?> = _todaySummaryPreview.asStateFlow()

    private val _isDailyClosingLoading = MutableStateFlow(false)
    val isDailyClosingLoading: StateFlow<Boolean> = _isDailyClosingLoading.asStateFlow()

    fun loadTodaySummaryPreview() {
        viewModelScope.launch {
            _isDailyClosingLoading.value = true
            try {
                val preview = repository.getTodaySummaryPreview()
                _todaySummaryPreview.value = preview
            } finally {
                _isDailyClosingLoading.value = false
            }
        }
    }

    fun closeDay(
        physicalCash: BigDecimal?,
        physicalGoldWeight: BigDecimal?,
        notes: String?,
        onComplete: (Result<DailyClosing>) -> Unit
    ) {
        val currentPreview = _todaySummaryPreview.value
        if (currentPreview == null) {
            onComplete(Result.failure(IllegalStateException("خلاصه روز هنوز بارگذاری نشده است")))
            return
        }
        viewModelScope.launch {
            val res = repository.closeDay(currentPreview, physicalCash, physicalGoldWeight, notes)
            if (res.isSuccess) {
                loadTodaySummaryPreview()
            }
            onComplete(res)
        }
    }

    fun reopenDay(closingId: Long, reason: String, onComplete: (Result<DailyClosing>) -> Unit) {
        viewModelScope.launch {
            val res = repository.reopenDay(closingId, reason)
            if (res.isSuccess) {
                loadTodaySummaryPreview()
            }
            onComplete(res)
        }
    }

    // --- STOCK TAKE (BARCODE AUDIT) ---
    val activeStockTakeSession: StateFlow<StockTakeSession?> = repository.activeStockTakeSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val stockTakeSessions: StateFlow<List<StockTakeSession>> = repository.stockTakeSessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentStockTakeItems = MutableStateFlow<List<StockTakeItem>>(emptyList())
    val currentStockTakeItems: StateFlow<List<StockTakeItem>> = _currentStockTakeItems.asStateFlow()

    private var stockTakeItemsJob: Job? = null

    init {
        viewModelScope.launch {
            activeStockTakeSession.collect { session ->
                stockTakeItemsJob?.cancel()
                if (session != null) {
                    stockTakeItemsJob = viewModelScope.launch {
                        repository.getStockTakeItems(session.id).collect { items ->
                            _currentStockTakeItems.value = items
                        }
                    }
                } else {
                    _currentStockTakeItems.value = emptyList()
                }
            }
        }
    }

    fun startStockTakeSession(notes: String? = null, onComplete: (Result<StockTakeSession>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.startStockTakeSession(notes)
            onComplete(res)
        }
    }

    fun scanBarcodeForStockTake(barcode: String, onResult: (StockTakeScanResult) -> Unit = {}) {
        val active = activeStockTakeSession.value ?: return
        viewModelScope.launch {
            val res = repository.scanBarcodeForStockTake(active.id, barcode)
            res.getOrNull()?.let { onResult(it) }
        }
    }

    fun undoLastStockTakeScan(productId: Int) {
        val active = activeStockTakeSession.value ?: return
        viewModelScope.launch {
            repository.undoLastStockTakeScan(active.id, productId)
        }
    }

    fun manualUpdateStockTakeItemCount(productId: Int, newCount: Int) {
        val active = activeStockTakeSession.value ?: return
        viewModelScope.launch {
            repository.manualUpdateStockTakeItemCount(active.id, productId, newCount)
        }
    }

    fun prepareStockTakeReview(sessionId: Long, onComplete: (Result<List<StockTakeItem>>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.prepareReconciliationReview(sessionId)
            onComplete(res)
        }
    }

    fun resolveStockTakeItemReview(sessionId: Long, productId: Int, verifiedCount: Int, onComplete: (Result<StockTakeItem>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.resolveStockTakeItemReview(sessionId, productId, verifiedCount)
            onComplete(res)
        }
    }

    fun applyStockTakeAdjustments(sessionId: Long, onComplete: (Result<StockTakeApplyResult>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.applyStockTakeAdjustments(sessionId)
            onComplete(res)
        }
    }

    fun cancelStockTakeSession(sessionId: Long, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.cancelStockTakeSession(sessionId)
            onComplete(res)
        }
    }
}
