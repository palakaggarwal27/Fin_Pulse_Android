# Bubble Notification Debugging Guide

## Quick Test Steps

### 1. Build and Install the App
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. Enable All Permissions
Open the app and you'll see a permission dialog. Enable:
- ✅ **SMS Access** - Tap "Enable" → Grant SMS permissions
- ✅ **Notification Access** - Tap "Enable" → Find "Fin-Pulse" and toggle it ON
- ✅ **Display Over Apps** - Tap "Enable" → Allow display over other apps

**IMPORTANT**: After enabling Notification Access, you MUST toggle it OFF and then ON again for the service to start!

### 3. Test the Bubble Manually
1. Open the app
2. On the Dashboard, tap the **"Test Bubble (Debug)"** button (orange button)
3. You should see:
   - A toast message: "Starting test bubble..."
   - A notification: "Transaction Detected"
   - A bubble overlay showing: ₹500 to "Test Merchant"

If the bubble appears → Permissions are working!
If the bubble doesn't appear → Check Logcat (see below)

---

## Debugging with Logcat

### Enable Logcat Filtering
In Android Studio, open Logcat and filter by: `FinPulse`

You'll see logs from different components:

### Expected Log Flow for Manual Test:
```
FinPulse-Test: Test bubble button clicked
FinPulse-Test: Test bubble service started successfully
FinPulse-Bubble: BubbleService started
FinPulse-Bubble: Service started in foreground
FinPulse-Bubble: Transaction details - Amount: 500.0, Party: Test Merchant...
FinPulse-Bubble: Attempting to show bubble overlay
FinPulse-Bubble: WindowManager obtained
FinPulse-Bubble: Window parameters configured
FinPulse-Bubble: Bubble overlay displayed successfully!
```

### If You See This Error:
```
FinPulse-Bubble: Overlay permission not granted! Cannot show bubble.
```
**Solution**: Go to Settings → Apps → Fin-Pulse → Display over other apps → Enable

### If You See This Error:
```
FinPulse-Bubble: Failed to add view to WindowManager
android.view.WindowManager$BadTokenException: Unable to add window...
```
**Solution**: The overlay permission might not be properly granted. Try:
1. Disable "Display over other apps"
2. Close the app completely
3. Enable "Display over other apps" again
4. Open the app and test again

---

## Testing Real Notifications

### SMS Testing:
1. Send yourself a test SMS with payment format:
   ```
   Paid Rs.500 to merchant@upi via UPI
   ```

2. Check Logcat:
   ```
   FinPulse-SMS: SMS broadcast received
   FinPulse-SMS: Received 1 SMS message(s)
   FinPulse-SMS: SMS from +1234567890: Paid Rs.500 to merchant@upi via UPI
   FinPulse-Parser: Parsing text: Paid Rs.500 to merchant@upi via UPI
   FinPulse-Parser: Amount found: 500.0
   FinPulse-Parser: Payment method: UPI
   FinPulse-Parser: Successfully parsed transaction...
   FinPulse-SMS: Transaction found in SMS! Starting BubbleService...
   FinPulse-Bubble: BubbleService started
   FinPulse-Bubble: Bubble overlay displayed successfully!
   ```

### App Notification Testing (PhonePe, TrueCaller, etc.):

1. Make a real payment via PhonePe or another payment app

2. Check if Notification Listener is running:
   ```
   FinPulse-Notification: Notification Listener Service connected!
   ```

3. When notification arrives:
   ```
   FinPulse-Notification: Notification received from: com.phonepe.app
   FinPulse-Notification: Notification text: Payment of Rs.500 to Merchant...
   FinPulse-Parser: Parsing text: Payment of Rs.500 to Merchant...
   FinPulse-Parser: Amount found: 500.0
   FinPulse-Notification: Transaction parsed! Amount: 500.0, Party: MERCHANT...
   FinPulse-Notification: Starting BubbleService as foreground service
   FinPulse-Bubble: BubbleService started
   FinPulse-Bubble: Bubble overlay displayed successfully!
   ```

---

## Common Issues & Solutions

### Issue 1: "Notification Listener Service not detected"
**Symptoms**: Permission dialog shows warning about service not running

**Solutions**:
1. Go to Settings → Apps → Special app access → Notification access
2. Find "Fin-Pulse" and toggle it OFF
3. Toggle it back ON
4. Return to app and tap "Recheck"
5. Restart your phone if issue persists

### Issue 2: No logs from "FinPulse-Notification"
**Symptoms**: No notification logs appear when you receive payments

**Solutions**:
1. Verify Notification Access is enabled (Settings → Notification access)
2. Toggle the permission off and on
3. Check if other apps (like TrueCaller) have permission - this confirms the setting works
4. Restart the app

### Issue 3: Parser returns null (no transaction found)
**Symptoms**: `FinPulse-Parser: No amount found in text`

**Solutions**:
1. Check the notification text in logs
2. Ensure it contains amount in formats: `Rs.500`, `₹500`, or `INR 500`
3. If format is different, update TransactionParser.kt regex patterns
4. Examples of supported formats:
   - "Paid Rs.500 to merchant@upi"
   - "Spent ₹500 at Starbucks"
   - "Debited INR 500 from account"
   - "Received Rs.1000 from John"

### Issue 4: Bubble appears but disappears immediately
**Symptoms**: Logs show "Bubble overlay displayed successfully!" but you don't see it

**Solutions**:
1. This was fixed by changing window flags to `FLAG_NOT_TOUCH_MODAL`
2. Ensure you're on the latest code
3. Check if any battery optimization is killing the foreground service:
   - Settings → Battery → Battery optimization
   - Find "Fin-Pulse" → Don't optimize

### Issue 5: Foreground service notification shows but no bubble
**Symptoms**: You see "Transaction Detected" notification but no bubble overlay

**Solutions**:
1. Check Logcat for overlay permission errors
2. Verify "Display over other apps" is enabled
3. Try the manual "Test Bubble" button first
4. Check if other apps can display overlays (confirms system permission works)

---

## Log Categories Reference

| Log Tag | Component | What It Tracks |
|---------|-----------|----------------|
| `FinPulse-SMS` | SmsReceiver | SMS message reception and parsing |
| `FinPulse-Notification` | NotificationService | App notification interception |
| `FinPulse-Parser` | TransactionParser | Transaction text parsing logic |
| `FinPulse-Bubble` | BubbleService | Bubble overlay display and lifecycle |
| `FinPulse-Test` | MainActivity Test Button | Manual bubble testing |
| `FinPulse` | MainActivity | Permission checking and general app logs |

---

## Step-by-Step Debug Session

1. **Open Android Studio Logcat**
2. **Set filter to**: `FinPulse`
3. **Clear logs** (click 🗑️ icon)
4. **Open the app** - should see permission checks
5. **Tap "Test Bubble"** button
6. **Watch the logs flow**:
   ```
   ✅ Test button clicked
   ✅ Service started
   ✅ Foreground notification posted
   ✅ Bubble overlay displayed
   ```
7. **If any step fails**, scroll up to find the error
8. **Look for red error logs** or exceptions

---

## Notification Format Examples

### Supported Formats:

#### PhonePe:
```
"Paid ₹500 to Merchant via UPI"
"Received ₹1000 from John Doe"
```

#### Banking Apps:
```
"Debit: Rs.500 debited from your account"
"Credit: INR 2000 credited to A/c"
```

#### TrueCaller:
```
"Spent on: Starbucks - ₹250"
"Received from: John - ₹5000"
```

#### Generic SMS:
```
"Your UPI payment of Rs.500 to merchant@upi is successful"
"Received Rs.1500 from +919876543210"
```

---

## Testing Checklist

- [ ] All 3 permissions enabled (SMS, Notification Access, Display Over Apps)
- [ ] Notification Listener Service shows as "running" in permission dialog
- [ ] "Test Bubble" button shows the bubble successfully
- [ ] Test SMS with "Rs.500" triggers bubble
- [ ] Real payment notification triggers bubble
- [ ] Bubble stays visible until dismissed/confirmed
- [ ] Confirming transaction saves it to expense list
- [ ] Logs show no errors in Logcat

---

## Advanced: Add Custom Notification Formats

If your payment app uses a different format, edit `TransactionParser.kt`:

```kotlin
// Add new amount regex pattern
private val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹|paid|spent)\\s*([\\d,]+\\.?\\d*)")

// Add new merchant extraction keyword
val keywords = listOf("paid to ", "sent to ", "spent at ", "YOUR_NEW_KEYWORD ")
```

Test with Logcat:
```
FinPulse-Parser: Parsing text: YOUR_NOTIFICATION_TEXT_HERE
FinPulse-Parser: Amount found: XXX
```

---

## Still Not Working?

1. **Share Logcat output** - Export logs and share for analysis
2. **Check Android version** - Some features require Android 8.0+
3. **Check if running in emulator** - Try on real device
4. **Clear app data** - Settings → Apps → Fin-Pulse → Storage → Clear data
5. **Reinstall app** - Uninstall completely and reinstall

---

## Success Indicators

✅ All permissions granted (green checkmarks)
✅ "Notification Listener Service is running ✓" in dialog
✅ Test bubble appears when button clicked
✅ Logs show "Bubble overlay displayed successfully!"
✅ Real payment notifications trigger bubble
✅ Bubble stays visible until user action

If all these work, your setup is complete! 🎉
