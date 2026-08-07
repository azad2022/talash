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
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    // --- SECURITY & PIN AUTHFLOW ---
    var pinError by mutableStateOf(false)

    // --- DASHBOARD VISIBILITY MASK ---
    var isDashboardValuesHidden by mutableStateOf(false)
        private set

    fun toggleDashboardValuesVisibility() {
        isDashboardValuesHidden = !isDashboardValuesHidden
    }
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isPinSetupRequired = MutableStateFlow(true)
    val isPinSetupRequired: StateFlow<Boolean> = _isPinSetupRequired.asStateFlow()

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
            calcGoldPriceToday = if (user.dailyGoldPrice > 0.0) user.dailyGoldPrice.toLong().toString() else "0"
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

    // Derived values computed in real-time
    val calcBaseGoldPrice: Double
        get() {
            val w = calcWeight.toDoubleOrNull() ?: 0.0
            val p = calcGoldPriceToday.toDoubleOrNull() ?: 0.0
            val karatCoeff = calcKarat.toDouble() / 18.0 // Normalized for 18k base pricing
            return w * p * karatCoeff
        }

    val calcWagePrice: Double
        get() {
            val base = calcBaseGoldPrice
            val w = calcWeight.toDoubleOrNull() ?: 0.0
            val wv = calcWageValue.toDoubleOrNull() ?: 0.0
            return if (calcWageType == "PERCENT") {
                base * (wv / 100.0)
            } else {
                wv * w
            }
        }

    val calcDealerProfit: Double
        get() = (calcBaseGoldPrice + calcWagePrice) * 0.07 // 7% Standard Jeweller Commission

    val calcTaxAndDuty: Double
        get() {
            val totalBeforeTax = calcBaseGoldPrice + calcWagePrice + calcDealerProfit
            val taxRateVal = calcTaxRate.toDoubleOrNull() ?: 9.0
            return totalBeforeTax * (taxRateVal / 100.0)
        }

    val calcFinalAmount: Double
        get() {
            val totalBeforeDiscount = calcBaseGoldPrice + calcWagePrice + calcDealerProfit + calcTaxAndDuty
            val disc = calcDiscount.toDoubleOrNull() ?: 0.0
            return (totalBeforeDiscount - disc).coerceAtLeast(0.0)
        }

    // --- CALCULATOR CONVERSION TO INVOICE CART ---
    fun loadCalculatorPriceToAppConfig() {
        viewModelScope.launch {
            val todayPrice = calcGoldPriceToday.toDoubleOrNull() ?: 0.0
            val taxVal = calcTaxRate.toDoubleOrNull() ?: 9.0
            val user = repository.getOrInitializeUser().copy(
                dailyGoldPrice = todayPrice,
                taxPercent = taxVal
            )
            repository.updateUser(user)
        }
    }

    fun updateGoldPrice(newPrice: Double) {
        viewModelScope.launch {
            val user = repository.getOrInitializeUser().copy(dailyGoldPrice = newPrice)
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

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
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
        val customGramPrice: Double, // customized based on daily gold rate + wage calculation
        val exactSalePrice: Double
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
            // Estimate price based on daily base price / live category price
            val estimatedPrice = product.calculateAssetValue(
                dailyPrice18k = user.dailyGoldPrice,
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
                taxRate = user.taxPercent
            )
            val index = draftItems.indexOfFirst { it.product.id == product.id }
            if (index >= 0) {
                val current = draftItems[index]
                draftItems[index] = current.copy(
                    qty = current.qty + quantity,
                    exactSalePrice = (current.qty + quantity) * estimatedPrice
                )
            } else {
                draftItems.add(
                    InvoiceItemDraft(
                        product = product,
                        qty = quantity,
                        customGramPrice = if (product.weightGram > 0.0) estimatedPrice / product.weightGram else estimatedPrice,
                        exactSalePrice = estimatedPrice * quantity
                    )
                )
            }
        }
    }

    val draftCartTotalAmount: Double
        get() {
            return draftItems.sumOf { it.exactSalePrice }
        }

    val draftFinalPriceAfterDiscount: Double
        get() {
            val total = draftCartTotalAmount
            val disc = draftDiscountInput.toDoubleOrNull() ?: 0.0
            return (total - disc).coerceAtLeast(0.0)
        }

    fun submitCurrentDraftInvoice(onSuccess: (Int) -> Unit): Boolean {
        val cust = draftCustomer ?: return false
        if (draftItems.isEmpty()) return false

        val totalAmt = draftFinalPriceAfterDiscount
        val discVal = draftDiscountInput.toDoubleOrNull() ?: 0.0
        val prepaymentVal = draftPrepaymentInput.toDoubleOrNull() ?: 0.0
        val instCountVal = draftInstallmentsCountInput.toIntOrNull() ?: 0

        viewModelScope.launch {
            val user = repository.getOrInitializeUser()
            val taxEst = totalAmt * (user.taxPercent / (100.0 + user.taxPercent)) // back-calculated tax
            
            val saleInvoice = SaleInvoice(
                customerId = cust.id,
                totalAmount = totalAmt,
                discount = discVal,
                tax = taxEst,
                paidAmount = if (draftPaymentType == "CASH") totalAmt else prepaymentVal,
                paymentType = draftPaymentType,
                installmentsCount = instCountVal,
                prepayment = prepaymentVal
            )

            // Convert drafts to SaleItem
            val itemsToSave = draftItems.map { draft ->
                SaleItem(
                    invoiceId = 0,
                    productId = draft.product.id,
                    quantity = draft.qty,
                    unitPrice = draft.exactSalePrice / draft.qty,
                    total = draft.exactSalePrice
                )
            }

            // Create installments list
            val installmentsList = mutableListOf<Installment>()
            if (draftPaymentType == "INSTALLMENT" && instCountVal > 0) {
                val remAmount = (totalAmt - prepaymentVal).coerceAtLeast(0.0)
                val perMonthAmount = remAmount / instCountVal
                val calendar = Calendar.getInstance()
                for (i in 1..instCountVal) {
                    calendar.add(Calendar.MONTH, 1)
                    installmentsList.add(
                        Installment(
                            invoiceId = 0,
                            dueDate = calendar.timeInMillis,
                            amount = perMonthAmount,
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
        val shopName = prefs.getString("receipt_shop_name", "گالری طلای گیلدار (شعبه مرکزی)") ?: "گالری طلای گیلدار (شعبه مرکزی)"
        val receiptTitle = prefs.getString("receipt_title", "فاکتور فروش معتبر کالا") ?: "فاکتور فروش معتبر کالا"
        val receiptFooter = prefs.getString("receipt_footer", "از خرید و حسن انتخاب شما سپاسگزاریم.") ?: "از خرید و حسن انتخاب شما سپاسگزاریم."
        val receiptAddress = prefs.getString("receipt_address", "آدرس: گالری اصلی طلا، تهران") ?: "آدرس: گالری اصلی طلا، تهران"
        
        val stringBuilder = StringBuilder()
        stringBuilder.append("===============================\n")
        stringBuilder.append("       $shopName\n")
        stringBuilder.append("       $receiptTitle\n")
        stringBuilder.append("===============================\n")
        stringBuilder.append("شماره فاکتور: ${invoice.invoice.id}\n")
        stringBuilder.append("تاریخ صدور: ${com.example.ui.util.JalaliCalendar.getJalaliDateTime(invoice.invoice.date)}\n")
        stringBuilder.append("نام مشتری: ${invoice.customer?.name ?: "مشتری متفرقه"}\n")
        stringBuilder.append("تلفن همراه: ${invoice.customer?.phone ?: "-"}\n")
        stringBuilder.append("-------------------------------\n")
        stringBuilder.append("شرح کالا / عیار / وزن (گرم) / فی کل \n")
        stringBuilder.append("-------------------------------\n")
        invoice.items.forEach { item ->
            val productName = item.customName ?: "طلای زینتی"
            stringBuilder.append("$productName (عیار 18) \n  ${item.quantity} عدد | فی: ${formatCurrency(item.unitPrice)} تومان\n")
        }
        stringBuilder.append("-------------------------------\n")
        stringBuilder.append("مبلغ کل اقلام: ${formatCurrency(invoice.invoice.totalAmount + invoice.invoice.discount)} تومان\n")
        if (invoice.invoice.discount > 0) {
            stringBuilder.append("تخفیف نقدی: ${formatCurrency(invoice.invoice.discount)} تومان\n")
        }
        stringBuilder.append("جمع نهایی پرداختی: ${formatCurrency(invoice.invoice.totalAmount)} تومان\n")
        stringBuilder.append("نوع تسویه حساب: ${if (invoice.invoice.paymentType == "CASH") "نقدی (کامل)" else "اقساطی"}\n")
        if (invoice.invoice.paymentType == "INSTALLMENT") {
            stringBuilder.append("پیش پرداخت: ${formatCurrency(invoice.invoice.prepayment)} تومان\n")
            stringBuilder.append("تعداد اقساط: ${invoice.invoice.installmentsCount} ماهه\n")
            val monthlyPay = if (invoice.invoice.installmentsCount > 0) {
                (invoice.invoice.totalAmount - invoice.invoice.prepayment) / invoice.invoice.installmentsCount
            } else 0.0
            stringBuilder.append("مبلغ هر قسط: ${formatCurrency(monthlyPay)} تومان\n")
        }
        stringBuilder.append("\n===============================\n")
        stringBuilder.append("$receiptFooter\n")
        stringBuilder.append("   $receiptAddress\n")
        stringBuilder.append("===============================\n")

        activePrintJobPayload = stringBuilder.toString()
        isShowingPrinterReceiptSimulation = true
        
        viewModelScope.launch {
            repository.logAction("RECEIPT_PRINT", "چاپ رسید فاکتور شماره ${invoice.invoice.id}")
        }
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
    fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getInstance(Locale("fa", "IR"))
            format.format(amount.toLong())
        } catch (e: Exception) {
            String.format("%,.0f", amount)
        }
    }

    fun formatWeight(weight: Double): String {
        return String.format("%.3f", weight)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
