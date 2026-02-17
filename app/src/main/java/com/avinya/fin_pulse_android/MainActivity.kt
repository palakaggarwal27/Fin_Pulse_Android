package com.avinya.fin_pulse_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.avinya.fin_pulse_android.ui.theme.Fin_Pulse_androidTheme
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground
import com.avinya.fin_pulse_android.ui.theme.FinPulseEmerald
import com.avinya.fin_pulse_android.ui.theme.FinPulseSurface
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Fin_Pulse_androidTheme {
                val context = LocalContext.current

                // State to control views
                // 0: Start Screen
                // 1: Onboarding
                // 2: Dashboard
                // 3: Profile Settings
                var currentView by remember { mutableStateOf(0) }
                
                var userName by remember { mutableStateOf(PreferenceManager.getUserName(context)) }
                var bankBalance by remember { mutableStateOf(PreferenceManager.getBankBalance(context)) }
                var cashOnHand by remember { mutableStateOf(PreferenceManager.getCashOnHand(context)) }
                var profileImageUri by remember { mutableStateOf(PreferenceManager.getProfileImageUri(context)) }

                when (currentView) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize().background(FinPulseBackground)) {
                            WelcomeScreen(onGetStarted = { currentView = 1 })
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
                            onProfileClick = { currentView = 3 }
                        )
                    }
                    3 -> {
                        ProfileSettingsScreen(
                            userName = userName,
                            bankBalance = bankBalance,
                            cashOnHand = cashOnHand,
                            profileImageUri = profileImageUri,
                            onBack = { currentView = 2 },
                            onEdit = { /* Future Edit logic */ }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    userName: String,
    bankBalance: Float,
    cashOnHand: Float,
    profileImageUri: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val totalBalance = bankBalance + cashOnHand
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FinPulseBackground,
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large Profile Picture
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(FinPulseSurface)
                    .border(2.dp, FinPulseEmerald.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUri),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Info Glassmorphic Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(FinPulseSurface.copy(alpha = 0.7f), FinPulseSurface.copy(alpha = 0.5f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Name", color = FinPulseEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = FinPulseEmerald)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))

                    Text("Total Net Worth", color = FinPulseEmerald, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(currencyFormat.format(totalBalance), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bank: ${currencyFormat.format(bankBalance)}", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                        Text("Cash: ${currencyFormat.format(cashOnHand)}", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    userName: String, 
    bankBalance: Float, 
    cashOnHand: Float,
    profileImageUri: String?,
    onProfileClick: () -> Unit
) {
    val totalBalance = bankBalance + cashOnHand
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }
    val totalBalanceStr = currencyFormat.format(totalBalance)
    val bankBalanceStr = currencyFormat.format(bankBalance)
    val cashOnHandStr = currencyFormat.format(cashOnHand)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = FinPulseBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Action */ },
                containerColor = FinPulseEmerald,
                contentColor = FinPulseBackground,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Good Evening, $userName",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.weight(1f)
                )
                
                // CLICKABLE Profile Picture
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FinPulseSurface)
                        .border(1.dp, FinPulseEmerald.copy(alpha = 0.3f), CircleShape)
                        .clickable { onProfileClick() }, // NAVIGATION TRIGGER
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUri),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(FinPulseSurface.copy(alpha = 0.6f), FinPulseSurface.copy(alpha = 0.4f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(32.dp).fillMaxWidth()) {
                    Text("Total Balance", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(totalBalanceStr, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp) ) {
                StatsCard("Digital", bankBalanceStr, FinPulseEmerald, Modifier.weight(1f))
                StatsCard("Cash", cashOnHandStr, Color(0xFF8B92A1), Modifier.weight(1f))
            }
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
                profileImageUri = if (profileImageUriStr != null) android.net.Uri.parse(profileImageUriStr) else null,
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
