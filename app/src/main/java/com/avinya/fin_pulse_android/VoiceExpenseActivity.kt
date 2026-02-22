package com.avinya.fin_pulse_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.avinya.fin_pulse_android.ui.theme.Fin_Pulse_androidTheme
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground

/**
 * Activity launched from the home screen widget
 * Provides voice input directly without opening the main app
 */
class VoiceExpenseActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceInput = true
        } else {
            Toast.makeText(
                this,
                "Microphone permission required for voice input",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
    
    private var showVoiceInput by mutableStateOf(false)
    private var showConfirmation by mutableStateOf(false)
    private var parsedExpense by mutableStateOf<VoiceExpense?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showVoiceInput = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        
        setContent {
            Fin_Pulse_androidTheme {
                VoiceExpenseScreen()
            }
        }
    }
    
    @Composable
    private fun VoiceExpenseScreen() {
        val context = LocalContext.current
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FinPulseBackground.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center
        ) {
            if (showVoiceInput && parsedExpense == null) {
                VoiceInputDialog(
                    onDismiss = {
                        showVoiceInput = false
                        finish()
                    },
                    onVoiceResult = { recognizedText ->
                        val expense = VoiceExpenseParser.parse(recognizedText, context)
                        if (expense != null) {
                            parsedExpense = expense
                            showVoiceInput = false
                            showConfirmation = true
                        } else {
                            Toast.makeText(
                                context,
                                "Couldn't understand. Try: 'spent 100 on coffee'",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                )
            }
            
            if (showConfirmation && parsedExpense != null) {
                VoiceExpenseConfirmationDialog(
                    voiceExpense = parsedExpense!!,
                    onConfirm = { confirmedExpense ->
                        // Log the expense
                        val expense = Expense(
                            amount = confirmedExpense.amount,
                            description = confirmedExpense.description,
                            category = confirmedExpense.category,
                            type = "Cash",
                            isCredit = false
                        )
                        
                        ExpenseManager.addExpense(context, expense)
                        
                        Toast.makeText(
                            context,
                            "Expense logged: ₹${confirmedExpense.amount}",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Update widget (optional - refresh last expense)
                        updateWidget()
                        
                        finish()
                    },
                    onDismiss = {
                        finish()
                    },
                    onCategoryChange = { newCategory ->
                        parsedExpense = parsedExpense?.copy(category = newCategory)
                    }
                )
            }
        }
    }
    
    private fun updateWidget() {
        // Trigger widget update to show last logged expense
        val intent = android.content.Intent(this, VoiceExpenseWidgetProvider::class.java)
        intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        sendBroadcast(intent)
    }
}
