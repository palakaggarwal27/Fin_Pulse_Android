package com.avinya.fin_pulse_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.avinya.fin_pulse_android.ui.theme.FinPulseBackground
import com.avinya.fin_pulse_android.ui.theme.FinPulseEmerald
import com.avinya.fin_pulse_android.ui.theme.FinPulseSurface
import java.util.*

class BubbleService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isForeground = false

    // Lifecycle, ViewModel, and SavedState management for the Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val _viewModelStore = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = _viewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        
        try {
            val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, "fin_pulse_channel")
                    .setContentTitle("Fin-Pulse Bubble Active")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(this)
                    .setContentTitle("Fin-Pulse Bubble Active")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build()
            }
            startForeground(1, notification)
            isForeground = true
            
            val amount = intent?.getFloatExtra("amount", 0f) ?: 0f
            val party = intent?.getStringExtra("party") ?: "Unknown"
            val method = intent?.getStringExtra("method") ?: "Digital"
            val upiId = intent?.getStringExtra("upiId")
            val isCredit = intent?.getBooleanExtra("isCredit", false) ?: false

            if (amount > 0) {
                showBubble(amount, party, method, upiId, isCredit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return START_NOT_STICKY
    }

    private fun showBubble(amount: Float, party: String, method: String, upiId: String?, isCredit: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        removeBubble()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 50
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@BubbleService)
            setViewTreeViewModelStoreOwner(this@BubbleService)
            setViewTreeSavedStateRegistryOwner(this@BubbleService)
            
            setContent {
                com.avinya.fin_pulse_android.ui.theme.Fin_Pulse_androidTheme {
                    BubbleUI(
                        amount = amount,
                        party = party,
                        method = method,
                        upiId = upiId,
                        isCredit = isCredit,
                        onDismiss = { 
                            removeBubble()
                            stopSelf()
                        },
                        onConfirm = { desc, cat ->
                            try {
                                ExpenseManager.addExpense(this@BubbleService, Expense(
                                    amount = amount,
                                    description = desc,
                                    category = cat,
                                    type = if (method == "Cash") "Cash" else "Digital",
                                    isCredit = isCredit
                                ))
                                Toast.makeText(this@BubbleService, "Transaction Logged!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            removeBubble()
                            stopSelf()
                        }
                    )
                }
            }
        }

        overlayView = composeView
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Composable
    fun BubbleUI(
        amount: Float,
        party: String,
        method: String,
        upiId: String?,
        isCredit: Boolean,
        onDismiss: () -> Unit,
        onConfirm: (String, String) -> Unit
    ) {
        var editableParty by remember { mutableStateOf(party) }
        val category = remember(editableParty) { ExpenseManager.predictCategory(this@BubbleService, editableParty, isCredit) }

        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, FinPulseEmerald.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
            color = FinPulseSurface.copy(alpha = 0.98f),
            tonalElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isCredit) "Payment Received" else "Payment Sent", color = if (isCredit) FinPulseEmerald else Color(0xFFFF4D4D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹ $amount", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Party / Organization", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                BasicTextField(
                    value = editableParty,
                    onValueChange = { editableParty = it },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Method", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(method, color = Color.White, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("AI Prediction", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(category, color = FinPulseEmerald, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onConfirm(if (isCredit) "Received from $editableParty" else "Paid to $editableParty", category) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinPulseEmerald, contentColor = FinPulseBackground),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Log", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    private fun removeBubble() {
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
                overlayView = null
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "fin_pulse_channel",
                "Fin-Pulse Tracker",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        removeBubble()
        _viewModelStore.clear()
        super.onDestroy()
    }
}
