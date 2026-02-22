package com.avinya.fin_pulse_android

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground
import com.avinya.fin_pulse_android.ui.theme.FinPulseEmerald
import com.avinya.fin_pulse_android.ui.theme.FinPulseSurface
import java.text.NumberFormat
import java.util.Locale

@Composable
fun VoiceMicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF1E88E5),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier.size(56.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Voice Input",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onVoiceResult: (String) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Tap to speak...") }
    var voiceHelper by remember { mutableStateOf<VoiceInputHelper?>(null) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening(
                context = context,
                onHelper = { voiceHelper = it },
                onListening = { isListening = it },
                onStatus = { statusMessage = it },
                onResult = onVoiceResult
            )
        } else {
            showPermissionRationale = true
        }
    }
    
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    DisposableEffect(Unit) {
        onDispose {
            voiceHelper?.destroy()
        }
    }
    
    Dialog(onDismissRequest = {
        voiceHelper?.cancel()
        onDismiss()
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            FinPulseSurface.copy(alpha = 0.95f),
                            FinPulseSurface.copy(alpha = 0.98f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Voice Expense Logger",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Mic button with animation
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale)
                                .background(
                                    Color(0xFF1E88E5).copy(alpha = 0.3f),
                                    CircleShape
                                )
                        )
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            if (!isListening) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasPermission) {
                                    startListening(
                                        context = context,
                                        onHelper = { voiceHelper = it },
                                        onListening = { isListening = it },
                                        onStatus = { statusMessage = it },
                                        onResult = onVoiceResult
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            } else {
                                voiceHelper?.stopListening()
                            }
                        },
                        containerColor = if (isListening) Color(0xFFFF4D4D) else Color(0xFF1E88E5),
                        contentColor = Color.White,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                Text(
                    text = statusMessage,
                    color = if (isListening) FinPulseEmerald else Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!isListening) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Try saying:",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "\"Spent 100 on ice cream\"",
                            "\"Paid 500 for lunch\"",
                            "\"Bought coffee for 150\""
                        ).forEach { example ->
                            Text(
                                text = "• $example",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        voiceHelper?.cancel()
                        onDismiss()
                    }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
    
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            containerColor = FinPulseSurface,
            title = { Text("Microphone Permission Required", color = Color.White) },
            text = {
                Text(
                    "To log expenses by voice, we need access to your microphone. " +
                            "Please grant the permission in app settings.",
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("OK", color = FinPulseEmerald)
                }
            }
        )
    }
}

private fun startListening(
    context: android.content.Context,
    onHelper: (VoiceInputHelper) -> Unit,
    onListening: (Boolean) -> Unit,
    onStatus: (String) -> Unit,
    onResult: (String) -> Unit
) {
    try {
        if (!VoiceInputHelper.isAvailable(context)) {
            onStatus("Speech recognition not available on this device")
            return
        }
        
        val helper = VoiceInputHelper(
            context = context,
            onResult = { text ->
                onStatus("Processing: \"$text\"")
                onListening(false)
                onResult(text)
            },
            onError = { error ->
                onStatus(error)
                onListening(false)
            },
            onReadyForSpeechCallback = {
                onStatus("Listening... Speak now!")
                onListening(true)
            },
            onBeginningOfSpeechCallback = {
                onStatus("Got it! Processing...")
            },
            onEndOfSpeechCallback = {
                onStatus("Processing your input...")
                onListening(false)
            }
        )
        
        onHelper(helper)
        helper.startListening()
    } catch (e: Exception) {
        android.util.Log.e("VoiceComposables", "Error starting voice input", e)
        onStatus("Error: ${e.message}")
        onListening(false)
    }
}

@Composable
fun VoiceExpenseConfirmationDialog(
    voiceExpense: VoiceExpense,
    onConfirm: (VoiceExpense) -> Unit,
    onDismiss: () -> Unit,
    onCategoryChange: (String) -> Unit
) {
    val context = LocalContext.current
    val categories = ExpenseManager.getCategories(context)
    var selectedCategory by remember { mutableStateOf(voiceExpense.category) }
    var editedDescription by remember { mutableStateOf(voiceExpense.description) }
    var editedAmount by remember { mutableStateOf(voiceExpense.amount.toString()) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { 
        maximumFractionDigits = 0 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FinPulseSurface,
        title = {
            Text(
                "Confirm Expense",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount
                OutlinedTextField(
                    value = editedAmount,
                    onValueChange = { editedAmount = it },
                    label = { Text("Amount", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinPulseEmerald,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Description", color = Color.White.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinPulseEmerald,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category dropdown
                var expanded by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            )
                        )
                    ) {
                        Text(
                            text = selectedCategory,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FinPulseSurface)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        category,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // AI suggestion indicator
                if (selectedCategory != voiceExpense.category) {
                    Text(
                        text = "AI suggested: ${voiceExpense.category}",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = editedAmount.toFloatOrNull() ?: voiceExpense.amount
                    val updatedExpense = voiceExpense.copy(
                        amount = finalAmount,
                        description = editedDescription,
                        category = selectedCategory
                    )
                    
                    // Train AI if user changed category
                    if (selectedCategory != voiceExpense.category) {
                        VoiceExpenseParser.trainPattern(
                            context,
                            editedDescription,
                            selectedCategory
                        )
                    }
                    
                    onConfirm(updatedExpense)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinPulseEmerald,
                    contentColor = FinPulseBackground
                )
            ) {
                Text("Log Expense", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.6f))
            }
        }
    )
}
