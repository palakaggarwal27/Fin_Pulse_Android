package com.avinya.fin_pulse_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

class BubbleService : Service() {

    companion object {
        const val ACTION_REFRESH_DATA = "com.avinya.fin_pulse_android.REFRESH_DATA"
    }

    private var windowManager: WindowManager? = null
    private var bubbleHead: ComposeView? = null
    private var popupView: ComposeView? = null
    
    private val TAG = "FinPulse-Bubble"
    private val hideHandler = Handler(Looper.getMainLooper())
    private val isIdle = mutableStateOf(false)

    private inner class BubbleLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun onCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    private var headLifecycleOwner: BubbleLifecycleOwner? = null
    private var popupLifecycleOwner: BubbleLifecycleOwner? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = Notification.Builder(this, "fin_pulse_channel")
            .setContentTitle("Fin-Pulse Bubble Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val amount = intent?.getFloatExtra("amount", 0f) ?: 0f
        val party = intent?.getStringExtra("party") ?: "Unknown"
        val method = intent?.getStringExtra("method") ?: "Digital"
        val isCredit = intent?.getBooleanExtra("isCredit", false) ?: false
        val rawText = intent?.getStringExtra("rawText") ?: ""
        val upiId = intent?.getStringExtra("upiId")

        if (amount > 0) {
            showBubbleHead(amount, party, method, isCredit, rawText, upiId)
        }
        return START_NOT_STICKY
    }

    private fun showBubbleHead(amount: Float, party: String, method: String, isCredit: Boolean, rawText: String, upiId: String?) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val screenWidth = resources.displayMetrics.widthPixels

        removeBubbleHead()
        removePopup()

        val headParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        val owner = BubbleLifecycleOwner()
        owner.onCreate()
        headLifecycleOwner = owner

        val headView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            
            setContent {
                val scale by animateFloatAsState(if (isIdle.value) 0.75f else 1f)
                val alpha by animateFloatAsState(if (isIdle.value) 0.6f else 1f)
                Box(
                    modifier = Modifier.size(60.dp).scale(scale).alpha(alpha).clip(CircleShape).background(FinPulseEmerald).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = FinPulseBackground, modifier = Modifier.size(30.dp))
                }
            }
        }

        resetIdleTimer()

        headView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                isIdle.value = false
                resetIdleTimer()
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = headParams.x; initialY = headParams.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 15 || Math.abs(dy) > 15) isDragging = true
                        if (isDragging) {
                            headParams.x = initialX + dx; headParams.y = initialY + dy
                            windowManager?.updateViewLayout(headView, headParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val center = screenWidth / 2
                        if (headParams.x + (v.width / 2) < center) headParams.x = -20 else headParams.x = screenWidth - v.width + 20
                        windowManager?.updateViewLayout(headView, headParams)
                        if (!isDragging) showPopup(amount, party, method, isCredit, rawText, upiId)
                        return true
                    }
                }
                return false
            }
        })

        bubbleHead = headView
        try {
            windowManager?.addView(bubbleHead, headParams)
            owner.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubble head", e)
        }
    }

    private fun resetIdleTimer() {
        hideHandler.removeCallbacksAndMessages(null)
        hideHandler.postDelayed({ isIdle.value = true }, 4000)
    }

    private fun showPopup(amount: Float, party: String, method: String, isCredit: Boolean, rawText: String, upiId: String?) {
        if (popupView != null) return
        bubbleHead?.visibility = View.GONE

        val popupParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER; dimAmount = 0.6f }

        val owner = BubbleLifecycleOwner()
        owner.onCreate()
        popupLifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                com.avinya.fin_pulse_android.ui.theme.Fin_Pulse_androidTheme {
                    BubbleUI(
                        initialAmount = amount, initialParty = party, initialMethod = method, isCredit = isCredit, rawText = rawText, upiId = upiId,
                        onDismiss = { 
                            if (rawText.isNotEmpty()) {
                                ExpenseManager.trainNonTransaction(this@BubbleService, rawText)
                            }
                            removePopup(); bubbleHead?.visibility = View.VISIBLE 
                        },
                        onConfirm = { amt, correctedParty, desc, cat, meth, finalIsCredit ->
                            if (rawText.isNotEmpty()) {
                                ExpenseManager.trainConfirmedTransaction(this@BubbleService, rawText, finalIsCredit)
                            }
                            // Save UPI mapping if UPI ID exists and user has edited the party name
                            if (upiId != null && correctedParty.isNotBlank() && correctedParty.uppercase() != upiId.uppercase()) {
                                Log.d(TAG, "Training UPI Mapping: $upiId -> $correctedParty")
                                ExpenseManager.trainUpiMapping(this@BubbleService, upiId, correctedParty)
                            }
                            ExpenseManager.addExpense(this@BubbleService, Expense(amount = amt, description = desc, category = cat, type = meth, isCredit = finalIsCredit))
                            
                            val refreshIntent = Intent(ACTION_REFRESH_DATA).setPackage(packageName)
                            sendBroadcast(refreshIntent)
                            
                            removePopup(); removeBubbleHead(); stopSelf()
                        }
                    )
                }
            }
        }

        popupView = composeView
        try {
            windowManager?.addView(popupView, popupParams)
            owner.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "Popup fail", e); bubbleHead?.visibility = View.VISIBLE
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BubbleUI(
        initialAmount: Float, initialParty: String, initialMethod: String, isCredit: Boolean, rawText: String, upiId: String?,
        onDismiss: () -> Unit, onConfirm: (Float, String, String, String, String, Boolean) -> Unit
    ) {
        var amount by remember { mutableStateOf(initialAmount.toString()) }
        var party by remember { mutableStateOf(initialParty) }
        var isCreditState by remember { mutableStateOf(isCredit) }
        var description by remember { mutableStateOf(if (isCreditState) "Received from $initialParty" else "Paid to $initialParty") }
        var selectedMethod by remember { mutableStateOf(if (initialMethod == "Cash") "Cash" else "Digital") }
        var selectedCategory by remember { mutableStateOf("Miscellaneous") }
        var isManualCategory by remember { mutableStateOf(false) }
        var isManualDescription by remember { mutableStateOf(false) }
        
        val context = LocalContext.current
        val categories = remember(context) { ExpenseManager.getCategories(context) }
        
        // Keep description synced with party name unless user manually edits it
        LaunchedEffect(party, isCreditState) {
            if (!isManualDescription) {
                description = if (isCreditState) "Received from ${party.uppercase()}" else "Paid to ${party.uppercase()}"
            }
        }

        LaunchedEffect(party, description, isCreditState) {
            if (!isManualCategory) {
                val predictorText = if (description.isNotBlank() && !description.contains("Paid to") && !description.contains("Received from")) description else party
                selectedCategory = ExpenseManager.predictCategory(context, predictorText, isCreditState)
            }
        }

        Surface(
            modifier = Modifier.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).border(1.dp, FinPulseEmerald.copy(alpha = 0.2f), RoundedCornerShape(32.dp)),
            color = FinPulseSurface.copy(alpha = 0.98f), tonalElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(FinPulseBackground).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(false to "SPEND", true to "INCOME").forEach { (type, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCreditState == type) (if(type) FinPulseEmerald else Color(0xFFFF4D4D)) else Color.Transparent)
                                    .clickable { isCreditState = type }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = if (isCreditState == type) FinPulseBackground else Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.4f)) }
                }
                
                if (rawText.isNotEmpty()) {
                    Text(
                        text = "Detected: \"${rawText.replace("\n", " ").trim()}\"", 
                        color = Color.White.copy(alpha = 0.3f), 
                        fontSize = 9.sp, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = { Text("Amount", fontSize = 12.sp) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)), 
                    prefix = { Text("₹ ", color = Color.White) },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = party, 
                    onValueChange = { party = it }, 
                    label = { Text(if (upiId != null) "Party Name (UPI: $upiId)" else "Party Name", fontSize = 12.sp) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                    singleLine = true,
                    supportingText = if (upiId != null && party.uppercase() != upiId.uppercase()) {
                        { Text("AI will remember: $upiId = $party", fontSize = 9.sp, color = FinPulseEmerald.copy(alpha = 0.7f)) }
                    } else null
                )

                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it; isManualDescription = true }, 
                    label = { Text("Description", fontSize = 12.sp) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    trailingIcon = { Icon(Icons.Filled.Mic, null, tint = FinPulseEmerald, modifier = Modifier.size(20.dp)) }, 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = FinPulseEmerald, unfocusedBorderColor = Color.White.copy(alpha = 0.1f)),
                    singleLine = true
                )
                
                Row(modifier = Modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(10.dp)).background(FinPulseBackground)) {
                    listOf("Digital", "Cash").forEach { m ->
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .background(if (selectedMethod == m) FinPulseEmerald else Color.Transparent)
                                .clickable { selectedMethod = m }, 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = m, color = if (selectedMethod == m) FinPulseBackground else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat, 
                            onClick = { selectedCategory = cat; isManualCategory = true }, 
                            label = { Text(cat, fontSize = 10.sp) }, 
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinPulseEmerald.copy(alpha = 0.15f), selectedLabelColor = FinPulseEmerald), 
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedCategory == cat, borderColor = Color.White.copy(alpha = 0.05f), selectedBorderColor = FinPulseEmerald.copy(alpha = 0.5f))
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onDismiss(); removePopup(); removeBubbleHead(); stopSelf() }, 
                        modifier = Modifier.weight(1f).height(48.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f), contentColor = Color.White.copy(alpha = 0.7f)), 
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Ignore", fontSize = 13.sp) }
                    
                    Button(
                        onClick = { onConfirm(amount.toFloatOrNull() ?: 0f, party, description, selectedCategory, selectedMethod, isCreditState) }, 
                        modifier = Modifier.weight(1f).height(48.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = FinPulseEmerald, contentColor = FinPulseBackground), 
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    private fun removeBubbleHead() { 
        headLifecycleOwner?.onDestroy()
        headLifecycleOwner = null
        if (bubbleHead != null) { 
            try { 
                windowManager?.removeView(bubbleHead)
                bubbleHead = null 
            } catch (e: Exception) {
                Log.e(TAG, "Error removing bubble head", e)
            } 
        } 
    }
    
    private fun removePopup() { 
        popupLifecycleOwner?.onDestroy()
        popupLifecycleOwner = null
        if (popupView != null) { 
            try { 
                windowManager?.removeView(popupView)
                popupView = null 
            } catch (e: Exception) {
                Log.e(TAG, "Error removing popup view", e)
            } 
        } 
    }
    
    private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("fin_pulse_channel", "Fin-Pulse Tracker", NotificationManager.IMPORTANCE_LOW)) }

    override fun onDestroy() {
        removePopup()
        removeBubbleHead()
        super.onDestroy()
    }
}
