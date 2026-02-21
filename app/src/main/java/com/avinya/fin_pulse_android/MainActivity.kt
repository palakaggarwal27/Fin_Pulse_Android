package com.avinya.fin_pulse_android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.avinya.fin_pulse_android.ui.theme.Fin_Pulse_androidTheme
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground
import com.avinya.fin_pulse_android.ui.theme.FinPulseEmerald
import com.avinya.fin_pulse_android.ui.theme.FinPulseSurface
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Fin_Pulse_androidTheme {
                val context = LocalContext.current

                var currentView by remember { mutableStateOf(0) }
                
                var userName by remember { mutableStateOf(PreferenceManager.getUserName(context)) }
                var bankBalance by remember { mutableStateOf(PreferenceManager.getBankBalance(context)) }
                var cashOnHand by remember { mutableStateOf(PreferenceManager.getCashOnHand(context)) }
                var profileImageUri by remember { mutableStateOf(PreferenceManager.getProfileImageUri(context)) }
                var recentExpenses by remember { mutableStateOf(ExpenseManager.getExpenses(context)) }

                // --- Permission Handling ---
                var showPermissionDialog by remember { mutableStateOf(false) }
                var permissionCheckKey by remember { mutableStateOf(0) }
                
                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) { 
                        android.util.Log.w("FinPulse", "SMS permission denied by user")
                    } else {
                        android.util.Log.i("FinPulse", "SMS permission granted")
                    }
                }

                // Check permissions every time the app comes to foreground
                LaunchedEffect(permissionCheckKey, currentView) {
                    if (currentView == 2) { // Only check on Dashboard
                        android.util.Log.d("FinPulse", "Checking all permissions...")
                        
                        // 1. Check SMS Permission
                        val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        android.util.Log.d("FinPulse", "SMS Permission: $hasSmsPermission")
                        
                        if (!hasSmsPermission) {
                            smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        }
                        
                        // 2. Check Notification Access
                        val isNotificationEnabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
                        android.util.Log.d("FinPulse", "Notification Access: $isNotificationEnabled")
                        
                        // 3. Check Overlay Permission
                        val isOverlayEnabled = Settings.canDrawOverlays(context)
                        android.util.Log.d("FinPulse", "Overlay Permission: $isOverlayEnabled")
                        
                        // 4. Check if SMS permission exists
                        val hasSmsReadPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                        android.util.Log.d("FinPulse", "SMS Read Permission: $hasSmsReadPermission")
                        
                        // Show dialog if any permission is missing
                        if (!isNotificationEnabled || !isOverlayEnabled || !hasSmsPermission || !hasSmsReadPermission) {
                            android.util.Log.w("FinPulse", "Missing permissions - showing dialog")
                            showPermissionDialog = true
                        } else {
                            android.util.Log.i("FinPulse", "All permissions granted!")
                        }
                    }
                }

                if (showPermissionDialog) {
                    PermissionCheckerDialog(
                        onDismiss = { showPermissionDialog = false },
                        onRecheckPermissions = { permissionCheckKey++ }
                    )
                }

                when (currentView) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize().background(FinPulseBackground)) {
                            WelcomeScreen(onGetStarted = { 
                                currentView = if (PreferenceManager.isOnboardingComplete(context)) 2 else 1 
                            })
                        }
                    }
                    1 -> {
                        OnboardingFlowFromIdentity(
                            onComplete = { name, bank, cash, imageUri ->
                                val bankF = bank.toFloatOrNull() ?: 0f
                                val cashF = cash.toFloatOrNull() ?: 0f
                                PreferenceManager.saveUserData(context, name, bankF, cashF, imageUri)
                                PreferenceManager.setOnboardingComplete(context, true)

                                userName = name
                                bankBalance = bankF
                                cashOnHand = cashF
                                profileImageUri = imageUri
                                recentExpenses = ExpenseManager.getExpenses(context)
                                currentView = 2
                            }
                        )
                    }
                    2 -> {
                        DashboardScreen(
                            userName = userName,
                            bankBalance = bankBalance,
                            cashOnHand = cashOnHand,
                            profileImageUri = profileImageUri,
                            recentExpenses = recentExpenses,
                            onProfileClick = { currentView = 3 },
                            onAddExpense = { currentView = 4 },
                            onShowAnalytics = { currentView = 5 },
                            onRefresh = { 
                                recentExpenses = ExpenseManager.getExpenses(context)
                                bankBalance = PreferenceManager.getBankBalance(context)
                                cashOnHand = PreferenceManager.getCashOnHand(context)
                            }
                        )
                    }
                    3 -> {
                        ProfileSettingsScreen(
                            userName = userName,
                            bankBalance = bankBalance,
                            cashOnHand = cashOnHand,
                            profileImageUri = profileImageUri,
                            onBack = { currentView = 2 },
                            onUpdateProfile = { newName, newBank, newCash, newUri ->
                                userName = newName
                                bankBalance = newBank
                                cashOnHand = newCash
                                profileImageUri = newUri ?: profileImageUri
                            },
                            onDeleteAccount = {
                                PreferenceManager.clearAll(context)
                                ExpenseManager.clearAll(context)
                                userName = ""
                                bankBalance = 0f
                                cashOnHand = 0f
                                profileImageUri = null
                                recentExpenses = emptyList()
                                currentView = 0
                            }
                        )
                    }
                    4 -> {
                        AddExpenseScreen(
                            currentBank = bankBalance,
                            currentCash = cashOnHand,
                            onBack = { currentView = 2 },
                            onExpenseAdded = {
                                userName = PreferenceManager.getUserName(context)
                                bankBalance = PreferenceManager.getBankBalance(context)
                                cashOnHand = PreferenceManager.getCashOnHand(context)
                                recentExpenses = ExpenseManager.getExpenses(context)
                                currentView = 2
                            }
                        )
                    }
                    5 -> {
                        SpendingAnalyticsScreen(
                            expenses = recentExpenses,
                            onBack = { currentView = 2 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsCard(label: String, amount: String, accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(FinPulseSurface.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Text(label, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(amount, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OnboardingFlowFromIdentity(onComplete: (String, String, String, String?) -> Unit) {
    var currentSubScreen by remember { mutableStateOf(1) }
    var userName by remember { mutableStateOf("") }
    var bankBalance by remember { mutableStateOf("") }
    var cashOnHand by remember { mutableStateOf("") }
    var profileImageUriStr by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(FinPulseBackground)) {
        if (currentSubScreen == 1) {
            IdentityScreen(
                userName = userName,
                profileImageUri = if (profileImageUriStr != null) Uri.parse(profileImageUriStr!!) else null,
                onNameChange = { userName = it },
                onImageSelected = { profileImageUriStr = it.toString() },
                onNext = { currentSubScreen = 2 }
            )
        } else {
            BalancesScreen(
                bankBalance = bankBalance,
                cashOnHand = cashOnHand,
                onBankBalanceChange = { bankBalance = it },
                onCashChange = { cashOnHand = it },
                onFinish = { onComplete(userName, bankBalance, cashOnHand, profileImageUriStr) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(currentBank: Float, currentCash: Float, onBack: () -> Unit, onExpenseAdded: () -> Unit) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Digital") }
    var transactionDirection by remember { mutableStateOf("Spend") }
    
    var selectedCategory by remember { mutableStateOf("Miscellaneous") }
    var isManualCategory by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var showNegativeConfirm by remember { mutableStateOf(false) }

    var categories by remember { mutableStateOf(ExpenseManager.getCategories(context)) }
    val isCredit = transactionDirection == "Received"
    
    val orderedCategories = remember(categories, selectedCategory) {
        val list = categories.toMutableList()
        if (list.contains(selectedCategory)) {
            list.remove(selectedCategory)
            list.add(0, selectedCategory)
        }
        list
    }

    LaunchedEffect(description, isCredit) {
        if (!isManualCategory) {
            selectedCategory = ExpenseManager.predictCategory(context, description, isCredit)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            description = data?.get(0) ?: ""
            isManualCategory = false
        }
    }

    fun handleSave() {
        val amountF = amount.toFloatOrNull() ?: 0f
        if (amountF > 0 && description.isNotBlank()) {
            val isNegative = !isCredit && (if (selectedType == "Digital") amountF > currentBank else amountF > currentCash)
            if (isNegative && !showNegativeConfirm) {
                showNegativeConfirm = true
            } else {
                ExpenseManager.trainAI(context, description, selectedCategory)
                ExpenseManager.addExpense(context, Expense(amount = amountF, description = description, category = selectedCategory, type = selectedType, isCredit = isCredit))
                onExpenseAdded()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FinPulseBackground,
        topBar = { TopAppBar(title = { Text(if (isCredit) "Add Credit" else "Log Spend", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(FinPulseSurface)) {
                listOf("Spend", "Received").forEach { dir ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (transactionDirection == dir) (if (dir == "Spend") Color(0xFFFF4D4D) else FinPulseEmerald) else Color.Transparent).clickable { transactionDirection = dir; isManualCategory = false }, contentAlignment = Alignment.Center) {
                        Text(text = dir, color = if (transactionDirection == dir) FinPulseBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)).background(FinPulseSurface), verticalAlignment = Alignment.CenterVertically) {
                listOf("Digital", "Cash").forEach { type ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(if (selectedType == type) FinPulseEmerald else Color.Transparent).clickable { selectedType = type }, contentAlignment = Alignment.Center) {
                        Text(text = type, color = if (selectedType == type) FinPulseBackground else Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '.' }) amount = it },
                label = { Text("Amount", color = FinPulseEmerald) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)),
                prefix = { Text("₹ ", color = Color.White) }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; if (isManualCategory) isManualCategory = false },
                label = { Text("Description", color = FinPulseEmerald) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM) }
                        speechLauncher.launch(intent)
                    }) { Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = FinPulseEmerald) }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f))
            )

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FinPulseEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isManualCategory) "Manual Selection" else "AI Predicted", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Text(text = "+ Add New", color = FinPulseEmerald, fontSize = 12.sp, modifier = Modifier.clickable { showCustomCategoryDialog = true })
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orderedCategories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category; isManualCategory = true },
                            label = { Text(category) },
                            enabled = true,
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = Color.White.copy(alpha = 0.6f), selectedContainerColor = FinPulseEmerald.copy(alpha = 0.2f), selectedLabelColor = FinPulseEmerald),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedCategory == category, borderColor = Color.White.copy(alpha = 0.1f), selectedBorderColor = FinPulseEmerald)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = { handleSave() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isCredit) FinPulseEmerald else Color(0xFFFF4D4D), contentColor = FinPulseBackground), shape = RoundedCornerShape(16.dp), enabled = amount.isNotEmpty() && description.isNotEmpty()) {
                Text(if (isCredit) "Add Credit" else "Log Spend", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNegativeConfirm) {
        AlertDialog(onDismissRequest = { showNegativeConfirm = false }, containerColor = FinPulseSurface, title = { Text("Low Balance", color = Color.White) }, text = { Text("This will take your $selectedType balance to negative. Continue?", color = Color.White.copy(alpha = 0.7f)) }, confirmButton = { TextButton(onClick = { handleSave() }) { Text("Continue", color = FinPulseEmerald) } }, dismissButton = { TextButton(onClick = { showNegativeConfirm = false }) { Text("Review", color = Color.White.copy(alpha = 0.6f)) } })
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            containerColor = FinPulseSurface,
            title = { Text("New Category", color = Color.White) },
            text = {
                OutlinedTextField(value = customCategoryName, onValueChange = { customCategoryName = it }, placeholder = { Text("Category name", color = Color.White.copy(alpha = 0.4f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customCategoryName.isNotBlank()) {
                        selectedCategory = customCategoryName
                        isManualCategory = true
                        if (!categories.contains(customCategoryName)) {
                            categories = listOf(customCategoryName) + categories
                        }
                        showCustomCategoryDialog = false
                    }
                }) { Text("Add", color = FinPulseEmerald) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingAnalyticsScreen(expenses: List<Expense>, onBack: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    
    // Pie graph: From first spend to now (lifetime spend)
    val allCategoryTotals = expenses.filter { !it.isCredit }.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount.toDouble() }.toFloat() }
    val totalSpendOverall = allCategoryTotals.values.sum()

    // Activity List: Last 3 months
    val threeMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -3) }.timeInMillis
    val recentActivity = expenses.filter { it.timestamp >= threeMonthsAgo }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FinPulseBackground,
        topBar = {
            TopAppBar(
                title = { Text("Insights & History", color = Color.White) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Text("Lifetime Spending", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(24.dp))
                if (totalSpendOverall > 0) {
                    SimplePieChart(allCategoryTotals, totalSpendOverall)
                } else {
                    Text("No spending data yet.", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                }
            }

            item { Text("Last 3 Months Activity", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }

            if (recentActivity.isEmpty()) {
                item { Text("No transactions in last 3 months.", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) }
            } else {
                items(recentActivity) { expense ->
                    TransactionItem(expense = expense, onClick = {}, onLongClick = {})
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SimplePieChart(categoryTotals: Map<String, Float>, total: Float) {
    val colors = listOf(FinPulseEmerald, Color(0xFF00D1FF), Color(0xFFFFD700), Color(0xFFFF4D4D), Color(0xFFA259FF), Color(0xFF8B92A1))
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(180.dp)) {
            var startAngle = -90f
            categoryTotals.toList().sortedByDescending { it.second }.forEachIndexed { index, pair ->
                val sweepAngle = (pair.second / total) * 360f
                drawArc(color = colors[index % colors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = 30.dp.toPx()))
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Spend", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Text(categoryTotals.toList().maxByOrNull { it.second }?.first ?: "N/A", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String, 
    bankBalance: Float, 
    cashOnHand: Float,
    profileImageUri: String?,
    recentExpenses: List<Expense>,
    onProfileClick: () -> Unit,
    onAddExpense: () -> Unit,
    onShowAnalytics: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Expense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Expense?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FinPulseBackground,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense, containerColor = FinPulseEmerald, contentColor = FinPulseBackground, shape = CircleShape, modifier = Modifier.size(64.dp)) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).padding(horizontal = 24.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Good Evening, $userName", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onShowAnalytics) { Icon(Icons.Filled.BarChart, contentDescription = "Analytics", tint = Color.White) }
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(FinPulseSurface).border(1.dp, FinPulseEmerald.copy(alpha = 0.3f), CircleShape).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
                            if (profileImageUri != null) Image(painter = rememberAsyncImagePainter(profileImageUri), contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            item {
                Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.linearGradient(colors = listOf(FinPulseSurface.copy(alpha = 0.6f), FinPulseSurface.copy(alpha = 0.4f))), shape = RoundedCornerShape(24.dp)).border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))) {
                    Column(modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                        Text("Total Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(currencyFormat.format(bankBalance + cashOnHand), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatsCard("Digital", currencyFormat.format(bankBalance), FinPulseEmerald, Modifier.weight(1f))
                    StatsCard("Cash", currencyFormat.format(cashOnHand), Color(0xFF8B92A1), Modifier.weight(1f))
                }
            }
            item {
                Button(
                    onClick = {
                        android.util.Log.d("FinPulse-Test", "Test bubble button clicked")
                        android.widget.Toast.makeText(context, "Starting test bubble...", android.widget.Toast.LENGTH_SHORT).show()
                        
                        val testIntent = Intent(context, BubbleService::class.java)
                        testIntent.putExtra("amount", 500f)
                        testIntent.putExtra("party", "Test Merchant")
                        testIntent.putExtra("method", "UPI")
                        testIntent.putExtra("upiId", "test@upi")
                        testIntent.putExtra("isCredit", false)
                        
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(testIntent)
                            } else {
                                context.startService(testIntent)
                            }
                            android.util.Log.i("FinPulse-Test", "Test bubble service started successfully")
                        } catch (e: Exception) {
                            android.util.Log.e("FinPulse-Test", "Failed to start test bubble", e)
                            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D), contentColor = FinPulseBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test Bubble (Debug)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            item { Text("Recent Activity", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
            if (recentExpenses.isEmpty()) item { Text("No transactions yet.", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) }
            else items(recentExpenses.take(10)) { expense -> TransactionItem(expense = expense, onClick = { transactionToEdit = expense; showCategoryDialog = true }, onLongClick = { transactionToDelete = expense; showDeleteDialog = true }) }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
        if (showDeleteDialog && transactionToDelete != null) {
            AlertDialog(onDismissRequest = { showDeleteDialog = false }, containerColor = FinPulseSurface, title = { Text("Delete Entry", color = Color.White) }, text = { Text("Restore balance?", color = Color.White.copy(alpha = 0.7f)) }, confirmButton = { TextButton(onClick = { ExpenseManager.deleteExpense(context, transactionToDelete!!, true); showDeleteDialog = false; onRefresh() }) { Text("Restore", color = FinPulseEmerald) } }, dismissButton = { TextButton(onClick = { ExpenseManager.deleteExpense(context, transactionToDelete!!, false); showDeleteDialog = false; onRefresh() }) { Text("Just Delete", color = Color.White.copy(alpha = 0.6f)) } })
        }
        if (showCategoryDialog && transactionToEdit != null) {
            AlertDialog(onDismissRequest = { showCategoryDialog = false }, containerColor = FinPulseSurface, title = { Text("Change Category", color = Color.White) }, text = { Column { ExpenseManager.getCategories(context).forEach { category -> Button(onClick = { ExpenseManager.updateExpenseCategory(context, transactionToEdit!!.id, category); showCategoryDialog = false; onRefresh() }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White)) { Text(category) } } } }, confirmButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel", color = FinPulseEmerald) } })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(expense: Expense, onClick: () -> Unit, onLongClick: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    Box(modifier = Modifier.fillMaxWidth().background(FinPulseSurface.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(expense.category, color = if (expense.isCredit) FinPulseEmerald else Color(0xFFFF4D4D), fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateFormat.format(Date(expense.timestamp)), color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (expense.isCredit) "+" else "-"} ${currencyFormat.format(expense.amount)}", color = if (expense.isCredit) FinPulseEmerald else Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(expense.type, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(userName: String, bankBalance: Float, cashOnHand: Float, profileImageUri: String?, onBack: () -> Unit, onUpdateProfile: (String, Float, Float, String?) -> Unit, onDeleteAccount: () -> Unit) {
    val context = LocalContext.current
    var editName by remember { mutableStateOf(userName) }
    var editBank by remember { mutableStateOf(bankBalance.toString()) }
    var editCash by remember { mutableStateOf(cashOnHand.toString()) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            PreferenceManager.saveUserData(context, editName, bankBalance, cashOnHand, it.toString())
            onUpdateProfile(editName, bankBalance, cashOnHand, it.toString())
        }
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = FinPulseBackground, topBar = { TopAppBar(title = { Text("Profile", color = Color.White) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Color.White) } }, actions = { if (!isEditing) { IconButton(onClick = { isEditing = true }) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = FinPulseEmerald) } } else { IconButton(onClick = { val newBank = editBank.toFloatOrNull() ?: bankBalance; val newCash = editCash.toFloatOrNull() ?: cashOnHand; val newName = if (editName.isNotBlank()) editName else userName; PreferenceManager.saveUserData(context, newName, newBank, newCash, profileImageUri); onUpdateProfile(newName, newBank, newCash, profileImageUri); isEditing = false }) { Icon(Icons.Filled.Check, contentDescription = "Save", tint = FinPulseEmerald) } } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)) }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(FinPulseSurface).border(2.dp, FinPulseEmerald.copy(alpha = 0.5f), CircleShape).clickable { imageLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (profileImageUri != null) Image(painter = rememberAsyncImagePainter(profileImageUri), contentDescription = "Profile", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.linearGradient(colors = listOf(FinPulseSurface.copy(alpha = 0.7f), FinPulseSurface.copy(alpha = 0.5f))), shape = RoundedCornerShape(24.dp)).border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    if (isEditing) OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name", color = FinPulseEmerald) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
                    else { Text("Name", color = FinPulseEmerald, fontSize = 12.sp); Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))
                    if (isEditing) {
                        OutlinedTextField(value = editBank, onValueChange = { editBank = it }, label = { Text("Bank", color = FinPulseEmerald) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(value = editCash, onValueChange = { editCash = it }, label = { Text("Cash", color = FinPulseEmerald) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text("Bank", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); Text(currencyFormat.format(bankBalance), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                            Column(horizontalAlignment = Alignment.End) { Text("Cash", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp); Text(currencyFormat.format(cashOnHand), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = { showDeleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4D4D))) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Account & Data", fontWeight = FontWeight.Bold)
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = FinPulseSurface,
            title = { Text("Delete Account?", color = Color.White) },
            text = { Text("This will permanently wipe all your profile data, transaction history, and AI learning. This action cannot be undone.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    onDeleteAccount() 
                }) { Text("Wipe Everything", color = Color(0xFFFF4D4D)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = Color.White) }
            }
        )
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Fin-Pulse", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Light, letterSpacing = 2.sp)
        Text("Zero-Friction Finance Tracking", color = Color(0xFF8B92A1), fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.8.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(80.dp))
        Button(onClick = onGetStarted, colors = ButtonDefaults.buttonColors(containerColor = FinPulseEmerald, contentColor = FinPulseBackground), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun IdentityScreen(userName: String, profileImageUri: android.net.Uri?, onNameChange: (String) -> Unit, onImageSelected: (android.net.Uri) -> Unit, onNext: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? -> uri?.let { onImageSelected(it) } }
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("Welcome to Fin-Pulse", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(60.dp))
        Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(FinPulseSurface).border(2.dp, FinPulseEmerald.copy(alpha = 0.3f), CircleShape).clickable { launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
            if (profileImageUri != null) Image(painter = rememberAsyncImagePainter(profileImageUri), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF8B92A1), modifier = Modifier.size(64.dp))
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        }
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(value = userName, onValueChange = onNameChange, modifier = Modifier.fillMaxWidth(), label = { Text("What's your name?", color = Color.White.copy(alpha = 0.8f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.2f)))
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onNext, enabled = userName.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = FinPulseEmerald, contentColor = FinPulseBackground), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun BalancesScreen(bankBalance: String, cashOnHand: String, onBankBalanceChange: (String) -> Unit, onCashChange: (String) -> Unit, onFinish: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("Set Your Balances", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(40.dp))
        GlassmorphicBalanceCard(label = "Initial Bank Balance", value = bankBalance, onValueChange = onBankBalanceChange, placeholder = "₹0")
        Spacer(modifier = Modifier.height(20.dp))
        GlassmorphicBalanceCard(label = "Cash on Hand", value = cashOnHand, onValueChange = onCashChange, placeholder = "₹0")
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = FinPulseEmerald, contentColor = FinPulseBackground), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start Tracking", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun GlassmorphicBalanceCard(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.linearGradient(colors = listOf(FinPulseSurface.copy(alpha = 0.6f), FinPulseSurface.copy(alpha = 0.4f))), shape = RoundedCornerShape(20.dp)).border(width = 0.5.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(text = label, color = FinPulseEmerald, fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = value, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onValueChange(it) }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder, color = Color(0xFF8B92A1).copy(alpha = 0.5f)) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald.copy(alpha = 0.5f), unfocusedBorderColor = Color.White.copy(alpha = 0.1f), cursorColor = FinPulseEmerald, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent), shape = RoundedCornerShape(12.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
fun PermissionCheckerDialog(onDismiss: () -> Unit, onRecheckPermissions: () -> Unit) {
    val context = LocalContext.current
    
    // Check each permission's current status
    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    val hasSmsReadPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    val hasNotificationAccess = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    
    // Check if NotificationListenerService is actually running
    val isListenerServiceRunning = try {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.getRunningServices(Integer.MAX_VALUE).any { 
            it.service.className == "com.avinya.fin_pulse_android.NotificationService" 
        }
    } catch (e: Exception) {
        false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinPulseSurface,
        title = { 
            Column {
                Text("Setup Required", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Enable these permissions for automatic expense tracking", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
        },
        text = { 
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // SMS Permission
                PermissionItem(
                    title = "SMS Access",
                    description = "Read payment SMS from banks",
                    isGranted = hasSmsPermission && hasSmsReadPermission,
                    onClick = {
                        android.util.Log.d("FinPulse", "User clicked SMS Permission - opening app settings")
                        android.widget.Toast.makeText(context, "Enable SMS permissions", android.widget.Toast.LENGTH_SHORT).show()
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        } catch (e: Exception) {
                            android.util.Log.e("FinPulse", "Failed to open app settings", e)
                        }
                    }
                )
                
                // Notification Access
                PermissionItem(
                    title = "Notification Access",
                    description = "Read payment notifications from apps like PhonePe, TrueCaller",
                    isGranted = hasNotificationAccess,
                    onClick = {
                        android.util.Log.d("FinPulse", "User clicked Notification Access - opening notification listener settings")
                        android.widget.Toast.makeText(context, "Find and enable Fin-Pulse", android.widget.Toast.LENGTH_LONG).show()
                        try {
                            context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        } catch (e: Exception) {
                            android.util.Log.e("FinPulse", "Failed to open notification settings", e)
                        }
                    }
                )
                
                // Overlay Permission
                PermissionItem(
                    title = "Display Over Apps",
                    description = "Show bubble overlay when payment is detected",
                    isGranted = hasOverlayPermission,
                    onClick = {
                        android.util.Log.d("FinPulse", "User clicked Display Over Apps - opening overlay settings")
                        android.widget.Toast.makeText(context, "Enable 'Allow display over other apps'", android.widget.Toast.LENGTH_LONG).show()
                        try {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } catch (e: Exception) {
                            android.util.Log.e("FinPulse", "Failed to open overlay settings", e)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Service Status Indicator
                if (hasNotificationAccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isListenerServiceRunning) FinPulseEmerald.copy(alpha = 0.1f) else Color(0xFFFFB74D).copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isListenerServiceRunning) FinPulseEmerald.copy(alpha = 0.3f) else Color(0xFFFFB74D).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                if (isListenerServiceRunning) Icons.Filled.Check else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (isListenerServiceRunning) FinPulseEmerald else Color(0xFFFFB74D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isListenerServiceRunning) 
                                    "Notification Listener Service is running ✓" 
                                else 
                                    "Notification Listener Service not detected. Try toggling it off and on again in settings.",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFB74D).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(0.5.dp, Color(0xFFFFB74D).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "After enabling permissions, tap 'Recheck' to verify everything works. Use the 'Test Bubble' button on dashboard to test.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                android.util.Log.d("FinPulse", "Rechecking permissions...")
                onRecheckPermissions()
                onDismiss()
            }) { 
                Text("Recheck", color = FinPulseEmerald, fontWeight = FontWeight.Bold) 
            }
        },
        dismissButton = {
            TextButton(onClick = {
                android.util.Log.d("FinPulse", "User dismissed permission dialog")
                onDismiss()
            }) { 
                Text("Later", color = Color.White.copy(alpha = 0.6f)) 
            }
        }
    )
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isGranted) FinPulseEmerald.copy(alpha = 0.1f) else Color(0xFFFF4D4D).copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .border(
                0.5.dp,
                if (isGranted) FinPulseEmerald.copy(alpha = 0.3f) else Color(0xFFFF4D4D).copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isGranted) { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isGranted) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Granted",
                    tint = FinPulseEmerald,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    "Enable",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFFFF4D4D).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
