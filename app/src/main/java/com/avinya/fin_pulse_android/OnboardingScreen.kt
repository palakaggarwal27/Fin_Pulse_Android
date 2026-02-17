package com.avinya.fin_pulse_android

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground
import com.avinya.fin_pulse_android.ui.theme.FinPulseEmerald
import com.avinya.fin_pulse_android.ui.theme.FinPulseSurface
import com.avinya.fin_pulse_android.ui.theme.FinPulseMutedGrey

@Composable
fun OnboardingFlow(onComplete: (String, String, String, String?) -> Unit) {
    var currentScreen by remember { mutableStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var bankBalance by remember { mutableStateOf("") }
    var cashOnHand by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FinPulseBackground)
    ) {
        when (currentScreen) {
            0 -> WelcomeScreen(
                onGetStarted = { currentScreen = 1 }
            )
            1 -> IdentityScreen(
                userName = userName,
                profileImageUri = profileImageUri,
                onNameChange = { userName = it },
                onImageSelected = { profileImageUri = it },
                onNext = { currentScreen = 2 }
            )
            2 -> BalancesScreen(
                bankBalance = bankBalance,
                cashOnHand = cashOnHand,
                onBankBalanceChange = { bankBalance = it },
                onCashChange = { cashOnHand = it },
                onFinish = { onComplete(userName, bankBalance, cashOnHand, profileImageUri?.toString()) }
            )
        }
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Fin-Pulse",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "Zero-Friction Finance Tracking",
            color = FinPulseMutedGrey,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(80.dp))
        
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinPulseEmerald,
                contentColor = FinPulseBackground
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Get Started",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun IdentityScreen(
    userName: String,
    profileImageUri: Uri?,
    onNameChange: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onNext: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = "Welcome to Fin-Pulse",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(60.dp))
        
        // Circular Profile with Image Picker
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(FinPulseSurface)
                .border(2.dp, FinPulseEmerald.copy(alpha = 0.3f), CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (profileImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    tint = FinPulseMutedGrey,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            // Hover-like Camera Icon Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Add Photo",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "What's your name?",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Enter your name", color = FinPulseMutedGrey.copy(alpha = 0.6f))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = FinPulseEmerald,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                cursorColor = FinPulseEmerald,
                focusedContainerColor = FinPulseSurface.copy(alpha = 0.5f),
                unfocusedContainerColor = FinPulseSurface.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            enabled = userName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = FinPulseEmerald,
                contentColor = FinPulseBackground,
                disabledContainerColor = FinPulseMutedGrey.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BalancesScreen(
    bankBalance: String,
    cashOnHand: String,
    onBankBalanceChange: (String) -> Unit,
    onCashChange: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        Text(
            text = "Set Your Balances",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 0.5.sp
        )
        
        Text(
            text = "You can adjust these anytime",
            color = FinPulseMutedGrey,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        GlassmorphicBalanceCard(
            label = "Initial Bank Balance",
            value = bankBalance,
            onValueChange = onBankBalanceChange,
            placeholder = "₹0"
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        GlassmorphicBalanceCard(
            label = "Cash on Hand",
            value = cashOnHand,
            onValueChange = onCashChange,
            placeholder = "₹0"
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinPulseEmerald,
                contentColor = FinPulseBackground
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Start Tracking",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun GlassmorphicBalanceCard(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        FinPulseSurface.copy(alpha = 0.6f),
                        FinPulseSurface.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = label,
                color = FinPulseEmerald,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(placeholder, color = FinPulseMutedGrey.copy(alpha = 0.5f))
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = FinPulseEmerald.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    cursorColor = FinPulseEmerald,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
